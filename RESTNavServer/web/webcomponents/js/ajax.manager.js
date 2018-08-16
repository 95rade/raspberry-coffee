/*
 * @author Olivier Le Diouris
 * Uses jQuery Deferreds
 */
function initAjax() {
	let interval = setInterval(function () {
		fetch();
	}, 1000);
}

const DEFAULT_TIMEOUT = 60000;

function getDeferred(
		url,                          // full api path
		timeout,                      // After that, fail.
		verb,                         // GET, PUT, DELETE, POST, etc
		happyCode,                    // if met, resolve, otherwise fail.
		data,                         // payload, when needed (PUT, POST...)
		show) {                       // Show the traffic [true]|false
	if (show === undefined) {
		show = true;
	}
	if (show === true) {
		document.body.style.cursor = 'wait';
	}
	let deferred = $.Deferred();  // a jQuery deferred
	let	xhr = new XMLHttpRequest();
	let TIMEOUT = timeout;

	let req = verb + " " + url;
	if (data !== undefined && data !== null) {
		req += ("\n" + JSON.stringify(data, null, 2));
	}

	xhr.open(verb, url, true);
	xhr.setRequestHeader("Content-type", "application/json");
	if (data === undefined) {
		xhr.send();
	} else {
		xhr.send(JSON.stringify(data));
	}

	let requestTimer = setTimeout(function() {
		xhr.abort();
		let mess = { message: 'Timeout' };
		deferred.reject(408, mess);
	}, TIMEOUT);

	xhr.onload = function() {
		clearTimeout(requestTimer);
		if (xhr.status === happyCode) {
			deferred.resolve(xhr.response);
		} else {
			deferred.reject(xhr.status, xhr.response);
		}
	};
	return deferred.promise();
}

function getNMEAData() {
	return getDeferred('/mux/cache', DEFAULT_TIMEOUT, 'GET', 200, null, false);
}

function fetch() {
	let getData = getNMEAData();
	getData.done(function (value) {
//      console.log("Done:", value);
		try {
			let json = JSON.parse(value);
			onMessage(json);
		} catch (err) {
			console.log("Error:", err, ("\nfor value [" + value + "]"));
		}
	});
	getData.fail(function (error, errmess) {
		let message ;
		if (errmess !== undefined) {
			if (typeof(errmess) === "string") {
				message = errmess;
			} else {
				let mess = JSON.parse(errmess);
				if (mess.message !== undefined) {
					message = mess.message;
				}
			}
		}
		alert("Failed to get nmea data..." + (error !== undefined ? error : ' - ') + ', ' + (message !== undefined ? message : ' - '));
	});
}

function getSkyGP(when, position, wandering, stars) {
	let url = "/astro/positions-in-the-sky";
	// Add date
	url += ("?at=" + when);
	url += ("&fromL=" + position.lat);
	url += ("&fromG=" + position.lng);
	// Wandering bodies
	if (wandering !== undefined && wandering === true) { // to minimize the size of the payload
		url += ("&wandering=true");
	}
	// Stars
	if (stars !== undefined && stars === true) { // to minimize the size of the payload
		url += ("&stars=true");
	}
	return getDeferred(url, DEFAULT_TIMEOUT, 'GET', 200, null, false);
}

function getAstroData(when, position, wandering, stars, callback) {
	let getData = getSkyGP(when, position, wandering, stars);
	getData.done(function(value) {
		let json = JSON.parse(value);
		if (callback !== undefined) {
			callback(json);
		} else {
			console.log(JSON.stringify(json, null, 2));
		}
	});
	getData.fail(function(error, errmess) {
		let message;
		if (errmess !== undefined) {
			if (errmess.message !== undefined) {
				message = errmess.message;
			} else {
				message = errmess;
			}
		}
		errManager("Failed to get the Astro Data..." + (error !== undefined ? error : ' - ') + ', ' + (message !== undefined ? message : ' - '));
	});
}

function getQueryParameterByName(name, url) {
	if (!url) url = window.location.href;
	name = name.replace(/[\[\]]/g, "\\$&");
	let regex = new RegExp("[?&]" + name + "(=([^&#]*)|&|#|$)"),
			results = regex.exec(url);
	if (!results) return null;
	if (!results[2]) return '';
	return decodeURIComponent(results[2].replace(/\+/g, " "));
}

// Takes care of re-broadcasting the data to whoever subscribed to it.
function onMessage(json) {
	try {
		let errMess = "";

		try {
			let latitude = json.Position.lat;
//          console.log("latitude:" + latitude)
			let longitude = json.Position.lng;
//          console.log("Pt:" + latitude + ", " + longitude);
			events.publish('pos', {
				'lat': latitude,
				'lng': longitude
			});
		} catch (err) {
			errMess += ((errMess.length > 0 ? ", " : "Cannot read ") + "position");
		}
		// Displays
		try {
			let bsp = json.BSP.speed;
			events.publish('bsp', bsp);
		} catch (err) {
			errMess += ((errMess.length > 0 ? ", " : "Cannot read ") + "boat speed");
		}
		try {
			let log = json.Log.distance;
			events.publish('log', log);
		} catch (err) {
			errMess += ((errMess.length > 0 ? ", " : "Cannot read ") + "log (" + err + ")");
		}
		try {
			let gdt = json["GPS Date & Time"];
			let gpsDate = new Date(gdt.fmtDate.year, gdt.fmtDate.month - 1, gdt.fmtDate.day, gdt.fmtDate.hour, gdt.fmtDate.min, gdt.fmtDate.sec, 0);
			// UTC dates
			events.publish('gps-time', gpsDate);
		} catch (err) {
			errMess += ((errMess.length > 0 ? ", " : "Cannot read ") + "GPS Date (" + err + ")");
		}
		try {
			let gpsSat = json["Satellites in view"];
			if (gpsSat !== undefined) {
				events.publish('gps-sat', gpsSat);
			}
		} catch (err) {
			errMess += ((errMess.length > 0 ? ", " : "Cannot read ") + "GPS Satellites data (" + err + ")");
		}

		try {
			let hdg = json["HDG true"].angle;
			events.publish('hdg', hdg);
		} catch (err) {
			errMess += ((errMess.length > 0 ? ", " : "Cannot read ") + "heading");
		}
		try {
			let twd = json.TWD.angle;
			events.publish('twd', twd);
		} catch (err) {
			errMess += ((errMess.length > 0 ? ", " : "Cannot read ") + "TWD");
		}
		try {
			let twa = json.TWA.angle;
			events.publish('twa', twa);
		} catch (err) {
			errMess += ((errMess.length > 0 ? ", " : "Cannot read ") + "TWA");
		}
		try {
			let tws = json.TWS.speed;
			events.publish('tws', tws);
		} catch (err) {
			errMess += ((errMess.length > 0 ? ", " : "Cannot read ") + "TWS");
		}

		try {
			let waterTemp = json["Water Temperature"].temperature;
			events.publish('wt', waterTemp);
		} catch (err) {
			errMess += ((errMess.length > 0 ? ", " : "Cannot read ") + "water temperature");
		}

		try {
			let airTemp = json["Air Temperature"].temperature;
			events.publish('at', airTemp);
		} catch (err) {
			errMess += ((errMess.length > 0 ? ", " : "Cannot read ") + "air temperature");
		}
		// Battery_Voltage, Relative_Humidity, Barometric_Pressure
		try {
			let baro = json["Barometric Pressure"].pressure;
			if (baro != 0) {
				events.publish('prmsl', baro);
			}
		} catch (err) {
			errMess += ((errMess.length > 0 ? ", " : "Cannot read ") + "PRMSL");
		}
		try {
			let hum = json["Relative Humidity"];
			if (hum > 0) {
				events.publish('hum', hum);
			}
		} catch (err) {
			errMess += ((errMess.length > 0 ? ", " : "Cannot read ") + "Relative_Humidity");
		}
		try {
			let aws = json.AWS.speed;
			events.publish('aws', aws);
		} catch (err) {
			errMess += ((errMess.length > 0 ? ", " : "Cannot read ") + "AWS");
		}
		try {
			let awa = json.AWA.angle;
			events.publish('awa', awa);
		} catch (err) {
			errMess += ((errMess.length > 0 ? ", " : "Cannot read ") + "AWA");
		}
		try {
			let cdr = json.CDR.angle;
			events.publish('cdr', cdr);
		} catch (err) {
			errMess += ((errMess.length > 0 ? ", " : "Cannot read ") + "CDR");
		}

		try {
			let cog = json.COG.angle;
			events.publish('cog', cog);
		} catch (err) {
			errMess += ((errMess.length > 0 ? ", " : "Cannot read ") + "COG");
		}
		try {
			let cmg = json.CMG.angle;
			events.publish('cmg', cmg);
		} catch (err) {
			errMess += ((errMess.length > 0 ? ", " : "Cannot read ") + "CMG");
		}
		try {
			let leeway = json.Leeway.angle;
			events.publish('leeway', leeway);
		} catch (err) {
			errMess += ((errMess.length > 0 ? ", " : "Cannot read ") + "Leeway");
		}
		try {
			let csp = json.CSP.speed;
			events.publish('csp', csp);
		} catch (err) {
			errMess += ((errMess.length > 0 ? ", " : "Cannot read ") + "CSP");
		}

		// Buffered current
		try {
			let buffered = json['Current calculated with damping'];
			if (buffered !== undefined) {
				let keys = Object.keys(buffered);
				for (i=0; i<keys.length; i++) {
					let k = keys[i];
//				console.log("K:" + k);
					let damp = buffered[k];
//				console.log("Publishing csp-" + k);
					events.publish("csp-" + k, damp.speed.speed);
					events.publish("cdr-" + k, damp.direction.angle);
				}
			}
		} catch (err) {
			console.log(err);
		}

		try {
			let sog = json.SOG.speed;
			events.publish('sog', sog);
		} catch (err) {
			errMess += ((errMess.length > 0 ? ", " : "Cannot read ") + "SOG");
		}
		// to-wp, vmg-wind, vmg-wp, b2wp
		try {
			let to_wp = json["To Waypoint"];
			let b2wp = json["Bearing to WP"].angle;
			events.publish('wp', {
				'to_wp': to_wp,
				'b2wp': b2wp
			});
		} catch (err) {
		}

		try {
			events.publish('vmg', {
				'onwind': json["VMG on Wind"],
				'onwp': json["VMG to Waypoint"]
			});

		} catch (err) {
			errMess += ((errMess.length > 0 ? ", " : "Cannot read ") + "VMG");
		}

		if (errMess !== undefined) {
			// displayErr(errMess); // Absorb
		}
	} catch (err) {
		displayErr(err);
	}
}

"use strict";

/*
 * @author Olivier LeDiouris
 * Uses ES6 Promises for Ajax.
 *
 * Promises interacting with the Relay service.
 */

const DEBUG = false;
const DEFAULT_TIMEOUT = 60000; // 1 minute

/* global events */

/* Uses ES6 Promises */
function getPromise(
		url,                          // full api path
		timeout,                      // After that, fail.
		verb,                         // GET, PUT, DELETE, POST, etc
		happyCode,                    // if met, resolve, otherwise fail.
		data = null,                  // payload, when needed (PUT, POST...)
		show = false) {               // Show the traffic [true]|false

	if (show === true) {
		document.body.style.cursor = 'wait';
	}

	if (DEBUG) {
		console.log(">>> Promise", verb, url);
	}

	let promise = new Promise(function (resolve, reject) {
		let xhr = new XMLHttpRequest();
		let TIMEOUT = timeout;

		let req = verb + " " + url;
		if (data !== undefined && data !== null) {
			req += ("\n" + JSON.stringify(data, null, 2));
		}

		xhr.open(verb, url, true);
		xhr.setRequestHeader("Content-type", "application/json");
		try {
			if (data === undefined || data === null) {
				xhr.send();
			} else {
				xhr.send(JSON.stringify(data));
			}
		} catch (err) {
			console.log("Send Error ", err);
		}

		let requestTimer = setTimeout(function () {
			xhr.abort();
			let mess = { code: 408, message: 'Timeout' };
			reject(mess);
		}, TIMEOUT);

		xhr.onload = function () {
			clearTimeout(requestTimer);
			if (xhr.status === happyCode) {
				resolve(xhr.response);
			} else {
				reject({ code: xhr.status, message: xhr.response });
			}
		};
	});
	return promise;
}

function getRelayStatus(relayNum) {
	return getPromise('/relay/status/' + relayNum, DEFAULT_TIMEOUT, 'GET', 200);
}

function getStatus(relay, callback) {
	let getData = getRelayStatus(relay);
	getData.then(function (value) { // Resolve
//  console.log("Done:", value);
		try {
			let json = JSON.parse(value);
			if (callback !== undefined) {
				callback(json);
			} else {
				console.info('Status', relay, json);
			}
		} catch (err) {
			console.log("Error:", err, ("\nfor value [" + value + "]"));
		}
	}, function (error) { // Reject
		console.log("Failed to get Status..." + (error !== undefined && error.code !== undefined ? error.code : ' - ') + ', ' + (error !== undefined && error.message !== undefined ? error.message : ' - '));
	});
}

function setRelayStatus(relay, payload) {
	let url = "/relay/status/" + relay;
	let obj = payload; // JSON.stringify(payload);
	return getPromise(url, DEFAULT_TIMEOUT, 'POST', 200, obj);
}

function setRelay(relay, checkbox, callback) {
	let setData = setRelayStatus(relay, { status: checkbox.checked });
	setData.then(function (value) { // resolve
		if (value !== undefined && value !== null && value.length > 0) {
			let json = JSON.parse(value);
			if (callback !== undefined) {
				callback(json);
			} else {
				console.log('Relay status set', JSON.stringify(json, null, 2));
			}
		}
	}, function (error) { // reject
		console.log("Failed to set the Relay status.." + (error !== undefined && error.code !== undefined ? error.code : ' - ') + ', ' + (error !== undefined && error.message !== undefined ? error.message : ' - '));
	});
}

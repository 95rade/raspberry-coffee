package main;

import com.pi4j.io.gpio.PinState;
import http.HTTPServer;
import loggers.DataLoggerInterface;
import loggers.LogData;
import org.fusesource.jansi.AnsiConsole;
import relay.RelayDriver;
import sensors.sparkfunsoilhumiditysensor.MCP3008Wrapper;
import utils.PinUtil;
import utils.StaticUtil;
import utils.StringUtils;
import utils.WeatherUtil;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import static utils.TimeUtil.fmtDHMS;
import static utils.TimeUtil.msToHMS;

/**
 * Example / Working prototype...
 * MCP3008, ADC for  SparkFun Soil Moisture Sensor. No temperature sensor.
 */
public class MCP3008 implements Probe {

	private final static Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
	static {
		LOGGER.setLevel(Level.INFO);
	}

	private static boolean go = true; // Keep looping.

	private final static int DEFAULT_HUMIDITY_THRESHOLD = 50; // 50 %
	private final static long DEFAULT_WATERING_DURATION = 10L; // 10 seconds
	private final static long DEFAULT_RESUME_SENSOR_WATCH_AFTER = 120L; // 120 seconds, 2 minutes

	private final static long DEFAULT_LOGGING_PACE = 10_000L; // 10 seconds

	// Default values
	private static int humidityThreshold = DEFAULT_HUMIDITY_THRESHOLD;
	private static long wateringDuration = DEFAULT_WATERING_DURATION;
	private static long resumeSensorWatchAfter = DEFAULT_RESUME_SENSOR_WATCH_AFTER;

	private static boolean withRESTServer = false;
	private static int restServerPort = 9999; // This is the default value
	private static boolean enforceSensorSimulation = false;

	private static long loggingPace = DEFAULT_LOGGING_PACE;

	private static Long lastWatering = null;

	// Program arguments
	private enum ARGUMENTS {
		HUMIDITY_THRESHOLD("--water-below:", // %
				String.format("Integer. Humidity threshold in %%, default is --water-below:%d, start watering below this value.", DEFAULT_HUMIDITY_THRESHOLD)),
		WATERING_DURATION("--water-during:", // seconds
				String.format("Integer. In seconds, default is --water-during:%d. Duration of the watering process.", DEFAULT_WATERING_DURATION)),
		RESUME_AFTER("--resume-after:", // seconds
				String.format("Integer. In seconds, default is --resume-after:%d. After watering, resume sensor monitoring after this amount of time.", DEFAULT_RESUME_SENSOR_WATCH_AFTER)),
		VERBOSE("--verbose:",
				"String. Verbose, default is --verbose:NONE, values can be 'NONE', 'STDOUT' or 'ANSI'."),

		// MCP3008: MISO, MOSI, CLK, CS, Channel
		MISO_PIN("--miso-pin:",
				"Integer. BCM (aka GPIO) pin number of the MISO pin of the MCP3008. Default is --miso-pin:13."),
		MOSI_PIN("--mosi-pin:",
				"Integer. BCM (aka GPIO) pin number of the MOSI pin of the MCP3008. Default is --mosi-pin:12."),
		CLK_PIN("--clk-pin:",
				"Integer. BCM (aka GPIO) pin number of the CLK pin of the MCP3008. Default is --clk-pin:14."),
		CS_PIN("--cs-pin:",
				"Integer. BCM (aka GPIO) pin number of the CS pin of the MCP3008. Default is --cs-pin:10."),
		CHANNEL_PIN("--adc-channel-pin:",
				"Integer. BCM (aka GPIO) pin number of the Channel (0-7) pin of the MCP3008. Default is --adc-channel-pin:0."),

		// Pump or valve relay
		RELAY_PIN("--relay-pin:",  // default is BCM 17 => GPIO_00
				"Integer. BCM (aka GPIO) pin number of the SIGNAL pin of the RELAY. Default is --relay-pin:17."),

		WITH_REST_SERVER("--with-rest-server:",
				"Boolean. Default 'false', starts a REST server is set to 'true'"),
		HTTP_PORT("--http-port:",
				String.format("Integer. The HTTP port of the REST Server. Default is %d.", restServerPort)),
		SIMULATE_SENSOR_VALUES("--simulate-sensor-values:",
				"Boolean. Enforce sensor values simulation, even if running on a Raspberry Pi. Default is 'false'. Note: Relay is left alone."),
		LOGGERS("--loggers:",
				"Comma-separated list of the loggers. Loggers must implement DataLoggerInterface. Ex: --loggers:loggers.iot.AdafruitIOClient,loggers.text.FileLogger "),
		LOGGING_PACE("--logging-pace:",
				String.format("Long, in milliseconds. The interval between each log entry. Default is %d.", DEFAULT_LOGGING_PACE)),
		HELP("--help", "Display the help and exit.");

		private String prefix, help;

		ARGUMENTS(String prefix, String help) {
			this.prefix = prefix;
			this.help = help;
		}

		public String prefix() {
			return this.prefix;
		}
		public String help() {
			return this.help;
		}
	}

	enum VERBOSE {
		NONE,
		STDOUT,
		ANSI
	}
	private static VERBOSE verbose = VERBOSE.NONE;

	private static MCP3008Wrapper probe = null;
	private static RelayDriver relay = null;

	// Simulators, to run on non-Raspberry Pis - for development and tests.
	// User manual entry (also suitable for REST)
	private static Supplier<Double> temperatureSimulator = MCP3008::simulateUserTemp; // Not used with MCP3008
	private static Supplier<Double> humiditySimulator = MCP3008::simulateUserHum;
	// Random values
//	private static Supplier<Double> temperatureSimulator = MCP3008::simulateTemp;
//	private static Supplier<Double> humiditySimulator = MCP3008::simulateHum;

	private static double temperature = 20d;
	private static double humidity = 50d;
	private static String message = "";

	private static double minSimTemp = temperature, maxSimTemp = temperature;
	private static double minSimHum = humidity, maxSimHum = humidity;

	private static PinState simulatedPinState = PinState.HIGH;
	private static PinState actualPinState = PinState.HIGH;
	private static Supplier<PinState> relaySignalSimulator = () -> simulatedPinState;
	private static Consumer<PinState> relayObserver = state -> {
		System.out.println(">> Relay is now " + state);
		simulatedPinState = state;
	};
	private static Consumer<PinState> relayListener = state -> actualPinState = state;

	private static HTTPServer httpServer = null;

	// Loggers
	private static List<DataLoggerInterface> loggers = new ArrayList<>(); //Arrays.asList(new AdafruitIOClient()); // Example
	private static long lastLog = -1;

	public static Logger getLogger() {
		return LOGGER;
	}

	// Sensor data Getters and Setters, for (optional) REST
	@Override
	public void setTemperature(double temp) {
		temperature = temp;
	}
	@Override
	public void setHumidity(double hum) {
		humidity = hum;
	}

	@Override
	public double getTemperature() {
		return temperature;
	}
	@Override
	public  double getHumidity() {
		return humidity;
	}

	@Override
	public PinState getRelayState() {
		PinState state = null;
		if (relay != null) {
			state = relay.getState();
		}
		return state;
	}

	@Override
	public void setRelayState(PinState state) {
		if (relay != null) {
			Thread relayFlip = null;
			if (state.isHigh()) {
				relayFlip = new Thread(() -> {
					synchronized (relay) {
						relay.off();
					}
				});
			} else {
				relayFlip = new Thread(() -> {
					synchronized (relay) {
						relay.on();
					}
				});
			}
			relayFlip.start();
		}
	}

	@Override
	public Long getLastWateringTime() {
		return lastWatering;
	}

	final static double ALPHA = 0.015;
	static double lowPassFilter(double alpha, double value, double acc) {
		return (value * alpha) + (acc * (1 - alpha));
	}

	@Override
	public PWSParameters getPWSParameters() {
		return new PWSParameters()
				.humidityThreshold(humidityThreshold)
				.wateringTime(wateringDuration)
				.resumeWatchAfter(resumeSensorWatchAfter);
	}

	@Override
	public void setPWSParameters(PWSParameters pwsParameters) {
		if (pwsParameters.humidityThreshold() != -1) {
			humidityThreshold = pwsParameters.humidityThreshold();
		}
		if (pwsParameters.wateringTime() != -1) {
			wateringDuration = pwsParameters.wateringTime();
		}
		if (pwsParameters.resumeWatchAfter() != -1) {
			resumeSensorWatchAfter = pwsParameters.resumeWatchAfter();
		}
	}

	@Override
	public String getStatus() {
		return message;
	}

	private static double randomDiff() {
		int sign = (int)System.currentTimeMillis() % 2;
		return Math.random() * (sign == 0 ? 1 : -1);
	}

	private static Double simulateTemp() {
		temperature += randomDiff();
		minSimTemp = Math.min(minSimTemp, temperature);
		maxSimTemp = Math.max(maxSimTemp, temperature);
		return temperature;
	}

	private static Double simulateHum() {
		humidity += randomDiff();
		minSimHum = Math.min(minSimHum, humidity);
		maxSimHum = Math.max(maxSimHum, humidity);
		return humidity;
	}

	private static void displayANSIConsole() {
		ANSIUtil.displayAnsiData(
				humidityThreshold,
				wateringDuration,
				resumeSensorWatchAfter,
				temperature,
				humidity,
				message,
				withRESTServer,
				restServerPort,
				lastWatering,
				(relay != null && relay.isSimulating() ? simulatedPinState : actualPinState));
	}
	// Interactive simulators, for dev and tests.
	private static Double simulateUserTemp() {
		return temperature;
	}
	private static Double simulateUserHum() {
		return humidity;
	}

	// Parse manual user input, for simulation
	private static void parseUserInput(String str) {
		// Input can be T:XX or H:xx
		if (str.startsWith("T:")) {
			try {
				temperature = Double.parseDouble(str.substring("T:".length()));
			} catch (NumberFormatException nfe) {
				nfe.printStackTrace();
			}
		} else if (str.startsWith("H:")) {
			try {
				humidity = Double.parseDouble(str.substring("H:".length()));
			} catch (NumberFormatException nfe) {
				nfe.printStackTrace();
			}
		}
	}

	private static boolean watchTheProbe = true;

	public static void main(String... args) {

		// Defaults --miso:23 --mosi:24 --clk:18 --cs:25 --channel:0
		int misoPin = 23,
				mosiPin = 24,
				clkPin = 18,
				csPin = 25,
				adcChannel = 0,
				relayPin = 17;

		// Parameter management.
		// Override values with runtime arguments
		for (String arg : args) {
			if (arg.startsWith(ARGUMENTS.HELP.prefix())) {
				// No value, display help
				System.out.println("+---------------------------------------");
				System.out.println("| Program arguments are:");
				System.out.println("+---------------------------------------");
				Arrays.stream(ARGUMENTS.values()).forEach(argument -> System.out.println("| " + argument.prefix() + "\t" + argument.help()));
				System.out.println("+---------------------------------------");
				System.exit(0);
			} else if (arg.startsWith(ARGUMENTS.VERBOSE.prefix())) {
				String val = arg.substring(ARGUMENTS.VERBOSE.prefix().length());
				verbose = VERBOSE.valueOf(val);
			} else if (arg.startsWith(ARGUMENTS.WITH_REST_SERVER.prefix())) {
				String val = arg.substring(ARGUMENTS.WITH_REST_SERVER.prefix().length());
				withRESTServer = "true".equals(val);
			} else if (arg.startsWith(ARGUMENTS.HTTP_PORT.prefix())) {
				String val = arg.substring(ARGUMENTS.HTTP_PORT.prefix().length());
				try {
					restServerPort = Integer.parseInt(val);
				} catch (NumberFormatException nfe) {
					nfe.printStackTrace();
				}
			} else if (arg.startsWith(ARGUMENTS.SIMULATE_SENSOR_VALUES.prefix())) {
				String val = arg.substring(ARGUMENTS.SIMULATE_SENSOR_VALUES.prefix().length());
				enforceSensorSimulation = "true".equals(val);
			} else if (arg.startsWith(ARGUMENTS.MISO_PIN.prefix())) {
				String val = arg.substring(ARGUMENTS.MISO_PIN.prefix().length());
				try {
					misoPin = Integer.parseInt(val);
				} catch (NumberFormatException nfe) {
					nfe.printStackTrace();
				}
			} else if (arg.startsWith(ARGUMENTS.MOSI_PIN.prefix())) {
				String val = arg.substring(ARGUMENTS.MOSI_PIN.prefix().length());
				try {
					mosiPin = Integer.parseInt(val);
				} catch (NumberFormatException nfe) {
					nfe.printStackTrace();
				}
			} else if (arg.startsWith(ARGUMENTS.CLK_PIN.prefix())) {
				String val = arg.substring(ARGUMENTS.CLK_PIN.prefix().length());
				try {
					clkPin = Integer.parseInt(val);
				} catch (NumberFormatException nfe) {
					nfe.printStackTrace();
				}
			} else if (arg.startsWith(ARGUMENTS.CS_PIN.prefix())) {
				String val = arg.substring(ARGUMENTS.CS_PIN.prefix().length());
				try {
					csPin = Integer.parseInt(val);
				} catch (NumberFormatException nfe) {
					nfe.printStackTrace();
				}
			} else if (arg.startsWith(ARGUMENTS.CHANNEL_PIN.prefix())) {
				String val = arg.substring(ARGUMENTS.CHANNEL_PIN.prefix().length());
				try {
					adcChannel = Integer.parseInt(val);
				} catch (NumberFormatException nfe) {
					nfe.printStackTrace();
				}
			} else if (arg.startsWith(ARGUMENTS.RELAY_PIN.prefix())) {
				String val = arg.substring(ARGUMENTS.RELAY_PIN.prefix().length());
				try {
					relayPin = Integer.parseInt(val);
				} catch (NumberFormatException nfe) {
					nfe.printStackTrace();
				}
			} else if (arg.startsWith(ARGUMENTS.HUMIDITY_THRESHOLD.prefix())) {
				String val = arg.substring(ARGUMENTS.HUMIDITY_THRESHOLD.prefix().length());
				try {
					humidityThreshold = Integer.parseInt(val);
					if (humidityThreshold < 0 || humidityThreshold > 100) {
						humidityThreshold = DEFAULT_HUMIDITY_THRESHOLD;
						System.err.println(String.format(">> Humidity Threshold must be in [0..100]. Reseting to %d ", DEFAULT_HUMIDITY_THRESHOLD));
					}
					humidity = humidityThreshold * 1.2; // Initialize low pass filter
				} catch (NumberFormatException nfe) {
					nfe.printStackTrace();
				}
			} else if (arg.startsWith(ARGUMENTS.WATERING_DURATION.prefix())) {
				String val = arg.substring(ARGUMENTS.WATERING_DURATION.prefix().length());
				try {
					wateringDuration = Long.parseLong(val);
					if (wateringDuration < 0) {
						wateringDuration = DEFAULT_WATERING_DURATION;
						System.err.println(">> Watering duration must be positive. Ignoring.");
					}
				} catch (NumberFormatException nfe) {
					nfe.printStackTrace();
				}
			} else if (arg.startsWith(ARGUMENTS.RESUME_AFTER.prefix())) {
				String val = arg.substring(ARGUMENTS.RESUME_AFTER.prefix().length());
				try {
					resumeSensorWatchAfter = Long.parseLong(val);
					if (resumeSensorWatchAfter < 0) {
						resumeSensorWatchAfter = DEFAULT_RESUME_SENSOR_WATCH_AFTER;
						System.err.println(">> Resume Watch After must be positive. Ignoring.");
					}
				} catch (NumberFormatException nfe) {
					nfe.printStackTrace();
				}
			} else if (arg.startsWith(ARGUMENTS.LOGGERS.prefix())) {
				String val = arg.substring(ARGUMENTS.LOGGERS.prefix().length());
				String[] logConsumers = val.split(",");
				for (String oneLogger : logConsumers) {
					try {
						Class logClass = Class.forName(oneLogger);
						Object consumer = ((Constructor)logClass.getConstructor()).newInstance();
						loggers.add(DataLoggerInterface.class.cast(consumer));
					} catch (Exception ex) {
						ex.printStackTrace();
					}
				}
			} else if (arg.startsWith(ARGUMENTS.LOGGING_PACE.prefix())) {
				String val = arg.substring(ARGUMENTS.LOGGING_PACE.prefix().length());
				try {
					loggingPace = Long.parseLong(val);
					if (loggingPace < 0) {
						loggingPace = DEFAULT_LOGGING_PACE;
						System.err.println(">> Logging Pace must be positive. Ignoring.");
					}
				} catch (NumberFormatException nfe) {
					nfe.printStackTrace();
				}
			}
		}
		if (verbose == VERBOSE.ANSI) {
			AnsiConsole.systemInstall();
			AnsiConsole.out.println(ANSIUtil.ANSI_CLS);
		}
		// Print summary
		if (verbose == VERBOSE.ANSI) {
			displayANSIConsole();
		} else {
			System.out.println("+------- P L A N T   W A T E R I N G   S Y S T E M --------");
			System.out.println(String.format("| Start watering under %d%% of humidity.", humidityThreshold));
			System.out.println(String.format("| Water during %s", fmtDHMS(msToHMS(wateringDuration * 1_000))));
			System.out.println(String.format("| Resume sensor watch %s after watering.", fmtDHMS(msToHMS(resumeSensorWatchAfter * 1_000))));
			if (withRESTServer) {
				System.out.println(String.format("| REST Server running on port %d.", restServerPort));
			}
			System.out.println("+----------------------------------------------------------");
		}

		if (verbose == VERBOSE.STDOUT) {
			System.out.println("Wiring:");
			// Compose mapping for PinUtil, physical numbers.
			String[] map = new String[5];
			map[0] = String.valueOf(PinUtil.findByPin(PinUtil.getPinByGPIONumber(misoPin)).pinNumber()) + ":" + "MISO";
			map[1] = String.valueOf(PinUtil.findByPin(PinUtil.getPinByGPIONumber(mosiPin)).pinNumber()) + ":" + "MOSI";
			map[2] = String.valueOf(PinUtil.findByPin(PinUtil.getPinByGPIONumber(clkPin)).pinNumber()) + ":" + "CLK";
			map[3] = String.valueOf(PinUtil.findByPin(PinUtil.getPinByGPIONumber(csPin)).pinNumber()) + ":" + "CS";

			map[4] = String.valueOf(PinUtil.findByPin(PinUtil.getPinByGPIONumber(relayPin)).pinNumber()) + ":" + "RELAY";
			PinUtil.print(true, map);

			System.out.println(String.format("Reading MCP3008 on channel %d", adcChannel));
			System.out.println(
					" Wiring of the MCP3008-SPI (without power supply):\n" +
							" +---------++-------------------------------------------------+\n" +
							" | MCP3008 || Raspberry Pi                                    |\n" +
							" +---------++------+--------------+------+---------+----------+\n" +
							" |         || Pin# | Name         | Role | GPIO    | wiringPI |\n" +
							" |         ||      |              |      | /BCM    | /PI4J    |\n" +
							" +---------++------+--------------+------+---------+----------+");
			System.out.println(String.format(" | CLK (13)|| #%02d  | %s | CLK  | GPIO_%02d | %02d       |",
					PinUtil.findByPin(PinUtil.getPinByGPIONumber(clkPin)).pinNumber(),
					StringUtils.rpad(PinUtil.findByPin(PinUtil.getPinByGPIONumber(clkPin)).pinName(), 12, " "),
					PinUtil.findByPin(PinUtil.getPinByGPIONumber(clkPin)).gpio(),
					PinUtil.findByPin(PinUtil.getPinByGPIONumber(clkPin)).wiringPi()));
			System.out.println(String.format(" | Din (11)|| #%02d  | %s | MOSI | GPIO_%02d | %02d       |",
					PinUtil.findByPin(PinUtil.getPinByGPIONumber(mosiPin)).pinNumber(),
					StringUtils.rpad(PinUtil.findByPin(PinUtil.getPinByGPIONumber(mosiPin)).pinName(), 12, " "),
					PinUtil.findByPin(PinUtil.getPinByGPIONumber(mosiPin)).gpio(),
					PinUtil.findByPin(PinUtil.getPinByGPIONumber(mosiPin)).wiringPi()));
			System.out.println(String.format(" | Dout(12)|| #%02d  | %s | MISO | GPIO_%02d | %02d       |",
					PinUtil.findByPin(PinUtil.getPinByGPIONumber(misoPin)).pinNumber(),
					StringUtils.rpad(PinUtil.findByPin(PinUtil.getPinByGPIONumber(misoPin)).pinName(), 12, " "),
					PinUtil.findByPin(PinUtil.getPinByGPIONumber(misoPin)).gpio(),
					PinUtil.findByPin(PinUtil.getPinByGPIONumber(misoPin)).wiringPi()));
			System.out.println(String.format(" | CS  (10)|| #%02d  | %s | CS   | GPIO_%02d | %02d       |",
					PinUtil.findByPin(PinUtil.getPinByGPIONumber(csPin)).pinNumber(),
					StringUtils.rpad(PinUtil.findByPin(PinUtil.getPinByGPIONumber(csPin)).pinName(), 12, " "),
					PinUtil.findByPin(PinUtil.getPinByGPIONumber(csPin)).gpio(),
					PinUtil.findByPin(PinUtil.getPinByGPIONumber(csPin)).wiringPi()));
			System.out.println(" +---------++------+--------------+-----+----------+----------+");
			System.out.println("Raspberry Pi is the Master, MCP3008 is the Slave:");
			System.out.println("- Dout on the MCP3008 goes to MISO on the RPi");
			System.out.println("- Din on the MCP3008 goes to MOSI on the RPi");
			System.out.println("Pins on the MCP3008 are numbered from 1 to 16, beginning top left, counter-clockwise.");
			System.out.println("       +--------+ ");
			System.out.println(String.format("%s CH0 -+  1  16 +- Vdd ",  (adcChannel == 0 ? "*" : " ")));
			System.out.println(String.format("%s CH1 -+  2  15 +- Vref ", (adcChannel == 1 ? "*" : " ")));
			System.out.println(String.format("%s CH2 -+  3  14 +- aGnd ", (adcChannel == 2 ? "*" : " ")));
			System.out.println(String.format("%s CH3 -+  4  13 +- CLK ",  (adcChannel == 3 ? "*" : " ")));
			System.out.println(String.format("%s CH4 -+  5  12 +- Dout ", (adcChannel == 4 ? "*" : " ")));
			System.out.println(String.format("%s CH5 -+  6  11 +- Din ",  (adcChannel == 5 ? "*" : " ")));
			System.out.println(String.format("%s CH6 -+  7  10 +- CS ",   (adcChannel == 6 ? "*" : " ")));
			System.out.println(String.format("%s CH7 -+  8   9 +- dGnd ", (adcChannel == 7 ? "*" : " ")));
			System.out.println("       +--------+ ");
		}

		// Initialization. ADC and Pump Relay.
		MCP3008 instance = new MCP3008();
		try {
			probe = MCP3008Wrapper.init(misoPin, mosiPin, clkPin, csPin, adcChannel);
			if (probe.isSimulating() || enforceSensorSimulation) {
				// Provide simulator here
				System.out.println(String.format(">> Will simulate MCP3008%s", (enforceSensorSimulation ? " (enforced)" : "")));
				if ("true".equals(System.getProperty("random.simulator"))) {
//					temperatureSimulator = MCP3008::simulateTemp;
					humiditySimulator = MCP3008::simulateHum;
				} else { // User input
//					temperatureSimulator = MCP3008::simulateUserTemp;
					humiditySimulator = MCP3008::simulateUserHum;
				}
				probe.setSimulators(humiditySimulator);
			}
	  } catch (UnsatisfiedLinkError ule) { // That one is trapped in the constructor of MCP3008Wrapper.
			System.out.println("You're not on a Raspberry Pi, or your wiring is wrong.");
			System.out.println("Exiting.");
			System.exit(1);
		}
		try {
			relay = new RelayDriver(PinUtil.getPinByGPIONumber(relayPin));
			if (relay.isSimulating()) {
				// Provide simulator here
				System.out.println(">> Will simulate Relay");
				relay.setSimulator(relayObserver, relaySignalSimulator);
			} else {
				// Relay listener
				relay.setListener(relayListener);
			}
		} catch (UnsatisfiedLinkError ule) { // That one is trapped in the constructor of RelayDriver.
			System.out.println("You're not on a Raspberry Pi, or your wiring is wrong.");
			System.out.println("Exiting.");
			System.exit(1);
		}

		// Shutdown hook
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			go = false;
			if (relay.getState() == PinState.LOW) {
				relay.off();
			}
			System.out.println("\nExiting (Main Hook)");

			loggers.forEach(DataLoggerInterface::close);

			probe.shutdown();
			relay.shutdownGPIO();
			try { Thread.sleep(1_500L); } catch (InterruptedException ie) {
				Thread.currentThread().interrupt();
			}
		}));

		// If simulating
		if ((probe.isSimulating() || enforceSensorSimulation) && !"true".equals(System.getProperty("random.simulator"))) {
			// Manual input, stdin.
			Thread manualThread = new Thread(() -> { // There is also a REST input
				while (go) {
					String userInput = StaticUtil.userInput(" T:XX, or H:XX > ");
					parseUserInput(userInput);
				}
			}, "manual-input");
			manualThread.start();
		}

		// HTTP Server + REST Request Manager
		if (withRESTServer) {
			httpServer = new RequestManager(instance).startHttpServer(restServerPort);
		}

		// Open/Close valve, for test, once at startup.
		if ("true".equals(System.getProperty("valve.test"))) {
			System.out.println("Testing the valve");
			synchronized (relay) {
				relay.on();
				try {
					Thread.sleep(1_000L);
				} catch (InterruptedException ie) {
				}
				relay.off();
			}
			System.out.println("Valve test completed.");
		}

		/*
		 * There we go.
		 * This is the main loop.
		 */
		if (verbose == VERBOSE.STDOUT) { // Can be used for logging
			System.out.println("-- LOGGING STARTS HERE --");
			System.out.println("Epoch(ms);Date;Temp(C);Hum(%);Dew-pt Temp(C)");
		}
		while (go) {

			if (!enforceSensorSimulation) {
//				try {
//					temperature = probe.readTemperature();
//				} catch (Exception ex) {
//					System.err.println(String.format("At %s :", new Date().toString()));
//					ex.printStackTrace();
//					probe.softReset();
//					System.err.println("Device was reset");
//				}
				try {
					double hum = probe.readHumidity(); // temperature);
					// Low Pass Filter
					humidity = lowPassFilter(ALPHA, hum, humidity);
				} catch (Exception ex) {
					System.err.println(String.format("At %s :", new Date().toString()));
					ex.printStackTrace();
//					probe.softReset();
					System.err.println("Device was reset");
				}
			}

			long now = System.currentTimeMillis();
			if (loggers.size() > 0 && (now - lastLog) > loggingPace) {
				lastLog = now;
				loggers.forEach(logger -> {
					Thread loggerThread = new Thread(() -> {
						try {
							logger.accept(new LogData()
									.feed(LogData.FEEDS.AIR)
									.value(temperature));
						} catch (Exception ex) {
							System.err.println(String.format("At %s :", new Date().toString()));
							System.err.println(ex.toString());
							//	ex.printStackTrace();
							LOGGER.log(Level.ALL, "Air Temp logging", ex);
						}
						try {
							logger.accept(new LogData()
									.feed(LogData.FEEDS.HUM)
									.value(humidity));
						} catch (Exception ex) {
							System.err.println(String.format("At %s :", new Date().toString()));
							System.err.println(ex.toString());
							//	ex.printStackTrace();
							LOGGER.log(Level.ALL, "Hunidity logging", ex);
						}
					});
					loggerThread.start();
				});
			}

			if (verbose == VERBOSE.STDOUT) { // Can be used for logging
				System.out.println(String.format("%d;%s;%.02f;%.02f;%.02f",
						System.currentTimeMillis(),
						new Date().toString(),
						temperature,
						humidity,
						WeatherUtil.dewPointTemperature(humidity, temperature)));
			} else if (verbose == VERBOSE.ANSI) {
				displayANSIConsole();
			}

			/*
			 * Here, test the sensor's values, and make the decision about the valve.
			 */
			if (watchTheProbe) {
				message = "Watching the probe..."; // Default value
			}
			/*
			 * the watchTheProbe variable is used to nap after watering.
			 */
			if (watchTheProbe && humidity < humidityThreshold) { // Ah! Need some water
				// Open the valve
				Thread waterRelayOn = new Thread(() -> {
					synchronized (relay) {
						relay.on();
					}
				});
				waterRelayOn.start();
				message = String.format("Watering (hum: %.02f / %d)...", humidity, humidityThreshold);
				if (verbose == VERBOSE.STDOUT) {
					System.out.println(message);
				} else if (verbose == VERBOSE.ANSI) {
					displayANSIConsole();
				}
				// Watering time
				try {
					final Thread mainThread = Thread.currentThread();
					final long _waterDuration = wateringDuration;
					Thread wateringThread = new Thread(() -> {
						for (int i=0; i<_waterDuration; i++) {
							try {
								Thread.sleep(1_000L);
							} catch (InterruptedException ie) {
								Thread.currentThread().interrupt();
							}
							// Tick, countdown...
							message = String.format("Stop watering in %d sec...", (_waterDuration - i));
							if (verbose == VERBOSE.STDOUT) {
								System.out.println(message);
							} else if (verbose == VERBOSE.ANSI) {
								displayANSIConsole();
							}
						}
						synchronized (mainThread) {
							message = "Ok! Enough water!";
							if (verbose == VERBOSE.STDOUT) {
								System.out.println(message);
							} else if (verbose == VERBOSE.ANSI) {
								displayANSIConsole();
							}
							mainThread.notify(); // Release the wait on main thread.
						}
					}, "watering-thread");
					wateringThread.start();

					synchronized (mainThread) {
						mainThread.wait();
						message = "";
						if (verbose == VERBOSE.STDOUT) {
							System.out.println(message);
						} else if (verbose == VERBOSE.ANSI) {
							displayANSIConsole();
						}
					}
					message = "Shutting off the pump.";
					lastWatering = System.currentTimeMillis();
					if (verbose == VERBOSE.STDOUT) {
						System.out.println(message);
					} else if (verbose == VERBOSE.ANSI) {
						displayANSIConsole();
					}
				} catch (Exception ex) {
					ex.printStackTrace();
				}
				message = "Done watering.";
				if (verbose == VERBOSE.STDOUT) {
					System.out.println(message);
				} else if (verbose == VERBOSE.ANSI) {
					displayANSIConsole();
				}
				// Shut the valve
				Thread waterRelayOff = new Thread(() -> {
					synchronized (relay) {
						relay.off();
					}
				});
				waterRelayOff.start();

				// Wait before resuming sensor watching

				message = "Napping a bit... Spreading the word...";
				watchTheProbe = false;
				if (verbose == VERBOSE.STDOUT) {
					System.out.println(message);
				} else if (verbose == VERBOSE.ANSI) {
					displayANSIConsole();
				}
				try {
					final Thread mainThread = Thread.currentThread();
					final long _napDuration = resumeSensorWatchAfter;
					Thread wateringThread = new Thread(() -> {
						for (int i = 0; i < _napDuration; i++) {
							try {
								Thread.sleep(1_000L);
							} catch (InterruptedException ie) {
								Thread.currentThread().interrupt();
							}
							// Tick, countdown...
							message = String.format("Resuming watch in %s...", fmtDHMS(msToHMS(((_napDuration - i) * 1_000L))));
							if (verbose == VERBOSE.STDOUT) {
								System.out.println(message);
							} else if (verbose == VERBOSE.ANSI) {
								displayANSIConsole();
							}
						}
						synchronized (mainThread) {
							message = "Resuming watch.";
							if (verbose == VERBOSE.STDOUT) {
								System.out.println(message);
							} else if (verbose == VERBOSE.ANSI) {
								displayANSIConsole();
							}
							watchTheProbe = true; // Resume!
			//			mainThread.notify(); // Release the wait on main thread.
						}
					}, "wait-for-resume-thread");
					wateringThread.start();

					synchronized (mainThread) {
			//		mainThread.wait(); // Wait for resume (above)
						message = "";
						if (verbose == VERBOSE.STDOUT) {
							System.out.println(message);
						} else if (verbose == VERBOSE.ANSI) {
							displayANSIConsole();
						}
					}
				} catch (Exception ex) {
					ex.printStackTrace();
//			} catch (InterruptedException ie) {
//				Thread.currentThread().interrupt();
				}
				// Resume watching
				message = "...";
				if (verbose == VERBOSE.ANSI) {
					displayANSIConsole();
				}
			} else {
				try {
					Thread.sleep(1_000L);
				} catch (InterruptedException ex) {
					Thread.currentThread().interrupt();
				}
			}
		}

		if (withRESTServer) {
			if (httpServer.isRunning()) {
				httpServer.stopRunning();
			}
		}

		if (verbose == VERBOSE.ANSI) {
			AnsiConsole.systemUninstall();
		}

		if (probe.isSimulating()) {
			System.out.println(String.format("Simulated temperature between %.02f and %.02f", minSimTemp, maxSimTemp));
			System.out.println(String.format("Simulated humidity between %.02f and %.02f", minSimHum, maxSimHum));
		}

		probe.shutdown();
		relay.shutdownGPIO();

		System.out.println("Bye-bye!");
	}
}

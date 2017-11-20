package nmea.consumers.reader;

import com.pi4j.io.i2c.I2CFactory;
import i2c.sensor.LSM303;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.List;
import nmea.api.NMEAEvent;
import nmea.api.NMEAListener;
import nmea.api.NMEAParser;
import nmea.api.NMEAReader;
import nmea.parser.StringGenerator;
import nmea.parser.StringGenerator.XDRTypes;

/**
 * Reads data from an LSM303 sensor.
 * Pitch and Roll, as XDR Strings.
 */
public class LSM303Reader extends NMEAReader {

	private LSM303 lsm303;
	private static final String DEFAULT_DEVICE_PREFIX = "RP";
	private String devicePrefix = DEFAULT_DEVICE_PREFIX;
	private int headingOffset = 0;

	private static final long BETWEEN_LOOPS = 1_000L; // TODO: Make it an external parameter?

	public LSM303Reader(List<NMEAListener> al) {
		super(al);
		try {
			this.lsm303 = new LSM303();
		} catch (I2CFactory.UnsupportedBusNumberException e) {
			e.printStackTrace();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}

	public String getDevicePrefix() {
		return this.devicePrefix;
	}

	public void setDevicePrefix(String devicePrefix) {
		this.devicePrefix = devicePrefix;
	}

	public int getHeadingOffset() {
		return this.headingOffset;
	}

	public void setHeadingOffset(int headingOffset) {
		this.headingOffset = headingOffset;
	}

	@Override
	public void startReader() {
		super.enableReading();
		this.lsm303.startReading();
		System.out.println(String.format(">> Starting reader [%s] (%s). Enabled:%s", this.getClass().getName(), this.devicePrefix, this.canRead()));
		while (this.canRead()) {
			// Read data every 1 second
			// Filter accordingly if needed, on XDR and HDM
			try {

				double pitch = lsm303.getPitch();
				double roll  = lsm303.getRoll();

				double heading = lsm303.getHeading();

				if ("true".equals(System.getProperty("lsm303.data.verbose", "false"))) {
					System.out.println(String.format(">>> From LSM303: Heading %f, Pitch: %f, Roll: %f", heading, pitch, roll));
				}

				// Generate NMEA String(s). OpenCPN recognizes those ones (Needs a 'II' prefix though).
				String nmeaXDR = StringGenerator.generateXDR(devicePrefix,
								new StringGenerator.XDRElement(XDRTypes.ANGULAR_DISPLACEMENT,
												pitch,
												"PTCH"), // No, it's not a typo, there is no 'I' in 'PTCH'.
								new StringGenerator.XDRElement(XDRTypes.ANGULAR_DISPLACEMENT,
												roll,
												"ROLL"));
				nmeaXDR += NMEAParser.NMEA_SENTENCE_SEPARATOR;
				fireDataRead(new NMEAEvent(this, nmeaXDR));

				String nmeaHDM = StringGenerator.generateHDM(devicePrefix, (int)Math.round(heading));
				nmeaHDM += NMEAParser.NMEA_SENTENCE_SEPARATOR;
				fireDataRead(new NMEAEvent(this, nmeaHDM));
			} catch (Exception e) {
				e.printStackTrace();
			}
			try {
				Thread.sleep(BETWEEN_LOOPS);
			} catch (InterruptedException ie) {
				ie.printStackTrace();
			}
		}
		System.out.println(String.format(">>> %s done reading. Bye.", this.getClass().getName()));
	}

	@Override
	public void closeReader() throws Exception {
		if (this.lsm303 != null) {
			this.lsm303.setKeepReading(false);
		}
	}
}

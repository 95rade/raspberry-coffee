package spi.lcd.nokia.samples;

import lcd.ScreenBuffer;
import spi.lcd.nokia.Nokia5110;
import utils.PinUtil;

public class Nokia5110Sample {

	// Bitmap generated with http://javl.github.io/image2cpp/
	public static int[] PELICAN_LOGO  = {
		// 'pelican, 64x64px
			0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x44, 0x00,
			0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x4c, 0xc0, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0xcc, 0x80,
			0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0xc9, 0x80, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0xd9, 0x88,
			0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0xdb, 0x18, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01, 0xb7, 0x30,
			0x00, 0x00, 0x00, 0x00, 0x00, 0x01, 0xb6, 0xe0, 0x00, 0x00, 0x00, 0x00, 0x00, 0x03, 0xef, 0xc0,
			0x00, 0x00, 0x00, 0x00, 0x00, 0x03, 0xff, 0x98, 0x00, 0x00, 0x00, 0x00, 0x00, 0x07, 0xff, 0xf0,
			0x00, 0x00, 0x00, 0x00, 0x00, 0x07, 0xff, 0xf0, 0x00, 0x00, 0x00, 0x00, 0x00, 0x0f, 0xff, 0xf8,
			0x00, 0x00, 0x00, 0x00, 0x00, 0x0f, 0xff, 0xf0, 0x00, 0x00, 0x00, 0x00, 0x00, 0x1f, 0xff, 0xf0,
			0x00, 0x00, 0x00, 0x00, 0x00, 0x1f, 0xff, 0xf0, 0x00, 0x00, 0x00, 0x00, 0x00, 0x3f, 0xff, 0xe0,
			0x00, 0x00, 0x00, 0x00, 0x00, 0x3f, 0xff, 0xe0, 0x00, 0x00, 0x00, 0x00, 0x00, 0x7f, 0xff, 0xe0,
			0x00, 0x00, 0x00, 0x00, 0x00, 0xff, 0xff, 0xc0, 0x00, 0x00, 0x00, 0x00, 0x00, 0xff, 0xff, 0xc0,
			0x00, 0x00, 0x00, 0x00, 0x01, 0xff, 0xff, 0x80, 0x00, 0x00, 0x00, 0x00, 0x03, 0xff, 0xff, 0x80,
			0x00, 0x00, 0x00, 0x00, 0x03, 0xff, 0xff, 0x80, 0x00, 0x00, 0x00, 0x00, 0x07, 0xff, 0xff, 0x80,
			0x00, 0x00, 0x00, 0x00, 0x07, 0xff, 0xff, 0x80, 0x00, 0x00, 0x00, 0x00, 0x07, 0xff, 0xff, 0x80,
			0x00, 0x00, 0x00, 0x08, 0x0f, 0xff, 0xff, 0x80, 0x00, 0x00, 0x00, 0x08, 0x8f, 0xff, 0xff, 0x80,
			0x00, 0x00, 0x00, 0x09, 0x8f, 0xff, 0xff, 0x80, 0x00, 0x00, 0x00, 0x19, 0x9f, 0xff, 0xff, 0x80,
			0x00, 0x00, 0x00, 0x1b, 0xbf, 0xff, 0xff, 0x80, 0x00, 0x00, 0x00, 0x1f, 0xef, 0xff, 0xff, 0x80,
			0x00, 0x00, 0x00, 0x1f, 0xef, 0xff, 0xff, 0x80, 0x00, 0x00, 0x00, 0x3f, 0xdf, 0xff, 0xff, 0x80,
			0x00, 0x00, 0x00, 0x3f, 0xff, 0xff, 0xff, 0x80, 0x00, 0x00, 0x00, 0x3f, 0xff, 0xff, 0xff, 0x80,
			0x00, 0x00, 0x00, 0x7f, 0xff, 0xff, 0xff, 0x00, 0x00, 0x00, 0x00, 0x7f, 0xff, 0xff, 0xff, 0x00,
			0x00, 0x00, 0x00, 0x7f, 0xff, 0xff, 0xff, 0x00, 0x00, 0x00, 0x00, 0x3f, 0xff, 0xff, 0xfe, 0x00,
			0x00, 0x00, 0x00, 0x1f, 0xff, 0xff, 0xfe, 0x00, 0x00, 0x00, 0xff, 0xcf, 0xff, 0xff, 0xfe, 0x00,
			0x00, 0x01, 0xff, 0xff, 0xff, 0xff, 0xfe, 0x00, 0x00, 0x03, 0xff, 0xff, 0xff, 0xff, 0xfc, 0x00,
			0x00, 0x07, 0xff, 0xf7, 0xff, 0xff, 0xf8, 0x00, 0x00, 0x1f, 0xff, 0xf3, 0xff, 0xff, 0xf0, 0x00,
			0x00, 0x7f, 0xff, 0xe3, 0xff, 0xff, 0xf0, 0x00, 0x03, 0xff, 0xff, 0xe7, 0xff, 0xff, 0xf0, 0x00,
			0x0f, 0xff, 0xff, 0xff, 0xff, 0xff, 0xf0, 0x00, 0x3f, 0xfb, 0xff, 0xff, 0xff, 0xff, 0xe0, 0x00,
			0x7f, 0x81, 0xff, 0xff, 0xff, 0xff, 0xe0, 0x00, 0xc0, 0x01, 0xff, 0xff, 0xff, 0xff, 0xe0, 0x00,
			0x00, 0x00, 0xff, 0xff, 0xff, 0xff, 0xe0, 0x00, 0x00, 0x00, 0x3f, 0xff, 0xff, 0xff, 0xf0, 0x00,
			0x00, 0x00, 0x0f, 0xff, 0xff, 0xff, 0xfc, 0x00, 0x00, 0x00, 0x03, 0xff, 0xff, 0xff, 0xff, 0x80,
			0x00, 0x00, 0x00, 0x1f, 0xff, 0xff, 0xff, 0xf8, 0x00, 0x00, 0x00, 0x03, 0xff, 0xff, 0xff, 0xfe,
			0x00, 0x00, 0x00, 0x00, 0x7f, 0xff, 0xff, 0xfe, 0x00, 0x00, 0x00, 0x00, 0x07, 0xff, 0xff, 0xf8,
			0x00, 0x00, 0x00, 0x00, 0x00, 0xff, 0xff, 0xf8, 0x00, 0x00, 0x00, 0x00, 0x00, 0x0f, 0xe0, 0x00
	};

	// Bitmap generated with http://javl.github.io/image2cpp/
	public static int[] ADAFRUIT_LOGO = {
			0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
			0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
			0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0xC0, 0xE0, 0xF0, 0xF8, 0xFC, 0xFC, 0xFE, 0xFF, 0xFC, 0xE0,
			0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
			0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
			0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
			0x00, 0x00, 0x00, 0x00, 0x00, 0xF8, 0xF8, 0xF8, 0xF8, 0xF8, 0xF8, 0xF8, 0xF8, 0xF8, 0xF8, 0xF8,
			0xF8, 0xF0, 0xF0, 0xE0, 0xE0, 0xC0, 0x80, 0xC0, 0xFC, 0xFF, 0xFF, 0xFF, 0xFF, 0x7F, 0x3F, 0x7F,
			0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
			0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
			0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
			0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x0F, 0x1F, 0x3F, 0x7F, 0xFF, 0xFF,
			0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xE7, 0xC7, 0xC7, 0x87, 0x8F, 0x9F, 0x9F, 0xFF, 0xFF, 0xFF,
			0xC1, 0xC0, 0xE0, 0xFC, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFC, 0xFC, 0xFC, 0xFC, 0xFE, 0xFE, 0xFE,
			0xFC, 0xFC, 0xF8, 0xF8, 0xF0, 0xE0, 0xC0, 0xC0, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
			0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
			0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
			0x00, 0x80, 0xC0, 0xE0, 0xF1, 0xFB, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0x7F, 0x1F, 0x0F, 0x0F, 0x87,
			0xE7, 0xFF, 0xFF, 0xFF, 0x1F, 0x1F, 0x3F, 0xF9, 0xF8, 0xF8, 0xF8, 0xF8, 0xF8, 0xF8, 0xFD, 0xFF,
			0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0x7F, 0x3F, 0x0F, 0x07, 0x01, 0x00, 0x00, 0x00,
			0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
			0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
			0x00, 0x00, 0x00, 0xF0, 0xFE, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFE,
			0x7E, 0x3F, 0x3F, 0x0F, 0x1F, 0xFF, 0xFF, 0xFF, 0xFC, 0xF0, 0xE0, 0xF1, 0xFF, 0xFF, 0xFF, 0xFF,
			0xFF, 0xFC, 0xF0, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00,
			0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
			0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
			0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x01,
			0x01, 0x01, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x03, 0x0F, 0x1F, 0x3F, 0x7F, 0x7F,
			0xFF, 0xFF, 0xFF, 0xFF, 0x7F, 0x7F, 0x1F, 0x03, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
			0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
			0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 };

	public static void main(String... args) {

		System.out.println("Nokia pins - from the TOP (seeing the screen), bottom pins (P1):");
		System.out.println("|                           |");
		System.out.println("+------o-o-o-o-o-o-o-o------+");
		System.out.println("       | | | | | | | |");
		System.out.println("       | | | | | | | GND");
		System.out.println("       | | | | | | VCC");
		System.out.println("       | | | | | CLK");
		System.out.println("       | | | | DIN");
		System.out.println("       | | | D/C");
		System.out.println("       | | CS");
		System.out.println("       | RST");
		System.out.println("       LED");
		System.out.println();

		String[] map = new String[7];
		map[0] = String.valueOf(PinUtil.GPIOPin.PWR_1.pinNumber()) + ":" + "VCC";
		map[1] = String.valueOf(PinUtil.GPIOPin.GRND_1.pinNumber()) + ":" + "GND";
		map[2] = String.valueOf(PinUtil.GPIOPin.GPIO_4.pinNumber()) + ":" + "D/C";
		map[3] = String.valueOf(PinUtil.GPIOPin.GPIO_5.pinNumber()) + ":" + "RST";
		map[4] = String.valueOf(PinUtil.GPIOPin.GPIO_10.pinNumber()) + ":" + "CS";
		map[5] = String.valueOf(PinUtil.GPIOPin.GPIO_14.pinNumber()) + ":" + "CLK";
		map[6] = String.valueOf(PinUtil.GPIOPin.GPIO_12.pinNumber()) + ":" + "DIN";
//	map[7] = String.valueOf(PinUtil.GPIOPin.PWR_1.pinNumber()) + ":" + "LED and VCC";

		PinUtil.print(map);
		System.out.println("VCC and LED are connected. This is also where a pot would go.");
		// For the pot: See https://learn.adafruit.com/nokia-5110-3310-monochrome-lcd?view=all#dimmable-backlight-option-3-12
		System.out.println();

		System.out.println("Starting");
		Nokia5110 lcd = new Nokia5110();
		lcd.begin();

		lcd.clear();
		lcd.display();
		System.out.println("Ready");

		lcd.setScreenBuffer(ADAFRUIT_LOGO);
//	lcd.setScreenBuffer(PELICAN_LOGO);

		//  lcd.data(Nokia5110.ADAFRUIT_LOGO);
		System.out.println("Displaying...");
		lcd.display();
		System.out.println("Displayed");
		try {
			Thread.sleep(5_000L);
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		ScreenBuffer sb = new ScreenBuffer(84, 48);
		sb.clear(ScreenBuffer.Mode.BLACK_ON_WHITE);
		sb.text("Hello Nokia!", 5, 20, ScreenBuffer.Mode.BLACK_ON_WHITE);
		sb.text("I speak Java!", 5, 30, ScreenBuffer.Mode.BLACK_ON_WHITE);
		lcd.setScreenBuffer(sb.getScreenBuffer());
		lcd.display();
		try {
			Thread.sleep(2_000);
		} catch (Exception ex) {
		}

		sb.clear(ScreenBuffer.Mode.WHITE_ON_BLACK);
		sb.text("Hello Nokia!", 5, 20, ScreenBuffer.Mode.WHITE_ON_BLACK);
		sb.text("I speak Java!", 5, 30, ScreenBuffer.Mode.WHITE_ON_BLACK);
		lcd.setScreenBuffer(sb.getScreenBuffer());
		lcd.display();
		try {
			Thread.sleep(2_000);
		} catch (Exception ex) {
		}

		sb.clear();
		for (int i = 0; i < 8; i++) {
			sb.rectangle(1 + (i * 2), 1 + (i * 2), 83 - (i * 2), 47 - (i * 2));
//    lcd.setScreenBuffer(sb.getScreenBuffer());
//    lcd.display();
			//  try { Thread.sleep(100); } catch (Exception ex) {}
		}
		lcd.setScreenBuffer(sb.getScreenBuffer());
		lcd.display();
		try {
			Thread.sleep(1_000);
		} catch (Exception ex) {
		}

		sb.clear(ScreenBuffer.Mode.WHITE_ON_BLACK);
		sb.text("Pi=", 2, 9, ScreenBuffer.Mode.WHITE_ON_BLACK);
		sb.text("3.1415926", 2, 19, 2, ScreenBuffer.Mode.WHITE_ON_BLACK);
		lcd.setScreenBuffer(sb.getScreenBuffer());
		lcd.display();
//  sb.dumpScreen();
		try {
			Thread.sleep(5_000);
		} catch (Exception ex) {
		}

		sb.clear(ScreenBuffer.Mode.WHITE_ON_BLACK);
		sb.text("Pi=", 2, 9, ScreenBuffer.Mode.WHITE_ON_BLACK, true); // true: ROTATE
		sb.text("3.1415926", 2, 19, 2, ScreenBuffer.Mode.WHITE_ON_BLACK, true); // true: ROTATE
		lcd.setScreenBuffer(sb.getScreenBuffer());
		lcd.display();
		//  sb.dumpScreen();
		try {
			Thread.sleep(5_000);
		} catch (Exception ex) {
		}

		lcd.clear();
		lcd.display();
		lcd.shutdown();
		System.out.println("Done");
	}
}

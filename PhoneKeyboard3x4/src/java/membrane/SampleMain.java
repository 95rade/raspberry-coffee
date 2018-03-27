package membrane;

import phonekeyboard3x4.KeyboardController;

import java.text.NumberFormat;

public class SampleMain {
	public static void main(String... args) {
		System.out.println("Hit the same key twice to exit");
		System.out.println("------------------------------");

		MembraneKeyPad1x4 membraneKeyPad1x4 = new MembraneKeyPad1x4(true);
		char prevchar = ' ';
		boolean go = true;
		while (go) {
			char c = membraneKeyPad1x4.getKey();
			System.out.println("At " + NumberFormat.getInstance().format(System.currentTimeMillis()) + ", Char: [" + c + "]");
			if (c == prevchar) {
				go = false;
			}
			prevchar = c;
			try {
				Thread.sleep(200L);
			} catch (Exception ex) {
			}
		}
		System.out.println("Bye");
		membraneKeyPad1x4.shutdown();
	}
}

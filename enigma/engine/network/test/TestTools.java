package enigma.engine.network.test;

public class TestTools {
	public static void sleepForMS(long ms) {
		try {
			Thread.sleep(ms);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public static void sleepForMS(int ms) {
		try {
			Thread.sleep(ms);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Determine if two values are equal within a certain threshold.
	 * 
	 * @param val1 first float
	 * @param val2 second float
	 * @param delta the threshold value
	 * @return whether the two floats are equal within the delta limit.
	 */
	public static boolean floatEqualsCompare(float val1, float val2, float delta) {
		float result = val1 - val2;
		result = Math.abs(result);
		return result < Math.abs(delta);
	}
}

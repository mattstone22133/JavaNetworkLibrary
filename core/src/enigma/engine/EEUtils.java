package enigma.engine;

/**
 * A utility class with useful functions in multiple classes
 * 
 * @author Matt Stone
 */
public class EEUtils {
	public static final float F_SMALL_NUMBER = 0.00001f;
	
	
	/**
	 * Determines if float is zero by checking if it is less than the threshold number. Numbers
	 * below the threshold number should be considered small enough to be zero. This is provided since
	 * the comparison of float values with 0.0f isn't safe due to the way floating point numbers are stored.
	 * 
	 * @param value the value to check.
	 * @param threshold a special value that represents the threshold where numbers are considered
	 *            zero; any number that is less than the threshold number should be considered zero.
	 * @return true if the number is below the threshold number.
	 */
	public static boolean floatIsZero(float value, float threshold) {
		return Math.abs(value) < threshold;
	}
	
	/**
	 * Get the distance between two 2D points using the pythagorean theorem.
	 * @param x1 - x of first point
	 * @param y1 - y of first point
	 * @param x2 - x of second point
	 * @param y2 - y of second point
	 * @return the positive distance between the two points
	 */
	public static float getDistance(float x1, float y1, float x2, float y2){
		float dX = x2 - x1;
		float dY = y2 - y1;
		
		//pythagorean theorem
		return (float) Math.sqrt(dX*dX + dY*dY);
	}
}

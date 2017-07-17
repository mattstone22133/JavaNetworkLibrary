package enigma.engine;

public class ConstGlobals {
	/**
	 * Sets the number of instances each actor will create to hold queued points to move to when interpolation is used.
	 * This is used in networking where updates to position are not smooth. Interpolation between two positions make the
	 * movement appear smooth and much more pleasant to the eye. 
	 */
	public static final int NUM_QUEUE_INTERPOLATE_POINTS = 10; 
}

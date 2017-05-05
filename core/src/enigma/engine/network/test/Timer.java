package enigma.engine.network.test;

import java.util.HashMap;

public class Timer <T>{
	private HashMap<T, Long> namedTimers;

	/**
	 * a constructor that create a timer manager, but no timers are created.
	 */
	public Timer() {
		namedTimers = new HashMap<T, Long>();
	}

	/**
	 * A constructor that creates a time manager and creates the first timer.
	 * 
	 * @param name the name of a new timer to create.
	 */
	public Timer(T name) {
		this();
		newTimer(name);
	}

	public void newTimer(T name) {
		if (namedTimers.get(name) != null) {
			throw new IllegalArgumentException("duplicate timer creation was attempted. " + name + " already exists");
		}
		namedTimers.put(name, System.currentTimeMillis());
	}

	public void startTimer(T name) {
		if (namedTimers.get(name) == null) {
			// have timers explicitly created so no confusion on using multiple timers can happen.
			throw new IllegalArgumentException("timer: " + name + " was not created");
		}

		namedTimers.put(name, System.currentTimeMillis());
	}

	public boolean timeUp(T name, long delayMS) {
		Long startTime = namedTimers.get(name);

		if (startTime == null) {
			throw new IllegalArgumentException("timer: " + name + "was not created");
		}

		return System.currentTimeMillis() - startTime > delayMS;
	}

}

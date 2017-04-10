package enigma.engine.network;

import java.util.HashMap;
import java.util.LinkedList;

/**
 * An id manager responsible for providing unique IDs. It also can accept ID's in use make them
 * valid IDs. Ids are provided in O(1) and return in O(1).
 * 
 * Maximum id number is 255 (and hence the maximum concurrent id's is 256 ids since 0 is a valid
 * id). This is because id's are provided as a single byte (char) to make transfer of id's over
 * network less costly.
 * 
 * @author Matt Stone
 * @version 4/4/17
 *
 */
public class IDManager {
	private static final int MAX_VALUES_IN_CHAR_TYPE = 256;
	private LinkedList<Character> availableIDs = new LinkedList<Character>();
	private HashMap<Character, Boolean> idMapForReinsertions = new HashMap<Character, Boolean>();
	private int idsInUse = 0;

	/**
	 * Create an IDManager that will provide unique IDs. Provides min(maxIDCount, 256) number of
	 * unique IDs
	 * 
	 * @param maxIDCount the maximum number of unique id's to provide. If it is over 256, then 256
	 *            will be the maximum number of unique IDs
	 */
	public IDManager(int maxIDCount) {
		for (int i = 0; i < maxIDCount && i < MAX_VALUES_IN_CHAR_TYPE; ++i) {
			idMapForReinsertions.put((char) i, true);
			availableIDs.add((char) i);
		}
	}

	/**
	 * Poll the IDManager to determine if there are remaining ID's to reserve and claim. This method
	 * is private for thread safety. Theoretically, and ID could be claimed after this returns true
	 * for one thread. Thus, user should attempt an getReservedIDAndRemoveFromIDPool() and check if
	 * the value is null.
	 * 
	 * @return true if IDManager has more available id's to provide, false if all id's are in use.
	 */
	public synchronized boolean hasMoreIds() {
		return availableIDs.size() > 0;
	}

	/**
	 * Remove an id from the pool of available ids. This id is gauranteed to be unique unless the
	 * returnIdToPool() method is called returning this id to the pool
	 * 
	 * @warning checking that hasMoreIds() returns true does not mean that the call to this function
	 *          will still have an ID available when this function is called. The return of this
	 *          call should always be checked for null.
	 * 
	 * @precondition hasMoreIds() returns true
	 * 
	 * 
	 * @return an available ID or null if there are no IDs left
	 */
	public synchronized Character getReservedIDAndRemoveFromIDPool() {
		if (hasMoreIds()) {
			// get the id, and mark it as removed in the map (to make re-insertions O(1))
			char nextId = availableIDs.poll();
			idMapForReinsertions.put(nextId, false);
			++idsInUse;
			return nextId;

		} else {
			// there are no ID's, return null;
			return null;
		}
	}

	public synchronized boolean unReserveIDAndReturnIdToPool(char id) {
		if (!idMapForReinsertions.get(id)) {
			// the key was removed, safe to reinsert
			idMapForReinsertions.put(id, true);
			availableIDs.add(id);
			--idsInUse;
			return true;
		} else {
			// the key is currently available, do not re-insert
			return false;
		}
	}

	public synchronized int getActiveNumberIds() {
		return idsInUse;
	}
}

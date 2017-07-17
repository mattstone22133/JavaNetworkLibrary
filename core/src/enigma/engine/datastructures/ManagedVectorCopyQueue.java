package enigma.engine.datastructures;

import java.util.LinkedList;
import java.util.Queue;
import java.util.Stack;

import com.badlogic.gdx.math.Vector3;

/**
 * A Queue that recycles objects for efficiency. Maximum queue size is also provided.
 * 
 * @note Queued objects are "copied" rather than stored by reference.
 * @note Queued objects must overload the clone() method.
 * 
 * @engineering Why not generic? Could not easily make generic due to necesity of instantiating
 *              object instances. This could be done with a factory interface or using reflection,
 *              but that will be developed when it becomes needed. There is currently only one case
 *              where it is needed, so that type will be hardcoded.
 * 
 * @author Matt Stone
 *
 * @param <T> type to queue
 */
public class ManagedVectorCopyQueue {
	private Queue<Vector3> internalQueue = new LinkedList<Vector3>();
	private Stack<Vector3> recycleStack = new Stack<Vector3>();
	private int objectCreationCap = -1;
	private Vector3 tail = null;

	public ManagedVectorCopyQueue(int maximumObjectsToCreated, boolean lazyCreate) {
		setObjectCreationCap(maximumObjectsToCreated);
		if (!lazyCreate && getObjectCreationCap() > 0) {
			for (int i = 0; i < getObjectCreationCap(); ++i) {
				recycleStack.push(new Vector3());
			}
		}
	}

	public boolean queueACopy(Vector3 toCopy) {
		// The recycled instance to load
		if (toCopy == null) return false;
		Vector3 willQueue = null;

		// Get an object to queue
		if (!recycleStack.isEmpty()) {
			// --- There is already an object created to recycle ---
			willQueue = recycleStack.pop();

		} else {
			// --- No object in recycle stack to recycle ---
			// When object creation cap is 0, there is no cap
			if (getObjectCreationCap() == 0) {
				willQueue = new Vector3();

			} else {
				// --- Cap on the number of objects ---
				if (internalQueue.size() < getObjectCreationCap()) {
					// already checked recycle stack, just create new object since we haven't hit
					// the limit
					willQueue = new Vector3();

				} else {
					// Cannot create any more objects due to cap, truncate last walk point
					willQueue = tail;
					if (tail == null) throw new RuntimeException("No tail pointer -- error state!");

					// The condition that the tail can accumulate massive changes exists.
					// Anytime the tail must be overwritten, discarding the front by popping will
					// ensure that the next queue will not overwrite the tail again, but have
					// availability at end of the queue. @Warning this does mean that some data
					// will be lost at the front of the queue, but since this is is primarily an
					// interpolation tool, it will be better to spread out the lag over the entire
					// queue rather than pile bad data at the tail of the queue.
					poll(null);
				}
			}
		}

		// Queue up object
		return loadQueue(willQueue, toCopy);
	}

	private boolean loadQueue(Vector3 willQueueInstance, Vector3 toCopyInstance) {
		if (willQueueInstance != null && toCopyInstance != null) {
			willQueueInstance.set(toCopyInstance);
			tail = willQueueInstance;
			return internalQueue.add(willQueueInstance);
		}
		return false;
	}

	/**
	 * Pops from the queue, but moves data into a profided object instance
	 * 
	 * @param buffer - object instance where data will be loaded
	 */
	public Vector3 poll(Vector3 buffer) {
		if (buffer == null || internalQueue.isEmpty()) return null;
		
		Vector3 polled = internalQueue.poll();
		buffer.set(polled);
		recycleStack.push(polled);
				
		return buffer;
	}
	
	public int size(){
		return internalQueue.size();
	}
	
	public boolean isEmpty(){
		return internalQueue.isEmpty();
	}

	// --------- Getters and Setters

	/**
	 * @return The maximum number of objects that can be held in the queue, or zero if no limit.
	 */
	public int getObjectCreationCap() {
		return objectCreationCap;
	}

	public void setObjectCreationCap(int objectCreationCap) {
		if (objectCreationCap <= 0) {
			objectCreationCap = 0;
		}
		this.objectCreationCap = objectCreationCap;
	}

}

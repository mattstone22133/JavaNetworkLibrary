package enigma.engine.network;

public class NetworkPlayer {
	/** The id that a server provides */
	public final char playerID;

	/**
	 * Constructor that can only be called from within the engima.engine.network package.
	 * 
	 * @param assignedID the id that a server has generated. 
	 */
	protected NetworkPlayer(char assignedID) {
		this.playerID = assignedID;
	}
}

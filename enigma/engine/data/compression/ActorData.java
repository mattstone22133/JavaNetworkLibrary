package enigma.engine.data.compression;

/**
 * @author Matt Stone
 *
 */
public class ActorData implements DataCompressor {
	private static final long serialVersionUID = 452971468926105198L;
	public int id;
	public float rotation; // This could be done in a "short" with loss of precision but gain in
	public float x;
	public float y;

	public ActorData(int id, float rotation, float x, float y) {
		this.id = id;
		this.rotation = rotation;
		this.x = x;
		this.y = y;
	}
	
	public ActorData clone(){
		return new ActorData(id, rotation, x, y);
	}

}

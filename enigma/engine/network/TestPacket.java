package enigma.engine.network;

public class TestPacket implements Packet{
	private static final long serialVersionUID = 1L;
	private int id;
	private float rotation;
	private float x;
	private float y;
	
	
	public TestPacket(int id, float x, float y, float rotation){
		this.id = id;
		this.x = x;
		this.y = y;
		this.rotation = rotation;
	}
	
	public void printData(){
		System.out.printf("x:%3.2f y:%3.2f rot:%3.2f id:%d.", x, y, rotation, id);
	}
	
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public float getRotation() {
		return rotation;
	}
	public void setRotation(float rotation) {
		this.rotation = rotation;
	}
	public float getX() {
		return x;
	}
	public void setX(float x) {
		this.x = x;
	}
	public float getY() {
		return y;
	}
	public void setY(float y) {
		this.y = y;
	}
	
	public static void main(String[] args) {
		TestPacket test = new TestPacket(55, 4, 4, 45.0f);
		test.printData();
	}

	@Override
	public Packet makeCopy() {
		TestPacket ret = new TestPacket(this.id, this.x, this.y, this.rotation);		
		return ret;
	}
}

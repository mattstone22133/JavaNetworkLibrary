package enigma.engine.network;

public class DemoConcretePacket implements Packet {
	private static final long serialVersionUID = 1L;
	private int id;
	private float rotation;
	private float x;
	private float y;

	public DemoConcretePacket(int id, float x, float y, float rotation) {
		this.id = id;
		this.x = x;
		this.y = y;
		this.rotation = rotation;
	}

	public void printData() {
		System.out.printf("x:%3.2f y:%3.2f rot:%3.2f id:%d.\n", x, y, rotation, id);
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
		DemoConcretePacket test = new DemoConcretePacket(55, 4, 4, 45.0f);
		test.printData();
	}

	@Override
	public Packet makeCopy() {
		DemoConcretePacket ret = new DemoConcretePacket(this.id, this.x, this.y, this.rotation);
		return ret;
	}

	// private void writeObject(ObjectOutputStream oos) throws IOException {
	// // default serialization
	// //oos.defaultWriteObject();
	//
	// oos.writeObject(localID);
	// oos.writeObject(rotation);
	// oos.writeObject(x);
	// oos.writeObject(y);
	//
	// }

	// private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException {
	// // default deserialization
	// //ois.defaultReadObject();

	// localID = ((Integer) ois.readObject());
	// rotation = (Float) ois.readObject();
	// x = (Float) ois.readObject();
	// y = (Float) ois.readObject();
	//
	// }
}

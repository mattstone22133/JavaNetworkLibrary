package enigma.engine.network.test;

import java.util.ArrayList;

import enigma.engine.network.Packet;

public class LargePacket implements Packet {
	private static final long serialVersionUID = 1123112L;

	private ArrayList<Float> values;

	public LargePacket(int numOfItems) {
		values = new ArrayList<Float>(numOfItems);
		for(int i = 0; i < numOfItems; ++i){
			values.add((float) i);
		}
	}
	
	@Override
	public Packet makeCopy() {
		LargePacket ret = new LargePacket(values.size());
		return ret;
	}

	public int size() {
		return values.size();
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

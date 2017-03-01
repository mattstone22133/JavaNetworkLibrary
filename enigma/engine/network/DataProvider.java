package enigma.engine.network;

import java.io.Serializable;
import java.util.Random;

public class DataProvider implements Serializable {
	private static final long serialVersionUID = 1L;
	public int localID;
	public float rotation;
	public float x;
	public float y;

	public void randomizeData() {
		Random rng = new Random();
		localID = rng.nextInt();
		x = 100 * rng.nextFloat();
		y = 100 * rng.nextFloat();
		rotation = 360 * rng.nextFloat();
		System.out.printf("x:%3.2f y:%3.2f rot:%3.2f id:%d.", x, y, rotation, localID);
	}

	public void print() {
		System.out.printf("x:%3.2f y:%3.2f rot:%3.2f id:%d.", x, y, rotation, localID);
	}

	public static void main(String[] args) {
		DataProvider x = new DataProvider();
		x.randomizeData();
	}

//	private void writeObject(ObjectOutputStream oos) throws IOException {
//		// default serialization
//		//oos.defaultWriteObject();
//		
//		oos.writeObject(localID);
//		oos.writeObject(rotation);
//		oos.writeObject(x);
//		oos.writeObject(y);
//
//	}

//	private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException {
//		// default deserialization
//		//ois.defaultReadObject();
	
//		localID = ((Integer) ois.readObject());
//		rotation = (Float) ois.readObject();
//		x = (Float) ois.readObject();
//		y = (Float) ois.readObject();
//
//	}
}

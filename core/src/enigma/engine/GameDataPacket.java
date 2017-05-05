package enigma.engine;

import java.util.ArrayList;

import enigma.engine.data.compression.ActorData;
import enigma.engine.data.compression.DataCompressor;
import enigma.engine.network.Packet;

public class GameDataPacket implements Packet {
	private static final long serialVersionUID = 4186186175428897466L;
	ArrayList<DataCompressor> actorsAdded = new ArrayList<DataCompressor>();

	public void addActor(Actor actor) {
		actorsAdded.add(actor.getCompresedData());
	}

	@Override
	public Packet makeCopy() {
		// copy actors
		GameDataPacket copy = new GameDataPacket();
		for (DataCompressor data : actorsAdded) {
			copy.actorsAdded.add((ActorData) data.clone());
		}
		return copy;
	}

//	private void writeObject(ObjectOutputStream oos) throws IOException {
//		// default serialization
//		// oos.defaultWriteObject();
//
//		// Write the actor objects
//		oos.writeObject(DataFlags.ACTOR);
//		oos.writeObject(actorsAdded.size());
//		for (ActorData data : actorsAdded) {
//			// oos.writeObject(data.id);
//			oos.writeInt(data.id);
//			oos.writeFloat(data.rotation);
//			oos.writeFloat(data.x);
//			oos.writeFloat(data.y);
//		}
//
//	}

//	private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException {
//		// default deserialization
//		// ois.defaultReadObject();
//
//		// Maybe should have a start flag that skips over data as long as it is not the start.
//
//		while (true) {
//			DataFlags flag = (DataFlags) ois.readObject();
//			switch (flag) {
//			case ACTOR:
//				getActorsFromPacket(ois);
//				break;
//			case DONE:
//				return;
//			}
//		}
//
//		// localID = ((Integer) ois.readObject());
//		// rotation = (Float) ois.readObject();
//		// x = (Float) ois.readObject();
//		// y = (Float) ois.readObject();
//
//	}

//	private void getActorsFromPacket(ObjectInputStream ois) throws IOException {
//		// TODO Auto-generated method stub
//		int amountOfActors = ois.readInt();
//		actorsAdded.clear();
//		for (int i = 0; i < amountOfActors; ++i) {
//			ActorData data = new ActorData(0, 0, 0, 0);
//			data.id = ois.readInt();
//			data.rotation = ois.readFloat();
//			data.x = ois.readFloat();
//			data.y = ois.readFloat();
//			actorsAdded.add(data);
//		}
//	}

}

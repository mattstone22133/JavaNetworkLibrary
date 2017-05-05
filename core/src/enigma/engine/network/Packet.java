package enigma.engine.network;

import java.io.Serializable;

public interface Packet extends Serializable{
	public Packet makeCopy();
}

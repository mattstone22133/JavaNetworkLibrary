package enigma.engine.data.compression;

import java.io.Serializable;

/**
 * The primary purpose for this interface is to ensure that non-serializeable exceptions
 * don't occur internally within the network.
 * @author Matt Stone
 *
 */
public interface DataCompressor extends Serializable{
	public DataCompressor clone();
}

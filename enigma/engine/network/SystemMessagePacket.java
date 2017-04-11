package enigma.engine.network;

public class SystemMessagePacket implements Packet {
	//TODO change these packets to have a single payload 
	//TODO write tests when SystemMessagePacket design is finalized
	private static final long serialVersionUID = 4552713072731934019L;
	private boolean quit = false;
	private char playerID = (char) -1;
	
	@Override
	public Packet makeCopy() {
		SystemMessagePacket copy = new SystemMessagePacket();
		copy.quit = this.quit;
		return copy;
	}
	
	public boolean connetionShouldClose(){
		return quit;
	}

	public void setConnectionShouldClose(boolean value) {
		quit = true;
	}

	public boolean containsPlayerID() {
		return playerID != -1;
	}
	
	public void setPlayerID(Character id){
		this.playerID = id;
	}
	
	
	public char getPlayerID() {
		return playerID;
	}

}

package enigma.engine.network;

public class SystemMessagePacket implements Packet {
	private static final long serialVersionUID = 4552713072731934019L;
	private boolean quit = false;
	
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

}

package enigma.engine.network;

public class FailedToConnect extends Exception{
	private static final long serialVersionUID = 100000L;
	
	public FailedToConnect(){
		super();
	}
	
	public FailedToConnect(String text){
		super(text);
	}
	
}

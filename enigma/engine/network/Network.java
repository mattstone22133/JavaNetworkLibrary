package enigma.engine.network;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * This class wraps the network behavior for the API.
 * 
 * @author Matt Stone
 * @version 1.0
 *
 */
public class Network {
	// false value for isServer means the object is acting as a client
	private boolean isServer = false;
	private Server server;
	private Client client;
	private int port = 25565;
	private String address;
	private long sendDelay;
	private long lastSendInMS = 0;
	public boolean verbose = false;

	/**
	 * Construct a basic network object. The address, port, and send delay need to be specified
	 * before attempting to establish any connections. 
	 */
	public Network() {
		this(null, 25565, 50);
	}

	public Network(String address, int port, long sendDelay) {
		this.setPort(port);
		this.setAddress(address);
		setSendDelay(sendDelay);

	}

	/**
	 * Sets the delay that the send timer will use. The send timer is to prevent a client from
	 * sending too much information too fast and overloading the server. The delay is the minimum
	 * amount of time that occurs between sends.
	 * 
	 * Note, if a client attempts to flood the send by disregarding the timer, the network will
	 * attempt to send the packets. It is up to the user to call sendDelayTimerExpired() to ensure
	 * good network behavior.
	 * 
	 * @param sendDelayMS to wait before allowing another send.
	 */
	public void setSendDelay(long sendDelayMS) {
		sendDelay = sendDelayMS;
	}

	public boolean sendDelayTimerExpired() {
		return (System.currentTimeMillis() - sendDelay > lastSendInMS);
	}

	private void runServer() throws IOException {
		if (server == null) {
			server = new Server(getPort());
		}
		server.run();
		if (verbose) System.out.println("Network: server run started");
	}

	private void runClient() throws FailedToConnect {
		if (client == null) {
			client = new Client();
		}
		client.connect(getAddress(), getPort());
		if (verbose) System.out.println("Network: client run started");
	}

	public void run() throws IOException, FailedToConnect {
		if (isServer) {
			runServer();
		} else {
			runClient();
		}
	}

	public void disconnect() {
		if (isServer) {
			if (server != null) server.disconnect();
		} else {
			if (client != null) client.disconnect();
		}
		if (verbose) System.out.println("Network: disconnect called - may take > 5 seconds");
	}

	public boolean inServerMode() {
		return isServer;
	}

	public boolean inClientMode() {
		return !isServer;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
		if (address == null) {
			try {
				address = InetAddress.getLocalHost().getHostAddress();
			} catch (UnknownHostException e) {
				System.out.println("local address could not be resolved");
			}
		}
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public void queueToSend(Packet packet) {
		if (isRunning()) {
			lastSendInMS = System.currentTimeMillis();
			if (isServer) {
				server.queueToSend(packet);
			} else {
				// client
				client.queueToSend(packet);
			}
		}
	}

	private boolean hasReceivedPacket() {
		if (isRunning()) {
			if (isServer) {
				return server.hasReceivedPacket();
			} else {
				return client.hasReceivedPacket();
			}
		}
		return false;
	}

	/**
	 * Get the next packet that was received.
	 * 
	 * @return the next packet or null if there is no packet.
	 */
	public Packet getNextReceivedPacket() {
		if (hasReceivedPacket()) {
			if (isServer) {
				return server.getNextReceivedPacket();
			} else {
				return client.getNextReceivedPacket();
			}
		}
		return null;
	}

	/**
	 * Returns whether the selected network mode is currently running.
	 * 
	 * @return If server or client is null and is to be used, then this returns false. If the
	 *         internal network object is not null, then it returns the call to isRunning();
	 */
	public boolean isRunning() {
		if (isServer) {
			if (server != null) {
				return server.isRunning();
			}
		} else {
			if (client != null) {
				return client.isRunning();
			}
		}
		return false;
	}

	/**
	 * Get the ID object that the server provided
	 * 
	 * @return an instance of an ID object if the server/client has been granted one or null if the
	 *         server/client hasn't been granted an id OR the id hasn't yet reached the client.
	 */
	public NetworkPlayer getPlayerID() {
		if (isRunning()) {
			if (isServer) {
				return server.getHostPlayerObj();
			} else {
				return client.getPlayerObject();
			}
		}
		return null;
	}

	public void serverMode() {
		isServer = true;
	}

	public void clientMode() {
		isServer = false;
	}
}

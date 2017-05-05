package enigma.engine.network;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Creates objects which encapsulate network behavior. A network object can act as a server or a
 * client. A user does not need to concern with how a connection is set up, only what data needs to
 * be sent.<p><p>
 * 
 * <h3><u>Basic API version 0.8:</u></h3><p>
 * 
 * <strong>1. Setting a network object as a server or client:</strong> A user simply needs to create a network object
 * and call serverMode() or clientMode() to behave as such.<p>
 * 
 * <strong>2. Creating packets to send:</strong> A user needs to create a packet class that extends the Packet.java interface.
 * The Packet interface uses basic java serialization to send data over a network. As long as
 * objects you create support such serialization, then they can be sent in a packet. However, it is
 * probably a good idea to instead define a data containing class that only sends absolutely
 * necessary information over the network. To improve performance even further, the serialization
 * methods can be overloaded to provide specific serialization implementation details in your
 * extended packet class.<p>
 * 
 * <strong>3. Hosting a server:</strong> It is up to the user to define how data is processed, but the network
 * activity of a server is encapsulated in the network object. First, the user needs to set the
 * network object to the serverMode() by calling the serverMode() method. Next, the user needs to
 * start the server running; the user can do this by calling the run() method. If the server fails
 * to start running, an exception will be thrown before the call to run() completes. Note, the run()
 * method is non-blocking and therefore the server may not start running immediately. To determine
 * when the server starts, the user can call isRunning() on the network. When isRunning() returns
 * true, the user can send data over the network.<p>
 * 
 * <strong>4. Connecting as a client:</strong> The user needs to set the Network object to client mode by calling the
 * clientMode() method. This configures the network object to connect to server, rather than host a
 * server. If the user did not set the address and port in the constructor, then the user should set
 * the address of the server and port of the server using the setAddress(String address) and
 * setPort(int port) respectively. After doing so, the user simply needs to call run() and the
 * Network object will attempt to connect to the address on the listening port. If it is
 * successfully, run() will simply return; if it fails, then run() will throw a FailedToConnect()
 * exception. The run() method call is non-blocking, and therefore the Network object may not have
 * connected immediately after run() returns. The user may poll() the network object to determine if
 * it is connected by using the isRunning() method call.<p>
 * 
 * <strong>5. Sending data as a server:</strong> The user can queue packets to send using the queueToSend(Packet
 * packet) method. This copies the packet and broadcasts it to ALL clients currently connected to
 * the server. Currently, there is no method for sending data to an individual user. If a user wants
 * to send data to a specific client, then the user should use the provided Network ID system
 * discussed later.<p>
 * 
 * <strong>6. Sending data as a client:</strong> The user queues packets in the same way as the server -- ie by
 * calling the queueToSend(Packet packet) method. However, when a client send data, only the server
 * receives the packet. It is up t the user to write code that makes the server extract data,
 * process it, update its game world, and then broadcast the new world to all clients.<p>
 * 
 * <strong>7. Sending data at an appropriate frequency:</strong> So, how often should you send data? That is up to
 * you but the network object provides some assistance with tracking when to send data. The
 * setSendDataDelay(long delayMS) allows the user to define an amount of time between each packet
 * send, ie the sendDelay. This delay IS NOT enforced by the server. Instead, it is up to the user
 * to poll the network object if the delay has expired with sendDelayTimerExpired() which returns
 * true when the enough time has passed for the user to send data. Since this timer is not enforced,
 * the user can send multiple packets back to back if needed - each of which will reset the timer
 * and send immediately.<p>
 * 
 * <strong>8. Receiving Data as a Server or a Client:</strong> The network object hasReceivedPacket() method returns
 * true when it has buffered a packet that the user needs to extract. When a user is ready to
 * receive a packet, it should call this method to determine if the server has a packet to receive.
 * The user can retrieve a received packet by calling getNextPacket() which returns a raw packet
 * type. The user should cast this packet to the defined packet class the user created. If the user
 * defined different types of packets to send, then the user is responsible for type checking the
 * packets before casting by using the instanceof operator. Alternatively to calling
 * hasReceivedPacket(), the user can simply call getNextPacket() which returns null when there is no
 * buffered packet. By default, the network transmits data over a TCP connection, therefore any
 * packet sent will be received at some point in time and the user should not concern their self
 * with sending duplicate packets.<p>
 * 
 * <strong>9. Disconnecting clients and server:</strong> The user simply needs to call disconnect() on the network
 * object. currently this method DOEs block, but will probably be changed in the future. please
 * consult the javadoc for the method for updates on behavior. Disconnecting a client simply leaves
 * the server. However, disconnecting a server network object shuts down the server and deallocates
 * buffers. A disconnect message is sent to all clients that were connected to the server, which
 * causes clients to be disconnected.<p>
 * 
 * 
 * @author Matt Stone
 * @version 0.8
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
	 * 
	 * Default address is null. Default port is 25565. Default wait delay is 50ms
	 */
	public Network() {
		this(null, 25565, 50);
	}

	/**
	 * Construct a network object with the specified address, port, and time to wait before sending.
	 * 
	 * @param address - The address to which the network object should connect. If the network is
	 *            purely a server acting object, this field is irrelevant and may be left as null.
	 *            Generally address are decimal dot notation. e.g. "192.169.1.1"
	 * @param port - The port number to which the network should connect. If the network object is
	 *            acting as a server, then this port specifics the port on which clients connect.
	 *            e.g. 25565.
	 * @param sendDelay - Represents a number of milliseconds to wait between each send. This wait
	 *            number is not forced by the network object. The user can queue packets to send as
	 *            fast as desired. However, this number does set an internal timer that the user can
	 *            poll to determine if it should generate a packet to send over the network. The
	 *            timer can be polled using sendDelayExpired(). It is up to the user to adhere to
	 *            this method call to determine if a packet should be sent. If the user simply
	 *            queues packets without consulting the timer, the network will simply send packets
	 *            regardless of the state of the timer.
	 */
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

	/**
	 * Asks the network object if enough time has passed since the last packet transmission to send
	 * another packet. The user may specify a time delay between sending two packets in setSendDelay
	 * or in a constructor. This method calls returns true if the specified amount of time has
	 * passed since the last sent packet. This method returns false if there has yet been enough
	 * time to send the packet according to the delay.
	 * 
	 * @return true if enough time since the last packet was sent has been sent, false otherwise.
	 */
	public boolean sendDelayTimerExpired() {
		return (System.currentTimeMillis() - sendDelay > lastSendInMS);
	}

	/**
	 * Tells the network object to act as a server and start running a server.
	 * 
	 * @throws IOException may be thrown by the server object.
	 */
	private void runServer() throws IOException {
		if (server == null) {
			server = new Server(getPort());
		}
		server.run();
		if (verbose) System.out.println("Network: server run started");
	}

	/**
	 * Tells the network object to act as a client and attempts to connect to the specified address
	 * and port.
	 * 
	 * @throws FailedToConnect thrown if client cannot connect to the specified address and port.
	 */
	private void runClient() throws FailedToConnect {
		if (client == null) {
			client = new Client();
		}
		client.connect(getAddress(), getPort());
		if (verbose) System.out.println("Network: client run started");
	}

	/**
	 * Runs the network object. If the network object is acting as a server, it runs a server. If
	 * the network object is set to act as a client, it attempts to connect the set address and
	 * port.
	 * 
	 * @throws IOException thrown if the server cannot start on the specified port.
	 * @throws FailedToConnect thrown if the client cannot connect to the specified address and
	 *             port.
	 */
	public void run() throws IOException, FailedToConnect {
		if (isServer) {
			runServer();
		} else {
			runClient();
		}
	}

	/**
	 * Disconnects the network device from running. If server, shuts down listening and the server
	 * resources. if client, simply disconnects from the server.
	 * 
	 * @warning method call may block
	 */
	public void disconnect() {
		if (isServer) {
			if (server != null) server.disconnect();
		} else {
			if (client != null) client.disconnect();
		}
		if (verbose) System.out.println("Network: disconnect called - may take > 5 seconds");
	}

	/**
	 * @return true if the network object is configured to act as a server, false otherwise.
	 */
	public boolean inServerMode() {
		return isServer;
	}

	/**
	 * @return return true if the network object is configured to act as a client, false otherwise.
	 */
	public boolean inClientMode() {
		return !isServer;
	}

	/**
	 * @return a string representation of the object.
	 */
	public String getAddress() {
		return address;
	}

	/**
	 * Set the target connecting address to the specified argument. e.g."192.168.1.1"
	 * 
	 * @param address - address to connect to. e.g. "192.168.1.1"
	 */
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

	/**
	 * @return the specified port on which to connect or listen for connections.
	 */
	public int getPort() {
		return port;
	}

	/**
	 * @param port the port on which to connect or listen for connections.
	 */
	public void setPort(int port) {
		this.port = port;
	}

	/**
	 * Queues a packet to be sent over the network. This method call behaves the same for client and
	 * server. If the network is acting is acting as a client and queues a packet, it will be sent
	 * to the server. If the network is acting as a server it will broadcast the packet to ALL
	 * connected clients.
	 * 
	 * @param packet - the packet to be sent over the network.
	 */
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
	 * Get the next packet that was received in the order in which it was received.
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
	 * Returns whether the selected network mode is currently running. If the network object is
	 * acting as a server, this mean the server is currently listening and processing client data.
	 * if the network object is acting as a client, this means that the client is connected to a
	 * server.
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

	/**
	 * Set the network object to act as a server.
	 */
	public void serverMode() {
		isServer = true;
	}

	/**
	 * Set the network object to act as a client.
	 */
	public void clientMode() {
		isServer = false;
	}
}

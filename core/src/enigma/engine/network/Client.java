package enigma.engine.network;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * This class handles the client aspect of the application.
 * 
 * @author Matt Stone
 * @version 1.0
 *
 */
public class Client {
	private String address;
	private int port;
	private Socket TCPSocket;
	private ObjectInputStream inStream;
	private ObjectOutputStream outStream;
	private Thread sendingThread;
	private Thread receivingThread;
	private Thread sendIntermediateStageThread;
	private boolean sendBufferLock = false;

	private ConcurrentLinkedQueue<Packet> sendBuffer = new ConcurrentLinkedQueue<Packet>();
	private ConcurrentLinkedQueue<Packet> receiveBuffer = new ConcurrentLinkedQueue<Packet>();
	private ConcurrentLinkedQueue<Packet> stageForSendBuffer = new ConcurrentLinkedQueue<Packet>();

	private volatile boolean threadsShouldLive = true;
	private int blockingTimeoutMS = 1000;
	public boolean verbose = false;

	private NetworkPlayer localPlayerInstance = null;

	// reconnection
	int sendFailures = 0;
	int receiveFailures = 0;
	private int sendFailureThreshold = 100;
	private int receiveFailureThreshold = 100;

	public Client() {
	}

	public void connect(String address, int dstPort) throws FailedToConnect {
		threadsShouldLive = true;

		if (TCPSocket != null && !TCPSocket.isClosed()) {
			throw new IllegalStateException("Client: Socket Not Closed");
		}

		// Connect the data socket
		if (!connectDataSocket(address, dstPort)) {
			throw new FailedToConnect("Could not establish socket");
		}

		// Set up the data Streams
		if (!setupStreams()) {
			closeTCPSocket();
			throw new FailedToConnect("Could not set up streams");
		}

		System.out.println("Client: Connected to server.");

		// start up sending thread @formatter:off
		sendingThread = new Thread(new Runnable() {
			public void run() {
				sendingThreadMethod();
			}
		});
		sendingThread.start();

		// start up receiving thread
		receivingThread = new Thread(new Runnable() {
			public void run() {
				receivingThreadMethod();
			}
		});
		receivingThread.start();
		
		//staging thread to prevent user from experiencing blocking when queuing send
		sendIntermediateStageThread = new Thread(new Runnable() {
			 public void run() {
				 loadStagedPacketToOutGoing();
		 	}
		 });
		sendIntermediateStageThread.start();

		// connection successful, update these fields @formatter:on
		this.address = address;
		this.port = dstPort;
	}

	private boolean setupStreams() {
		// set up streams
		try {
			outStream = new ObjectOutputStream(TCPSocket.getOutputStream());
			inStream = new ObjectInputStream(TCPSocket.getInputStream());
			outStream.flush();
		} catch (IOException e) {
			return false;
		}

		return true;
	}

	private boolean connectDataSocket(String address, int port) throws FailedToConnect {
		boolean ret = true;
		try {
			TCPSocket = new Socket(address, port);
			TCPSocket.setSoTimeout(blockingTimeoutMS);
		} catch (UnknownHostException e2) {
			ret = false;
			throw new FailedToConnect();
		} catch (IOException e) {
			ret = false;
		}

		return ret;
	}

	private boolean closeTCPSocket() {
		// attempt to close at most 5 times!
		for (int i = 0; i < 5; ++i) {
			try {
				TCPSocket.close();
				return true;
			} catch (IOException e) {
			}
		}
		return false;
	}

	protected void receivingThreadMethod() {
		while (threadsShouldLive) {
			try {
				// Packet inbound = (Packet) inStream.readObject(); //saves reference to packet
				Packet inbound = (Packet) inStream.readUnshared(); // doesn't save ref to packet

				if (!checkForSystemMessage(inbound) && inbound != null) {
					// not a system message, add the packet to buffer
					receiveBuffer.add(inbound);
				} else {
					// avoid busy waiting
					// sleepThread(1); //TODO (note)this cause wait if system msg!
				}

			} catch (ClassNotFoundException e) {
				e.printStackTrace();
				return;
			} catch (SocketTimeoutException e) {
				// Do nothing, but prevent this from being caught in IOException
			} catch (IOException e) {
				receiveFailures++;
				if (receiveFailures > receiveFailureThreshold) {
					reestablishConnection();
					receiveFailures = 0;
				}
			}
		}
	}

	private void sendingThreadMethod() {
		// Only this method should ever do peeks and polls from the sendBuffer;
		while (threadsShouldLive) {
			Packet toSend = sendBuffer.peek();
			if (toSend != null) {
				try {
					send(toSend);
					sendBuffer.poll();
				} catch (IOException e) {
					// failed to send - do not remove packet from queue
					sendFailures++;
					if (sendFailures > sendFailureThreshold) {
						reestablishConnection();
						sendFailures = 0;
					}
				}
			} else {
				// avoid busy waiting
				sleepThread(1);
			}
		}
	}

	private boolean checkForSystemMessage(Packet inbound) {
		if (inbound instanceof SystemMessagePacket) {
			SystemMessagePacket systemMessage = (SystemMessagePacket) inbound;
			handleSystemMessage(systemMessage);
			return true;
		}
		// was not a system message.
		return false;
	}

	private void handleSystemMessage(SystemMessagePacket systemMessage) {
		// Check if the server is telling the client to disconnect (security vulnerability?)
		// Implement a hash code check to verify validity of server if this becomes a problem
		if (systemMessage.connetionShouldClose()) {
			// if TCP protocol is flawed (which I have read may be case) then some redundancy might
			// be needed in order to ensure a disconnect was actually sent.
			// for example, maybe require a counter of 3 disconnect packets.
			if (verbose) System.out.println("received disconnect syste message");
			disconnect(false);
		} else {
			if (systemMessage.containsPlayerID()) {
				localPlayerInstance = new NetworkPlayer(systemMessage.getPlayerID());
			}
		}
	}

	private void reestablishConnection() {
		if (TCPSocket == null || !TCPSocket.isConnected() || TCPSocket.isClosed()) {
			// stop receiving and sending threads
			stopThreads();
			threadsShouldLive = true;

			TCPSocket = null;

			// attempt a reconnect until user decides to disconnect (which sets
			// threadsShouldLive)
			while (threadsShouldLive && !isRunning()) {
				try {
					connect(address, port);
				} catch (FailedToConnect e) {
				}
			}
		}
	}

	private void stopThreads() {
		threadsShouldLive = false;
		// TODO kill receive stream to interrupt with thread with IO exception,
		// which will check threadsShouldLive
		while (receivingThread != null && receivingThread.isAlive()) {
			sleepThread(100);
		}
		while (sendingThread != null && sendingThread.isAlive()) {
			sleepThread(100);
		}
		while (sendIntermediateStageThread != null && sendIntermediateStageThread.isAlive()) {
			sleepThread(100);
		}
	}

	private void send(Packet packet) throws IOException {
		if (TCPSocket == null || TCPSocket.isClosed()) {
			throw new IllegalStateException("send() called when socket was not set up");
		}
		// outStream.writeObject(packet); //saves a reference to every packet written (not good)
		outStream.writeUnshared(packet); // doesn't save references

		outStream.reset(); // clear references

	}

	private boolean disconnect(boolean sendDisconnectMessage) {
		// send a disconnect system message to server
		if (sendDisconnectMessage) {
			sendDisconnectSystemMessage();
		}
		// kill threads after the message to system has been sent
		threadsShouldLive = false;
		localPlayerInstance = null;

		try {
			if (TCPSocket != null) {
				TCPSocket.close();
			}
			if (verbose) {
				System.out.println("Client: disconnecting initiated");
			}
			return true;
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("Client: disconnecting failed");
			return false;
		}
	}

	/**
	 * A non-blocking disconnect.
	 */
	public void disconnect() {
		if (!isRunning()) return;

		// launch disconnect in new thread to prevent blocking for user.
		Thread disconnectThread = new Thread(new Runnable() {
			public void run() {
				disconnect(true);
			}
		});
		disconnectThread.start();
	}

	/**
	 * Sends a system message indicating to the server that the client will disconnect.
	 * 
	 * @SideEffect the method locks the sendBuffer until the method is complete.
	 * @SideEffect the method blocks for at maximum 5 seconds
	 */
	private void sendDisconnectSystemMessage() {
		// lock the send buffer
		sendBufferLock = true;

		// clear the buffer so that the system message can be placed at the start
		sendBuffer.clear();

		// create a disconnect system message to send
		SystemMessagePacket closeMessage = new SystemMessagePacket();
		closeMessage.setConnectionShouldClose(true);
		sendBuffer.add(closeMessage);

		// loop until message sent (send buffer == 0; ie null head) or 5 seconds is up
		long start = System.currentTimeMillis();
		long delayMS = 5000; // if changed update java doc TODO - this will block user!
		while (sendBuffer.peek() != null && System.currentTimeMillis() - start < delayMS) {
			sleepThread(1);
		}

		// unlock the send buffer
		sendBufferLock = false;
	}

	// removed because isRunning does same job
	// public boolean isConnected() {
	// boolean tcpIsConnected = false;
	// if (TCPSocket != null) {
	// tcpIsConnected = TCPSocket.isClosed();
	// }
	// return tcpIsConnected && inStream != null && outStream != null;
	// }

	/**
	 * Prepare a packet to be sent next. Once a packet is queued, it cannot be removed.
	 * 
	 * @param packet
	 */
	public void queueToSend(Packet packet) {
		final Packet copy = packet.makeCopy();

		// addition staging buffer to reduce chance of server blocking user
		stageForSendBuffer.add(copy);

	}

	protected void loadStagedPacketToOutGoing() {
		while (threadsShouldLive) {
			// use "if" instead of "while" so that threadsShouldLive checked every loading iter
			if (stageForSendBuffer.peek() != null && !sendBufferLock) {
				Packet toSend = stageForSendBuffer.poll();
				sendBuffer.add(toSend);
			} else {
				// stop busy waiting
				 sleepThread(1);
			}
		}
	}

	private void sleepThread(long ms) {
		try {
			Thread.sleep(ms);
		} catch (InterruptedException e) {
			System.out.println("SLEEP FAIL IN CLIENT");
		}
	}

	public boolean isRunning() {
		boolean ret = false;

		ret |= threadsShouldLive;
		if (receivingThread != null) {
			ret |= receivingThread.isAlive();
		}
		if (sendingThread != null) {
			ret |= sendingThread.isAlive();
		}
		if (TCPSocket != null) {
			ret |= !TCPSocket.isClosed();
		}

		return ret;
	}

	public boolean hasReceivedPacket() {
		return receiveBuffer.peek() != null;
	}

	public Packet getNextReceivedPacket() {
		return receiveBuffer.poll();
	}

	public NetworkPlayer getPlayerObject() {
		return this.localPlayerInstance;
	}

}

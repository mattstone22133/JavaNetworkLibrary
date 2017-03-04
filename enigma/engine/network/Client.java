package enigma.engine.network;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
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
public class Client extends Network {
	private String address;
	private int port;
	private Socket TCPSocket;
	private ObjectInputStream inStream;
	private ObjectOutputStream outStream;
	private Thread sendingThread;
	private Thread receivingThread;

	private ConcurrentLinkedQueue<Packet> queuedPackets = new ConcurrentLinkedQueue<Packet>();
	private ConcurrentLinkedQueue<Packet> receiveBuffer = new ConcurrentLinkedQueue<Packet>();
	
	private boolean threadsShouldLive = true;
	private int blockingTimeoutMS = 5000;
	private int sendSleepDelay;

	// reconnection
	int sendFailures = 0;
	int receiveFailures = 0;
	private int sendFailureThreshold = 100;
	private int receiveFailureThreshold = 100;

	public Client() {
	}

	public void connect(String address, int port) throws FailedToConnect {

		if (TCPSocket != null && !TCPSocket.isClosed()) {
			throw new IllegalStateException("Client: Socket Not Closed");
		}

		// Try with resources automatically closes resources if any should fail to be initialized
		if (!connectSocket(address, port)) {
			throw new FailedToConnect("Could not establish socket");
		}

		if (!setupStreams()) {
			closeTCPSocket();
			throw new FailedToConnect("Could not set up streams");
		}

		System.out.println("Client: Connected to server.");

		//start up sending thread @formatter:off
		sendingThread =  new Thread(new Runnable(){public void run(){sendingThreadMethod();}});
			
		//start up receiving thread
		receivingThread =  new Thread(new Runnable(){public void run(){receivingThreadMethod();}});

		//connection successful, update these fields @formatter:on
		this.address = address;
		this.port = port;

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

	private boolean connectSocket(String address, int port) throws FailedToConnect {
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
				Packet inbound = (Packet) inStream.readObject();
				receiveBuffer.add(inbound);
				
			} catch (ClassNotFoundException e){
				e.printStackTrace();
				return;
			} catch (SocketTimeoutException e){
				//Do nothing, but prevent this from being caught in IOException
			} catch (IOException e) {
				receiveFailures++;
				if(receiveFailures > receiveFailureThreshold){
					reestablishConnection();
					receiveFailures = 0;
				}
			}
			
			// pause thread
			if (sendSleepDelay > 0) {
				sleepThread(sendSleepDelay);
			}
		}
	}

	
	public void sendingThreadMethod() {
		while (threadsShouldLive) {
			if (queuedPackets.size() > 0) {
				Packet toSend = queuedPackets.peek();
				try {
					send(toSend);
					queuedPackets.poll();
				} catch (IOException e) {
					// failed to send - do not remove packet from queue
					sendFailures++;
					if (sendFailures > sendFailureThreshold) {
						reestablishConnection();
						sendFailures = 0;
					}
				}

				// pause thread
				if (sendSleepDelay > 0) {
					sleepThread(sendSleepDelay);
				}
			}

		}
	}

	private void reestablishConnection() {
		if (TCPSocket == null || !TCPSocket.isConnected() || TCPSocket.isClosed()) {
			// stop receiving and sending threads
			stopThreads();
			threadsShouldLive = true;

			TCPSocket = null;

			// attempt a reconnect until user decides to disconnect (which sets threadsShouldLive)
			while (threadsShouldLive && !isConnected()) {
				try {
					connect(address, port);
				} catch (FailedToConnect e) {
				}
			}
		}
	}

	private void stopThreads() {
		threadsShouldLive = false;
		//TODO kill receive stream to interrupt with thread with IO exception, which will check threadsShouldLive
		while (receivingThread != null && receivingThread.isAlive()) {
			sleepThread(1000);
		}
		while (sendingThread != null && sendingThread.isAlive()) {
			sleepThread(1000);
		}
	}

	private void send(Packet packet) throws IOException {
		if (TCPSocket == null || TCPSocket.isClosed()) {
			throw new IllegalStateException("send() called when socket was not set up");
		}
		outStream.writeObject(packet);
	}

	public void disconnect() {
		try {
			if (TCPSocket != null) {
				TCPSocket.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public boolean isConnected() {
		return TCPSocket.isConnected() && inStream != null && outStream != null;
	}

	/**
	 * Prepare a packet to be sent next. Once a packet is queued, it cannot be removed.
	 * 
	 * @param packet
	 */
	public void queueToSend(Packet packet) {
		queuedPackets.add(packet.makeCopy());
	}

	private void sleepThread(int ms) {
		try {
			Thread.sleep(sendSleepDelay);
		} catch (InterruptedException e) {
		}
	}

	public static void main(String[] args) throws UnknownHostException {
		Client client = new Client();
		try {
			client.connect(InetAddress.getLocalHost().getHostAddress(), 25565);
		} catch (FailedToConnect e1) {
			System.out.println("Could not connect");
			return;
		}
		TestPacket test = new TestPacket(12, 0, 0, 45);
		while (true) {
			try {
				Thread.sleep(1000);
				client.send(test);

			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}

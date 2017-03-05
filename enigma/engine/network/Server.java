package enigma.engine.network;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * This class handles the server aspect of the application.
 * 
 * @author Matt Stone
 * @version 1.0
 *
 */
public class Server extends Network {
	private ServerSocket listener;
	private Thread listeningThread;
	private ConcurrentHashMap<Integer, Socket> sockets = new ConcurrentHashMap<Integer, Socket>();
	private ConcurrentHashMap<Socket, Thread> outThreads = new ConcurrentHashMap<Socket, Thread>();
	private ConcurrentHashMap<Socket, Thread> inThreads = new ConcurrentHashMap<Socket, Thread>();
	private ConcurrentHashMap<Socket, ObjectInputStream> inStreams = new ConcurrentHashMap<Socket, ObjectInputStream>();
	private ConcurrentHashMap<Socket, ObjectOutputStream> outStreams = new ConcurrentHashMap<Socket, ObjectOutputStream>();
	private ConcurrentHashMap<Socket, ConcurrentLinkedQueue<Packet>> sendBuffer = new ConcurrentHashMap<>();
	private ConcurrentHashMap<Socket, ConcurrentLinkedQueue<Packet>> receiveBuffers = new ConcurrentHashMap<>();
	private ConcurrentHashMap<Socket, Integer> receiveFailures = new ConcurrentHashMap<Socket, Integer>();
	private ConcurrentHashMap<Socket, Integer> sendFailures = new ConcurrentHashMap<Socket, Integer>();

	private int port;
	private short maxPlayers = 8;
	private int blockingTimeoutMS = 5000;
	private boolean ready = false;
	private boolean closeAllThreads = false;
	private boolean threadsShouldLive = true;
	private int receiveFailureThreshold = 100;
	private int sendFailureThreshold = 100;
	private boolean isRunning;
	private boolean hasReceived;
	/** provides a storage place to lump all received packets for user processing */
	private LinkedList<Packet> stagedPackets = new LinkedList<Packet>();

	public Server(int port) {
		this.port = port;
		ready = true;

	}

	/**
	 * Start running the server. This method is intended to be called in another thread. Throws
	 * IOException if failure to initiate the listening socket.
	 * 
	 * @throws IOException failure to initiate the listening socket.
	 */
	public void run() throws IOException {
		threadsShouldLive = true;
		listener = new ServerSocket(port);
		isRunning = true;
		while (threadsShouldLive) {
			if (!ready) {
				sleepForMS(1000L);
			} else {
				//start a continuously listening thread //@formatter:off
				listeningThread = new Thread(new Runnable() {public void run() {listen();}});
				listeningThread.start();

				// continually sleep until the server is to be restarted 
				while (!closeAllThreads) {sleepForMS(1000L);}
				
				//server is shutting down, wait until all threads have shut down
				while (!allThreadsClosed()) {sleepForMS(1000L);}
				
				//all threads are now dead, clean up variables and hashmaps
				resetAllStateVariables();
				
				//@formatter:on
			}
		}
	}

	private boolean allThreadsClosed() {
		boolean oneIsAlive = false;
		oneIsAlive |= listeningThread.isAlive();
		for (Socket socket : sockets.values()) {
			oneIsAlive |= outThreads.get(socket).isAlive();
			oneIsAlive |= inThreads.get(socket).isAlive();
		}

		// return true only when no thread was detected to be alive!
		return !oneIsAlive;
	}

	private void listen() {
		while (threadsShouldLive) {
			if (sockets.size() < maxPlayers - 1) {
				try {
					// TODO concern: add timeout to listener so that thread may shut down?
					listener.setSoTimeout(5000);
					final Socket newSocket = listener.accept();

					newSocket.setSoTimeout(blockingTimeoutMS);

					// save socket reference into hashmap
					sockets.put(newSocket.hashCode(), newSocket);

					// init streams
					inStreams.put(newSocket, new ObjectInputStream(newSocket.getInputStream()));
					outStreams.put(newSocket, new ObjectOutputStream(newSocket.getOutputStream()));

					// init buffers
					sendBuffer.put(newSocket, new ConcurrentLinkedQueue<Packet>());
					receiveBuffers.put(newSocket, new ConcurrentLinkedQueue<Packet>());

					// init failure counts
					sendFailures.put(newSocket, 0);
					receiveFailures.put(newSocket, 0);

					//launch a thread to receive from this socket @formatter:off
					Thread sendThread = new Thread(new Runnable() {public void run(){send(newSocket);}});
					outThreads.put(newSocket, sendThread);
					sendThread.start();
					
					//launch a thread to broadcast from this socket
					Thread receiveThread = new Thread(new Runnable(){public void run(){receive(newSocket);}});
					inThreads.put(newSocket, receiveThread);
					receiveThread.start();
					
					//@formatter:on
				} catch (SocketTimeoutException e) {
					// timeout event is normal
				} catch (IOException e) {
					System.out.println("Failed to accept socket - IO Exception");
					e.printStackTrace();
				}
			}
		}
	}

	private void receive(Socket fromSocket) {
		while (threadsShouldLive) {
			try {
				ObjectInputStream inStream = inStreams.get(fromSocket);
				Packet inbound = (Packet) inStream.readObject();
				if (inbound != null) {
					receiveBuffers.get(fromSocket).add(inbound);
					hasReceived = true;
				}

			} catch (ClassNotFoundException e) {
				// cast error kill thread and pass exception
				e.printStackTrace();
				dropConnection(fromSocket);
				return;
			} catch (SocketTimeoutException e) {
				// Do nothing, but prevent this from being caught in IOException
			} catch (IOException e) {
				int failures = receiveFailures.get(fromSocket);
				failures++;
				receiveFailures.put(fromSocket, failures);
				if (failures > receiveFailureThreshold) {
					dropConnection(fromSocket);
				}
			}
		}
	}

	private void send(Socket toSocket) {
		while (threadsShouldLive) {
			try {
				// peek what is to be sent, rather than removing from queue
				Packet toSend = sendBuffer.get(toSocket).peek();
				if (toSend != null) {
					outStreams.get(toSocket).writeObject(toSend);

					// remove packet from buffer because exception was not thrown
					sendBuffer.get(toSocket).poll();
				}

			} catch (IOException e) {
				// socket should not timeout for sends unless there's a problem
				// thus socket timeouts are in this catch.
				int failures = sendFailures.get(toSocket);
				failures++;
				sendFailures.put(toSocket, failures);
				if (failures > sendFailureThreshold) {
					dropConnection(toSocket);
				}
			}
		}
	}

	private void dropConnection(Socket socket) {
		// TODO create this feature
		System.out.println("Dropping connection" + socket.toString());
		System.out.println("NOT IMPLEMENTED");
	}

	private void resetAllStateVariables() {
		// TODO still need to clean up hash maps
		threadsShouldLive = true;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public void prepareServerToStart() throws IOException {
		listener = new ServerSocket(port);

		ready = true;
	}

	private void sleepForMS(long ms) {
		// formatter:off
		try {
			Thread.sleep(ms);
		} catch (InterruptedException e) {
		}
		// formatter:on
	}

	private void disconnect() {
		threadsShouldLive = false;
		for (Socket socket : sockets.values()) {
			dropConnection(socket);
		}
	}

	public boolean isRunning() {
		return isRunning;
	}

	public boolean hasReceivedPacket() {
		return hasReceived;
		// for(ConcurrentLinkedQueue<Packet> receiveBuffer : receiveBuffers.values()){
		// if(receiveBuffer.size() > 0){
		// return true;
		// }
		// }
		// return false;
	}

	private ArrayList<Socket> keys = new ArrayList<Socket>();
	private HashMap<Socket, Integer> queueSizes = new HashMap<Socket, Integer>();

	/**
	 * This method loads all receive buffers into a single collection. It is designed so that
	 * packets received will be equally distributed.
	 * 
	 * If collections were to be iterated while still being updated, then sockets that were accessed
	 * later may have newer packets than earlier accessed sockets.
	 * 
	 * This method will not stage more packets until
	 */
	public void stageReceivedPacketsForRemoval() {
		if (stagedPackets.size() > 0) {
			return;
		}
		keys.clear();
		queueSizes.clear();

		// gather sizes before any processing, (so queues need not be frozen)
		Enumeration<Socket> enumKeys = receiveBuffers.keys();
		for (; enumKeys.hasMoreElements();) {
			keys.add(enumKeys.nextElement());
		}
		for (Socket socket : keys) {
			queueSizes.put(socket, receiveBuffers.get(socket).size());
		}
		// sizes recorded; only polling stage #packets of those sizes
		for (Socket socket : keys) {
			int size = queueSizes.get(socket);
			ConcurrentLinkedQueue<Packet> buffer = receiveBuffers.get(socket);
			for (int i = 0; i < size; ++i) {
				// TODO Concern : what if buffer is dumped? (nullptr).
				// Solutions: create lock specifically for buffer dumping?
				// OR - Maybe require dumping to dump staged packets?
				stagedPackets.add(buffer.poll());
			}
		}
	}

	public boolean hasStagedPackets() {
		return stagedPackets.size() > 0;
	}

	public Packet getNextStagedPacket() {
		return stagedPackets.poll();
	}

	public static void main(String[] args) throws UnknownHostException, InterruptedException {
		final Server server = new Server(25565);

		// Start the server running
		new Thread(new Runnable() {
			public void run() {
				System.out.println("Starting Server Up");
				try {
					server.run();
				} catch (IOException e) {
					System.out.println("Failed to start server listening for connections");
					e.printStackTrace();
				}
			}
		}).start();
		
		//Creating a delay before starting the un-packaging.
		Thread.sleep(1000);
			
	
		// Start a thread to print messages from the server (independent of this thread)
		new Thread(new Runnable() {
			public void run() {
				System.out.println("Starting staging and unpackaging phases");
				while (server.isRunning()) {
					if (server.hasReceivedPacket()) {
						server.stageReceivedPacketsForRemoval();
						while (server.hasStagedPackets()) {
							DemoConcretePacket packet = (DemoConcretePacket) server.getNextStagedPacket();
							// here is where you would use your defined packet to extra data
							packet.printData();
						}
					}
				}
			}
		}).start();

		Thread.sleep(1000);
		System.out.println("Close the server by pressing enter");
		Scanner kb = new Scanner(System.in);
		kb.nextLine();
		kb.close();
		server.disconnect();
	}

}

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
public class Server {
	private ServerSocket listener;
	private Thread listeningThread;
	private Thread socketValidationThread;
	private Thread systemMsgThread;
	private ConcurrentHashMap<Integer, Socket> sockets = new ConcurrentHashMap<Integer, Socket>();
	private volatile int activeSockets = 0; // This value has potential to be concurrent bottleneck
	private ConcurrentHashMap<Socket, Thread> outThreads = new ConcurrentHashMap<Socket, Thread>();
	private ConcurrentHashMap<Socket, Thread> inThreads = new ConcurrentHashMap<Socket, Thread>();
	private Thread stagingSendThread;
	private ConcurrentHashMap<Socket, ObjectInputStream> inStreams = new ConcurrentHashMap<Socket, ObjectInputStream>();
	private ConcurrentHashMap<Socket, ObjectOutputStream> outStreams = new ConcurrentHashMap<Socket, ObjectOutputStream>();
	private ConcurrentHashMap<Socket, ConcurrentLinkedQueue<Packet>> sendBuffers = new ConcurrentHashMap<>();
	private ConcurrentHashMap<Socket, ConcurrentLinkedQueue<Packet>> receiveBuffers = new ConcurrentHashMap<>();
	private ConcurrentHashMap<Socket, Integer> receiveFailures = new ConcurrentHashMap<Socket, Integer>();
	private ConcurrentHashMap<Socket, Integer> sendFailures = new ConcurrentHashMap<Socket, Integer>();
	private ConcurrentHashMap<Socket, Boolean> threadShouldLive = new ConcurrentHashMap<Socket, Boolean>();
	private ConcurrentHashMap<Socket, Character> ID = new ConcurrentHashMap<Socket, Character>();
	private ConcurrentHashMap<ConcurrentLinkedQueue<Packet>, Boolean> sendBufferLocks = new ConcurrentHashMap<ConcurrentLinkedQueue<Packet>, Boolean>();
	private ConcurrentLinkedQueue<SocketMessagePair> socketsForSystemToDrop = new ConcurrentLinkedQueue<SocketMessagePair>();
	private IDManager idManager;
	private Character hostID = null;
	private Character nextID = null;

	private int port;
	private short maxPlayers = 8;
	private int blockingTimeoutMS = 1000;
	private long socketAliveCheckTimeoutMS = 30000;

	private volatile boolean threadsShouldLive = true;
	private volatile boolean listenForceShutdown = false;
	// private int receiveFailureThreshold = 100;
	private int sendFailureThreshold = 100;
	private long failureSleepMSTime = 50;
	private boolean isRunning;
	private boolean hasReceived;
	/**
	 * provides a storage place to lump all received packets for user processing. Non-concurrency is
	 * intentional.
	 * 
	 */
	private LinkedList<Packet> stagedReceivePackets = new LinkedList<Packet>();
	private ConcurrentLinkedQueue<Packet> stagedSendPackets = new ConcurrentLinkedQueue<Packet>();
	private boolean pingSocketsPeriodically = false;

	public Server(int port) {
		this(port, (short) 8, true);
	}

	public Server(int port, short maxPlayers, boolean createPlayerForHost) {
		this.port = port;
		setMaxPlayers(maxPlayers);
		idManager = new IDManager(maxPlayers);
		if (createPlayerForHost) {
			hostID = idManager.getReservedIDAndRemoveFromIDPool();
		}
		// ready = true; //TODO remove this if decide against this logic approach
	}

	private void setMaxPlayers(short newMax) {
		if (newMax <= 2) {
			newMax = 2;
		}
		this.maxPlayers = newMax;
	}

	/**
	 * Start the server running. Uses the listening port that has been set through constructor or
	 * setter. If the server is already running, this method simply returns.
	 * 
	 * @throws IOException if server can not start a socket.
	 */
	public void run() throws IOException {
		if (isRunning()) {
			System.out.println("Server already running");
			return;
		}

		threadsShouldLive = true;
		listener = new ServerSocket(port); // throws IOException
		listener.setSoTimeout(blockingTimeoutMS); // throws IOException

		// start a continuously listening thread //@formatter:off
		listeningThread = new Thread(new Runnable() {
			public void run() {
				listen();
			}
		});
		listeningThread.start();

		socketValidationThread = new Thread(new Runnable() {
			public void run() {
				validateSocketsAreAlive();
			}
		});
		socketValidationThread.start();
		
		stagingSendThread = new Thread(new Runnable() {
			public void run() {
				loadPacketIntoAllOutgoingBuffers();
			}
		});
		stagingSendThread.start();
		
		systemMsgThread = new Thread(new Runnable() {
			public void run() {
				SystemMessageHandlerThreadMethod();
			}
		});
		systemMsgThread.start();
		
		// @formatter:on
		isRunning = true;
	}

	private void listen() {
		while (threadsShouldLive && !listener.isClosed()) {
			if (nextID == null) {
				// prepare the ID for the next connection
				nextID = idManager.getReservedIDAndRemoveFromIDPool();
			}

			if (activeSockets < maxPlayers - 1 && nextID != null) {
				try {
					// listen for socket - timeout exception will occur if no connection (will loop
					// back)
					final Socket newSocket = listener.accept();

					// conduct all activity that will cause exceptions, before adding to hashmaps
					newSocket.setSoTimeout(blockingTimeoutMS);

					// init streams
					ObjectInputStream objInStream = new ObjectInputStream(newSocket.getInputStream());
					ObjectOutputStream objOutStream = new ObjectOutputStream(newSocket.getOutputStream());

					// activity that won't throw network/io exceptions - safe to add to class fields
					threadShouldLive.put(newSocket, true);

					// save socket reference into container (doesn't have to be a hashmap)
					sockets.put(newSocket.hashCode(), newSocket);

					// store streams
					inStreams.put(newSocket, objInStream);
					outStreams.put(newSocket, objOutStream);

					// init buffers
					ConcurrentLinkedQueue<Packet> sendBuffer = new ConcurrentLinkedQueue<Packet>();
					ConcurrentLinkedQueue<Packet> receiveBuffer = new ConcurrentLinkedQueue<Packet>();
					sendBuffers.put(newSocket, sendBuffer);
					receiveBuffers.put(newSocket, receiveBuffer);
					sendBufferLocks.put(sendBuffer, false);

					// handle ID creation (and set up for next)
					ID.put(newSocket, nextID);
					nextID = idManager.getReservedIDAndRemoveFromIDPool();

					// init failure counts
					sendFailures.put(newSocket, 0);
					receiveFailures.put(newSocket, 0);

					// launch a thread to receive from this socket @formatter:off
					Thread sendThread = new Thread(new Runnable() {
						public void run() {
							sendThreadMethod(newSocket);
						}
					});
					outThreads.put(newSocket, sendThread);
					sendThread.start();

					// launch a thread to broadcast from this socket
					Thread receiveThread = new Thread(new Runnable() {
						public void run() {
							receiveThreadMethod(newSocket);
						}
					});
					inThreads.put(newSocket, receiveThread);
					receiveThread.start();
					activeSockets++;

					// @formatter:on
				} catch (SocketTimeoutException e) {
					// timeout event is normal
				} catch (IOException e) {
					// when listener is closed will accepting, throws an exception.
					// the below flag is set so that an error message isn't printed.
					if (!listenForceShutdown) {
						System.out.println("Failed to accept socket - IO Exception");
						e.printStackTrace();
					}
				}
			} else {
				sleepForMS(1);
			}
		}
	}

	private void validateSocketsAreAlive() {
		if (!pingSocketsPeriodically) {
			return;
		}
		while (threadsShouldLive) {
			sleepForMS(socketAliveCheckTimeoutMS);
			for (Socket socket : sockets.values()) {
				if (socket.isClosed()) {
					// TODO implement a queueing processing map to prevent
					// duplicate disconnects from happening
					// as it stands, the server should disconnect a client
					// if it fails to send for a set threshold
				}
			}
			sleepForMS(1000); // TODO remove this when implemented.
		}
	}

	private void receiveThreadMethod(Socket fromSocket) {
		while (threadsShouldLive && threadShouldLive.get(fromSocket)) {
			try {
				ObjectInputStream inStream = inStreams.get(fromSocket);
				Packet inbound = (Packet) inStream.readObject();
				if (!checkForSystemMessage(inbound, fromSocket) && inbound != null) {
					receiveBuffers.get(fromSocket).add(inbound);
					hasReceived = true;
				}
				receiveFailures.put(fromSocket, 0);

			} catch (ClassNotFoundException e) {
				// cast error kill thread and pass exception
				e.printStackTrace();
				dropConnection(fromSocket, true);
				return;
			} catch (SocketTimeoutException e) {
				// Do nothing, but prevent this from being caught in IOException
			} catch (IOException e) {
				int failures = receiveFailures.get(fromSocket);
				failures++;
				receiveFailures.put(fromSocket, failures);
				// Connections are only dropped from the sending thread
				// if (failures > receiveFailureThreshold) {
				// dropConnection(fromSocket); return;}
				sleepForMS(failureSleepMSTime * failures);
			}
		}
	}

	private void sendThreadMethod(Socket toSocket) {
		while (threadsShouldLive && threadShouldLive.get(toSocket)) {
			try {
				// peek what is to be sent, rather than removing from queue
				Packet toSend = sendBuffers.get(toSocket).peek();
				if (toSend != null) {
					outStreams.get(toSocket).writeObject(toSend);

					// remove packet from buffer because exception was not
					// thrown
					sendBuffers.get(toSocket).poll();
				}
				sendFailures.put(toSocket, 0);
			} catch (IOException e) {
				// socket should not timeout for sends unless there's a problem
				// thus socket timeouts are in this catch.
				int failures = sendFailures.get(toSocket);
				failures++;
				sendFailures.put(toSocket, failures);
				if (failures > sendFailureThreshold) {
					dropConnectionInNewThread(toSocket);
					return;
				}
				sleepForMS(failureSleepMSTime * failures);
			}
		}
	}

	private boolean checkForSystemMessage(Packet packet, Socket socket) {
		if (packet instanceof SystemMessagePacket) {
			loadSystemMessageJobToThread((SystemMessagePacket) packet, socket);
			return true;
		}
		return false;
	}

	/**
	 * Loads system packet so that it can be handled by the SystemMessageHandlerThread. Some
	 * preprocessing of packet occurs. Packets are loaded into the SystemMessageHandlerThread to
	 * prevent blocking of thread and deadlock.
	 * 
	 * For example, a receive thread can be deadlocked if it attempts to drop its own socket's
	 * connection because the receive thread cannot end while it waits for dropConnection to
	 * complete. dropConnection cannot complete because it waits for all receive threads to
	 * terminate; thus the receive lower in the call stack that called drop connection will block
	 * the completion of its own dropConnection call.
	 * 
	 * @param packet the packet to be added to the system message
	 * @param socket the socket that sent the message.
	 */
	private void loadSystemMessageJobToThread(SystemMessagePacket packet, Socket socket) {
		if (packet.connetionShouldClose()) {
			// calling drop connection here will deadlock the receive thread
			// socketsForSystemToDrop.add(socket);

			// do not send a drop message since the client alerted server of intent of dropping
			socketsForSystemToDrop.add(new SocketMessagePair(socket, false));
		}
	}

	/**
	 * Alerts dropping thread whether the socket should be dropped and whether a "disconnect"
	 * message is to be sent.
	 * 
	 * @author Matt Stone
	 */
	private class SocketMessagePair {
		public final boolean dropMessage;
		public final Socket socket;

		SocketMessagePair(Socket socket, boolean dropMessage) {
			this.socket = socket;
			this.dropMessage = dropMessage;
		}
	}

	private void SystemMessageHandlerThreadMethod() {
		while (threadsShouldLive) {
			// since ConcurrentQueue.size() is O(n), just poll and check if non-null head
			SocketMessagePair pair = socketsForSystemToDrop.poll();

			// Socket dropSocket = socketsForSystemToDrop.poll();
			if (pair != null && pair.socket != null) {
				dropConnection(pair.socket, pair.dropMessage);
			} else {
				// sleep thread
				sleepForMS(10);
			}
		}
	}

	/**
	 * Starts a thread for dropping a connection. This is provided because the dropping mechanism
	 * requires that a certain threads stop processing (such as receive, and send). Therefore, the
	 * function call to drop a connection within those threads will deadlock the send/receive
	 * thread.
	 * 
	 * In order for the the thread calling dropConenction to complete, then receive thread and send
	 * thread must come to a halt after setting a flag for those threads to terminate. If the thread
	 * that calls dropConnection() happens to be a send or receive thread, then the function call
	 * will wait indefinitely on itself to terminate.
	 * 
	 * @param socketToDrop
	 */
	private void dropConnectionInNewThread(final Socket socketToDrop) {
		// Start the server running
		new Thread(new Runnable() {
			public void run() {
				dropConnection(socketToDrop, true);
			}
		}).start();
	}

	/**
	 * To prevent complications, this method should only ever be accessible from a single thread.
	 * For example, if a client is to time out and be dropped, then it should either the receive
	 * thread or the send thread should call this method. Both threads should not call it.
	 * 
	 * @param socket the socket to be dropped.
	 */
	private void dropConnection(Socket socket, boolean sendDisconnectMessage) {
		String socketStr = socket.toString();
		String extraMsgs = " ";
		System.out.println("Server: dropping connection " + socketStr);

		// send disconnect message - blocks for max 5 seconds before moving on
		if (sendDisconnectMessage) {
			sendDisconnectMessageTo(socket);
		}

		// Flag threads associate with this socket to stop
		if (threadShouldLive.put(socket, false) == null) {
			// thread no longer exists, remove accidental insertion and return.
			threadShouldLive.remove(socket);
			return;
		}

		// Wait for threads to stop
		int counter = 0;
		while (threadsAliveFor(socket)) {
			sleepForMS(100);
			if (counter > 10000) {
				throw new RuntimeException("Server: dropConnection inifite loop - cannot kill thread");
			}
			counter++;
		}

		try {
			socket.close();
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("failed to close socket");
		}
		idManager.unReserveIDAndReturnIdToPool(ID.get(socket));

		// Threads are now dead - deallocate from most containers
		sendFailures.remove(socket);
		sendBuffers.remove(socket);
		receiveFailures.remove(socket);
		receiveBuffers.remove(socket);
		inStreams.remove(socket);
		outStreams.remove(socket);
		threadShouldLive.remove(socket);
		inThreads.remove(socket);
		outThreads.remove(socket);
		sockets.remove(socket);
		sendBufferLocks.remove(socket);
		ID.remove(socket);
		activeSockets--;

		System.out.println("\tServer: dropped " + socketStr + extraMsgs);
	}

	/**
	 * Send a disconnect message to client and clear the send buffer to that client.
	 * 
	 * @SideEffect clears send buffer of the socket.
	 * @param socket - the socket used to obtain the send buffer.
	 */
	private void sendDisconnectMessageTo(Socket socket) {
		// get the sendBuffer that should be cleared
		ConcurrentLinkedQueue<Packet> socketSendBuffer = sendBuffers.get(socket);

		// lock all sending until the message is complete.
		if (socketSendBuffer != null && sendBufferLocks.get(socketSendBuffer) != null) {
			sendBufferLocks.put(socketSendBuffer, true);
		} else {
			// there isn't a lock for this socket
			return;
		}

		// clear buffer - the disconnect message should be the first queued to send
		socketSendBuffer.clear();

		// create a system message that signals the client should shut down
		SystemMessagePacket closeMessage = new SystemMessagePacket();
		closeMessage.setConnectionShouldClose(true);

		// add the system message to the normal send buffer
		socketSendBuffer.add(closeMessage);

		// record the start time in case the client is un-receptive and server needs to move on
		long start = System.currentTimeMillis();

		// wait 5 seconds or until the system message is sent to drop threads (or until msg set)
		long delayMS = 5000;
		while (socketSendBuffer.peek() != null && System.currentTimeMillis() - start < delayMS) {
			sleepForMS(1);
		}

		// unlock the sending
		sendBufferLocks.put(socketSendBuffer, false);
	}

	private boolean threadsAliveFor(Socket socket) {
		Thread receiveThread = inThreads.get(socket);
		Thread sendThread = outThreads.get(socket);
		boolean ret = false;
		if (receiveThread != null) {
			ret |= receiveThread.isAlive();
		}
		if (sendThread != null) {
			ret |= sendThread.isAlive();
		}
		return ret;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public void prepareServerToStart() throws IOException {
		listener = new ServerSocket(port);
		// ready = true; TODO: remove this if decide against logic approach
	}

	private void sleepForMS(long ms) {
		// formatter:off
		try {
			Thread.sleep(ms);
		} catch (InterruptedException e) {
		}
		// formatter:on
	}

	/**
	 * This method may take up to 5 seconds to complete the effect of disconnecting all clients.
	 * Sending/Receiving threads are not closed immediately and must wait a cycle to detect that
	 * they should close.
	 */
	public void disconnect() {
		// below needed to prevent infinite loops on multiple calls to disconnect
		if (!isRunning()) return;

		// TODO finish developing this
		// threadsShouldLive = false;
		for (Socket socket : sockets.values()) {
			// socketsForSystemToDrop.add(socket);
			socketsForSystemToDrop.add(new SocketMessagePair(socket, true));
		}
		try {
			// below is a flag to prevent listen thread for printing exception msg
			listenForceShutdown = true;
			if (listener != null) {
				listener.close();
			}
		} catch (IOException e) {
			System.out.println("Failed to close listener");
			e.printStackTrace();
		}
		// wait for the thread to drop all sockets before flagging isRunning to false
		// TODO consider isRunning in favor of checking to see if server
		// is still running by calling isRunning method.
		// while (socketsForSystemToDrop.size() > 0) {
		while (socketsForSystemToDrop.peek() != null) {
			sleepForMS(1);
		}
		// once the connections are dropped, shut down the threads
		threadsShouldLive = false;
		listenForceShutdown = false;

		isRunning = false;
	}

	public boolean isRunning() {
		boolean ret = false;
		if (listeningThread != null && sockets != null) {
			ret |= listeningThread.isAlive();
		}

		// TODO remove variable isRunning and just have it calculated any time it is called
		return isRunning || ret;
	}

	public boolean hasReceivedPacket() {
		return hasReceived || stagedReceivePackets.peek() != null;
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
	 * This method will not stage more packets until the previously staged packets have been
	 * handled.
	 */
	public void stageReceivedPacketsForRemoval() {
		// if there are still packets staged, then simply return.
		if (stagedReceivePackets.peek() != null) {
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
			// size() is O(n) and implementation should produce non-stale value
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
				stagedReceivePackets.add(buffer.poll());
			}
		}
		// set flag for packets in receive buffer false (all should be staged)
		hasReceived = false;
	}

	public boolean hasStagedPackets() {
		return stagedReceivePackets.peek() != null;
	}

	public Packet getNextStagedPacket() {
		return stagedReceivePackets.poll();
	}

	public Packet getNextReceivedPacket() {
		if (hasStagedPackets()) {
			return getNextStagedPacket();
		}
		if (hasReceived) {
			stageReceivedPacketsForRemoval();
			return getNextStagedPacket();
		}
		// no packets to receive
		return null;
	}

	public void queueToSend(Packet packet) {
		// TODO concern: potentially make another buffer between this and the
		// buffer that threads collect from
		final Packet copy = packet.makeCopy();

		// the staged send packets cause async threads to maintain ordering of
		// packets
		stagedSendPackets.add(copy);

	}

	/**
	 * Simply loads 1 packet from the staged send packets to be sent over the server.
	 */
	private synchronized void loadPacketIntoAllOutgoingBuffers() {
		while (threadsShouldLive) {
			// system messages may lock send thread temporarily
			if (stagedSendPackets.peek() != null) {
				Packet packet = stagedSendPackets.poll();
				for (ConcurrentLinkedQueue<Packet> buffer : sendBuffers.values()) {
					boolean locked = sendBufferLocks.get(buffer);
					if (!locked) {
						buffer.add(packet.makeCopy());
					}
				}
			}
		}
	}

	/**
	 * This method should be used sparingly because it has the potential to cause bottlenecks during
	 * the server listening thread and server disconnecting threads.
	 * 
	 * @return The atomic number of current active sockets.
	 */
	public int activeConnections() {
		// the ConcurrentHashMap doesn't have an atomic size() method. It recalculates the list size
		// everytime it is called. This is because having a atomic int for size could cause a
		// concurrent bottle neck and since the size() method isn't thought to be very important for
		// concurrent problems, is built in this way. the size() method with ConcurrentHashMaps can
		// even return old stale values.
		//
		// My implementation requires a valid size, so I have created my own atomic variable to
		// track size
		return activeSockets;
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

		// Creating a delay before starting the un-packaging.
		while (server.isRunning()) {
			Thread.sleep(1000);
		}

		// Start a thread to print messages from the server (independent of this
		// thread)
		new Thread(new Runnable() {
			public void run() {
				System.out.println("Starting staging and unpackaging phases");
				while (server.isRunning()) {
					if (server.hasReceivedPacket()) {
						server.stageReceivedPacketsForRemoval();
						while (server.hasStagedPackets()) {
							DemoConcretePacket packet = (DemoConcretePacket) server.getNextStagedPacket();
							// here is where you would use your defined packet
							// to extra data
							packet.printData();
						}
					}
				}
			}
		}).start();

		// note: above thread's prints may stop printing to eclipse terminal,
		// drop breakpnt to
		// restore
		Thread.sleep(10000);
		System.out.println("Close the server by pressing enter");
		Scanner kb = new Scanner(System.in);
		kb.nextLine();
		kb.close();
		server.disconnect();
	}
}

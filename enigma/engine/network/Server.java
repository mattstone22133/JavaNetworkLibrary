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
	private ConcurrentHashMap<Integer, Socket> sockets = new ConcurrentHashMap<Integer, Socket>();
	private ConcurrentHashMap<Socket, Thread> outThreads = new ConcurrentHashMap<Socket, Thread>();
	private ConcurrentHashMap<Socket, Thread> inThreads = new ConcurrentHashMap<Socket, Thread>();
	private ConcurrentHashMap<Socket, ObjectInputStream> inStreams = new ConcurrentHashMap<Socket, ObjectInputStream>();
	private ConcurrentHashMap<Socket, ObjectOutputStream> outStreams = new ConcurrentHashMap<Socket, ObjectOutputStream>();
	private ConcurrentHashMap<Socket, ConcurrentLinkedQueue<Packet>> sendBuffer = new ConcurrentHashMap<>();
	private ConcurrentHashMap<Socket, ConcurrentLinkedQueue<Packet>> receiveBuffers = new ConcurrentHashMap<>();
	private ConcurrentHashMap<Socket, Integer> receiveFailures = new ConcurrentHashMap<Socket, Integer>();
	private ConcurrentHashMap<Socket, Integer> sendFailures = new ConcurrentHashMap<Socket, Integer>();
	private ConcurrentHashMap<Socket, Boolean> threadShouldLive = new ConcurrentHashMap<Socket, Boolean>();

	private int port;
	private short maxPlayers = 8;
	private int blockingTimeoutMS = 5000;
	private long socketAliveCheckTimeoutMS = 30000;
	private boolean ready = false;
	private boolean closeAllThreads = false;
	private volatile boolean threadsShouldLive = true;
	// private int receiveFailureThreshold = 100;
	private int sendFailureThreshold = 100;
	private long failureSleepMSTime = 50;
	private boolean isRunning;
	private boolean hasReceived;
	/**
	 * provides a storage place to lump all received packets for user
	 * processing. Non-concurrency is intentional.
	 * 
	 */
	private LinkedList<Packet> stagedReceivePackets = new LinkedList<Packet>();
	private ConcurrentLinkedQueue<Packet> stagedSendPackets = new ConcurrentLinkedQueue<Packet>();
	private boolean pingSocketsPeriodically = false;

	public Server(int port) {
		this.port = port;
		ready = true;

	}

	/**
	 * Start running the server. This method is intended to be called in another
	 * thread. Throws IOException if failure to initiate the listening socket.
	 * 
	 * @throws IOException
	 *             failure to initiate the listening socket.
	 */
	public void runBlockingMode() throws IOException {
		threadsShouldLive = true;
		listener = new ServerSocket(port);
		isRunning = true;
		while (threadsShouldLive) {
			if (!ready) {
				sleepForMS(1000L);
			} else {
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

				// continually sleep until the server is to be restarted
				while (!closeAllThreads) {
					sleepForMS(1000L);
				}

				// server is shutting down, wait until all threads have shut
				// down
				while (!allThreadsClosed()) {
					sleepForMS(1000L);
				}

				// all threads are now dead, clean up variables and hashmaps
				resetAllStateVariables();

				// @formatter:on
			}
		}
	}

	/**
	 * @deprecated this is no longer the way to run a server. however, gives
	 *             example of thread exception handling
	 * 
	 * 
	 *             Public interface for run. This is launched in a new thread
	 *             and does not cause blocking. It is suggested that users wait
	 *             a few seconds before attempting operations.
	 * 
	 *             Because this method initiates a thread internal to the
	 *             server, the user will not be notified if the server fails to
	 *             start listening. The user should call isRunning() before
	 *             attempting operations.
	 */
	protected void run_deprecated() {
		// Create a way to alert user that server failed to run.
		Thread.UncaughtExceptionHandler handler = new Thread.UncaughtExceptionHandler() {
			public void uncaughtException(Thread th, Throwable ex) {
				System.out.println("Failed to run server, Exception: " + ex);
				isRunning = false;
			}
		};

		// Create the thread.
		Thread runThread = new Thread(new Runnable() {
			public void run() {
				run();
			}
		});

		// set up thread exception handler and run the thread.
		runThread.setUncaughtExceptionHandler(handler);
		runThread.start();
	}

	public void run() throws IOException {
		threadsShouldLive = true;
		listener = new ServerSocket(port);

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

		// @formatter:on
		isRunning = true;
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
					// TODO concern: add timeout to listener so that thread may
					// shut down?
					listener.setSoTimeout(5000);
					final Socket newSocket = listener.accept();

					newSocket.setSoTimeout(blockingTimeoutMS);

					// save socket reference into hashmap
					sockets.put(newSocket.hashCode(), newSocket);
					threadShouldLive.put(newSocket, true);

					// init streams
					inStreams.put(newSocket, new ObjectInputStream(newSocket.getInputStream()));
					outStreams.put(newSocket, new ObjectOutputStream(newSocket.getOutputStream()));

					// init buffers
					sendBuffer.put(newSocket, new ConcurrentLinkedQueue<Packet>());
					receiveBuffers.put(newSocket, new ConcurrentLinkedQueue<Packet>());

					// init failure counts
					sendFailures.put(newSocket, 0);
					receiveFailures.put(newSocket, 0);

					// launch a thread to receive from this socket
					// @formatter:off
					Thread sendThread = new Thread(new Runnable() {
						public void run() {
							send(newSocket);
						}
					});
					outThreads.put(newSocket, sendThread);
					sendThread.start();

					// launch a thread to broadcast from this socket
					Thread receiveThread = new Thread(new Runnable() {
						public void run() {
							receive(newSocket);
						}
					});
					inThreads.put(newSocket, receiveThread);
					receiveThread.start();

					// @formatter:on
				} catch (SocketTimeoutException e) {
					// timeout event is normal
				} catch (IOException e) {
					System.out.println("Failed to accept socket - IO Exception");
					e.printStackTrace();
				}
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
		}
	}

	private void receive(Socket fromSocket) {
		while (threadsShouldLive && threadShouldLive.get(fromSocket)) {
			try {
				ObjectInputStream inStream = inStreams.get(fromSocket);
				Packet inbound = (Packet) inStream.readObject();
				if (inbound != null) {
					receiveBuffers.get(fromSocket).add(inbound);
					hasReceived = true;
				}
				receiveFailures.put(fromSocket, 0);
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
				// Connections are only dropped from the sending thread
				// if (failures > receiveFailureThreshold) {
				// dropConnection(fromSocket); return;}
				sleepForMS(failureSleepMSTime * failures);
			}
		}
	}

	private void send(Socket toSocket) {
		while (threadsShouldLive && threadShouldLive.get(toSocket)) {
			try {
				// peek what is to be sent, rather than removing from queue
				Packet toSend = sendBuffer.get(toSocket).peek();
				if (toSend != null) {
					outStreams.get(toSocket).writeObject(toSend);

					// remove packet from buffer because exception was not
					// thrown
					sendBuffer.get(toSocket).poll();
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

	/**
	 * Starts a thread for dropping a connection. This is provided because the
	 * dropping mechanism requires that a certain threads stop processing (such
	 * as receive, and send). Therefore, the function call to drop a connection
	 * within those threads will deadlock the send/receive thread.
	 * 
	 * In order for the the thread calling dropConenction to complete, then
	 * receive thread and send thread must come to a halt after setting a flag
	 * for those threads to terminate. If the thread that calls dropConnection()
	 * happens to be a send or receive thread, then the function call will wait
	 * indefinitely on itself to terminate.
	 * 
	 * @param socketToDrop
	 */
	private void dropConnectionInNewThread(final Socket socketToDrop) {
		// Start the server running
		new Thread(new Runnable() {
			public void run() {
				dropConnection(socketToDrop);
			}
		}).start();
	}

	/**
	 * To prevent complications, this method should only ever be accessible from
	 * a single thread. For example, if a client is to time out and be dropped,
	 * then it should either the receive thread or the send thread should call
	 * this method. Both threads should not call it.
	 * 
	 * @param socket
	 *            the socket to be dropped.
	 */
	private void dropConnection(Socket socket) {
		String socketStr = socket.toString();
		String extraMsgs = " ";
		System.out.println("Dropping connection" + socketStr);

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
				throw new RuntimeException("dropConnection inifite loop - cannot kill thread");
			}
			counter++;
		}

		try {
			socket.close();
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("failed to close socket");
		}

		// Threads are now dead - deallocate from most containers
		sendFailures.remove(socket);
		sendBuffer.remove(socket);
		receiveFailures.remove(socket);
		receiveBuffers.remove(socket);
		inStreams.remove(socket);
		outStreams.remove(socket);
		threadShouldLive.remove(socket);
		inThreads.remove(socket);
		outThreads.remove(socket);
		sockets.remove(socket);

		System.out.println("dropped: " + socketStr + extraMsgs);
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

	/**
	 * This method may take up to 5 seconds to complete the effect of
	 * disconnecting all clients. Sending/Receiving threads are not closed
	 * immediately and must wait a cycle to detect that they should close.
	 */
	public void disconnect() {
		// TODO finish developing this
		threadsShouldLive = false;
		for (Socket socket : sockets.values()) {
			dropConnection(socket);
		}
		try {
			listener.close();
		} catch (IOException e) {
			System.out.println("Failed to close listener");
			e.printStackTrace();
		}
		isRunning = false;
	}

	public boolean isRunning() {
		return isRunning;
	}

	public boolean hasReceivedPacket() {
		return hasReceived || stagedReceivePackets.size() > 0;
		// for(ConcurrentLinkedQueue<Packet> receiveBuffer :
		// receiveBuffers.values()){
		// if(receiveBuffer.size() > 0){
		// return true;
		// }
		// }
		// return false;
	}

	private ArrayList<Socket> keys = new ArrayList<Socket>();
	private HashMap<Socket, Integer> queueSizes = new HashMap<Socket, Integer>();

	/**
	 * This method loads all receive buffers into a single collection. It is
	 * designed so that packets received will be equally distributed.
	 * 
	 * If collections were to be iterated while still being updated, then
	 * sockets that were accessed later may have newer packets than earlier
	 * accessed sockets.
	 * 
	 * This method will not stage more packets until
	 */
	public void stageReceivedPacketsForRemoval() {
		if (stagedReceivePackets.size() > 0) {
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
				stagedReceivePackets.add(buffer.poll());
			}
		}
		// set flag for packets in receive buffer false (all should be staged)
		hasReceived = false;
	}

	public boolean hasStagedPackets() {
		return stagedReceivePackets.size() > 0;
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

		// cause the server to load one packet from the stagedQueue
		new Thread(new Runnable() {
			public void run() {
				loadPacketIntoAllOutgoingBuffers();
			}
		}).start();
	}

	/**
	 * Simply loads 1 packet from the staged send packets to be sent over the
	 * server.
	 */
	private synchronized void loadPacketIntoAllOutgoingBuffers() {
		Packet packet = stagedSendPackets.poll();
		for (ConcurrentLinkedQueue<Packet> buffer : sendBuffer.values()) {
			buffer.add(packet.makeCopy());
		}
	}

	public static void main(String[] args) throws UnknownHostException, InterruptedException {
		final Server server = new Server(25565);

		// Start the server running
		new Thread(new Runnable() {
			public void run() {
				System.out.println("Starting Server Up");
				try {
					server.runBlockingMode();
				} catch (IOException e) {
					System.out.println("Failed to start server listening for connections");
					e.printStackTrace();
				}
			}
		}).start();

		// Creating a delay before starting the un-packaging.
		Thread.sleep(1000);

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

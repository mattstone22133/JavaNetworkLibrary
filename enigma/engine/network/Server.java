package enigma.engine.network;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
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
	private ConcurrentHashMap<Socket, ConcurrentLinkedQueue<Packet>> sendQueues = new ConcurrentHashMap<>();
	private ConcurrentHashMap<Socket, ConcurrentLinkedQueue<Packet>> receiveBuffers = new ConcurrentHashMap<>();
	private ConcurrentHashMap<Socket, Integer> receiveFailures = new ConcurrentHashMap<Socket, Integer>();
	private ConcurrentHashMap<Socket, Integer> sendFailures = new ConcurrentHashMap<Socket, Integer>();

	private int port;
	private short maxPlayers = 8;
	private boolean ready = false;
	private boolean closeAllThreads = false;
	private boolean threadsShouldLive = true;
	private int receiveFailureThreshold = 100;
	private int sendFailureThreshold = 100;


	public Server() {

	}

	public void run() {
		while (threadsShouldLive) {
			if (!ready) {
				sleepForMS(1000L);
			} else {

				//start a continuously listening thread //@formatter:off
				listeningThread = new Thread(new Runnable() {public void run() {listen();}});

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
					listener.setSoTimeout(1000);
					final Socket newSocket = listener.accept();

					// save socket ref into hashmap
					sockets.put(newSocket.hashCode(), newSocket);
					sendQueues.put(newSocket, new ConcurrentLinkedQueue<Packet>());
					receiveBuffers.put(newSocket, new ConcurrentLinkedQueue<Packet>());
					sendFailures.put(newSocket, 0);
					receiveFailures.put(newSocket, 0);

					//launch a thread to receive from this socket @formatter:off
					Thread broadcast = new Thread(new Runnable() {public void run(){broadcast(newSocket);}});
					outThreads.put(newSocket, broadcast);
					
					//launch a thread to broadcast from this socket
					Thread receive = new Thread(new Runnable(){public void run(){receive(newSocket);}});
					inThreads.put(newSocket, receive);
					
					//@formatter:on
				} catch (SocketException e) {
					// timeout event is normal
				} catch (IOException e) {
					System.out.println("Failed to acept socket - IO Exception");
					e.printStackTrace();
				}
			}
		}
	}

	private void receive(Socket fromSocket) {
		while (threadsShouldLive) {
			try {
				Packet inbound = (Packet) inStreams.get(fromSocket).readObject();
				receiveBuffers.get(fromSocket).add(inbound);

			} catch (ClassNotFoundException e) {
				e.printStackTrace();
				return;
			} catch (IOException e) {
				int failures = receiveFailures.get(fromSocket);
				failures++;
				receiveFailures.put(fromSocket, failures);
				if (failures > receiveFailureThreshold) {
					dropConnection(fromSocket);
					receiveFailures.put(fromSocket, 0);
				}
			}
		}
	}



	private void broadcast(Socket toSocket) {
		while (threadsShouldLive) {

		}
	}

	private void dropConnection(Socket fromSocket) {
		// TODO create this feature 
		
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

	public static void main(String[] args) throws UnknownHostException {
		Server server = new Server();
		server.setPort(25565);
		// server.run();
		System.out.println("Close the server by pressing enter");
		Scanner kb = new Scanner(System.in);
		kb.nextLine();
		kb.close();
	}
}

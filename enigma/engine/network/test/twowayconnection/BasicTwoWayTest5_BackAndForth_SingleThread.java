package enigma.engine.network.test.twowayconnection;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import enigma.engine.network.Client;
import enigma.engine.network.DemoConcretePacket;
import enigma.engine.network.FailedToConnect;
import enigma.engine.network.Server;
import enigma.engine.network.test.TestTools;

public class BasicTwoWayTest5_BackAndForth_SingleThread {

	private Client client;
	private Server server;
	private int listenPort = 25565;
	private DemoConcretePacket clientPacket;
	private DemoConcretePacket serverPacket;
	private int startID = 1;
	private int expectID = 2;
	int waitForPacket = 50;


	@Before
	public void setup() {
		try {
			client = new Client();
			server = new Server(listenPort);
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println();
			fail("failed to set up in: " + this.getClass().toString() + "\n previous resources may not have been closed");
		}
	}

	@After
	public void teardown() {
		server.disconnect();
		client.disconnect();
		TestTools.sleepForMS(500);
		int counter = 0;
		while ((server.isRunning() || client.isRunning() && counter < 10)) {
			TestTools.sleepForMS(500);
			counter++;
		}
	}

	/**
	 * Testing that a the communication can be in two directions.
	 */
	@Test
	public void testTwoWay1a_Connection1pkt() {
		setUpServerAndClient();
		
		//Test client initiating a send and server end
		clientStart();
		serverEnd();
		
		//test server initiating a send and client ending
		serverStart();
		clientEnd();
		
		//test a multiple message exchange with client starting
		clientStart();
		serverRecieveThenSend();
		clientRecieveThenSend();
		serverEnd();
		
		//test a multiple message exchange with server starting
		serverStart();
		clientRecieveThenSend();
		serverRecieveThenSend();
		clientEnd();

		
		//test a multiple message exchange with client starting
		clientStart();
		serverRecieveThenSend();
		clientRecieveThenSend();
		serverRecieveThenSend();
		clientRecieveThenSend();
		serverRecieveThenSend();
		clientRecieveThenSend();
		serverRecieveThenSend();
		clientRecieveThenSend();
		serverRecieveThenSend();
		clientRecieveThenSend();
		serverRecieveThenSend();
		clientRecieveThenSend();
		serverRecieveThenSend();
		clientRecieveThenSend();
		serverRecieveThenSend();
		clientRecieveThenSend();
		serverRecieveThenSend();
		clientRecieveThenSend();
		serverRecieveThenSend();
		clientRecieveThenSend();
		serverRecieveThenSend();
		clientRecieveThenSend();
		serverRecieveThenSend();
		clientRecieveThenSend();
		serverEnd();
		
		//test a multiple message exchange with server starting
		serverStart();
		clientRecieveThenSend();
		serverRecieveThenSend();
		clientRecieveThenSend();
		serverRecieveThenSend();
		clientRecieveThenSend();
		serverRecieveThenSend();
		clientRecieveThenSend();
		serverRecieveThenSend();
		clientRecieveThenSend();
		serverRecieveThenSend();
		clientRecieveThenSend();
		serverRecieveThenSend();
		clientRecieveThenSend();
		serverRecieveThenSend();
		clientRecieveThenSend();
		serverRecieveThenSend();
		clientRecieveThenSend();
		serverRecieveThenSend();
		clientRecieveThenSend();
		serverRecieveThenSend();
		clientRecieveThenSend();
		serverRecieveThenSend();
		clientRecieveThenSend();
		serverRecieveThenSend();
		clientRecieveThenSend();
		serverRecieveThenSend();
		clientRecieveThenSend();
		serverRecieveThenSend();
		clientRecieveThenSend();
		serverRecieveThenSend();
		clientRecieveThenSend();
		serverRecieveThenSend();
		clientRecieveThenSend();
		serverRecieveThenSend();
		clientRecieveThenSend();
		serverRecieveThenSend();
		clientRecieveThenSend();
		serverRecieveThenSend();
		clientRecieveThenSend();
		serverRecieveThenSend();
		clientEnd();
		
		
	}

	private void setUpServerAndClient() {
		Long start = System.currentTimeMillis();

		// Start a server
		try {
			server.run();
		} catch (IOException e) {
			fail("failed to start server from call to run");
		}
		int sleepForConnectMS = 30;
		TestTools.sleepForMS(sleepForConnectMS);
		assertTrue("Failed to start server", server.isRunning());

		// start a client!
		try {
			client.connect(InetAddress.getLocalHost().getHostAddress(), listenPort);
		} catch (UnknownHostException | FailedToConnect e) {
			fail("failed to connect client to server.\n" + e.toString());
		}
		System.out.println("\tServer and client connected at: " + (System.currentTimeMillis() - start - sleepForConnectMS) + "ms from start.");
	}

	private void clientRecieveThenSend() {
		TestTools.sleepForMS(waitForPacket);
		long receiveStart = System.currentTimeMillis();
		DemoConcretePacket receive;
		// check if packet was received at server
		if (client.hasReceivedPacket()) {
			receive = (DemoConcretePacket) client.getNextReceivedPacket();
			System.out.println("\tTime to process receive: " + (System.currentTimeMillis() - receiveStart) + "ms.");

			assertEquals("packet's didn't have same playerID field", expectID, receive.getId());
			assertEquals("packets had different X values", 2, receive.getX(), 0.001);
			assertEquals("packets had different Y values", 3, receive.getY(), 0.001);
			assertEquals("packets had different rotation values", 4, receive.getRotation(), 0.001);
			assertTrue("packets are same instance", receive != serverPacket);
			
			
			// create and send a packet to the client
			expectID++;
			clientPacket = new DemoConcretePacket(expectID, 2, 3, 4);
			client.queueToSend(clientPacket);
		} else {
			fail("client did not identify that it received a packet after" + waitForPacket + "ms.");
		}
	}

	private void serverRecieveThenSend() {
		TestTools.sleepForMS(waitForPacket);
		long receiveStart = System.currentTimeMillis();
		DemoConcretePacket receive;
		// check if packet was received at server
		if (server.hasReceivedPacket()) {
			receive = (DemoConcretePacket) server.getNextReceivedPacket();
			System.out.println("\tTime to process receive: " + (System.currentTimeMillis() - receiveStart) + "ms.");

			assertEquals("packet's didn't have same playerID field", expectID, receive.getId());
			assertEquals("packets had different X values", 2, receive.getX(), 0.001);
			assertEquals("packets had different Y values", 3, receive.getY(), 0.001);
			assertEquals("packets had different rotation values", 4, receive.getRotation(), 0.001);
			assertTrue("packets are same instance", receive != clientPacket);
			
			
			// create and send a packet to the client
			expectID++;
			serverPacket = new DemoConcretePacket(expectID, 2, 3, 4);
			server.queueToSend(serverPacket);
		} else {
			fail("server did not identify that it received a packet after" + waitForPacket + "ms.");
		}
	}

	public void clientStart() {
		// create and send a packet to the server
		clientPacket = new DemoConcretePacket(startID, 2, 3, 4);
		expectID = clientPacket.getId();
		client.queueToSend(clientPacket);
	}
	
	public void serverStart() {
		// create and send a packet to the server
		serverPacket = new DemoConcretePacket(startID, 2, 3, 4);
		expectID = serverPacket.getId();
		server.queueToSend(serverPacket);
	}
	
	public void serverEnd(){
		TestTools.sleepForMS(waitForPacket);
		if(server.hasReceivedPacket()){
			DemoConcretePacket received = (DemoConcretePacket) server.getNextReceivedPacket();
			assertTrue(received.getId() == expectID);
		}
	}
	
	public void clientEnd(){
		TestTools.sleepForMS(waitForPacket);
		if(client.hasReceivedPacket()){
			DemoConcretePacket received = (DemoConcretePacket) client.getNextReceivedPacket();
			assertTrue(received.getId() == expectID);
		}
	}
}

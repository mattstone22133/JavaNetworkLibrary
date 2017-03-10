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

public class BasicTwoWayTest1_Server1packet_client1packet {

	private Client client;
	private Server server;
	private int listenPort = 25565;
	private DemoConcretePacket clientPacket;
	private DemoConcretePacket serverPacket;

	@Before
	public void setup() {
		try {
			client = new Client();
			server = new Server(listenPort);
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println();
			fail("failed to set up in: " + this.getClass().toString()
					+ "\n previous resources may not have been closed");
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
		// Long start = System.currentTimeMillis();
		setUpServerAndClient();

		//basic client send test
		clientSend();

		//basic server send test 
		serverSend();
		
		assertTrue(clientPacket != serverPacket);
	}
	
	/**
	 * Testing that a the communication can be in two directions
	 * AND that it can occur multiple times.
	 */
	@Test
	public void testTwoWay1b_Connection1pkt() {
		// Long start = System.currentTimeMillis();
		setUpServerAndClient();

		//basic client send test
		clientSend();

		//basic server send test 
		serverSend();
		
		//basic client send test
		clientSend();

		//basic server send test 
		serverSend();
	}
	
	@Test
	public void testTwoWay1C_Connection1pkt(){
		setUpServerAndClient();
		serverSend();
		clientSend();
		clientSend();
		serverSend();
		clientSend();
		serverSend();
		clientSend();
		clientSend();
		clientSend();
		serverSend();
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
		System.out.println("\tServer and client connected at: "
				+ (System.currentTimeMillis() - start - sleepForConnectMS) + "ms from start.");
	}

	private void serverSend() {
		// create and send a packet to the client
		serverPacket = new DemoConcretePacket(1, 2, 3, 4);
		server.queueToSend(serverPacket);

		int waitForPacket = 50;
		TestTools.sleepForMS(waitForPacket);
		long receiveStart = System.currentTimeMillis();

		// check if packet was received at client
		if (client.hasReceivedPacket()) {
			DemoConcretePacket receivePkt = (DemoConcretePacket) client.getNextReceivedPacket();
			System.out.println("\tTime to process receive: " + (System.currentTimeMillis() - receiveStart) + "ms.");
			if (receivePkt != null) {
				assertEquals("packet's didn't have same playerID field", serverPacket.getId(), receivePkt.getId());
				assertEquals("packets had different X values", serverPacket.getX(), receivePkt.getX(), 0.001);
				assertEquals("packets had different Y values", serverPacket.getY(), receivePkt.getY(), 0.001);
				assertEquals("packets had different rotation values", serverPacket.getRotation(), receivePkt.getRotation(),
						0.001);
				assertTrue("packets are same instance", receivePkt != serverPacket);
			} else {
				fail("receive packet was null");
			}
		} else {
			fail("client did not receive a packet within " + waitForPacket + "ms.");
		}
	}

	private void clientSend() {
		// create and send a packet to the server
		clientPacket = new DemoConcretePacket(1, 2, 3, 4);
		client.queueToSend(clientPacket);

		int waitForPacket = 50;
		TestTools.sleepForMS(waitForPacket);

		long receiveStart = System.currentTimeMillis();
		// check if packet was received at server
		if (server.hasReceivedPacket()) {
			DemoConcretePacket pkt = (DemoConcretePacket) server.getNextReceivedPacket();
			System.out.println("\tTime to process receive: " + (System.currentTimeMillis() - receiveStart) + "ms.");

			assertEquals("packet's didn't have same playerID field", clientPacket.getId(), pkt.getId());
			assertEquals("packets had different X values", clientPacket.getX(), pkt.getX(), 0.001);
			assertEquals("packets had different Y values", clientPacket.getY(), pkt.getY(), 0.001);
			assertEquals("packets had different rotation values", clientPacket.getRotation(), pkt.getRotation(), 0.001);
			assertTrue("packets are same instance", pkt != clientPacket);
		} else {
			fail("server did not identify that it received a packet after" + waitForPacket + "ms.");
		}
	}
}

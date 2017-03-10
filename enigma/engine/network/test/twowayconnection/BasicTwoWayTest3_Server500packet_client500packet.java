package enigma.engine.network.test.twowayconnection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import enigma.engine.network.Client;
import enigma.engine.network.DemoConcretePacket;
import enigma.engine.network.FailedToConnect;
import enigma.engine.network.Packet;
import enigma.engine.network.Server;
import enigma.engine.network.test.TestTools;

public class BasicTwoWayTest3_Server500packet_client500packet {

	private Client client;
	private Server server;
	private int listenPort = 25565;
	int waitForPacket = 550;
	// private DemoConcretePacket clientPacket;
	// private DemoConcretePacket serverPacket;

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
	public void testTwoWay3a_Connection5pkt() {
		// Long start = System.currentTimeMillis();
		setUpServerAndClient();

		// basic client send test
		clientSend_500();

		// basic server send test
		serverSend_500();
	}
	
	@Test
	public void testTwoWay3b_Connection5pkt_3x() {
		// Long start = System.currentTimeMillis();
		setUpServerAndClient();

		clientSend_500();
		serverSend_500();
		clientSend_500();
		serverSend_500();
		clientSend_500();
		serverSend_500();
	}
	
	@Test
	public void testTwoWay3c_Connection5pkt_variablesends() {
		// Long start = System.currentTimeMillis();
		setUpServerAndClient();

		clientSend_500();
		serverSend_500();
		clientSend_500();
		serverSend_500();
		clientSend_500();
		serverSend_500();
		
		clientSend_500();
		serverSend_500();
		clientSend_500();
		serverSend_500();
		serverSend_500();
		serverSend_500();
		serverSend_500();
		clientSend_500();
		serverSend_500();
		clientSend_500();
		serverSend_500();
		clientSend_500();
		serverSend_500();
		clientSend_500();
		clientSend_500();
		clientSend_500();
		clientSend_500();
		serverSend_500();
		clientSend_500();
		serverSend_500();
		clientSend_500();
		serverSend_500();
		clientSend_500();
		clientSend_500();
		serverSend_500();
		clientSend_500();
		serverSend_500();
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

	private void serverSend_500() {
		// create and send a packet to the client
		ArrayList<DemoConcretePacket> packets = new ArrayList<DemoConcretePacket>();
		for (int i = 0; i < 500; ++i) {
			packets.add(new DemoConcretePacket(i, 2 * i, 3 * i, 4 * i));
		}
		for (Packet packet : packets) {
			server.queueToSend(packet);
		}

		TestTools.sleepForMS(waitForPacket);
		DemoConcretePacket lastPacket = null;
		long receiveStart = System.currentTimeMillis();
		int counter = 0;

		if (!client.hasReceivedPacket()) {
			fail("server did not identify that it received a packet after" + waitForPacket + "ms.");
		}
		// check if packet was received at server
		while (client.hasReceivedPacket()) {
			DemoConcretePacket packet = packets.get(counter);
			DemoConcretePacket pkt = (DemoConcretePacket) client.getNextReceivedPacket();
			System.out.println("\tTime to process receive: " + (System.currentTimeMillis() - receiveStart) + "ms.");

			assertEquals("packet's didn't have same playerID field", packet.getId(), pkt.getId());
			assertEquals("packets had different X values", packet.getX(), pkt.getX(), 0.001);
			assertEquals("packets had different Y values", packet.getY(), pkt.getY(), 0.001);
			assertEquals("packets had different rotation values", packet.getRotation(), pkt.getRotation(), 0.001);
			assertTrue("packets are same instance", pkt != packet);
			assertTrue("current received packet and last packet are the same instance", pkt != lastPacket);
			counter++;
			lastPacket = pkt;
		}
		assertTrue("server only received " + counter + " packets, expected: " + packets.size(),
				counter == packets.size());
	}

	private void clientSend_500() {
		// create and send a packet to the server
		ArrayList<DemoConcretePacket> packets = new ArrayList<DemoConcretePacket>();
		for (int i = 0; i < 500; ++i) {
			packets.add(new DemoConcretePacket(i, 2 * i, 3 * i, 4 * i));
		}
		for (Packet packet : packets) {
			client.queueToSend(packet);
		}

		TestTools.sleepForMS(waitForPacket);
		DemoConcretePacket lastPacket = null;
		long receiveStart = System.currentTimeMillis();
		int counter = 0;

		if (!server.hasReceivedPacket()) {
			fail("server did not identify that it received a packet after" + waitForPacket + "ms.");
		}
		// check if packet was received at server
		while (server.hasReceivedPacket()) {
			DemoConcretePacket packet = packets.get(counter);
			DemoConcretePacket pkt = (DemoConcretePacket) server.getNextReceivedPacket();
			System.out.println("\tTime to process receive: " + (System.currentTimeMillis() - receiveStart) + "ms.");

			assertEquals("packet's didn't have same playerID field", packet.getId(), pkt.getId());
			assertEquals("packets had different X values", packet.getX(), pkt.getX(), 0.001);
			assertEquals("packets had different Y values", packet.getY(), pkt.getY(), 0.001);
			assertEquals("packets had different rotation values", packet.getRotation(), pkt.getRotation(), 0.001);
			assertTrue("packets are same instance", pkt != packet);
			assertTrue("current received packet and last packet are the same instance", pkt != lastPacket);
			counter++;
			lastPacket = pkt;
		}
		assertTrue("server only received " + counter + " packets, expected: " + packets.size(),
				counter == packets.size());
	}
}

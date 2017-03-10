package enigma.engine.network.test.BasicTests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

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

public class BasicTestConnect2_ClientSendData {
	private Client client;
	private Server server;
	private int listenPort = 25565;

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
	 * Test a client connecting to a server. No data exchanged.
	 */
	@Test
	public void test() {
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

		// create and send a packet to the server
		DemoConcretePacket packet = new DemoConcretePacket(1, 2, 3, 4);
		client.queueToSend(packet);

		int waitForPacket = 100;
		TestTools.sleepForMS(waitForPacket);

		long receiveStart = System.currentTimeMillis();
		// check if packet was received at server
		if (server.hasReceivedPacket()) {
			DemoConcretePacket pkt = (DemoConcretePacket) server.getNextReceivedPacket();
			System.out.println("\tTime to process receive: " + (System.currentTimeMillis() - receiveStart) + "ms.");

			assertEquals("packet's didn't have same playerID field", packet.getId(), packet.getId());
			assertEquals("packets had different X values", packet.getX(), pkt.getX(), 0.001);
			assertEquals("packets had different Y values", packet.getY(), pkt.getY(), 0.001);
			assertEquals("packets had different rotation values", packet.getRotation(), pkt.getRotation(), 0.001);
			assertTrue("packets are same instance", pkt != packet);
		} else {
			fail("server did not identify that it received a packet after" + waitForPacket + "ms.");
		}

	}

}

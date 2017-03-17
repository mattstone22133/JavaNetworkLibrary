package enigma.engine.network.test.connect.disconnect.blackbox;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.InetAddress;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import enigma.engine.network.Client;
import enigma.engine.network.DemoConcretePacket;
import enigma.engine.network.FailedToConnect;
import enigma.engine.network.Server;
import enigma.engine.network.test.TestTools;

public class TestBasicDisconnect1 {
	public static int listenPort = 25565;
	public static Client client;
	public static Server server;
	public static String address;

	@BeforeClass
	public static void setup() {
		try {
			client = new Client();
			server = new Server(listenPort);
			address = InetAddress.getLocalHost().getHostAddress();
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println();
			fail("failed to set up, previous test server closed?");
		}
	}

	@AfterClass
	public static void teardown() {
		server.disconnect();
		client.disconnect();
		TestTools.sleepForMS(500);
		int counter = 0;
		while ((server.isRunning() || client.isRunning() && counter < 10)) {
			TestTools.sleepForMS(500);
			counter++;
		}
	}

	@Test
	public void testClientDisconnect() throws FailedToConnect, IOException {
		TestTools.sleepForMS(100);
		if (!server.isRunning()) {
			server.run();
		}

		int counter = 1;

		// Test that basic disconnect functionality works
		client.connect(address, listenPort);
		TestTools.sleepForMS(10);
		client.disconnect();
		long timeoutMS = 10000;
		long start = System.currentTimeMillis();

		while (client.isRunning() && System.currentTimeMillis() - start < timeoutMS) {
			// loop until client disconnects or timeout happens
		}
		System.out.printf("\tclient disconnected/timeout in %dms\n", (System.currentTimeMillis()) - start);
		assertTrue("client returned true from isRunning() after disconnect call:" + counter, !client.isRunning());

		// test when client attempts connect again before client finishes disconnect.
		client.connect(address, listenPort);
		TestTools.sleepForMS(10);
		client.disconnect();
		try {
			client.connect(address, listenPort);
			client.disconnect();
			fail("client should not be able to connect before disconnect is complete");
		} catch (Exception e) {
			// exception SHOULD occur - do not fail test
		} finally {
			client.disconnect();
		}

		// test another reconnect
		start = System.currentTimeMillis();
		while (client.isRunning() && System.currentTimeMillis() - start < timeoutMS) {
			// loop until client disconnects or timeout happens
		}
		System.out.printf("\tclient disconnected/timeout in %dms\n", (System.currentTimeMillis()) - start);

		client.connect(address, listenPort);
		assertTrue(client.isRunning());
		client.disconnect();

		TestTools.sleepForMS(100);
	}

	@Test
	public void testServerDisconnectWhileNotRunning() {
		try {
			server.disconnect();
		} catch (Exception e) {
			fail("A disconnect() to a non-running server caused an exception");
		}
		System.out.println("Server isRunning result: " + server.isRunning());
	}

	@Test
	public void testClientNotifiesServerOfUpcomingDisconnect() {
		// the client should send the server a message of its intent to disconnect so
		// that the server may remove it from it's allocated buffers. This helps
		// when the client is reconnecting with the server.
		// TODO very import test that might get overlooked, implement this.
		startServer();
		TestTools.sleepForMS(100);

		int cntNum = server.activeConnections();
		assertTrue("server did not have 0 connection,\n got: " + cntNum, cntNum == 0);

		try {
			client.connect(address, listenPort);
		} catch (FailedToConnect e) {
			fail("failed to connect client");
		}

		cntNum = server.activeConnections();
		client.disconnect();
		TestTools.sleepForMS(1000);
		assertTrue("server did not have 1 connection,\n got: " + cntNum, cntNum == 1);

		// sleep so server has time to remove the socket connection
		long delay = 1000;
		long start = System.currentTimeMillis();
		while (server.activeConnections() != 0 && System.currentTimeMillis() - start > delay) {
			TestTools.sleepForMS(100);
		}
		cntNum = server.activeConnections();
		assertTrue("server did not have 0 connection,\n got: " + cntNum, cntNum == 0);
	}

	@Test
	public void test5ClientsConnecting() throws IOException, FailedToConnect {
		// creat the 5 clients
		Client client1 = client;
		Client client2 = new Client();
		Client client3 = new Client();
		Client client4 = new Client();
		Client client5 = new Client();

		// start the server
		server.disconnect();
		while (server.isRunning()) {
			// wait for disconnect
			TestTools.sleepForMS(100);
		}
		server.run();
		// if (!server.isRunning()) server.run();

		client1.connect(address, listenPort);
		TestTools.sleepForMS(10);
		client2.connect(address, listenPort);
		TestTools.sleepForMS(10);
		client3.connect(address, listenPort);
		TestTools.sleepForMS(10);
		client4.connect(address, listenPort);
		TestTools.sleepForMS(10);
		client5.connect(address, listenPort);

		System.out.println("preparing to send 5 packets");
		boolean[] receivedPackets = new boolean[5];
		client1.queueToSend(new DemoConcretePacket(1, 0, 0, 0));
		client2.queueToSend(new DemoConcretePacket(2, 0, 0, 0));
		client3.queueToSend(new DemoConcretePacket(3, 0, 0, 0));
		client4.queueToSend(new DemoConcretePacket(4, 0, 0, 0));
		client5.queueToSend(new DemoConcretePacket(5, 0, 0, 0));
		System.out.println("5 packets sent");

		int connectedClient = server.activeConnections();
		assertEquals("server doesn't state 5 connected clients, instead", 5, connectedClient);

		int counter = 0;
		Long start = System.currentTimeMillis();
		while (server.hasReceivedPacket() || counter < 5) {
			DemoConcretePacket inPacket = (DemoConcretePacket) server.getNextReceivedPacket();
			if (inPacket != null) {
				System.out.printf("packet%d at: %dms\n", counter, System.currentTimeMillis() - start);
				receivedPackets[inPacket.getId() - 1] = true;
				counter++;
			} else {
				TestTools.sleepForMS(1);
			}
		}
		System.out.printf("received all 5 packets after: %dms\n", System.currentTimeMillis() - start);

		// test that all packets were received and reset the variables back to false
		for (int i = 0; i < receivedPackets.length; ++i) {
			assertTrue("did not receive packet: " + i, receivedPackets[i]);
		}

		client1.disconnect();
		client2.disconnect();
		client3.disconnect();
		client4.disconnect();
		client5.disconnect();
		// TODO: in CLIENT AND SERVER change all buffers to test if head is null, rather than using
		// size()
		fail("in CLIENT AND SERVER change all buffers to test if head is null, rather than using size()");
		fail("implementation is not complete");
	}

	@Test
	public void testThatDisconnectMessageReceivedExplicitly() {
		// note: this is really tested with the first three tests.
		// may drop this test.
		fail("not implemented");
	}

	public void startServer() {
		try {
			server.run();
		} catch (IOException e) {
			fail("failed to start server");
		}
	}

}

package enigma.engine.network.test.connect.disconnect.blackbox;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;

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
		while (server.activeConnections() != 0 && System.currentTimeMillis() - start < delay) {
			TestTools.sleepForMS(100);
		}
		cntNum = server.activeConnections();
		assertTrue("server did not have 0 connection,\n got: " + cntNum, cntNum == 0);
	}

	@Test
	public void testServerNotifiesClientOfUpcomingDisconnect() throws IOException, FailedToConnect {
		// create the 5 clients
		Client client1 = client;
		client.verbose = true;
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

		// connect the clients
		client1.connect(address, listenPort);
		TestTools.sleepForMS(10);
		client2.connect(address, listenPort);
		TestTools.sleepForMS(10);
		client3.connect(address, listenPort);
		TestTools.sleepForMS(10);
		client4.connect(address, listenPort);
		TestTools.sleepForMS(10);
		client5.connect(address, listenPort);

		// test that server has registered 5 connections (within 1 second)
		makeServerWaitForNumConnections(5, 1000);
		int connectedClient = server.activeConnections();
		assertEquals("server doesn't state 5 connected clients, instead", 5, connectedClient);

		// disconnect server and see if clients disconnect due to a server disconnect message
		server.disconnect();

		// give the server 1 second to to disconnect all connections
		waitForServerToDisconnectOrXseconds(1000);

		// give clients 1 second to receive and act on disconnect system message
		long start = System.currentTimeMillis();
		long delayMS = 1500;
		while ((client1.isRunning() || client2.isRunning() || client3.isRunning() || client4.isRunning() || client5.isRunning()) && System.currentTimeMillis() - start < delayMS) {
			TestTools.sleepForMS(1);
		}
		
		//capture the current state of each client
		boolean client1Disconnected = !client1.isRunning();
		boolean client2Disconnected = !client2.isRunning();
		boolean client3Disconnected = !client3.isRunning();
		boolean client4Disconnected = !client4.isRunning();
		boolean client5Disconnected = !client5.isRunning();
		
		//disconnect clients in case test fails, otherwise next test will throw exception.
		client1.disconnect();
		client2.disconnect();
		client3.disconnect();
		client4.disconnect();
		client5.disconnect();
		
		assertTrue("client 1 still running after ~1.5sec and disconnect", client1Disconnected);
		assertTrue("client 2 still running after ~1.5sec and disconnect", client2Disconnected);
		assertTrue("client 3 still running after ~1.5sec and disconnect", client3Disconnected);
		assertTrue("client 4 still running after ~1.5sec and disconnect", client4Disconnected);
		assertTrue("client 5 still running after ~1.5sec and disconnect", client5Disconnected);

		client1.verbose = false;
	}

	@Test
	public void test5ClientsConnecting() throws IOException, FailedToConnect {
		connect5();
		connect5();
	}

	public void connect5() throws FailedToConnect, IOException {
		// create the 5 clients
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
		makeServerWaitForNumConnections(0, 300);
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

		// make server send each client a packet
		server.queueToSend(new DemoConcretePacket(10, 0, 0, 0));

		// have the client's send the server packets
		System.out.println("preparing to send 5 packets");
		boolean[] receivedPackets = new boolean[5];
		client1.queueToSend(new DemoConcretePacket(1, 0, 0, 0));
		client2.queueToSend(new DemoConcretePacket(2, 0, 0, 0));
		client3.queueToSend(new DemoConcretePacket(3, 0, 0, 0));
		client4.queueToSend(new DemoConcretePacket(4, 0, 0, 0));
		client5.queueToSend(new DemoConcretePacket(5, 0, 0, 0));
		System.out.println("5 packets sent");

		// test that server has registered 5 connections
		makeServerWaitForNumConnections(5, 1000);
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
			receivedPackets[i] = false;
		}

		// test that each client received a server packet
		ArrayList<Client> clients = new ArrayList<Client>(Arrays.asList(client1, client2, client3, client4, client5));
		for (int i = 0; i < clients.size(); ++i) {
			client = clients.get(i);
			if (client.hasReceivedPacket()) {
				DemoConcretePacket packet = (DemoConcretePacket) client.getNextReceivedPacket();
				assertEquals("client's received packet didn't match what server sent", 10, packet.getId());
			} else {
				fail("a client did not receive a packet from the server.");
			}
		}

		client1.disconnect();
		client2.disconnect();
		client3.disconnect();
		client4.disconnect();
		client5.disconnect();

		long delayMS = 1000;
		start = System.currentTimeMillis();
		while ((client1.isRunning() || client2.isRunning() || client3.isRunning() || client4.isRunning() || client5.isRunning()) && System.currentTimeMillis() - start < delayMS) {
			TestTools.sleepForMS(1);
		}
		assertTrue("client 1 still running after 1sec and disconnect", !client1.isRunning());
		assertTrue("client 2 still running after 1sec and disconnect", !client2.isRunning());
		assertTrue("client 3 still running after 1sec and disconnect", !client3.isRunning());
		assertTrue("client 4 still running after 1sec and disconnect", !client4.isRunning());
		assertTrue("client 5 still running after 1sec and disconnect", !client5.isRunning());

		// give server at maximum 1 seconds to drop all connections
		makeServerWaitForNumConnections(0, 1000);
		int servConnections = server.activeConnections();

		assertEquals(String.format("Server had %d connections, not 0", servConnections), 0, servConnections);
	}

	public void makeServerWaitForNumConnections(int number, int delayMS) {
		int servConnections = server.activeConnections();
		// give server at maximum 1 seconds to drop all connections
		long start = System.currentTimeMillis();
		while (servConnections != 0 && System.currentTimeMillis() - start < delayMS) {
			TestTools.sleepForMS(1);
			servConnections = server.activeConnections();
		}
	}

	public void waitForServerToDisconnectOrXseconds(int delayMS) {
		// give server at maximum 1 seconds to drop all connections
		long start = System.currentTimeMillis();
		while (server.isRunning() && System.currentTimeMillis() - start < delayMS) {
			TestTools.sleepForMS(1);
		}
	}

	public void startServer() {
		try {
			server.run();
		} catch (IOException e) {
			fail("failed to start server");
		}
	}

}

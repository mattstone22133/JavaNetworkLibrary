package enigma.engine.network.test.client.blackbox;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import enigma.engine.network.Client;
import enigma.engine.network.FailedToConnect;
import enigma.engine.network.Server;
import enigma.engine.network.test.TestTools;

/**
 * @author Matt Stone There are instances where objects reference static variables without using
 *         class name. I did this for readability. For example, rather than saying
 *         TestClientMethods.client or testClientMethods.server, I simply use client and server. I
 *         do not do this practice in actual production code.
 *
 */
public class TestClientMethods {
	public static int listenPort = 25565;
	public static Client client;
	public static Server server;
	public static String address;

	@BeforeClass
	public static void setup() {
		try {
			client = new Client();
			server = new Server(listenPort);
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println();
			fail("failed to set up, previous test server closed?");
		}
		try {
			server.run();
		} catch (IOException e) {
			System.out.println("Could not make the server start running!");
		}
		try {
			address = InetAddress.getLocalHost().getHostAddress();
		} catch (UnknownHostException e) {
			fail("could not resolve host local address for testing");
		}
	}

	@AfterClass
	public static void teardown() {
		server.disconnect();
		client.disconnect();
		TestTools.sleepForMS(500);
		int counter = 0;
		while ((server.isRunning() || client.isRunning()) && counter < 10) {
			TestTools.sleepForMS(500);
			counter++;
		}
	}

	@Test
	public void testIsConnected() {
		// ---- Test method when client chooses to disconnect ----//
		connectClient("testIsConnected");
		assertTrue("client should be connected", client.isRunning());
		client.disconnect();
		assertTrue("client should not be connected!", client.isRunning());

		// ---- Test when server disconnects ----//
		long start = System.currentTimeMillis();
		long delayMS = 1000;
		while(client.isRunning() && System.currentTimeMillis() - start < delayMS){
			TestTools.sleepForMS(1);
		}
		connectClient("testIsConnected");
		server.disconnect();
		// give the server a maximum of 1 second to shut down
		start = System.currentTimeMillis();
		while (server.isRunning() && System.currentTimeMillis() - start < delayMS ) {
			TestTools.sleepForMS(10);
		}
		start = System.currentTimeMillis();
		while(client.isRunning() && System.currentTimeMillis() - start < delayMS){
			TestTools.sleepForMS(1);
		}
		assertTrue("Dependency on Server failure - Server should have disconnected within 1 second of disconnect call", !server.isRunning());
		assertTrue("Server shut down, client should sigal that it isn't connected", !client.isRunning());
		client.disconnect();
	}

	public void connectClient(String methodName) {
		// Connect Client;
		try {
			client.connect(TestClientMethods.address, TestClientMethods.listenPort);
		} catch (FailedToConnect e) {
			fail("failed to connect client to test the method in " + methodName);

		}
	}
}

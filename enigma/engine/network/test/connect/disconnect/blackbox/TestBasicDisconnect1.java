package enigma.engine.network.test.connect.disconnect.blackbox;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.InetAddress;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import enigma.engine.network.Client;
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
		if(!server.isRunning()){
			server.run();
		}

		int counter = 1;
		
		//Test that basic disconnect functionality works
		client.connect(address, listenPort);
		TestTools.sleepForMS(10);
		client.disconnect();
		long timeoutMS = 10000;
		long start = System.currentTimeMillis();
		
		while(client.isRunning() && System.currentTimeMillis() - start < timeoutMS){
			//loop until client disconnects or timeout happens 
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
		} finally{
			client.disconnect();
		}

		//test another reconnect
		start = System.currentTimeMillis();
		while(client.isRunning() && System.currentTimeMillis() - start < timeoutMS){
			//loop until client disconnects or timeout happens 
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
		try {
			client.connect(address, listenPort);
		} catch (FailedToConnect e) {
			System.out.println("failed to connect client");
		}

		int cntNum = server.activeConnections();
		client.disconnect();
		TestTools.sleepForMS(1000);
		assertTrue("server did not have 1 connection,\n got: " + cntNum, cntNum == 1);

		// sleep server so it has time to remove the socket connection
		TestTools.sleepForMS(100);
		cntNum = server.activeConnections();
		assertTrue("server did not have 0 connection,\n got: " + cntNum, cntNum == 0);
	}

	public void startServer() {
		try {
			server.run();
		} catch (IOException e) {
			fail("failed to start server");
		}
	}

}

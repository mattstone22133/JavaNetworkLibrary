package enigma.engine.network.test.server.blackbox;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.InetAddress;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import enigma.engine.network.Client;
import enigma.engine.network.Server;
import enigma.engine.network.test.TestTools;

public class TestServerPublicMethods {
	public static int listenPort = 25565;
	public static Client client;
	public static Server server;

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
	public void testRun() {
		try {
			server.run();
		} catch (IOException e) {
			fail("failed to start server");
			e.printStackTrace();
		}

		try {
			server.run();
		} catch (Exception e) {
			fail("calling run to an already running server caused an exception");
		}
	}

	@Test
	public void testIsRunning() {
		//@formatter:off
		
		//test running a server then calling isRunning
		try {server.run();} 
		catch (Exception e) {fail("failed to start a server - exception occured");}
		assertTrue("server should be running, it was just started", server.isRunning());
		
		// ---- test that isRunning after disconnect doesn't return false until threads are dead ----
		testIsRunningQuicklyAfterDisconnect();
		
		// ---- test that isRunning eventually terminates
		long start = System.currentTimeMillis();
		while(server.isRunning() && System.currentTimeMillis() - start < 1000){
			TestTools.sleepForMS(10);
		}
		assertTrue("Server should have disconnected within 1 second of disconnect call", !server.isRunning());
		
		//---- test that server isRunning properly signals even when a client is connected
		try {
			server.run();
		} catch (IOException e) {
			System.out.println("failed to start server running again");
		}
		testIsRunningQuicklyAfterDisconnectWithClient();
		start = System.currentTimeMillis();
		long delayMS = 1000;
		while(server.isRunning()){
			TestTools.sleepForMS(10);
			if(System.currentTimeMillis() - start > delayMS){
				fail(String.format("server was running after disconnect call for more than %dms.", delayMS));
			}
		}


		
		//@formatter:on
	}

	private void testIsRunningQuicklyAfterDisconnectWithClient() {
		// connect a client first
		try {
			client.connect(InetAddress.getLocalHost().getHostAddress(), listenPort);
		} catch (Exception e) {
			fail("failed to connect a client when trying to test server");
		}
		testIsRunningQuicklyAfterDisconnect();
		client.disconnect();// not trying to test client methods here.
	}

	private void testIsRunningQuicklyAfterDisconnect() {
		Thread listenThread = null;
		try {
			Field listeningThread = Server.class.getDeclaredField("listeningThread");
			listeningThread.setAccessible(true);
			listenThread = (Thread) listeningThread.get(server);
		} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
			fail("could not find the listening thread field");
		}
		server.disconnect();
		// must hope that this reached before listening thread ends its timeout and stops its loop.
		if (listenThread.isAlive()) {
			assertTrue("server hasn't stopped listening thread, but isRunning returns false", server.isRunning());
		} else {
			System.out.println("listen thread ended before test reached if statement to test isRunning");
			System.out.println("Advise: re-running test to catch this scenario.");
		}
	}

}

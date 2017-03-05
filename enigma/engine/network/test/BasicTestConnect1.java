package enigma.engine.network.test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import enigma.engine.network.Client;
import enigma.engine.network.FailedToConnect;
import enigma.engine.network.Server;

public class BasicTestConnect1 {
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
		while((server.isRunning() || client.isRunning() && counter < 10)){
			TestTools.sleepForMS(500);
			counter++;
		}
	}

	/**
	 * Test a client connecting to a server. No data exchanged.
	 */
	@Test
	public void test() {
		//Start a server
		try {
			server.run();
		} catch (IOException e) {
			fail("failed to start server from call to run");
		}
		TestTools.sleepForMS(30);
		assertTrue("Failed to start server", server.isRunning());
		
		//start a client!
		try {
			client.connect(InetAddress.getLocalHost().getHostAddress(), listenPort);
		} catch (UnknownHostException | FailedToConnect e) {
			fail("failed to connect client to server.\n" + e.toString());
		}
		
		//wait for server to start up threads before exiting test and tearing down connections
		TestTools.sleepForMS(100);
	}

}

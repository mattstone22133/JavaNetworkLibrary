package enigma.engine.network.test.twowayconnection;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.InetAddress;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import enigma.engine.network.Client;
import enigma.engine.network.Server;
import enigma.engine.network.test.LargePacket;
import enigma.engine.network.test.TestTools;
import enigma.engine.network.test.Timer;

public class BasicTwoWayTest7_BackAndForth_Sthread_LargePacket {

	private Client client;
	private Server server;
	private int listenPort = 25565;
	private String adr;
	private LargePacket clientPacket;
	private LargePacket serverPacket;
	int waitForPacket = 50;

	@Before
	public void setup() {
		try {
			adr = InetAddress.getLocalHost().getHostAddress();
			client = new Client();
			server = new Server(listenPort);
			server.run();
			TestTools.sleepForMS(100);
			client.connect(adr, listenPort);

			// 379 is roughly 1500 bytes, the size of ethernet frame
			serverPacket = new LargePacket(400);
			clientPacket = new LargePacket(400);

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

	public void clientSend() {
		client.queueToSend(clientPacket);
	}

	public void clientReceive() {
		Timer<String> time = new Timer<String>();
		time.newTimer("g");

		// wait 1 second (or quick early) for server to receive a packet.
		while (!client.hasReceivedPacket() && !time.timeUp("g", 1000)) {
			TestTools.sleepForMS(1);
		}

		LargePacket received = (LargePacket) client.getNextReceivedPacket();
		assertTrue("did not receive packet at client", received != null);
		assertTrue("packet had fewer fields that required", received.size() > 300);

	}

	public void serverSend() {
		server.queueToSend(serverPacket);
	}

	public void serverReceive() {
		Timer<String> time = new Timer<String>();
		time.newTimer("g");

		// wait 1 second (or quick early) for server to receive a packet.
		while (!server.hasReceivedPacket() && !time.timeUp("g", 1000)) {
			TestTools.sleepForMS(1);
		}

		LargePacket received = (LargePacket) server.getNextReceivedPacket();
		assertTrue("did not receive packet at server", received != null);
		assertTrue("packet had fewer fields that required", received.size() > 300);

	}

	/**
	 * Testing that a the communication can be in two directions.
	 */
	@Test
	public void testLargePacketSend1() {
		for (int i = 0; i < 100; ++i) {
			clientSend();
			serverReceive();
			serverSend();
			clientReceive();
		}
	}

}

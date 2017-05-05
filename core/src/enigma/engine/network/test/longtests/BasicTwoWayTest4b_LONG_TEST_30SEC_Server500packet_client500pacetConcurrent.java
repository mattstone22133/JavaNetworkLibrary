package enigma.engine.network.test.longtests;

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

public class BasicTwoWayTest4b_LONG_TEST_30SEC_Server500packet_client500pacetConcurrent {

	private Client client;
	private Server server;
	private int listenPort = 25565;
	int waitForPacket = 2000;
	// private DemoConcretePacket clientPacket;
	// private DemoConcretePacket serverPacket;

	// Below variables are due to junit not able to fail a test within separate
	// thread.
	volatile boolean failOccured = false;
	volatile boolean idFail = false;
	volatile boolean xFail = false;
	volatile boolean yFail = false;
	volatile boolean rotFail = false;
	volatile boolean sendAndReceiveSameInstanceFail = false;
	volatile boolean instanceLastWasSameFail = false;
	volatile float expected = 0;
	volatile float was = 0;
	volatile boolean errorWasClient = false;
	volatile boolean incorrectNumberOfPackets = false;

	@Before
	public void setup() {
		failOccured = false;
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
	 * Sends 500 packets both ways concurrently. However, this test waits enough time for all 500
	 * packets to be sent. Another test (BasicTwowayTest5) tests exchanges where server and clients
	 * do not wait.
	 */
	@Test
	public void testTwoWay3a_Connection500ConcurrentPkt() {
		// Long start = System.currentTimeMillis();
		setUpServerAndClient();

		// Create a way to alert user that server failed to run.
		Thread.UncaughtExceptionHandler handler = new Thread.UncaughtExceptionHandler() {
			public void uncaughtException(Thread th, Throwable ex) {
				fail("concurrent test failed: " + ex);
				failOccured = true;
			}
		};

		// basic client send test
		Thread clientT = new Thread(new Runnable() {
			public void run() {
				clientSend_500();
			}
		});

		// basic server send test
		Thread serverT = new Thread(new Runnable() {
			public void run() {
				serverSend_500();
			}
		});

		clientT.setUncaughtExceptionHandler(handler);
		serverT.setUncaughtExceptionHandler(handler);
		clientT.start();
		serverT.start();

		// sleep the test to give time for concurent sends
		sleepForMS(waitForPacket * 2);
		assertTrue("client thread still running after: " + waitForPacket * 2, !clientT.isAlive());
		assertTrue("server thread still running after: " + waitForPacket * 2, !serverT.isAlive());

		assertTrue(generateErrorMessage(), !failOccured);
	}

	private String generateErrorMessage() {
		StringBuffer sb = new StringBuffer();
		if (failOccured) {
			if (errorWasClient) {
				sb.append("client");
			} else {
				sb.append("server");
			}
			sb.append(" encountered an error.\n ");

			if (idFail)
				sb.append("Id's did not match, expected: " + expected + " got: " + was);
			else if (xFail)
				sb.append("x vals did not match, expected: " + expected + " got: " + was);
			else if (yFail)
				sb.append("y vals did not match, expected: " + expected + " got: " + was);
			else if (rotFail)
				sb.append("rot vals did not match, expected: " + expected + " got: " + was);
			else if (instanceLastWasSameFail)
				sb.append("duplicate packet instance received");
			else if (sendAndReceiveSameInstanceFail) sb.append("sent pack and sreceived packet are same instance");
			else if (incorrectNumberOfPackets) sb.append("packet count did not match, expected: " + expected + " got: " + was);
		}
		return sb.toString();
	}

	private void sleepForMS(int ms) {
		try {
			Thread.sleep(ms);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
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
		System.out.println("\tServer and client connected at: " + (System.currentTimeMillis() - start - sleepForConnectMS) + "ms from start.");
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
			fail("client did not identify that it received a packet after" + waitForPacket + "ms.");
		}
		// check if packet was received at server
		while (client.hasReceivedPacket()) {
			DemoConcretePacket packet = packets.get(counter);
			DemoConcretePacket pkt = (DemoConcretePacket) client.getNextReceivedPacket();
			System.out.println("\tClient - Time to process receive: " + (System.currentTimeMillis() - receiveStart) + "ms.");

			if(counter % 100 == 0){
				sleepForMS(30);
			}
			
			// Only record failures if no failure has happened yet
			if (!failOccured) {
				// Test ID
				if (packet.getId() != packet.getId()) {
					failOccured = true;
					idFail = true;
					expected = packet.getId();
					was = pkt.getId();
				}
				if (!TestTools.floatEqualsCompare(packet.getX(), pkt.getX(), 0.001f) && !failOccured) {
					failOccured = true;
					xFail = true;
					expected = packet.getX();
					was = pkt.getX();
				}
				if (!TestTools.floatEqualsCompare(packet.getY(), pkt.getY(), 0.001f) && !failOccured) {
					failOccured = true;
					yFail = true;
					expected = packet.getY();
					was = pkt.getY();
				}
				if (!TestTools.floatEqualsCompare(packet.getRotation(), pkt.getRotation(), 0.001f) && !failOccured) {
					failOccured = true;
					rotFail = true;
					expected = packet.getRotation();
					was = pkt.getRotation();
				}
				if (pkt == packet && !failOccured) {
					failOccured = true;
					sendAndReceiveSameInstanceFail = true;
				}
				if (pkt == lastPacket && !failOccured) {
					failOccured = true;
					instanceLastWasSameFail = true;
				}
				if (failOccured) {
					errorWasClient = false;
				}
			}

			counter++;
			lastPacket = pkt;
		}
		if (counter != packets.size()) {
			failOccured = true;
			incorrectNumberOfPackets = true;
			expected = packets.size();
			was = counter;
			errorWasClient = false;
		}
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
			System.out.println("\tServer - Time to process receive: " + (System.currentTimeMillis() - receiveStart) + "ms.");
			// Only record failures if no failure has happened yet
			if (!failOccured) {
				// Test ID
				if (packet.getId() != packet.getId()) {
					failOccured = true;
					idFail = true;
					expected = packet.getId();
					was = pkt.getId();
				}
				if (!TestTools.floatEqualsCompare(packet.getX(), pkt.getX(), 0.001f) && !failOccured) {
					failOccured = true;
					xFail = true;
					expected = packet.getX();
					was = pkt.getX();
				}
				if (!TestTools.floatEqualsCompare(packet.getY(), pkt.getY(), 0.001f) && !failOccured) {
					failOccured = true;
					yFail = true;
					expected = packet.getY();
					was = pkt.getY();
				}
				if (!TestTools.floatEqualsCompare(packet.getRotation(), pkt.getRotation(), 0.001f) && !failOccured) {
					failOccured = true;
					rotFail = true;
					expected = packet.getRotation();
					was = pkt.getRotation();
				}
				if (pkt == packet && !failOccured) {
					failOccured = true;
					sendAndReceiveSameInstanceFail = true;
				}
				if (pkt == lastPacket && !failOccured) {
					failOccured = true;
					instanceLastWasSameFail = true;
				}
				if (failOccured) {
					errorWasClient = true;
				}
			}
			counter++;
			lastPacket = pkt;
		}
		if (counter != packets.size()) {
			failOccured = true;
			incorrectNumberOfPackets = true;
			expected = packets.size();
			was = counter;
			errorWasClient = true;
		}
	}
}

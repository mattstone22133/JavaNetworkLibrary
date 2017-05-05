package enigma.engine.network.test.stresstest;

import static org.junit.Assert.fail;

import java.net.InetAddress;
import java.util.Scanner;

import enigma.engine.network.Client;
import enigma.engine.network.Server;
import enigma.engine.network.test.LargePacket;
import enigma.engine.network.test.TestTools;
import enigma.engine.network.test.Timer;

public class LargePacket_StressTest {

	private Client client;
	private Server server;
	private int listenPort = 25565;
	private String adr;
	private LargePacket clientPacket;
	private LargePacket serverPacket;
	int waitForPacket = 50;

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
		if (received == null) {
			System.out.println("did not receive packet at server");
		}

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
		if (received == null) {
			System.out.println("did not receive packet at client");
		}

	}

	public void exchange() {
		clientSend();
		serverReceive();
		serverSend();
		clientReceive();
	}

	private volatile boolean continueRunning = true;
	private volatile int bytes = 0;
	private volatile long delayTyped = 1L;

	/**
	 * Testing that a the communication can be in two directions.
	 */
	public void testLargePacketSend1() {
		Scanner kb = new Scanner(System.in);
		
		System.out.println("Enter number of bytes to store in packet");
		bytes = kb.nextInt() / 4;
		
		System.out.println("Enter number of microseconds between client/server exchange.");
		System.out.println("Hint: 14 will give you 60 packets per second.");
		delayTyped = kb.nextLong();
		
		// clear buffer
		kb.nextLine();

		serverPacket = new LargePacket(bytes);
		clientPacket = new LargePacket(bytes);

		Thread talk = new Thread(new Runnable() {
			public void run() {
				int i = 1;
				while (continueRunning) {
					exchange();
					TestTools.sleepForMS(delayTyped);

					// print out every X number of packets
					if (i % 500 == 0) {
						System.out.println("\n" + (i * 2) + " packets sent of size:" + 4 * bytes);
						System.out.println("You should monitor CPU usage and Memory now.\nType q to quit");

					}
					++i;
				}
			}
		});

		talk.start();
		System.out.println("You should monitor CPU usage and Memory now.\nType q to quit");
		kb.nextLine();
		continueRunning = false;
		kb.close();
	}

	public static void main(String[] args) {
		LargePacket_StressTest obj = new LargePacket_StressTest();
		
		obj.setup();
		obj.testLargePacketSend1();
		obj.teardown();
	}
}

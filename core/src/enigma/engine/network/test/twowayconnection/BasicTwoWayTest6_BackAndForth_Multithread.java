package enigma.engine.network.test.twowayconnection;

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

public class BasicTwoWayTest6_BackAndForth_Multithread {


	private Client client;
	private Server server;
	private int listenPort = 25565;
	private volatile int startID = 1;
	int waitForPacket = 50;

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
	private int messageID = startID;
	private boolean threadsShouldLive = true;

	
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
	 * Testing that a the communication can be in two directions.
	 */
	@Test
	public void test_ClientStart_MultiThreaded() {
		setUpServerAndClient();
		Thread clientThread = new Thread( new Runnable() {
			public void run() {
				for(int i = 0; i < 500; ++i){
					clientSend();
					clientReceive();
				}
			}
		});
		Thread serverThread = new Thread(new Runnable() {
			public void run() {
				serverEcho();
			}
		});
		
		
		clientThread.start();
		serverThread.start();
		TestTools.sleepForMS(1000);
		
		assertTrue("an out of order packet occured during multithreaded transmission", !failOccured);
	}
	
	/**
	 * Testing that a the communication can be in two directions.
	 */
	@Test
	public void test_ServerStart_MultiThreaded() {
		setUpServerAndClient();
		Thread serverThread = new Thread( new Runnable() {
			public void run() {
				for(int i = 0; i < 500; ++i){
					serverSend();
					serverReceive();
				}
			}
		});
		Thread clientThread = new Thread(new Runnable() {
			public void run() {
				clientEcho();
			}
		});
		
		serverThread.start();
		clientThread.start();
		TestTools.sleepForMS(1000);
		
		assertTrue("an out of order packet occured during multithreaded transmission", !failOccured);
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
	
	public void clientSend(){
		DemoConcretePacket send = new DemoConcretePacket(messageID +1, 0, 0, 0);
		client.queueToSend(send);
	}
	public void clientReceive(){
		DemoConcretePacket packet = null;
		while(packet == null){
			if(client.hasReceivedPacket()){
				packet = (DemoConcretePacket) client.getNextReceivedPacket();
				if(messageID + 2 != packet.getId()){
					failOccured = true;
				}				
			} else {
				TestTools.sleepForMS(1);
			}
		}
	}
	
	public void serverEcho(){
		while(threadsShouldLive){
			if(server.hasReceivedPacket()){
				DemoConcretePacket packet = (DemoConcretePacket) server.getNextReceivedPacket();
				DemoConcretePacket echoPacket = (DemoConcretePacket) packet.makeCopy();
				echoPacket.setId(echoPacket.getId() + 1);
				server.queueToSend(echoPacket);
			}else {
				TestTools.sleepForMS(1);
			}
		}
	}
	
	public void serverSend(){
		DemoConcretePacket send = new DemoConcretePacket(messageID +1, 0, 0, 0);
		server.queueToSend(send);
	}
	public void serverReceive(){
		DemoConcretePacket packet = null;
		while(packet == null){
			if(server.hasReceivedPacket()){
				packet = (DemoConcretePacket) server.getNextReceivedPacket();
				if(messageID + 2 != packet.getId()){
					failOccured = true;
				}				
			} else {
				TestTools.sleepForMS(1);
			}
		}
	}
	
	public void clientEcho(){
		while(threadsShouldLive){
			if(client.hasReceivedPacket()){
				DemoConcretePacket packet = (DemoConcretePacket) client.getNextReceivedPacket();
				DemoConcretePacket echoPacket = (DemoConcretePacket) packet.makeCopy();
				echoPacket.setId(echoPacket.getId() + 1);
				client.queueToSend(echoPacket);
			}else {
				TestTools.sleepForMS(1);
			}
		}
	}
	

}

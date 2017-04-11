package enigma.engine.network.test.id;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.InetAddress;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import enigma.engine.network.Client;
import enigma.engine.network.DemoConcretePacket;
import enigma.engine.network.FailedToConnect;
import enigma.engine.network.NetworkPlayer;
import enigma.engine.network.Server;
import enigma.engine.network.test.Timer;

public class TestIDInstances {

	private Server server;
	private Client client1;
	private Client client2;
	private Timer<String> timer = new Timer<String>();

	@Before
	public void setup() throws IOException, FailedToConnect, InterruptedException {
		int port = 25565;
		server = new Server(port);
		client1 = new Client();
		client2 = new Client();

		server.run();

		// loop until server has connected or timeout
		timer.newTimer("server");
		while (!server.isRunning() && !timer.timeUp("server", 1000)) {
			Thread.sleep(1);
		}

		String address = InetAddress.getLocalHost().getHostAddress();
		client1.connect(address, port);
		timer.newTimer("client1");
		while (!client1.isRunning() && !timer.timeUp("client1", 1000)) {
			Thread.sleep(1);
		}
		client2.connect(address, port);
		timer.newTimer("client2");
		while (!client2.isRunning() && !timer.timeUp("client2", 1000)) {
			Thread.sleep(1);
		}

		timer.newTimer("g");
		System.out.println("setup complete");

	}

	@After
	public void tearDown() throws InterruptedException {
		server.disconnect();
		client1.disconnect();
		client2.disconnect();

		timer.startTimer("server");
		while (timer.timeUp("server", 1000) && (client1.isRunning() || client2.isRunning() || server.isRunning())) {
			// wait for the clients and server to disconnect.
			Thread.sleep(1);
		}
	}

	@Test
	public void testClientIdInstancesUpdate() throws InterruptedException {
		// give time for the client to receive its ID and process it.
		NetworkPlayer c1ID = client1.getPlayerObject();
		timer.startTimer("g");
		while (c1ID == null && !timer.timeUp("g", 500)) {
			c1ID = client1.getPlayerObject();
		}

		NetworkPlayer c2ID = client2.getPlayerObject();
		timer.startTimer("g");
		// give time for the client to receive its ID and process it.
		while (c2ID == null && !timer.timeUp("g", 500)) {
			c2ID = client2.getPlayerObject();
		}

		NetworkPlayer sID = server.getHostPlayerObj();

		// test that ID's are unique
		assertTrue("id's are not unique for client1", c1ID.getID() != c2ID.getID() && c1ID.getID() != sID.getID());
		assertTrue("id's are not unique for client2", c2ID.getID() != sID.getID());

		// test that data sent can be tied with ID
		testUsingIDs(c1ID, c2ID, sID);
	}

	private void testUsingIDs(NetworkPlayer c1id, NetworkPlayer c2id, NetworkPlayer sID) throws InterruptedException {
		// Some data to send
		float c1r = 100;
		float c2r = -100;
		float sr = 0;
		float decrementAmount = 50;

		// have first client send
		makeClientSendData(client1, c1id, c1r);
		makeServerDecrementRotationOfReceivedAndRespond(decrementAmount, sr);
		c1r = makeClientUpdateDataFromServerData(client1, c1id, c1r);
		c2r = makeClientUpdateDataFromServerData(client2, c2id, c2r);
		assertTrue(c1r == 100 - decrementAmount);
		assertTrue(c2r == -100);

		// have second client send
		makeClientSendData(client2, c2id, c2r);
		makeServerDecrementRotationOfReceivedAndRespond(decrementAmount, sr);
		c1r = makeClientUpdateDataFromServerData(client1, c1id, c1r);
		c2r = makeClientUpdateDataFromServerData(client2, c2id, c2r);
		assertTrue(c1r == 50);
		assertTrue(c2r == -150);
		
		// have first client send a second message
		makeClientSendData(client1, c1id, c1r);
		sr = makeServerDecrementRotationOfReceivedAndRespond(decrementAmount, sr);
		c1r = makeClientUpdateDataFromServerData(client1, c1id, c1r);
		c2r = makeClientUpdateDataFromServerData(client2, c2id, c2r);
		assertTrue(c1r == 50 - decrementAmount);
		assertTrue(c2r == -150);
		assertTrue(sr == 0);

		
		// have second client send second packet
		makeClientSendData(client2, c2id, c2r);
		sr = makeServerDecrementRotationOfReceivedAndRespond(decrementAmount, sr);
		c1r = makeClientUpdateDataFromServerData(client1, c1id, c1r);
		c2r = makeClientUpdateDataFromServerData(client2, c2id, c2r);
		assertTrue(c1r == 0);
		assertTrue(c2r == -200);
		assertTrue(sr == 0);

		
		//test that server handling of change
		makeClientSendData(client2, sID, sr);
		sr = makeServerDecrementRotationOfReceivedAndRespond(decrementAmount, sr);
		c1r = makeClientUpdateDataFromServerData(client1, c1id, c1r);
		c2r = makeClientUpdateDataFromServerData(client2, c2id, c2r);
		assertTrue(c1r == 0);
		assertTrue(c2r == -200);
		assertTrue(sr == -50);
	}

	private float makeServerDecrementRotationOfReceivedAndRespond(float decrementAmount, float serverRotation) throws InterruptedException {
				
		timer.startTimer("g");
		// give the server 100ms to extract the packet from client.
		while (!timer.timeUp("g", 100) && !server.hasReceivedPacket()) {
			Thread.sleep(1);
		}

		DemoConcretePacket packet = (DemoConcretePacket) server.getNextReceivedPacket();
		
		if(packet.getId() == server.getHostPlayerObj().getID()){
			serverRotation -= decrementAmount;
		}
		
		DemoConcretePacket send = new DemoConcretePacket(packet.getId(), 0, 0, packet.getRotation() - decrementAmount);
		server.queueToSend(send);
		
		return serverRotation;
	}

	private void makeClientSendData(Client client, NetworkPlayer idObj, float rotation) {
		DemoConcretePacket c1p1 = new DemoConcretePacket(idObj.getID(), 0, 0, rotation);
		client1.queueToSend(c1p1);
	}

	private float makeClientUpdateDataFromServerData(Client client, NetworkPlayer idObj, float curRotation) throws InterruptedException {
		timer.startTimer("g");
		// wait a 10th of a second for the client to receive a packet.
		while (!timer.timeUp("g", 100) && !client.hasReceivedPacket()) {
			Thread.sleep(1);
		}
		if (client.hasReceivedPacket()) {
			DemoConcretePacket packet = (DemoConcretePacket) client.getNextReceivedPacket();

			if (packet.getId() == idObj.getID()) {
				return packet.getRotation();
			} else {
				return curRotation;
			}

		} else {
			return 0.0f;
		}
	}

}

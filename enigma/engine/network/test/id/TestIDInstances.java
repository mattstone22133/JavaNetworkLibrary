package enigma.engine.network.test.id;

import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.InetAddress;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import enigma.engine.network.Client;
import enigma.engine.network.FailedToConnect;
import enigma.engine.network.Server;
import enigma.engine.network.test.Timer;

public class TestIDInstances {

	private Server server;
	private Client client1;
	private Client client2;
	private Timer<String> timer = new Timer<String>();

	@Before
	public void setup() throws IOException, FailedToConnect, InterruptedException{
		int port = 25565;
		server = new Server(port);
		client1 = new Client();
		client2 = new Client();
		
		server.run();
		
		//loop until server has connected or timeout
		timer.newTimer("server");
		while(!server.isRunning() && !timer.timeUp("server", 1000)){
			Thread.sleep(1);
		}
		
		String address = InetAddress.getLocalHost().getHostAddress();
		client1.connect(address, port);
		timer.newTimer("client1");
		while(!client1.isRunning() && !timer.timeUp("client1", 1000)){
			Thread.sleep(1);
		}
		client2.connect(address, port);
		timer.newTimer("client2");
		while(!client2.isRunning() && !timer.timeUp("client2", 1000)){
			Thread.sleep(1);
		}
		
		System.out.println("setup complete");

	}
	
	@After
	public void tearDown() throws InterruptedException{
		server.disconnect();
		client1.disconnect();
		client2.disconnect();
		
		timer.startTimer("server");
		while(timer.timeUp("server", 1000) && (client1.isRunning() || client2.isRunning() || server.isRunning())){
			//wait for the clients and server to disconnect.
			Thread.sleep(1);
		}
	}
	
	@Test
	public void testIdInstancesUpdate() {
		fail("Not yet implemented");
	}

}

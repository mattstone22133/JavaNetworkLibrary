package enigma.engine.network.test;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import enigma.engine.network.test.basictests.BasicTestConnect4_ClientSendServer5UniquePackets;
import enigma.engine.network.test.basictests.BasicTestConnect5_ServerSendClient5UniquePackets;
import enigma.engine.network.test.client.blackbox.TestClientMethods;
import enigma.engine.network.test.connect.disconnect.blackbox.TestBasicDisconnect1;
import enigma.engine.network.test.id.TestIDInstances;
import enigma.engine.network.test.id.TestIDManager;
import enigma.engine.network.test.server.blackbox.TestServerPublicMethods;
import enigma.engine.network.test.twowayconnection.BasicTwoWayTest2_Server5packet_client5packet;
import enigma.engine.network.test.twowayconnection.BasicTwoWayTest5_BackAndForth_SingleThread;
import enigma.engine.network.test.twowayconnection.BasicTwoWayTest6_BackAndForth_Multithread;

@RunWith(Suite.class)
@SuiteClasses({ 
	BasicTestConnect4_ClientSendServer5UniquePackets.class,
	BasicTestConnect5_ServerSendClient5UniquePackets.class,
	BasicTestConnect5_ServerSendClient5UniquePackets.class,
	BasicTwoWayTest5_BackAndForth_SingleThread.class,
	BasicTwoWayTest6_BackAndForth_Multithread.class,
	BasicTwoWayTest2_Server5packet_client5packet.class,
	TestServerPublicMethods.class,
	TestBasicDisconnect1.class,
	TestClientMethods.class,
	TestServerPublicMethods.class,
	TestIDManager.class,
	TestIDInstances.class,
	})
public class QuickTests {

}

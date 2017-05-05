package enigma.engine.network.test.basictests;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({ BasicTestConnect1.class, BasicTestConnect2_ClientSendData.class,
		BasicTestConnect3_ServerSendToClient.class, BasicTestConnect4_ClientSendServer5UniquePackets.class,
		BasicTestConnect5_ServerSendClient5UniquePackets.class,
		BasicTestConnect6_ClientSendServer500UniquePackets.class,
		BasicTestConnect7_ServerSendClient500UniquePackets2.class,})
public class AllBasicTests {
	
}

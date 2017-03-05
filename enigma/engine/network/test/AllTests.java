package enigma.engine.network.test;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({ BasicTestConnect1.class, BasicTestConnect2_ClientSendData.class, BasicTestConnect3_ServerSendToClient.class, TestClientBasicDisconnect.class, TestServerClientTimeout.class, TestServerDisconnect.class,
		TestServerDisconnectReconnect.class, TestServerDisconnectReconnectDisonnectReconnectDisconnect.class, TestServerWhenClientDisconnect.class, TestTimeToRemoveFromReceiveQueue.class })
public class AllTests {

}

package enigma.engine.network.test.connect.disconnect;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({ TestClientBasicDisconnect.class, TestServerClientTimeout.class, TestServerDisconnect.class,
		TestServerDisconnectReconnect.class, TestServerDisconnectReconnectDisonnectReconnectDisconnect.class,
		TestServerWhenClientDisconnect.class, TestTimeToRemoveFromReceiveQueue.class })
public class AllConnectDisconnectTests {

}

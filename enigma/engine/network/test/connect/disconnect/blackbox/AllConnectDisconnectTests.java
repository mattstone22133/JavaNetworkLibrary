package enigma.engine.network.test.connect.disconnect.blackbox;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({ TestBasicDisconnect1.class, TestServerClientTimeout.class, TestServerDisconnect.class,
		TestServerDisconnectReconnect.class, TestServerDisconnectReconnectDisonnectReconnectDisconnect.class,
		TestServerWhenClientDisconnect.class, TestTimeToRemoveFromReceiveQueue.class })
public class AllConnectDisconnectTests {

}

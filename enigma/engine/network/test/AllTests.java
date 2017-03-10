package enigma.engine.network.test;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import enigma.engine.network.test.BasicTests.AllBasicTests;
import enigma.engine.network.test.connect.disconnect.AllConnectDisconnectTests;
import enigma.engine.network.test.listentests.AllListenTests;
import enigma.engine.network.test.twowayconnection.AllTwoWayTests;

@RunWith(Suite.class)
@SuiteClasses({ AllBasicTests.class, AllTwoWayTests.class, AllConnectDisconnectTests.class, AllListenTests.class })
public class AllTests {
	// The abnormally long tests are not included in this suite; however, this suite is designed to
	// test complete system level functionality.
}

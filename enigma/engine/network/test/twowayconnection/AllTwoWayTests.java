package enigma.engine.network.test.twowayconnection;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({ BasicTwoWayTest1_Server1packet_client1packet.class, BasicTwoWayTest2_Server5packet_client5packet.class, BasicTwoWayTest3_Server500packet_client500packet.class, BasicTwoWayTest4_Server500packet_client500pacetConcurrent.class,
		BasicTwoWayTest5_BackAndForth_SingleThread.class })
public class AllTwoWayTests {

}

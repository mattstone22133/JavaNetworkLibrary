package enigma.engine.network.test.id;

import static org.junit.Assert.*;

import org.junit.Test;

import enigma.engine.network.IDManager;

public class TestIDManager {

	@Test
	public void testVarietyOfIDManagerFunctionality() {
		IDManager idManager = new IDManager(2);		
		
		//attempt to find bug in repeated use
		basicIdFunctinalityTestWith2IDsCap(idManager);
		basicIdFunctinalityTestWith2IDsCap(idManager);
		basicIdFunctinalityTestWith2IDsCap(idManager);
		basicIdFunctinalityTestWith2IDsCap(idManager);
		basicIdFunctinalityTestWith2IDsCap(idManager);

	}
	
	public void basicIdFunctinalityTestWith2IDsCap(IDManager idManager){
		char firstID = idManager.getReservedIDAndRemoveFromIDPool();
		char secondID = idManager.getReservedIDAndRemoveFromIDPool();
		idManager.unReserveIDAndReturnIdToPool(firstID);
		char thirdID = idManager.getReservedIDAndRemoveFromIDPool();

		//third id should be the first ID recycled
		assertTrue("the first ID and Third ID should be the same since they were recycled.", thirdID == firstID);
		assertTrue("The id manager should not have any more ids available", !idManager.hasMoreIds());
		assertEquals("Expected there to be 2 active ids", 2, idManager.getActiveNumberIds());	
		
		idManager.unReserveIDAndReturnIdToPool(secondID);
		assertTrue("Since id was just freed, the manager should have an ID available", idManager.hasMoreIds());
		assertEquals("Expected there to be 1 active id", 1, idManager.getActiveNumberIds());	
		
		idManager.unReserveIDAndReturnIdToPool(thirdID);
		assertTrue("Since id was just freed, the manager should have an ID available", idManager.hasMoreIds());
		assertEquals("Expected there to be 0 active ids", 0, idManager.getActiveNumberIds());	
		
	}
}

package org.infinispan.api.mvcc.read_committed;

import org.infinispan.api.CacheAPITest;
import org.infinispan.util.concurrent.IsolationLevel;
import org.testng.annotations.Test;

@Test(groups = "functional", testName = "api.mvcc.read_committed.CacheAPIMVCCTest")
public class CacheAPIMVCCTest extends CacheAPITest {
   @Override
   protected IsolationLevel getIsolationLevel() {
      return IsolationLevel.READ_COMMITTED;
   }
}

package org.infinispan.tx;

import org.infinispan.config.Configuration;
import org.infinispan.transaction.tm.DummyTransaction;
import org.infinispan.transaction.tm.DummyXid;
import org.infinispan.transaction.xa.GlobalTransaction;
import org.infinispan.transaction.xa.GlobalTransactionFactory;
import org.infinispan.transaction.xa.LocalTransaction;
import org.infinispan.transaction.xa.TransactionTable;
import org.infinispan.transaction.xa.TransactionXaAdapter;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;

/**
 * @author Mircea.Markus@jboss.com
 * @since 4.2
 */
@Test(testName = "tx.TransactionXaAdapterTest", groups = "unit")
public class TransactionXaAdapterTmIntegrationTest {
   private Configuration configuration;
   private TransactionTable txTable;
   private GlobalTransaction globalTransaction;
   private LocalTransaction localTx;
   private TransactionXaAdapter xaAdapter;
   private DummyXid xid;

   @BeforeMethod
   public void setUp() {
      txTable = new TransactionTable();
      GlobalTransactionFactory gtf = new GlobalTransactionFactory();
      globalTransaction = gtf.newGlobalTransaction(null, false);
      localTx = new LocalTransaction(new DummyTransaction(null), globalTransaction);
      xid = new DummyXid();
      localTx.setXid(xid);
      txTable.addLocalTransactionMapping(localTx);      

      configuration = new Configuration();
      xaAdapter = new TransactionXaAdapter(localTx, txTable, null, configuration, null, null);
   }

   public void testPrepareOnNonexistentXid() {
      DummyXid xid = new DummyXid();
      try {
         xaAdapter.prepare(xid);
         assert false;
      } catch (XAException e) {
         assert e.errorCode == XAException.XAER_NOTA;
      }
   }

   public void testCommitOnNonexistentXid() {
      DummyXid xid = new DummyXid();
      try {
         xaAdapter.commit(xid, false);
         assert false;
      } catch (XAException e) {
         assert e.errorCode == XAException.XAER_NOTA;
      }
   }

   public void testRollabckOnNonexistentXid() {
      DummyXid xid = new DummyXid();
      try {
         xaAdapter.rollback(xid);
         assert false;
      } catch (XAException e) {
         assert e.errorCode == XAException.XAER_NOTA;
      }
   }

   public void testPrepareTxMarkedForRollback() {
      localTx.markForRollback();
      try {
         xaAdapter.prepare(xid);
         assert false;
      } catch (XAException e) {
         assert e.errorCode == XAException.XA_RBROLLBACK;
      }
   }

   public void testOnePhaseCommitConfigured() throws XAException {
      configuration.setCacheMode(Configuration.CacheMode.INVALIDATION_ASYNC);//this would force 1pc
      assert XAResource.XA_OK == xaAdapter.prepare(xid);
   }

   public void test1PcAndNonExistentXid() {
      configuration.setCacheMode(Configuration.CacheMode.INVALIDATION_ASYNC);
      try {
         DummyXid doesNotExists = new DummyXid();
         xaAdapter.commit(doesNotExists, false);
         assert false;
      } catch (XAException e) {
         assert e.errorCode == XAException.XAER_NOTA;
      }
   }

   public void test1PcAndNonExistentXid2() {
      configuration.setCacheMode(Configuration.CacheMode.DIST_SYNC);
      try {
         DummyXid doesNotExists = new DummyXid();
         xaAdapter.commit(doesNotExists, true);
         assert false;
      } catch (XAException e) {
         assert e.errorCode == XAException.XAER_NOTA;
      }
   }
}

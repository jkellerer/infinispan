package org.infinispan.client.hotrod;

import org.infinispan.Cache;
import org.infinispan.client.hotrod.impl.transport.tcp.TcpTransportFactory;
import org.infinispan.config.Configuration;
import org.infinispan.lifecycle.ComponentStatus;
import org.infinispan.manager.CacheManager;
import org.infinispan.server.hotrod.HotRodServer;
import org.infinispan.test.MultipleCacheManagersTest;
import org.infinispan.test.TestingUtil;
import org.infinispan.test.fwk.TestCacheManagerFactory;
import org.testng.annotations.Test;

import java.net.InetSocketAddress;

import static org.testng.AssertJUnit.assertEquals;

/**
 * // TODO: Document this
 *
 * @author Mircea.Markus@jboss.com
 * @since 4.1
 */
@Test (testName = "client.hotrod.MultipleCacheManagersTest", groups = "functional")
public class TopologyChangeTest extends MultipleCacheManagersTest {

   HotRodServer hotRodServer1;
   HotRodServer hotRodServer2;
   HotRodServer hotRodServer3;

   RemoteCache remoteCache;
   private RemoteCacheManager remoteCacheManager;
   private TcpTransportFactory tcpConnectionFactory;
   private Configuration config;

   @Override
   protected void assertSupportedConfig() {      
   }

   @Override
   protected void createCacheManagers() throws Throwable {
      config = getDefaultClusteredConfig(Configuration.CacheMode.DIST_SYNC);
      CacheManager cm1 = TestCacheManagerFactory.createClusteredCacheManager(config);
      CacheManager cm2 = TestCacheManagerFactory.createClusteredCacheManager(config);
      registerCacheManager(cm1);
      registerCacheManager(cm2);

      hotRodServer1 = TestHelper.startHotRodServer(manager(0));
      hotRodServer2 = TestHelper.startHotRodServer(manager(1));

      manager(0).getCache();
      manager(1).getCache();

      TestingUtil.blockUntilViewReceived(manager(0).getCache(), 2, 10000);
      TestingUtil.blockUntilCacheStatusAchieved(manager(0).getCache(), ComponentStatus.RUNNING, 10000);
      TestingUtil.blockUntilCacheStatusAchieved(manager(1).getCache(), ComponentStatus.RUNNING, 10000);


      manager(0).getCache().put("k","v");
      manager(0).getCache().get("k").equals("v");
      manager(1).getCache().get("k").equals("v");

      log.info("Local replication test passed!");

      //Important: this only connects to one of the two servers!
      remoteCacheManager = new RemoteCacheManager("localhost", hotRodServer2.getPort());
      remoteCache = remoteCacheManager.getCache();

      tcpConnectionFactory = (TcpTransportFactory) TestingUtil.extractField(remoteCacheManager, "transportFactory");
   }


   public void testTwoMembers() {
      InetSocketAddress server1Address = new InetSocketAddress("localhost", hotRodServer1.getPort());
      expectTopologyChange(server1Address, true);
      assertEquals(2, tcpConnectionFactory.getServers().size());
   }

   @Test(dependsOnMethods = "testTwoMembers")
   public void testAddNewServer() {
      CacheManager cm3 = TestCacheManagerFactory.createClusteredCacheManager(config);
      registerCacheManager(cm3);
      hotRodServer3 = TestHelper.startHotRodServer(manager(2));
      manager(2).getCache();

      TestingUtil.blockUntilViewsReceived(10000, true, manager(0), manager(1), manager(2));
      TestingUtil.blockUntilCacheStatusAchieved(manager(0).getCache(), ComponentStatus.RUNNING, 10000);
      TestingUtil.blockUntilCacheStatusAchieved(manager(1).getCache(), ComponentStatus.RUNNING, 10000);
      TestingUtil.blockUntilCacheStatusAchieved(manager(2).getCache(), ComponentStatus.RUNNING, 10000);

      expectTopologyChange(new InetSocketAddress("localhost",hotRodServer3.getPort()), true);
      assertEquals(3, tcpConnectionFactory.getServers().size());
   }

   @Test(dependsOnMethods = "testAddNewServer")
   public void testDropServer() {
      manager(2).stop();
      TestingUtil.blockUntilViewsReceived(10000, true, manager(0), manager(1));
      TestingUtil.blockUntilViewsReceived(10000, true, manager(0), manager(1), manager(2));
      TestingUtil.blockUntilCacheStatusAchieved(manager(0).getCache(), ComponentStatus.RUNNING, 10000);
      TestingUtil.blockUntilCacheStatusAchieved(manager(1).getCache(), ComponentStatus.RUNNING, 10000);
      
      InetSocketAddress server3Address = new InetSocketAddress("localhost", hotRodServer3.getPort());
      hotRodServer3.stop();
      expectTopologyChange(server3Address, false);
      assertEquals(2, tcpConnectionFactory.getServers().size());
   }

   private void expectTopologyChange(InetSocketAddress server1Address, boolean added) {
      for (int i = 0; i < 10; i++) {
         try {
            remoteCache.put("k" + i, "v" + i);
//            remoteCache.ping();
         } catch (Exception e) {
            if (added) {
               throw new IllegalStateException(e);
            } //else it is acceptable, as the transport hasn't changed
         }
         if (added == tcpConnectionFactory.getServers().contains(server1Address)) break;
      }
      assertEquals(server1Address + " not found", added, tcpConnectionFactory.getServers().contains(server1Address));
   }
}
package org.infinispan.server.hotrod

import org.infinispan.test.MultipleCacheManagersTest
import org.infinispan.config.Configuration
import org.testng.annotations.{AfterMethod, AfterClass, Test}
import test.HotRodClient
import test.HotRodTestingUtil._
import org.infinispan.config.Configuration.CacheMode

/**
 * // TODO: Document this
 * @author Galder Zamarreño
 * @since 4.1
 */
abstract class HotRodMultiNodeTest extends MultipleCacheManagersTest {
   import HotRodServer._
   private[this] var hotRodServers: List[HotRodServer] = List()
   private[this] var hotRodClients: List[HotRodClient] = List()

   @Test(enabled=false) // Disable explicitly to avoid TestNG thinking this is a test!!
   override def createCacheManagers {
      for (i <- 0 until 2) {
         val cm = addClusterEnabledCacheManager()
         cm.defineConfiguration(cacheName, createCacheConfig)
      }
      hotRodServers = hotRodServers ::: List(startHotRodServer(cacheManagers.get(0)))
      hotRodServers = hotRodServers ::: List(startHotRodServer(cacheManagers.get(1), hotRodServers.head.getPort + 50))
      hotRodServers.foreach {s =>
         hotRodClients = new HotRodClient("127.0.0.1", s.getPort, cacheName, 60) :: hotRodClients
      }
   }

   @AfterClass(alwaysRun = true)
   override def destroy {
      log.debug("Test finished, close Hot Rod server", null)
      hotRodClients.foreach(_.stop)
      hotRodServers.foreach(_.stop)
      super.destroy // Stop the caches last so that at stoppage time topology cache can be updated properly
   }

   @AfterMethod(alwaysRun=true)
   override def clearContent() {
      // Do not clear cache between methods so that topology cache does not get cleared
   }

   protected def servers = hotRodServers

   protected def clients = hotRodClients

   protected def cacheName: String

   protected def createCacheConfig: Configuration

}
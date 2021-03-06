package org.infinispan.server.core

import java.net.InetSocketAddress
import transport.netty.{EncoderAdapter, NettyTransport}
import transport.Transport
import org.infinispan.server.core.VersionGenerator._
import org.infinispan.manager.EmbeddedCacheManager
import org.infinispan.server.core.Main._
import java.util.Properties
import org.infinispan.util.{TypedProperties, Util}

/**
 * A common protocol server dealing with common property parameter validation and assignment and transport lifecycle.
 *
 * @author Galder Zamarreño
 * @since 4.1
 */
abstract class AbstractProtocolServer(threadNamePrefix: String) extends ProtocolServer with Logging {
   protected var host: String = _
   protected var port: Int = _
   protected var masterThreads: Int = _
   protected var workerThreads: Int = _
   protected var transport: Transport = _
   protected var cacheManager: EmbeddedCacheManager = _

   def start(properties: Properties, cacheManager: EmbeddedCacheManager, defaultPort: Int) {
      val typedProps = TypedProperties.toTypedProperties(properties)
      // Enabled added to make it easy to enable/disable endpoints in JBoss MC based beans for EDG
      val toStart = typedProps.getBooleanProperty("enabled", true, true)

      if (toStart) {
         // By doing parameter validation here, both programmatic and command line clients benefit from it.
         this.host = typedProps.getProperty(PROP_KEY_HOST, HOST_DEFAULT, true)
         this.port = typedProps.getIntProperty(PROP_KEY_PORT, defaultPort, true)
         this.masterThreads = typedProps.getIntProperty(PROP_KEY_MASTER_THREADS, MASTER_THREADS_DEFAULT, true)
         if (masterThreads < 0)
            throw new IllegalArgumentException("Master threads can't be lower than 0: " + masterThreads)

         this.workerThreads = typedProps.getIntProperty(PROP_KEY_WORKER_THREADS, WORKER_THREADS_DEFAULT, true)
         if (workerThreads < 0)
            throw new IllegalArgumentException("Worker threads can't be lower than 0: " + masterThreads)

         this.cacheManager = cacheManager
         val idleTimeout = typedProps.getIntProperty(PROP_KEY_IDLE_TIMEOUT, IDLE_TIMEOUT_DEFAULT, true)
         if (idleTimeout < -1)
            throw new IllegalArgumentException("Idle timeout can't be lower than -1: " + idleTimeout)

         val tcpNoDelay = typedProps.getBooleanProperty(PROP_KEY_TCP_NO_DELAY, TCP_NO_DELAY_DEFAULT, true)

         val sendBufSize = typedProps.getIntProperty(PROP_KEY_SEND_BUF_SIZE, SEND_BUF_SIZE_DEFAULT, true)
         if (sendBufSize < 0) {
            throw new IllegalArgumentException("Send buffer size can't be lower than 0: " + sendBufSize)
         }

         val recvBufSize = typedProps.getIntProperty(PROP_KEY_RECV_BUF_SIZE, RECV_BUF_SIZE_DEFAULT, true)
         if (recvBufSize < 0) {
            throw new IllegalArgumentException("Send buffer size can't be lower than 0: " + sendBufSize)
         }

         if (isDebugEnabled) {
            debug("Starting server with basic settings: host={0}, port={1}, masterThreads={2}, workerThreads={3}, " +
                  "idleTimeout={4}, tcpNoDelay={5}, sendBufSize={6}, recvBufSize={7}", host, port,
                  masterThreads, workerThreads, idleTimeout, tcpNoDelay, sendBufSize, recvBufSize)
         }

         // Register rank calculator before starting any cache so that we can capture all view changes
         cacheManager.addListener(getRankCalculatorListener)
         // Start default cache
         startDefaultCache
         startTransport(idleTimeout, tcpNoDelay, sendBufSize, recvBufSize, typedProps)
      }
   }

   def startTransport(idleTimeout: Int, tcpNoDelay: Boolean, sendBufSize: Int, recvBufSize: Int, typedProps: TypedProperties) {
      val address = new InetSocketAddress(host, port)
      val encoder = getEncoder
      val nettyEncoder = if (encoder != null) new EncoderAdapter(encoder) else null
      transport = new NettyTransport(this, nettyEncoder, address, masterThreads, workerThreads, idleTimeout,
         threadNamePrefix, tcpNoDelay, sendBufSize, recvBufSize)
      transport.start
   }

   def start(propertiesFileName: String, cacheManager: EmbeddedCacheManager) {
      val propsObject = new TypedProperties()
      val stream = Util.loadResourceAsStream(propertiesFileName)
      propsObject.load(stream)
      start(propsObject, cacheManager)
   }

   override def stop {
      val isDebug = isDebugEnabled
      if (isDebug)
         debug("Stopping server listening in {0}:{1}", host, port)

      if (transport != null)
         transport.stop

      if (isDebug)
         debug("Server stopped")
   }

   def getCacheManager = cacheManager

   def getHost = host

   def getPort = port

   def startDefaultCache = cacheManager.getCache()
}

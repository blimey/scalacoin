package scalacoin.network

import com.typesafe.config.ConfigFactory

object ServerConfig {
  val config = ConfigFactory.load()

  val httpInterface = config.getString("http.interface")
  val httpPort = config.getInt("http.port")

  val nodeName = config.getString("blockchain.nodeName")
}
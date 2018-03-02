package scalacoin

import com.typesafe.config.ConfigFactory

object NodeConfig {
  val config = ConfigFactory.load()

  val httpInterface = config.getString("http.interface")
  val httpPort = config.getInt("http.port")

  val nodeName = config.getString("blockchain.nodeName")
}
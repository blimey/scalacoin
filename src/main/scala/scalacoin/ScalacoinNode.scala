package scalacoin

import akka.actor.{ActorSystem, Props}
import akka.stream.ActorMaterializer
import akka.http.scaladsl.Http

import scala.io.StdIn
import scala.concurrent.ExecutionContext

import scalacoin.network.BlockchainActor

object ScalacoinNode extends App with RestInterface {
  import NodeConfig._

  implicit val system: ActorSystem = ActorSystem("scalacoin-actor-system")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val executionContext: ExecutionContext = system.dispatcher

  val blockchainActor = system.actorOf(BlockchainActor.props, "blockchain-actor")

  val bindingFuture = Http().bindAndHandle(routes, httpInterface, httpPort)
  
  println(s"Node $nodeName coming online at $httpInterface:$httpPort")

  StdIn.readLine()

  bindingFuture
    .flatMap(_.unbind())
    .onComplete(_ => system.terminate())
}
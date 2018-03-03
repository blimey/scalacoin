package scalacoin

import akka.actor.{ActorSystem, Props}
import akka.stream.ActorMaterializer
import akka.http.scaladsl.Http

import scala.io.StdIn
import scala.concurrent.ExecutionContext

import scalacoin.network.P2PActor

object ScalacoinNode extends App with RestInterface {
  import NodeConfig._

  implicit val system: ActorSystem = ActorSystem("scalacoin-actor-system")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val executionContext: ExecutionContext = system.dispatcher

  val p2pActor = system.actorOf(P2PActor.props, "scalacoin-p2p-actor")

  val bindingFuture = Http().bindAndHandle(routes, httpInterface, httpPort)
  
  println(s"Node $nodeName coming online at $httpInterface:$httpPort")

  StdIn.readLine()

  bindingFuture
    .flatMap(_.unbind())
    .onComplete(_ => system.terminate())
}
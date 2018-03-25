package scalacoin.network

import akka.actor.{ActorSystem, Props}
import akka.stream.ActorMaterializer
import akka.http.scaladsl.Http

import scala.io.StdIn
import scala.concurrent.ExecutionContext

object ServerNode extends ServerInterface {
  import ServerConfig._

  implicit val system: ActorSystem = ActorSystem("scalacoin-actor-system")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val executionContext: ExecutionContext = system.dispatcher

  val actor = system.actorOf(ServerActor.props, "scalacoin-server-node")

  def start = {
    val bindingFuture = Http().bindAndHandle(routes, httpInterface, httpPort)
    
    println(s"Node $nodeName coming online at $httpInterface:$httpPort")

    StdIn.readLine()

    bindingFuture
      .flatMap(_.unbind())
      .onComplete(_ => system.terminate())
  }
}
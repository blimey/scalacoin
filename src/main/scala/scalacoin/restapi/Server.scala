package scalacoin.restapi

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshalling.ToResponseMarshallable
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._

import scala.io.StdIn
import scala.concurrent.ExecutionContext

import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import io.circe.generic.auto._

import scalacoin.blockchain._

object Server extends FailFastCirceSupport {
  var blockchain: Blockchain = Blockchain()

  def main(args: Array[String]) {
    implicit val system: ActorSystem = ActorSystem("scalacoin-actor-system")
    implicit val materializer: ActorMaterializer = ActorMaterializer()

    implicit val exectutionContext: ExecutionContext = system.dispatcher

    val bindingFuture = Http().bindAndHandle(routes, "localhost", 8080)

    println(s"Scalacoin server online at http://localhost:8080/\nPress RETURN to stop...")

    StdIn.readLine() // let it run until user presses return

    bindingFuture
      .flatMap(_.unbind()) // trigger unbinding from the port
      .onComplete(_ => system.terminate()) // and shutdown when done
  }

  private def routes = {
    get {
      path("blockchain") {
        complete { blockchain }
      }
    } ~
    post {
      path("mine") {
        entity(as[String]) { data =>
          blockchain = blockchain.addBlock(data)
          complete { blockchain.lastBlock }
        }
      }
    }
  }
}
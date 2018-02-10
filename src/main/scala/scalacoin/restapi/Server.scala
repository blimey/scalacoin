package scalacoin.restapi

import akka.actor.{ActorSystem, Props}
import akka.stream.ActorMaterializer
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshalling.ToResponseMarshallable
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.model.StatusCodes
import akka.pattern.ask
import akka.util.Timeout

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.io.StdIn

import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import io.circe.generic.auto._

import scalacoin.blockchain._
import scalacoin.restapi.BlockchainActor.{GetBlockchain, GetLastBlock, AddBlock}

object Server extends FailFastCirceSupport {
  implicit val system: ActorSystem = ActorSystem("scalacoin-actor-system")
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  implicit val exectutionContext: ExecutionContext = system.dispatcher

  val blockchainActor = system.actorOf(BlockchainActor.props, "blockchainActor")

  def main(args: Array[String]) {
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
        implicit val timeout: Timeout = 5.seconds
        val chain: Future[Blockchain] = (blockchainActor ? GetBlockchain).mapTo[Blockchain]
        complete { chain }
      } ~
      path("lastBlock") {
        implicit val timeout: Timeout = 5.seconds
        val block: Future[Block] = (blockchainActor ? GetLastBlock).mapTo[Block]
        complete { block }
      }
    } ~
    post {
      path("addBlock") {
        entity(as[String]) { data =>
          blockchainActor ! AddBlock(data)
          complete((StatusCodes.Created, "Block added successfully."))
        }
      }
    }
  }
}
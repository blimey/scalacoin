package scalacoin.network

import akka.actor.ActorRef
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.model.StatusCodes
import akka.pattern.ask
import akka.util.Timeout

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.duration._

import io.circe.generic.auto._

import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport

import scalacoin.network.ServerActor.{ReqFullBlockchain, ReqListPeers, ReqRegisterPeer}
import scalacoin.network.ServerActor.{ResFullBlockchain, ResListPeers}

trait ServerInterface extends FailFastCirceSupport {
  implicit val executionContext: ExecutionContext
  val actor: ActorRef

  implicit val timeout: Timeout = 5.seconds

  val routes = {
    get {
      path("blockchain") {
        val chain: Future[ResFullBlockchain] = (actor ? ReqFullBlockchain).mapTo[ResFullBlockchain]
        complete { chain }
      } ~
      path("lastBlock") {
        complete { (StatusCodes.NotFound) }
      } ~
      path("peers") {
        val peers: Future[ResListPeers] = (actor ? ReqListPeers).mapTo[ResListPeers]
        complete { peers }
      }
    } ~
    post {
      path("mineBlock") {
        complete { (StatusCodes.NotFound) }
      } ~
      path("registerPeer") {
        entity(as[String]) { data =>
          actor ! ReqRegisterPeer(data)
          complete((StatusCodes.Accepted, "Peer added successfully."))
        }
      }
    }
  }
}
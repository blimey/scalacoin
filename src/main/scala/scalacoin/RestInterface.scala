package scalacoin

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

import scalacoin.blockchain.Blockchain.Implicits._
import scalacoin.network.BlockchainActor.{GetBlockchain, GetLastBlock, MineBlock, CurrentBlockchain, LastBlock}
import scalacoin.network.P2PActor.{GetPeers, ResolvePeer, Peers}

trait RestInterface extends FailFastCirceSupport {
  implicit val executionContext: ExecutionContext
  val p2pActor: ActorRef

  implicit val timeout: Timeout = 5.seconds

  val routes = {
    get {
      path("blockchain") {
        val chain: Future[CurrentBlockchain] = (p2pActor ? GetBlockchain).mapTo[CurrentBlockchain]
        complete { chain }
      } ~
      path("lastBlock") {
        val block: Future[LastBlock] = (p2pActor ? GetLastBlock).mapTo[LastBlock]
        complete { block }
      } ~
      path("peers") {
        val peers: Future[Peers] = (p2pActor ? GetPeers).mapTo[Peers]
        complete { peers }
      }
    } ~
    post {
      path("mineBlock") {
        entity(as[String]) { data =>
          p2pActor ! MineBlock(data)
          complete((StatusCodes.Created, "Block mined successfully."))
        }
      } ~
      path("addPeer") {
        entity(as[String]) { data =>
          p2pActor ! ResolvePeer(data)
          complete((StatusCodes.Accepted, "Peer added successfully."))
        }
      }
    }
  }
}
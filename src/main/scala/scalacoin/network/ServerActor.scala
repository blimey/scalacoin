package scalacoin.network

import akka.actor.{Actor, ActorRef, Props}

import akka.pattern.pipe
import akka.util.Timeout

import scala.concurrent.Future
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

import scalacoin.mining.Miner._

class ServerActor extends Actor {
  import ServerActor._

  implicit val timeout: Timeout = 5.seconds
  implicit val executionContext: ExecutionContext = context.dispatcher

  def receive = active(ServerState(makeGenesis, Set.empty, List.empty))

  def active(state: ServerState): Receive = {
    case ReqFullBlockchain => sender() ! ResFullBlockchain(state.longestChain)

    case ReqListPeers =>
      sender() ! ResListPeers(state.peers.toList.map(_.path.toSerializationFormat))

    case ReqHandshake =>
      context become active(state.copy(peers = state.peers + sender()))
    
    case ReqRegisterPeer(address) =>
      context.actorSelection(address).resolveOne().map { resolvedPeer =>
        if (resolvedPeer != self && !state.peers.contains(resolvedPeer)) {
          context.watch(resolvedPeer)

          // Introduce ourselves
          resolvedPeer ! ReqHandshake

          // Ask for peers
          resolvedPeer ! ReqListPeers

          // Tell our peers
          state.peers.foreach(_ ! ReqRegisterPeer(resolvedPeer.path.toSerializationFormat))

          // Add new peer
          context become active(state.copy(peers = state.peers + resolvedPeer))
        }
      } pipeTo self

    case ResListPeers(peers) => peers.foreach(self ! ReqRegisterPeer(_))
    
    case ReqTerminatePeer(peer) => context become active(state.copy(peers = state.peers - peer))
  }
}

object ServerActor {
  def props: Props = Props[ServerActor]

  final case object ReqFullBlockchain
  final case object ReqListPeers
  final case class ReqRegisterPeer(address: String)
  final case object ReqHandshake
  final case class ReqTerminatePeer(ref: ActorRef)

  final case class ResFullBlockchain(blockchain: Blockchain)
  final case class ResListPeers(peers: List[String])
}

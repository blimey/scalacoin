package scalacoin.network

import akka.actor.{Actor, ActorRef, Props}
import akka.pattern.pipe
import akka.util.Timeout

import scala.concurrent.Future
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

import scalacoin.network.BlockchainActor._

class P2PActor extends Actor {
  import P2PActor._

  implicit val timeout: Timeout = 5.seconds
  implicit val executionContext: ExecutionContext = context.dispatcher

  val blockchainActor: ActorRef = context.actorOf(BlockchainActor.props(self))

  def receive = active(P2PState(Set[ActorRef]()))

  def active(state: P2PState): Receive = {
    case GetPeers =>
      sender() ! Peers(state.peers.toList.map(_.path.toSerializationFormat))
    case Peers(peers) =>
      peers.foreach(self ! ResolvePeer(_))
    case AddPeer =>
      context become active(state.copy(peers = state.peers + sender()))
    case ResolvePeer(address) =>
      context.actorSelection(address).resolveOne().map(ResolvedPeer(_)) pipeTo self
    case ResolvedPeer(peer) => {
      if (peer != self && !state.peers.contains(peer)) {
        context.watch(peer)

        // Introduce yourself to new peer
        peer ! AddPeer

        // Ask for peer's peers
        peer ! GetPeers

        // Tell our existing peers to new peer
        state.peers.foreach(_ ! ResolvePeer(peer.path.toSerializationFormat))

        // Add new peer
        context become active(state.copy(peers = state.peers + peer))
      }
    }
    case TerminatedPeer(peer) => context become active(state.copy(peers = state.peers - peer))
    case QueryBlockchain =>
      state.peers.foreach(_ ! GetBlockchain)
    case blockchainMessage @ (GetBlockchain) =>
      blockchainActor forward blockchainMessage
  }
}

object P2PActor {
  def props: Props = Props[P2PActor]

  // Input
  case object GetPeers
  case object AddPeer
  case object QueryBlockchain
  final case class Peers(peers: List[String])
  final case class ResolvePeer(address: String)
  final case class ResolvedPeer(ref: ActorRef)
  final case class TerminatedPeer(ref: ActorRef)
}
package scalacoin.network

import akka.actor.{Actor, ActorRef, Props}
import akka.pattern.pipe
import akka.util.Timeout

import scala.concurrent.Future
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

import scalacoin.blockchain._

class BlockchainNodeActor extends Actor {
  import BlockchainNodeActor._

  implicit val timeout: Timeout = 5.seconds
  implicit val executionContext: ExecutionContext = context.dispatcher

  def receive = active(BlockchainNodeState(Blockchain(), Set()))

  def active(state: BlockchainNodeState): Receive = {
    case GetBlockchain => sender() ! state.chain
    case GetLastBlock => sender() ! state.chain.lastBlock
    case AddBlock(data) => context become active(state.copy(chain = state.chain.addBlock(data)))

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
  }
}

object BlockchainNodeActor {
  def props: Props = Props[BlockchainNodeActor]

  case object GetBlockchain
  case object GetLastBlock
  final case class AddBlock(data: String)

  case object GetPeers
  final case class Peers(peers: List[String])
  case object AddPeer
  final case class ResolvePeer(address: String)
  final case class ResolvedPeer(ref: ActorRef)
  final case class TerminatedPeer(ref: ActorRef)
}

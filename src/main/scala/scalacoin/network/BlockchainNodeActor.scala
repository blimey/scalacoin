package scalacoin.network

import akka.actor.{Actor, ActorRef, Props}
import akka.pattern.pipe

import scala.concurrent.Future
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

import akka.util.Timeout

import scalacoin.blockchain._

class BlockchainNodeActor extends Actor {
  import BlockchainNodeActor._

  implicit val timeout: Timeout = 5.seconds
  implicit val executionContext: ExecutionContext = context.dispatcher

  def receive = active(BlockchainNodeState(Blockchain(), List()))

  def active(state: BlockchainNodeState): Receive = {
    case GetBlockchain => sender() ! state.chain
    case GetLastBlock => sender() ! state.chain.lastBlock
    case AddBlock(data) => context become active(state.copy(chain = state.chain.addBlock(data)))

    case GetPeers => sender() ! Peers(state.peers.map(_.path.toSerializationFormat))
    case AddPeer(address) =>
      context.actorSelection(address).resolveOne().map(ResolvedPeer(_)) pipeTo self
    case ResolvedPeer(peer) => context become active(state.copy(peers = peer :: state.peers))
  }
}

object BlockchainNodeActor {
  def props: Props = Props[BlockchainNodeActor]

  case object GetBlockchain
  case object GetLastBlock
  case object GetPeers
  final case class AddBlock(data: String)
  final case class AddPeer(address: String)

  final case class Peers(peers: List[String])
  final case class ResolvedPeer(ref: ActorRef)
}

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
    case GetBlockchain => sender() ! CurrentBlockchain(state.chain)
    case GetLastBlock => sender() ! LastBlock(state.chain.lastBlock)
    case MineBlock(data) =>
      context become active(state.copy(chain = state.chain.addBlock(data)))
      self ! BroadcastLastBlock
    case LastBlock(block) if block.index <= state.chain.lastBlock.index => {} // Last block index greater then received, Do nothing
    case LastBlock(block) if block.previousHash == Block.hash(state.chain.lastBlock) =>
      // Last block hash matches received block previous hash, try add received block to local blockchain
      state.chain.addBlock(block) match {
        case Right(blockchain) => 
          context become active(state.copy(chain = blockchain))
          self ! BroadcastLastBlock
        case Left(error) => {} // Do nothing
      }
    case LastBlock(_) =>
      // Received block index higher than last block but hashes mismatch, request full blockchain to peers
      state.peers.foreach(_ ! GetBlockchain)
    case CurrentBlockchain(chain) =>
      // Received blockchain, try to replace local one
      state.chain.replaceWith(chain) match {
        case Right(blockchain) =>
          context become active(state.copy(chain = blockchain))
          self ! BroadcastLastBlock
        case Left(error) => {}  // Do nothing
      }
    case BroadcastLastBlock =>
      // Broadcast last block in the local chain
      state.peers.foreach(_ ! LastBlock(state.chain.lastBlock))

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
  final case class CurrentBlockchain(blockchain: Blockchain)
  final case class LastBlock(block: Block)
  final case class MineBlock(data: String)
  case object BroadcastLastBlock

  case object GetPeers
  final case class Peers(peers: List[String])
  case object AddPeer
  final case class ResolvePeer(address: String)
  final case class ResolvedPeer(ref: ActorRef)
  final case class TerminatedPeer(ref: ActorRef)
}

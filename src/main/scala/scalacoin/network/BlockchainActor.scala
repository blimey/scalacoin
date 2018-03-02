package scalacoin.network

import akka.actor.{Actor, ActorRef, Props}
import akka.pattern.pipe
import akka.util.Timeout

import scala.concurrent.Future
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

import scalacoin.blockchain._

class BlockchainActor extends Actor {
  import BlockchainActor._

  implicit val timeout: Timeout = 5.seconds
  implicit val executionContext: ExecutionContext = context.dispatcher

  def receive = active(BlockchainState(Blockchain(), Set()))

  def active(state: BlockchainState): Receive = {
    case GetBlockchain => sender() ! CurrentBlockchain(state.chain)
    case GetLastBlock => sender() ! LastBlock(Blockchain.lastBlock(state.chain))
    case MineBlock(data) =>
      context become active(state.copy(chain = Blockchain.addBlock(state.chain, data)))
      self ! BroadcastLastBlock
    case LastBlock(receivedBlock) if receivedBlock.index <= Blockchain.lastBlock(state.chain).index => () // Last block index greater then received, do nothing
    case LastBlock(receivedBlock) if receivedBlock.previousHash == Blockchain.hashForBlock(Blockchain.lastBlock(state.chain)) =>
      // Last block hash matches received block previous hash, add received block to local blockchain
      context become active(state.copy(chain = Blockchain.addBlock(state.chain, receivedBlock)))
      self ! BroadcastLastBlock
    case LastBlock(_) =>
      // Received block index higher than last block but hashes mismatch, request full blockchain to peers
      state.peers.foreach(_ ! GetBlockchain)
    case CurrentBlockchain(receivedChain) =>
      // Received blockchain, try to replace local one
      if (Blockchain.isValid(receivedChain)) {
        context become active(state.copy(chain = Blockchain.selectLongest(state.chain, receivedChain)))
        self ! BroadcastLastBlock
      }
    case BroadcastLastBlock =>
      // Broadcast last block in the local chain
      state.peers.foreach(_ ! LastBlock(Blockchain.lastBlock(state.chain)))

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

object BlockchainActor {
  def props: Props = Props[BlockchainActor]

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

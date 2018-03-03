package scalacoin.network

import akka.actor.{Actor, ActorRef, Props}

import scalacoin.blockchain.{Blockchain, Block}
import scalacoin.network.P2PActor.{BroadcastBlock, QueryBlockchain}

class BlockchainActor(p2pActor: ActorRef) extends Actor {
  import BlockchainActor._

  def receive = active(BlockchainState(Blockchain()))

  def active(state: BlockchainState): Receive = {
    case GetBlockchain => sender() ! CurrentBlockchain(state.chain)
    case GetLastBlock => sender() ! LastBlock(Blockchain.lastBlock(state.chain))
    case MineBlock(data) =>
      context become active(state.copy(chain = Blockchain.addBlock(state.chain, data)))
      self ! NewBlock
    case LastBlock(receivedBlock) if receivedBlock.index <= Blockchain.lastBlock(state.chain).index => () // Last block index greater then received, do nothing
    case LastBlock(receivedBlock) if receivedBlock.previousHash == Blockchain.hashForBlock(Blockchain.lastBlock(state.chain)) =>
      // Last block hash matches received block previous hash, add received block to local blockchain
      context become active(state.copy(chain = Blockchain.addBlock(state.chain, receivedBlock)))
      self ! NewBlock
    case LastBlock(_) =>
      // Received block index higher than last block but hashes mismatch, request full blockchain to peers
      p2pActor ! QueryBlockchain
    case CurrentBlockchain(receivedChain) =>
      // Received blockchain, try to replace local one
      if (Blockchain.isValid(receivedChain)) {
        context become active(state.copy(chain = Blockchain.selectLongest(state.chain, receivedChain)))
        self ! NewBlock
      }
    case NewBlock =>
      p2pActor ! BroadcastBlock(Blockchain.lastBlock(state.chain))
  }
}

object BlockchainActor {
  def props(p2pActor: ActorRef): Props = Props(new BlockchainActor(p2pActor))

  // Input
  case object GetBlockchain
  case object GetLastBlock
  case object NewBlock
  final case class MineBlock(data: String)
  final case class CurrentBlockchain(blockchain: Blockchain)
  final case class LastBlock(block: Block)
}

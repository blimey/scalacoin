package scalacoin.restapi

import akka.actor.{Actor, Props}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext

import scalacoin.blockchain._

class BlockchainActor extends Actor {
  import BlockchainActor._

  def receive = active(BlockchainState(Blockchain()))

  def active(state: BlockchainState): Receive = {
    case GetBlockchain => sender() ! state.chain
    case GetLastBlock => sender() ! state.chain.lastBlock
    case AddBlock(data) => context become active(BlockchainState(state.chain.addBlock(data)))
  }
}

object BlockchainActor {
  def props: Props = Props[BlockchainActor]

  case object GetBlockchain
  case object GetLastBlock
  final case class AddBlock(data: String)
}

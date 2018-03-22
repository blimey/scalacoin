package scalacoin.network

import akka.actor.{Actor, ActorRef, Props}

import scalacoin.mining.Miner._

class BlockchainActor(p2pActor: ActorRef) extends Actor {
  import BlockchainActor._

  def receive = active(BlockchainState(makeGenesis))

  def active(state: BlockchainState): Receive = {
    case GetBlockchain => sender() ! CurrentBlockchain(state.chain)
  }
}

object BlockchainActor {
  def props(p2pActor: ActorRef): Props = Props(new BlockchainActor(p2pActor))

  // Input
  case object GetBlockchain
  case class CurrentBlockchain(blockchain: Blockchain)
}

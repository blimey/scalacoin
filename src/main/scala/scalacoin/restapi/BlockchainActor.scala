package scalacoin.restapi

import akka.actor.Actor

import scalacoin.blockchain._

class BlockchainActor extends Actor {
  import BlockchainActor._

  var blockchain: Blockchain = Blockchain()

  def receive: Receive = {
    case RequestBlockchain => sender() ! ResponseBlockchain(blockchain)
    case RequestAddBlock(data) => {
      blockchain = blockchain.addBlock(data)
      sender() ! ResponseAddBlock(blockchain.lastBlock)
    }
  }
}

object BlockchainActor {
  case object RequestBlockchain
  final case class RequestAddBlock(data: String)

  final case class ResponseBlockchain(chain: Blockchain)
  final case class ResponseAddBlock(block: Block)
}
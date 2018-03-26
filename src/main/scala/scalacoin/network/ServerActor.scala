package scalacoin.network

import akka.actor.{Actor, ActorRef, Props}

import akka.pattern.pipe
import akka.util.Timeout

import scala.concurrent.Future
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

import scalacoin.mining.Miner._
import scalacoin.types.{BlockHeader, Transaction}

class ServerActor extends Actor {
  import ServerActor._

  implicit val timeout: Timeout = 5.seconds
  implicit val executionContext: ExecutionContext = context.dispatcher

  def receive = active(ServerState(makeGenesis, Set.empty, Set.empty))

  def active(state: ServerState): Receive = {
    case ReqFullBlockchain => sender() ! ResFullBlockchain(state.longestChain)

    case ReqListTransactions => sender() ! ResListTransactions(state.transactionPool.toList)

    case ReqNewTransaction(transaction) =>
      if (isValidTransaction(state.longestChain, transaction)) {
        context become active(state.copy(transactionPool = state.transactionPool + transaction))
      }
      ResNewTransaction

    case ReqNewBlock(block, header) =>
      val bc: Blockchain = state.longestChain
      if (isValidChain(bc) && isValidBlock(block, header, bc)) {
        context become active(state.copy(longestChain = addBlock(block, header, state.longestChain)))
      }
      ResNewBLock

    case ReqListPeers => sender() ! ResListPeers(peerAddresses(state.peers))

    case ReqHandshake =>
      context become active(state.copy(peers = state.peers + sender()))
      ResHandshake
    
    case ReqRegisterPeer(address) =>
      context.actorSelection(address).resolveOne().map { resolvedPeer =>
        if (resolvedPeer != self && !state.peers.contains(resolvedPeer)) {
          context.watch(resolvedPeer)

          // Introduce ourselves
          resolvedPeer ! ReqHandshake

          // Ask for sharing peers
          resolvedPeer ! ReqSharePeers

          // Tell our peers
          state.peers.foreach(_ ! ReqRegisterPeer(resolvedPeer.path.toSerializationFormat))

          // Add new peer
          context become active(state.copy(peers = state.peers + resolvedPeer))
        }
      } pipeTo self
      ResRegisterPeer

    case ReqSharePeers =>
      peerAddresses(state.peers).foreach(sender() ! ReqRegisterPeer(_))
      ResSharePeers
    
    case ReqTerminatePeer(peer) =>
      context become active(state.copy(peers = state.peers - peer))
      ResTerminatePeer
  }

  def peerAddresses(peers: Set[ActorRef]): List[String] = peers.toList.map(_.path.toSerializationFormat)
}

object ServerActor {
  def props: Props = Props[ServerActor]

  case object ReqFullBlockchain
  case object ReqListTransactions
  case class ReqNewTransaction(transaction: Transaction)
  case class ReqNewBlock(block: Block, header: BlockHeader)
  case object ReqListPeers
  case object ReqHandshake
  case object ReqSharePeers
  case class ReqRegisterPeer(address: String)
  case class ReqTerminatePeer(ref: ActorRef)

  case class ResFullBlockchain(blockchain: Blockchain)
  case class ResListTransactions(transactions: List[Transaction])
  case object ResNewTransaction
  case object ResNewBLock
  case class ResListPeers(peers: List[String])
  case object ResHandshake
  case object ResSharePeers
  case object ResRegisterPeer
  case object ResTerminatePeer
}

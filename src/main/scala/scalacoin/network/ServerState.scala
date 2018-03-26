package scalacoin.network

import akka.actor.ActorRef

import scalacoin.types.Transaction
import scalacoin.mining.Miner._

case class ServerState(longestChain: Blockchain, peers: Set[ActorRef], transactionPool: Set[Transaction])
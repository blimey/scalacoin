package scalacoin.network

import akka.actor.ActorRef

import scalacoin.blockchain._

case class BlockchainState(chain: Blockchain, peers: Set[ActorRef])
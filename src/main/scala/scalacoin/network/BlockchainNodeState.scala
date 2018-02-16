package scalacoin.network

import akka.actor.ActorRef

import scalacoin.blockchain._

case class BlockchainNodeState(chain: Blockchain, peers: List[ActorRef])
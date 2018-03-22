package scalacoin.network

import akka.actor.ActorRef

case class P2PState(peers: Set[ActorRef])
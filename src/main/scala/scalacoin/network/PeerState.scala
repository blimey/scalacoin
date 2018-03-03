package scalacoin.network

import akka.actor.ActorRef

case class PeerState(peers: Set[ActorRef])
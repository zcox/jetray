package com.pongr.jetray

import akka.actor.ActorRef
import akka.routing.CyclicIterator
import akka.routing.Routing.loadBalancerActor

/** Actor utilities. */
object Actors {
  /** Returns a load balancer actor that balances across the specified number of actors returned by the specified function.
   *  @param count number of actors to balance load across.
   *  @param newActor function that returns a new actor on each invocation.
   */
  def loadBalance(count: Int, newActor: => ActorRef): ActorRef =
    loadBalancerActor(new CyclicIterator((for (i <- 1 to count) yield newActor).toList))
}
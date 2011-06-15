/*
 * Copyright (c) 2011 Pongr, Inc.
 *   
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
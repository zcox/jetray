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

import akka.actor.{ Actor, ActorRef, PoisonPill }
import akka.actor.Actor.actorOf
import akka.actor.Scheduler.scheduleOnce
import akka.event.EventHandler
import java.util.concurrent.TimeUnit.{ MILLISECONDS, SECONDS }
import java.util.{ Properties, UUID }
import scala.math.round
import javax.mail.Message
import java.io.{ File, FileInputStream }

/** Specifies the rate at which emails should be sent and how many emails to send.
 *  The companion object provides convenience methods for creating instances.
 *
 *  @param frequency Number of emails per second to send, inverse of period.
 *  @param period Number of milliseconds between emails, inverse of frequency.
 *  @param count Total number of emails to send.
 *  @param time Total length of time (in milliseconds) to send email.
 */
case class Section(frequency: Int, period: Long, count: Int, time: Long)
case class ControllerParams(sections: List[Section])

/** Factory for [[com.pongr.jetray.ControllerParams]] instances. */
object ControllerParams extends FromProperties[ControllerParams] {
  /** Reads params from the specified Properties object. */
  def fromProperties(props: Properties) = {
    /*val frequency = Integer.parseInt(props.getProperty("frequency"))
    val period = round(1000d / frequency)
    val count = Integer.parseInt(props.getProperty("count"))
    val time = count * period
    ControllerParams(frequency, period, count, time)*/
    val periods = props.getProperty("period") split "," map { _.toLong }
    val counts = props.getProperty("count") split "," map { _.toInt }
    ControllerParams(periods zip counts map { case (p, c) => Section(round(1000f / p), p, c, c * p) } toList)
  }
}

/** Tells the Controller to generate an email. */
case object Tick

/** Message specifying information about the email to generate. */
case class Generate(runId: UUID, emailId: Long)

/** Controls the periodic generation of emails. */
class Controller(params: ControllerParams, generator: ActorRef, mailer: ActorRef, runId: UUID = UUID.randomUUID) extends Actor {
  //val runId = UUID.randomUUID
  var sent = 0l
  var id = 0l
  var (section :: sections) = params.sections
  val poisonDelay = 1000l

  def receive = {
    case Tick =>
      sent += 1
      id += 1
      generator ! Generate(runId, id)

      if (sent < section.count)
        scheduleOnce(self, Tick, section.period, MILLISECONDS)
      else if (sections.nonEmpty) {
        val period = section.period
        section = sections.head
        sections = sections.tail
        EventHandler.debug(this, "Sent " + sent + " emails, moving to next section: " + section)
        sent = 0
        scheduleOnce(self, Tick, period, MILLISECONDS)
      }
      else {
        EventHandler.debug(this, "Sent " + sent + " emails")

        //TODO should we even do this?
        scheduleOnce(self, PoisonPill, poisonDelay, MILLISECONDS)
        EventHandler.info(this, "Sending PoisonPills to MailerActors...")
        for (mailer <- Actor.registry.actorsFor[MailerActor])
          scheduleOnce(mailer, PoisonPill, poisonDelay, MILLISECONDS)
      }
    case s: Send => mailer forward s
  }
}
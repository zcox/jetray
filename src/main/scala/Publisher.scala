package com.pongr.jetray

import akka.actor.Actor
import akka.routing.Listeners
import java.util.{Date, UUID}
import javax.mail.Message

/** An event that gets published. */
sealed trait Event

/** The specified email was sent. */
case class Sent(runId: UUID, emailId: Long, message: Message, generated: Date, preSend: Date, postSend: Date) extends Event

/** The specified email was received. */
case class Received(runId: UUID, emailId: Long, message: Message, received: Date) extends Event

/** A central actor that publishes events. */
class Publisher extends Actor with Listeners {
  def receive = listenerManagement orElse {
    case e: Event => gossip(e)
  }
}
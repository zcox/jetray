package com.pongr.jetray

import akka.actor.{Actor, ActorRef, PoisonPill}
import akka.actor.Actor.actorOf
import akka.actor.Scheduler.scheduleOnce
import akka.event.EventHandler
import java.util.concurrent.TimeUnit.{ MILLISECONDS, SECONDS }
import java.util.{Properties, UUID}
import scala.math.round
import javax.mail.Message

/** Specifies the rate at which emails should be sent and how many emails to send.
 *  The companion object provides convenience methods for creating instances.
 *
 *  @param frequency Number of emails per second to send, inverse of period.
 *  @param period Number of milliseconds between emails, inverse of frequency.
 *  @param count Total number of emails to send.
 *  @param time Total length of time (in milliseconds) to send email.
 */
case class ControllerParams(frequency: Int, period: Long, count: Int, time: Long)

/** Factory for [[com.pongr.jetray.ControllerParams]] instances. */
object ControllerParams {
  /** Reads params from the specified resource file. Uses getClass.getResourceAsStream
   *  to read the file. The file should contain these properties (others will be supported in the future):
   *  {{{
   *  frequency=6
   *  count=18
   *  }}}
   */
  def fromResource(name: String = "/controller.props") = {
    val props = new Properties()
    val stream = getClass.getResourceAsStream(name)
    if (stream == null)
      throw new IllegalArgumentException("Resource " + name + " not found")
    props.load(stream)
    
    val frequency = Integer.parseInt(props.getProperty("frequency"))
    val period = round(1000d / frequency)
    val count = Integer.parseInt(props.getProperty("count"))
    val time = count * period
    ControllerParams(frequency, period, count, time)
  }
}

/** Tells the Controller to generate an email. */
case object Tick

/** Message specifying information about the email to generate. */
case class Generate(runId: UUID, emailId: Long)

/** Controls the periodic generation of emails. */
class Controller(params: ControllerParams, generator: ActorRef, mailer: ActorRef) extends Actor {
  val runId = UUID.randomUUID
  var sent = 0l
  val poisonDelay = 1000l

  def receive = {
    case Tick =>
      sent += 1
      generator ! Generate(runId, sent)
      
      if (sent < params.count)
        scheduleOnce(self, Tick, params.period, MILLISECONDS)
      else {
        EventHandler.debug(this, "Sent " + sent + " emails")
        
        //TODO should we even do this?
        scheduleOnce(self, PoisonPill, poisonDelay, MILLISECONDS)
        EventHandler.debug(this, "Sending PoisonPills to MailerActors...")
        for (mailer <- Actor.registry.actorsFor[MailerActor])
          scheduleOnce(mailer, PoisonPill, poisonDelay, MILLISECONDS)
      }
    case msg: Message => 
      mailer ! msg
  }
}
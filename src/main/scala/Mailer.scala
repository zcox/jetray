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

import javax.mail.{ Session, Transport, Message }
import akka.actor.Actor
import akka.event.EventHandler
import akka.routing.Listeners
import java.util.{ UUID, Properties, Date }

trait MailSession {
  val smtpProtocol = "smtp"
  val properties = {
    val props = System.getProperties
    smtpProtocol match {
      case "smtp" => props.put("mail.smtp.auth", "true")
      case "smtps" => props.put("mail.smtps.auth", "true")
      case _ =>
    }
    props
  }
  val session: Session = Session.getInstance(properties)
}

//TODO doc
case class SmtpParams(hosts: List[String], port: Int, user: String, password: String, connections: Option[Int])

object SmtpParams extends FromProperties[SmtpParams] {
  def fromProperties(props: Properties) =
    SmtpParams(
      props.getProperty("host") split "," toList,
      Integer.parseInt(props.getProperty("port")),
      props.getProperty("user"),
      props.getProperty("password"),
      option(props.getProperty("connections")) map { _.toInt })
}

/** Send the specified message for the specified runId and emailId. */
case class Send(runId: UUID, emailId: Long, message: Message)

//TODO doc
class MailerActor(host: String, port: Int, user: String, password: String) extends Actor with MailSession with Listeners {
  var transport: Transport = _

  def newTransport() = time("Connected Transport to %s:%d in %d msec".format(host, port, _)) {
    val t = session.getTransport(smtpProtocol)
    t.connect(host, port, user, password)
    t
  }

  override def preStart() = {
    transport = newTransport()
  }

  override def postStop() = time("Closed Transport in %d msec" format _) {
    transport.close()
  }

  def receive = listenerManagement orElse {
    case Send(runId, emailId, message) => time("Sent message %s to %s in %d msec".format(message.getSubject, host, _)) {
      val preSend = new Date
      
      //mail.pepsico.com will close connections after 100 sent emails, so just open a new connection if that happens
      try {
        transport.sendMessage(message, message.getAllRecipients)
      } catch {
        case e: com.sun.mail.smtp.SMTPSendFailedException =>
          EventHandler.info(this, "Caught SMTPSendFailedException, opening new connection: " + e.getMessage)
          transport = newTransport()
          transport.sendMessage(message, message.getAllRecipients)
      }
      
      val postSend = new Date
      for (publisher <- Actor.registry.actorFor[Publisher]) publisher ! Sent(runId, emailId, message, message.getSentDate, preSend, postSend)
    }
  }

  def time[A](log: Long => String)(f: => A): A = {
    val t1 = System.currentTimeMillis
    val a = f
    val t2 = System.currentTimeMillis
    EventHandler.info(this, log(t2 - t1))
    a
  }
}
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
import java.util.Properties

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
case class SmtpParams(host: String, port: Int, user: String, password: String)

case object SmtpParams {
  def fromResource(name: String = "/smtp.props") = {
    val props = new Properties()
    val stream = getClass.getResourceAsStream(name)
    if (stream == null)
      throw new IllegalArgumentException("Resource " + name + " not found")
    props.load(stream)
    
    SmtpParams(
      props.getProperty("host"),
      Integer.parseInt(props.getProperty("port")),
      props.getProperty("user"),
      props.getProperty("password"))
  }
}

//TODO doc
class MailerActor(params: SmtpParams) extends Actor with MailSession {
  var transport: Transport = _

  override def preStart() = time("Connected Transport in %d msec" format _) {
    transport = session.getTransport(smtpProtocol)
    transport.connect(params.host, params.port, params.user, params.password)
  }

  override def postStop() = time("Closed Transport in %d msec" format _) {
    transport.close()
  }

  def receive = {
    case message: Message => time("Sent message %s in %d msec".format(message.getSubject, _)) {
      transport.sendMessage(message, message.getAllRecipients)
    }
  }

  def time[A](log: Long => String)(f: => A): A = {
    val t1 = System.currentTimeMillis
    val a = f
    val t2 = System.currentTimeMillis
    EventHandler.debug(this, log(t2 - t1))
    a
  }
}
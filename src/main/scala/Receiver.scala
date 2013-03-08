package com.pongr.jetray

import java.util.UUID
import javax.mail.Message
import javax.mail.Message.RecipientType.TO
import javax.mail.internet.InternetAddress
import akka.actor.Actor
import java.lang.{ Long => JLong }
import java.util.Date

/** Processes a message to see if it's a Jetray email. */
class Receiver(codec: EmailCodec, repository: EmailRepository) {
  /** Publishes a Received event if a runId and emailId can be decoded from the specified message. */
  def process(message: Message) =
    for ((runId, emailId) <- codec decode message) {
      repository.storeSecond(SecondEmail(
        runId.toString,
        emailId,
        message.getFrom.mkString(", "),
        message.getAllRecipients.mkString(", "),
        message.getSubject,
        "",
        new Date))
    }
}

/** Encodes a runId & emailId in to and out of a message. */
trait EmailCodec {
  def encode(runId: UUID, emailId: Long, message: Message): Unit
  def decode(message: Message): Option[(UUID, Long)]
}

object AddressEmailCodec {
  def useToAddress(m: Message) = m.getRecipients(TO).headOption map { _.asInstanceOf[InternetAddress] }
  def useFromAddress(m: Message) = m.getFrom.headOption map { _.asInstanceOf[InternetAddress] }
}

/** Encodes runId & emailId into From address and decodes them from To address.
 *  @param domain domain of the email address like gmail.com
 *  @param prefix optional prefix for encoding like user+runId-emailId@gmail.com
 *  @param includeName true to include a first and last name in the email address
 *  @param getAddress function that returns an email address to decode runId and emailId out of
 */
class AddressEmailCodec(
  domain: String,
  prefix: Option[String] = None,
  includeName: Boolean = false,
  getAddress: Message => Option[InternetAddress] = AddressEmailCodec.useToAddress) extends EmailCodec {

  def encode(runId: UUID, emailId: Long): String = {
    val email = (prefix map (_ + "+") getOrElse "") + runId + "-" + emailId + "@" + domain
    if (includeName) {
      val name = "Test%s-%d X".format(runId.toString, emailId)
      "%s <%s>".format(name, email)
    } else
      email
  }

  def encode(runId: UUID, emailId: Long, message: Message) {
    val from = encode(runId, emailId)
    message.setFrom(new InternetAddress(from))
  }

  def decode(address: String): Option[(UUID, Long)] = try {
    val extracted = if ((address contains "<") && (address contains ">"))
      address.substring((address indexOf '<') + 1, address indexOf '>')
    else address
    val encoded = extracted.replace("@" + domain, "").replace(prefix map (_ + "+") getOrElse "", "")
    val index = encoded lastIndexOf "-"
    val runId = encoded.substring(0, index)
    val emailId = encoded.substring(index + 1)
    Some(UUID.fromString(runId), JLong.parseLong(emailId))
  } catch {
    case e: Exception => None
  }

  def decode(message: Message): Option[(UUID, Long)] = try {
    getAddress(message) flatMap { a => decode(a.getAddress) }
  } catch {
    case e: Exception => None
  }
}
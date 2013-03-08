package com.pongr.jetray

import java.util.{ Calendar, Date }
import javax.mail.Message
import akka.actor.Actor
import akka.event.EventHandler
import scala.collection.SortedMap

/*case class Email(
  runId: String,
  emailId: Long,
  dateTime: Date,
  from: String,
  to: String,
  subject: String,
  body: String) {
  //TODO attachments? parts?

  val id = runId + "-" + emailId
}

object Email {
  def sent(runId: String, emailId: Long, message: Message, dateTime: Date = new Date): Email =
    Email(
      runId,
      emailId,
      //orNow(message.getSentDate),
      dateTime,
      message.getFrom.mkString(", "),
      message.getAllRecipients.mkString(", "),
      message.getSubject,
      "")

  def received(runId: String, emailId: Long, message: Message, dateTime: Date = new Date): Email =
    Email(
      runId,
      emailId,
      //orNow(message.getReceivedDate),
      dateTime,
      message.getFrom.mkString(", "),
      message.getAllRecipients.mkString(", "),
      message.getSubject,
      "")

  def orNow(d: Date) = if (d != null) d else new Date
}*/

trait Email {
  def runId: String
  def emailId: Long
  def from: String
  def to: String
  def subject: String
  def body: String

  def id = runId + "-" + emailId
}

case class FirstEmail(
  runId: String,
  emailId: Long,
  from: String,
  to: String,
  subject: String,
  body: String,
  generated: Date,
  preSend: Date,
  postSend: Date) extends Email

case class SecondEmail(
  runId: String,
  emailId: Long,
  from: String,
  to: String,
  subject: String,
  body: String,
  received: Date) extends Email

case class Run(runId: String, emails: List[(FirstEmail, Option[SecondEmail])]) {
  val percentComplete: Double = emails.count(_._2.isDefined) / emails.size.toDouble

  def diff(d1: Date, d2: Option[Date]): Long = d2 map { _.getTime - d1.getTime } getOrElse -1l

  //TODO lots of duplication across times below, create some reusable methods to calculate times...
  val responseTimes: List[Long] = emails map { case (e1, e2) => diff(e1.postSend, e2.map(_.received)) }
  val okResponseTimes = responseTimes filter { _ >= 0 }
  val minimumResponseTime: Long = (Long.MaxValue /: okResponseTimes) { _ min _ }
  val maximumResponseTime: Long = (Long.MinValue /: okResponseTimes) { _ max _ }
  val averageResponseTime: Long = math.round((0l /: okResponseTimes) { _ + _ } / okResponseTimes.size.toDouble)

  val smtpSendTimes: List[Long] = emails map { case (e1, _) => diff(e1.preSend, Some(e1.postSend)) }
  val okSmtpSendTimes = smtpSendTimes filter { _ >= 0 }
  val minimumSmtpSendTime: Long = (Long.MaxValue /: okSmtpSendTimes) { _ min _ }
  val maximumSmtpSendTime: Long = (Long.MinValue /: okSmtpSendTimes) { _ max _ }
  val averageSmtpSendTime: Long = math.round((0l /: okSmtpSendTimes) { _ + _ } / okSmtpSendTimes.size.toDouble)

  val generationLagTimes: List[Long] = emails map { case (e1, _) => diff(e1.generated, Some(e1.postSend)) }
  val okGenerationLagTimes = generationLagTimes filter { _ >= 0 }
  val minimumGenerationLagTime: Long = (Long.MaxValue /: okGenerationLagTimes) { _ min _ }
  val maximumGenerationLagTime: Long = (Long.MinValue /: okGenerationLagTimes) { _ max _ }
  val averageGenerationLagTime: Long = math.round((0l /: okGenerationLagTimes) { _ + _ } / okGenerationLagTimes.size.toDouble)

  implicit val dateOrdering = new Ordering[Date] { def compare(d1: Date, d2: Date) = if (d1 == d2) 0 else if (d1 before d2) -1 else 1 }
  
  /** The list of first emails actually sent during each one-second period. The period starts at the specified date/time. */
  val postSendBySecond: SortedMap[Date, List[FirstEmail]] =
    (SortedMap[Date, List[FirstEmail]]() /: emails) {
      case (m, (e, _)) =>
        val postSend = round(e.postSend)
        m + (postSend -> (m.getOrElse(postSend, Nil) :+ e))
    }
  /** Rounds the milliseconds of the specified date down to the lower second. */
  def round(d: Date): Date = {
    val c = Calendar.getInstance()
    c.setTime(d)
    c.set(Calendar.MILLISECOND, 0)
    c.getTime
  }
  /** The number of first emails actually sent during each one-second period. */
  val sentEmailFrequencies: SortedMap[Date, Int] = postSendBySecond map { case (d, es) => (d, es.size) }
  /** The overall average frequency of sent emails, in emails/sec. */
  val averageOfSentEmailFrequencies: Double = (0d /: sentEmailFrequencies) { case (t, (_, c)) => t + c } / sentEmailFrequencies.size.toDouble
  
  val averageSentEmailFrequency = emails.size.toDouble / (diff(emails.head._1.postSend, Some(emails.last._1.postSend)) / 1000d).toDouble
}

trait EmailRepository {
  def storeFirst(email: FirstEmail)
  def storeSecond(email: SecondEmail)
  def getRun(runId: String): Option[Run]
}

class StoreFirstActor(repository: EmailRepository) extends Actor {
  def receive = {
    case Sent(runId, emailId, message, generated, preSend, postSend) =>
      //repository.storeFirst(Email.sent(runId.toString, emailId, message, dateTime))
      repository.storeFirst(FirstEmail(
        runId.toString,
        emailId,
        message.getFrom.mkString(", "),
        message.getAllRecipients.mkString(", "),
        message.getSubject,
        "",
        generated,
        preSend,
        postSend))
      EventHandler.info(this, "Stored %s-%s".format(runId, emailId))
    case _ => //ignore
  }
}

class StoreSecondActor(repository: EmailRepository) extends Actor {
  def receive = {
    case Received(runId, emailId, message, received) =>
      //repository.storeSecond(Email.received(runId.toString, emailId, message, dateTime))
      repository.storeSecond(SecondEmail(
        runId.toString,
        emailId,
        message.getFrom.mkString(", "),
        message.getAllRecipients.mkString(", "),
        message.getSubject,
        "",
        received))
      EventHandler.info(this, "Stored %s-%s".format(runId, emailId))
    case _ => //ignore
  }
}

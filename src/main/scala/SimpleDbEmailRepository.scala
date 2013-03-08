package com.pongr.jetray

import javax.mail.Message
import com.amazonaws.services.simpledb._
import com.amazonaws.services.simpledb.model._
import scala.collection.JavaConversions._
import java.text.SimpleDateFormat

class SimpleDbEmailRepository(
  db: AmazonSimpleDBAsync,
  firstDomain: String = "firstEmails",
  secondDomain: String = "secondEmails") extends EmailRepository {

  //TODO can we do this somewhere other than every instance creation?
  db.createDomainAsync(new CreateDomainRequest(firstDomain))
  db.createDomainAsync(new CreateDomainRequest(secondDomain))

  implicit def map2Attributes(m: Map[String, String]): java.util.List[ReplaceableAttribute] =
    m map { case (k, v) => new ReplaceableAttribute(k, v, false) } toSeq

  implicit def attributes2Map(as: java.util.List[Attribute]): Map[String, String] = {
    val seq: scala.collection.mutable.Buffer[Attribute] = as //this is really stupid...
    seq.map(a => (a.getName, a.getValue)).toMap
  }

  implicit def item2Map(i: Item): Map[String, String] = attributes2Map(i.getAttributes)

  def toAttrs(email: Email): Map[String, String] =
    Map(
      "runId" -> email.runId,
      "emailId" -> email.emailId.toString,
      //"dateTime" -> (dateTimeFormat format email.dateTime),
      "from" -> email.from,
      "to" -> email.to,
      "subject" -> email.subject,
      "body" -> email.body)

  def toAttributes(email: FirstEmail): Map[String, String] = toAttrs(email) +
    ("generated" -> (dateTimeFormat format email.generated)) +
    ("preSend" -> (dateTimeFormat format email.preSend)) +
    ("postSend" -> (dateTimeFormat format email.postSend))

  def toAttributes(email: SecondEmail): Map[String, String] = toAttrs(email) +
    ("received" -> (dateTimeFormat format email.received))

  def dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS Z")

  def storeFirst(email: FirstEmail) = db.putAttributesAsync(new PutAttributesRequest(firstDomain, email.id, toAttributes(email)))

  def storeSecond(email: SecondEmail) = db.putAttributesAsync(new PutAttributesRequest(secondDomain, email.id, toAttributes(email)))

  def toFirstEmail(item: Item) = {
    val attrs: Map[String, String] = item
    FirstEmail(
      attrs("runId"),
      attrs("emailId").toLong,
      attrs("from"),
      attrs("to"),
      attrs("subject"),
      attrs("body"),
      dateTimeFormat.parse(attrs("generated")),
      dateTimeFormat.parse(attrs("preSend")),
      dateTimeFormat.parse(attrs("postSend")))
  }

  def toSecondEmail(item: Item) = {
    val attrs: Map[String, String] = item
    SecondEmail(
      attrs("runId"),
      attrs("emailId").toLong,
      attrs("from"),
      attrs("to"),
      attrs("subject"),
      attrs("body"),
      dateTimeFormat.parse(attrs("received")))
  }

  def getRun(runId: String): Option[Run] = {
    def getItems(domain: String): List[Item] = {
      val query = "select * from %s where runId='%s'".format(domain, runId)
      var nextToken: String = null
      var items: List[Item] = Nil
      do {
        val resp = db.select(new SelectRequest(query).withNextToken(nextToken))
        nextToken = resp.getNextToken
        items = items ++ resp.getItems
      } while (nextToken != null)
      items
    }
    val emails1 = getItems(firstDomain) map { toFirstEmail } sortWith { _.emailId < _.emailId }
    val emails2 = getItems(secondDomain) map { toSecondEmail }
    val emails = emails1 map { e1 => (e1, emails2.find(_.emailId == e1.emailId)) }
    Some(Run(runId, emails))
  }
}
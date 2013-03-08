package com.pongr.jetray

import org.specs2.mutable._
import javax.mail.{ Message, Session }
import javax.mail.Message.RecipientType.TO
import javax.mail.internet.{ InternetAddress, MimeMessage }
import java.util.{ UUID, Properties }

class AddressEmailCodecSpec extends Specification {
  def newMessage: Message = new MimeMessage(Session.getInstance(new Properties))

  "The encoder" should {
    "encode without a prefix" in {
      val codec = new AddressEmailCodec("pongr.com")
      val runId = UUID.randomUUID
      val emailId = 5l
      val message = newMessage
      val from = runId + "-" + emailId + "@pongr.com"

      codec.encode(runId, emailId, message)
      val froms = message.getFrom
      froms.size must_== 1
      froms(0) must_== new InternetAddress(from)
    }
    
    "encode with a prefix" in {
      val codec = new AddressEmailCodec("gmail.com", Some("someuser"))
      val runId = UUID.randomUUID
      val emailId = 5l
      val message = newMessage
      val from = "someuser+" + runId + "-" + emailId + "@gmail.com"

      codec.encode(runId, emailId, message)
      val froms = message.getFrom
      froms.size must_== 1
      froms(0) must_== new InternetAddress(from)
    }
    
    "encode with a first & last name" in {
      val codec = new AddressEmailCodec(domain = "pongr.com", includeName = true)
      val runId = UUID.randomUUID
      val emailId = 5l
      val message = newMessage
      val name = "Test%s-%d X".format(runId.toString, emailId)
      val from = "%s <%s-%d@pongr.com>".format(name, runId, emailId)

      val encoded = codec.encode(runId, emailId)
      encoded must_== from
    }
  }
  
  "The decoder" should {
    "decode without a prefix" in {
      val codec = new AddressEmailCodec("pongr.com")
      val runId = UUID.randomUUID
      val emailId = 5l
      val message = newMessage
      val to = runId + "-" + emailId + "@pongr.com"
      message.setRecipient(TO, new InternetAddress(to))
      
      val Some((runId2, emailId2)) = codec.decode(message)
      runId2 must_== runId
      emailId2 must_== emailId
    }
    
    "decode with a prefix" in {
      val codec = new AddressEmailCodec("gmail.com", Some("someuser"))
      val runId = UUID.randomUUID
      val emailId = 5l
      val message = newMessage
      val to = "someuser+" + runId + "-" + emailId + "@gmail.com"
      message.setRecipient(TO, new InternetAddress(to))
      
      val Some((runId2, emailId2)) = codec.decode(message)
      runId2 must_== runId
      emailId2 must_== emailId
    }
    
    "not decode malformed TO address" in {
      val codec = new AddressEmailCodec("pongr.com")
      val message = newMessage
      val to = "test@test.org"
      message.setRecipient(TO, new InternetAddress(to))
      
      codec.decode(message) must_== None
    }
    
    "decode with a first & last name" in {
      val codec = new AddressEmailCodec(domain = "pongr.com", includeName = true)
      val runId = UUID.randomUUID
      val emailId = 5l
      
      val encoded = codec.encode(runId, emailId)
      codec.decode(encoded) must_== Some(runId, emailId)
    }
  }
}
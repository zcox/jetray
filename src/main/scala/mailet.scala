package com.pongr.jetray

import org.apache.mailet._
import org.apache.mailet.base._
import akka.actor.Actor.actorOf
import akka.routing.Listen

/** Mailet that passes off every email to a [[com.pongr.jetray.Receiver]]. 
 * Clients must provide instances of [[com.pongr.jetray.EmailRepository]] and [[com.pongr.jetray.EmailCodec]]
 * and also register the concrete subclass with James. */
trait JetrayMailet extends GenericMailet {
  override def getMailetName() = getClass.getSimpleName
  override def getMailetInfo() = "Jetray rulez"
  def log(mail: Mail) { log(getMailetName() + " processing " + mail.getName + " from " + mail.getSender + " to " + mail.getRecipients) }
  //override def log(msg: String) { super.log(getMailetName + " " + msg) }
  
  val repository = newEmailRepository

  override def init() {
    log(getMailetName() + " starting up...")
    val publisher = actorOf[Publisher].start
    val storeSecond = actorOf(new StoreSecondActor(repository)).start
    publisher ! Listen(storeSecond)
  }

  override def service(mail: Mail) {
    log(mail)
    new Receiver(newEmailCodec, repository) process mail.getMessage
    log("After Receiver")
    mail.setState(Mail.GHOST)
    log("After setting to GHOST")
  }
  
  /** Clients must override this and return a new [[com.pongr.jetray.EmailRepository]]. */
  protected def newEmailRepository: EmailRepository
  /** Clients must override this and return a new [[com.pongr.jetray.EmailCodec]]. */
  protected def newEmailCodec: EmailCodec
}
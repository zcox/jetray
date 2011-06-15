# Overview

Can your mail server handle 1000 incoming emails/min? To truly know the answer, you need to throw 1000 emails/min at your mail server.  Jetray can help you do that.

Jetray provides some Scala tools to periodically generate & send emails via SMTP.  You tell Jetray how many emails/sec to send and provide an actor that creates emails using [JavaMail](http://www.oracle.com/technetwork/java/javamail/index.html), and Jetray does the rest.

By using the [Typesafe stack](http://typesafe.com/stack) (specifically [Akka](http://akka.io/)), Jetray can generate & send email concurrently, a requirement for delivering multiple emails/sec.  Most Jetray components are actors, and you have full control over them.

# Future Work

Jetray is pretty young (but functional) and here are some other things it may do someday:

 - Verify email was delivered (maybe using IMAP?)
 - Collect statistics
 - Distributed high-volume workloads across a cluster of Jetray servers

# Example

Jetray is basically just a .jar with some tools to make generating & sending email simple. Using sbt, we just need a Main class and a few .props files.

projct/build/Project.scala - NOTE: until Jetray is available in a public repo, you'd need to publish-local jetray for this to work :(

``` scala
  val akkaRepo = "akka" at "http://akka.io/repository/"
  val javaNetRepo = "java.net" at "http://download.java.net/maven/2/"
  
  val jetray = "com.pongr" %% "jetray" % "0.2.4"
```

src/main/resources/smtp.props

```
host=yourdomain.com
port=25
user=TheUsernameOrLeaveBlank
password=ThePasswordOrLeaveBlank
```

src/main/resources/controller.props, frequency is emails/sec and count is the total number of emails you want sent.

```
frequency=6
count=18
```

src/main/resources/akka.conf, this is optional but if you don't provide it you won't see any logging from your app.

```
akka {
  event-handlers = ["akka.event.EventHandler$DefaultListener"]
  event-handler-level = "DEBUG"
}
```

src/main/scala/Main.scala

``` scala
import javax.mail._
import javax.mail.Message.RecipientType._
import javax.mail.internet._
import java.util.Date
import akka.actor.Actor
import akka.actor.Actor._
import com.pongr.jetray._
import com.pongr.jetray.Actors._

object Main {
  def main(args: Array[String]) {
    //read params from props files
    val controllerParams = ControllerParams.fromResource()
    val smtpParams = SmtpParams.fromResource()
    
    //this is where you create your email (see example below)
    val generator = actorOf(new Generator("user1@domain1.com", "user2@domain2.com")).start
    
    //setup Jetray actors and kick everything off
    val mailer = loadBalance(controllerParams.frequency, actorOf(new MailerActor(smtpParams)).start)
    val controller = actorOf(new Controller(controllerParams, generator, mailer)).start
    controller ! Tick
  }
}

/** You must provide an actor that receives a com.pongr.jetray.Generate message 
  * and replies with a new javax.mail.Message object to send. */
class Generator(from: String, to: String) extends Actor with MailSession {
  def receive = {
    case Generate(runId, emailId) => 
      val msg = new MimeMessage(session)
      msg.setFrom(new InternetAddress(from))
      msg.setRecipients(TO, Array[Address](new InternetAddress(to)))
      msg.setSubject(runId + " / " + emailId)
      msg.setText("This is a test message")
      msg.setSentDate(new Date)
      
      self reply msg
  }
}
```

Now just do ```sbt run``` and you should see output like this:

```
[info] == run ==
[info] Running com.pongr.jetrayexample.Main 
Loading config [akka.conf] from the application classpath.
[DEBUG]   [6/15/11 2:54 PM] [run-main] [MailerActor] Connected Transport in 234 msec
[DEBUG]   [6/15/11 2:54 PM] [run-main] [MailerActor] Connected Transport in 203 msec
[DEBUG]   [6/15/11 2:54 PM] [run-main] [MailerActor] Connected Transport in 203 msec
[DEBUG]   [6/15/11 2:54 PM] [run-main] [MailerActor] Connected Transport in 202 msec
[DEBUG]   [6/15/11 2:54 PM] [run-main] [MailerActor] Connected Transport in 184 msec
[DEBUG]   [6/15/11 2:54 PM] [run-main] [MailerActor] Connected Transport in 185 msec
[DEBUG]   [6/15/11 2:54 PM] [akka:event-driven:dispatcher:global-11] [MailerActor] Sent message 39a882a3-68e5-45b9-bac8-60222d9693ac / 1 in 183 msec
[DEBUG]   [6/15/11 2:55 PM] [akka:event-driven:dispatcher:global-16] [MailerActor] Sent message 39a882a3-68e5-45b9-bac8-60222d9693ac / 2 in 154 msec
[DEBUG]   [6/15/11 2:55 PM] [akka:event-driven:dispatcher:global-23] [MailerActor] Sent message 39a882a3-68e5-45b9-bac8-60222d9693ac / 3 in 157 msec
[DEBUG]   [6/15/11 2:55 PM] [akka:event-driven:dispatcher:global-8] [MailerActor] Sent message 39a882a3-68e5-45b9-bac8-60222d9693ac / 4 in 155 msec
[DEBUG]   [6/15/11 2:55 PM] [akka:event-driven:dispatcher:global-15] [MailerActor] Sent message 39a882a3-68e5-45b9-bac8-60222d9693ac / 5 in 200 msec
[DEBUG]   [6/15/11 2:55 PM] [akka:event-driven:dispatcher:global-16] [MailerActor] Sent message 39a882a3-68e5-45b9-bac8-60222d9693ac / 6 in 163 msec
[DEBUG]   [6/15/11 2:55 PM] [akka:event-driven:dispatcher:global-9] [MailerActor] Sent message 39a882a3-68e5-45b9-bac8-60222d9693ac / 7 in 165 msec
[DEBUG]   [6/15/11 2:55 PM] [akka:event-driven:dispatcher:global-8] [MailerActor] Sent message 39a882a3-68e5-45b9-bac8-60222d9693ac / 8 in 172 msec
[DEBUG]   [6/15/11 2:55 PM] [akka:event-driven:dispatcher:global-19] [MailerActor] Sent message 39a882a3-68e5-45b9-bac8-60222d9693ac / 9 in 170 msec
[DEBUG]   [6/15/11 2:55 PM] [akka:event-driven:dispatcher:global-14] [MailerActor] Sent message 39a882a3-68e5-45b9-bac8-60222d9693ac / 11 in 168 msec
[DEBUG]   [6/15/11 2:55 PM] [akka:event-driven:dispatcher:global-23] [MailerActor] Sent message 39a882a3-68e5-45b9-bac8-60222d9693ac / 10 in 393 msec
[DEBUG]   [6/15/11 2:55 PM] [akka:event-driven:dispatcher:global-20] [MailerActor] Sent message 39a882a3-68e5-45b9-bac8-60222d9693ac / 12 in 160 msec
[DEBUG]   [6/15/11 2:55 PM] [akka:event-driven:dispatcher:global-25] [MailerActor] Sent message 39a882a3-68e5-45b9-bac8-60222d9693ac / 13 in 159 msec
[DEBUG]   [6/15/11 2:55 PM] [akka:event-driven:dispatcher:global-12] [MailerActor] Sent message 39a882a3-68e5-45b9-bac8-60222d9693ac / 14 in 165 msec
[DEBUG]   [6/15/11 2:55 PM] [akka:event-driven:dispatcher:global-14] [MailerActor] Sent message 39a882a3-68e5-45b9-bac8-60222d9693ac / 15 in 163 msec
[DEBUG]   [6/15/11 2:55 PM] [akka:event-driven:dispatcher:global-16] [MailerActor] Sent message 39a882a3-68e5-45b9-bac8-60222d9693ac / 16 in 184 msec
[DEBUG]   [6/15/11 2:55 PM] [akka:event-driven:dispatcher:global-12] [Controller] Sent 18 emails
[DEBUG]   [6/15/11 2:55 PM] [akka:event-driven:dispatcher:global-12] [Controller] Sending PoisonPills to MailerActors...
[DEBUG]   [6/15/11 2:55 PM] [akka:event-driven:dispatcher:global-25] [MailerActor] Sent message 39a882a3-68e5-45b9-bac8-60222d9693ac / 17 in 175 msec
[DEBUG]   [6/15/11 2:55 PM] [akka:event-driven:dispatcher:global-12] [MailerActor] Sent message 39a882a3-68e5-45b9-bac8-60222d9693ac / 18 in 157 msec
[DEBUG]   [6/15/11 2:55 PM] [akka:event-driven:dispatcher:global-23] [ReflectiveAccess$Remote$] java.lang.ClassNotFoundException: akka.remote.netty.NettyRemoteSupport
[DEBUG]   [6/15/11 2:55 PM] [akka:event-driven:dispatcher:global-19] [MailerActor] Closed Transport in 37 msec
[DEBUG]   [6/15/11 2:55 PM] [akka:event-driven:dispatcher:global-13] [MailerActor] Closed Transport in 37 msec
[DEBUG]   [6/15/11 2:55 PM] [akka:event-driven:dispatcher:global-14] [MailerActor] Closed Transport in 38 msec
[DEBUG]   [6/15/11 2:55 PM] [akka:event-driven:dispatcher:global-20] [MailerActor] Closed Transport in 42 msec
[DEBUG]   [6/15/11 2:55 PM] [akka:event-driven:dispatcher:global-7] [MailerActor] Closed Transport in 40 msec
[DEBUG]   [6/15/11 2:55 PM] [akka:event-driven:dispatcher:global-10] [MailerActor] Closed Transport in 286 msec
```

# License

Jetray is licensed under the [Apache 2 License](http://www.apache.org/licenses/LICENSE-2.0.txt).


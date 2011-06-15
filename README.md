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

# License

Jetray is licensed under the [Apache 2 License](http://www.apache.org/licenses/LICENSE-2.0.txt).


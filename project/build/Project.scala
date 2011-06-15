import sbt._
import de.element34.sbteclipsify._

class Project(info: ProjectInfo) extends DefaultProject(info) with AkkaProject with Eclipsify {
  val javaNetRepo = "java.net" at "http://download.java.net/maven/2/"
  
  val commonsIo = "commons-io" % "commons-io" % "2.0.1"

  //This will pull in activation-1.1.jar
  //val mail = "javax.mail" % "mail" % "1.4.4"
  
  //This will exclude activation-1.1.jar
  override def ivyXML = 
    <dependencies>
      <dependency org="javax.mail" name="mail" rev="1.4.4">
        <exclude org="javax.activation" name="activation" />
      </dependency>
    </dependencies>
}

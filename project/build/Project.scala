import sbt._
import de.element34.sbteclipsify._

class Project(info: ProjectInfo)
  extends DefaultProject(info)
  with AkkaProject
  with Eclipsify
  with LicenseHeaders
  with ApacheLicense2 {

  def copyrightLine = "Copyright (c) 2011 Pongr, Inc."

  val javaNetRepo = "java.net" at "http://download.java.net/maven/2/"
  val commonsIo = "commons-io" % "commons-io" % "2.0.1"
  val amazon = "com.amazonaws" % "aws-java-sdk" % "1.2.1"
  val mailet = "org.apache.james" % "apache-mailet-base" % "1.1"

  //This includes activation-1.1.jar
  //val mail = "javax.mail" % "mail" % "1.4.4"

  //This excludes activation-1.1.jar (assume everyone is on Java 1.6+)
  override def ivyXML =
    <dependencies>
      <dependency org="javax.mail" name="mail" rev="1.4.4">
        <exclude org="javax.activation" name="activation"/>
      </dependency>
    </dependencies>

  val specs2 = "org.specs2" %% "specs2" % "1.5" % "test"
  val scalaz = "org.specs2" %% "specs2-scalaz-core" % "6.0.RC2" % "test"
  def specs2Framework = new TestFramework("org.specs2.runner.SpecsFramework")
  override def testFrameworks = super.testFrameworks ++ Seq(specs2Framework)

  override def managedStyle = ManagedStyle.Maven
  val publishTo = "scala-tools Releases" at "http://nexus.scala-tools.org/content/repositories/releases/"
  Credentials(Path.userHome / ".ivy2" / ".scala_tools_credentials", log)
}

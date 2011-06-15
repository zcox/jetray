import sbt._

class Plugins(info: ProjectInfo) extends PluginDefinition(info) {
  lazy val eclipse = "de.element34" % "sbt-eclipsify" % "0.7.0"
  val licensePlugin = "com.banno" % "sbt-license-plugin" % "0.0.2" from "http://cloud.github.com/downloads/T8Webware/sbt-license-plugin/sbt-license-plugin-0.0.2.jar"
  
  val akkaRepo = "akka" at "http://akka.io/repository/"
  val akkaPlugin = "se.scalablesolutions.akka" % "akka-sbt-plugin" % "1.1.2"
}

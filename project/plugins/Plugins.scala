import sbt._

class Plugins(info: ProjectInfo) extends PluginDefinition(info) {
  lazy val eclipse = "de.element34" % "sbt-eclipsify" % "0.7.0"
  
  val akkaRepo = "akka" at "http://akka.io/repository/"
  val akkaPlugin = "se.scalablesolutions.akka" % "akka-sbt-plugin" % "1.1.2"
}

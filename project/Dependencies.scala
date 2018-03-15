import sbt._

object Dependencies {
  lazy val akkaActor = "com.typesafe.akka" %% "akka-actor" % "2.5.9"
  lazy val akkaRemote = "com.typesafe.akka" %% "akka-remote" % "2.5.9"
  lazy val akkaHttp = "com.typesafe.akka" %% "akka-http" % "10.0.11"
  lazy val circeCore = "io.circe" %% "circe-core" % "0.9.0"
  lazy val circeGeneric = "io.circe" %% "circe-generic" % "0.9.0"
  lazy val akkaHttpCirce = "de.heikoseeberger" %% "akka-http-circe" % "1.20.0-RC1"

  lazy val scalaTest = "org.scalatest" %% "scalatest" % "3.0.3"
}

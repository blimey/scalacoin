name := "scalacoin"

version := "1.0"

scalaVersion := "2.12.4"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.5.9",
  "com.typesafe.akka" %% "akka-stream" % "2.5.9",
  "com.typesafe.akka" %% "akka-http" % "10.0.11",
  "io.circe" %% "circe-core" % "0.9.0",
  "io.circe" %% "circe-generic" % "0.9.0",
  "de.heikoseeberger" %% "akka-http-circe" % "1.20.0-RC1",
  "com.typesafe.akka" %% "akka-testkit" % "2.5.9" % Test,
  "com.typesafe.akka" %% "akka-stream-testkit" % "2.5.9" % Test,
  "com.typesafe.akka" %% "akka-http-testkit" % "10.0.11" % Test
)
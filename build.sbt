import Dependencies._

lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization := "blimey",
      scalaVersion := "2.12.4",
      version      := "0.1.0-SNAPSHOT"
    )),
    name := "Scalacoin",
    scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature"),
    libraryDependencies ++= Seq(akkaActor, akkaRemote, akkaHttp, circeCore, circeGeneric, akkaHttpCirce),
    libraryDependencies += scalaTest % Test
  )

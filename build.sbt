import Dependencies._

lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization := "org.typegrade",
      organizationName := "Typegrade",
      scalaVersion := "2.12.5",
      version      := "0.1.0"
    )),
    name := "Scalacoin",
    scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature"),
    resolvers += Resolver.bintrayRepo("matteomeli", "maven"),
    libraryDependencies ++= Seq(glue, akkaActor, akkaRemote, akkaHttp, circeCore, circeGeneric, akkaHttpCirce),
    libraryDependencies += scalaTest % Test
  )

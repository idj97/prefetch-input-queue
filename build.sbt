ThisBuild / scalaVersion := "2.13.12"
ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / organization := "com.idj"
ThisBuild / organizationName := "idj"

lazy val root = (project in file("."))
  .settings(
    name := "prefetch-input-queue",
    libraryDependencies ++= Seq(
      "com.lihaoyi" %% "castor" % "0.3.0",
      "org.slf4j" % "slf4j-api" % "2.0.9",
      "ch.qos.logback" % "logback-classic" % "1.4.11" % Test,
      "org.scalatest" %% "scalatest" % "3.2.17" % Test,
      "org.mockito" % "mockito-core" % "5.7.0" % Test
    )
  )

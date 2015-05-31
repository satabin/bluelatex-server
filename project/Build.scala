package blue

import sbt._
import Keys._

import java.io.File

import sbtbuildinfo.Plugin._

import com.typesafe.sbt.SbtScalariform._
import scalariform.formatter.preferences._

object BlueBuild extends BlueBuild

class BlueBuild extends Build {

  val blueVersion = "2.0.0-SNAPSHOT"

  lazy val commonSettings = Seq(
    resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/",
    resolvers += "Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/",
    organization := "org.gnieh",
    version := blueVersion,
    scalaVersion := "2.11.6",
    scalacOptions ++= Seq("-deprecation", "-feature"),
    libraryDependencies ++= commonDeps,
    fork in run := true) ++ scalariformSettings ++ Seq(
      ScalariformKeys.preferences := {
        ScalariformKeys.preferences.value
          .setPreference(AlignSingleLineCaseStatements, true)
          .setPreference(DoubleIndentClassDeclaration, true)
          .setPreference(MultilineScaladocCommentsStartOnFirstLine, true)
      })

  lazy val bluelatex = (Project(id = "bluelatex",
    base = file(".")) settings(commonSettings: _*) settings (
      name := "bluelatex"
  )) aggregate(blueCore)


  lazy val blueCore =
    (Project(id = "blue-core", base = file("blue-core"))
      settings(commonSettings: _*)
      settings(buildInfoSettings: _*)
      settings(
        name := "blue-core",
        sourceGenerators in Compile <+= buildInfo,
        buildInfoKeys := Seq[BuildInfoKey](
          version,
          scalaVersion,
          BuildInfoKey.action("buildTime") {
            System.currentTimeMillis
          }
        ),
        buildInfoPackage := "gnieh.blue",
        buildInfoObject := "BlueInfo"
      )
    )

  lazy val commonDeps = Seq(
    "io.spray" %% "spray-routing" % "1.3.3",
    "io.spray" %% "spray-can" % "1.3.3",
    "io.spray" %% "spray-json" % "1.3.2",
    "net.tanesha.recaptcha4j" % "recaptcha4j" % "0.0.7",
    "org.apache.pdfbox" % "pdfbox" % "1.8.9" exclude("commons-logging", "commons-logging"),
    "com.typesafe.akka" %% "akka-actor" % "2.3.11",
    "org.gnieh" %% "tekstlib" % "0.1.0-SNAPSHOT",
    "org.gnieh" %% "sohva-client" % "2.0.0-SNAPSHOT",
    "org.gnieh" %% "sohva-entities" % "2.0.0-SNAPSHOT",
    "org.gnieh" %% "sohva-dm" % "2.0.0-SNAPSHOT",
    "org.gnieh" %% "diffson" % "1.0.0",
    "javax.mail" % "mail" % "1.4.7",
    "ch.qos.logback" % "logback-classic" % "1.1.3",
    "org.slf4j" % "jcl-over-slf4j" % "1.7.12",
    "com.jsuereth" %% "scala-arm" % "1.4",
    "com.typesafe" % "config" % "1.3.0"
  )

}

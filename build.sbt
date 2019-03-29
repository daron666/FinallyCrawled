organization := "org.daron"

name := "pure-crawler"

version := "0.0.1"

scalaVersion := "2.12.8"

lazy val sttpVersion = "1.5.11"

libraryDependencies ++= Seq(
  "org.typelevel" %% "cats-core" % "1.6.0",
  "org.typelevel" %% "cats-effect" % "1.2.0",
  "io.chrisdavenport" %% "log4cats-slf4j" % "0.3.0",
  "ch.qos.logback" % "logback-classic" % "1.2.3",
  "com.softwaremill.sttp" %% "core" % sttpVersion,
  "com.softwaremill.sttp"      %% "async-http-client-backend-cats" % sttpVersion,
  "org.jsoup" % "jsoup" % "1.11.3"
)

scalacOptions ++= Seq(
  "-deprecation",
  "-encoding", "UTF-8",
  "-language:experimental.macros",
  "-language:higherKinds",
  "-language:postfixOps",
  "-feature",
  "-unchecked",
  "-Xfuture",
  "-Yno-adapted-args",
  "-Ywarn-numeric-widen",
  "-Ypartial-unification"
)

addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.9.9")
addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.0-M4")
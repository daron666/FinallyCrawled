organization := "org.daron"

name := "pure-crawler"

version := "0.0.1"

scalaVersion := "2.12.8"

resolvers += Resolver.sonatypeRepo("snapshots")

lazy val sttpVersion = "1.5.11"
lazy val http4sVersion = "0.20.0-SNAPSHOT"
lazy val circeVersion = "0.10.0"

lazy val catsDeps = Seq(
  "org.typelevel" %% "cats-core" % "1.6.0",
  "org.typelevel" %% "cats-effect" % "1.2.0",
  "io.chrisdavenport" %% "log4cats-slf4j" % "0.3.0"
)

lazy val monixDeps = Seq(
  "io.monix" %% "monix" % "3.0.0-RC2"
)

lazy val sttpDeps = Seq(
  "com.softwaremill.sttp" %% "core" % sttpVersion,
  "com.softwaremill.sttp" %% "async-http-client-backend-cats" % sttpVersion
)

lazy val http4sDeps = Seq(
  "org.http4s" %% "http4s-dsl" % http4sVersion,
  "org.http4s" %% "http4s-blaze-server" % http4sVersion,
  "org.http4s" %% "http4s-blaze-client" % http4sVersion,
  "org.http4s" %% "http4s-circe" % http4sVersion
)

lazy val sangriaDeps = Seq(
  "org.sangria-graphql" %% "sangria" % "1.4.2",
  "org.sangria-graphql" %% "sangria-circe" % "1.2.1"
)

lazy val circeDeps = Seq(
  "io.circe" %% "circe-core" % circeVersion,
  "io.circe" %% "circe-generic" % circeVersion,
  "io.circe" %% "circe-optics" % circeVersion
)

lazy val otherDeps = Seq(
  "ch.qos.logback" % "logback-classic" % "1.2.3",
  "org.jsoup" % "jsoup" % "1.11.3",
  "org.scalactic" %% "scalactic" % "3.0.5",
  "org.scalatest" %% "scalatest" % "3.0.5" % "test",
  "com.github.pureconfig" %% "pureconfig" % "0.10.2"
)

libraryDependencies ++= catsDeps ++ sttpDeps ++ http4sDeps ++ sangriaDeps ++ circeDeps ++ otherDeps ++ monixDeps

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
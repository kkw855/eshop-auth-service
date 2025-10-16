ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / scalaVersion := "3.7.0"

lazy val org = "com.eshop"

lazy val catsEffectVersion = "3.6.3"

lazy val fs2Version = "3.12.2"

lazy val pureConfigVersion = "0.17.9"

lazy val log4catsVersion = "2.7.1"
lazy val slf4jVersion = "2.0.17"

lazy val circeVersion = "0.14.1"
lazy val http4sVersion = "0.23.30"

lazy val JwtHttp4sVersion = "2.0.10"
lazy val JwtScalaVersion = "11.0.2"

lazy val mongoVersion = "5.5.1"

lazy val scalaTestVersion = "3.2.19"
lazy val scalaTestCatsEffectVersion = "1.6.0"
lazy val testContainerVersion = "1.21.3"
lazy val logbackVersion = "1.4.14"

resolvers += "Maven Central Server" at "https://repo1.maven.org/maven2"

lazy val server = (project in file("."))
  .settings(
    name := "eshop-auth-service",
    scalaVersion := scalaVersion.value,
    organization := org,
    assembly / mainClass := Some("com.eshop.auth.Application"), // Set your main class here
    libraryDependencies ++= Seq(
      "io.lettuce" % "lettuce-core" % "6.8.1.RELEASE",
      "co.fs2" %% "fs2-core" % fs2Version,
      "org.typelevel" %% "cats-effect" % catsEffectVersion,
      "com.github.pureconfig" %% "pureconfig-core" % pureConfigVersion,
      // Logging
      "org.typelevel" %% "log4cats-slf4j" % log4catsVersion,
      "ch.qos.logback" % "logback-classic" % logbackVersion,
      // Email
      "com.sun.mail" % "javax.mail" % "1.6.2",
      "com.sun.mail" % "jakarta.mail" % "2.0.2",
      // Optional for auto-derivation of JSON codecs
      "io.circe" %% "circe-generic" % circeVersion,
      "io.circe" %% "circe-fs2" % circeVersion,
      "io.circe" %% "circe-parser" % circeVersion,
      "org.http4s" %% "http4s-dsl" % http4sVersion,
      "org.http4s" %% "http4s-ember-server" % http4sVersion,
      "org.http4s" %% "http4s-circe" % http4sVersion,
      "org.mongodb" % "mongodb-driver-sync" % mongoVersion,
      "org.bouncycastle" % "bcpkix-jdk18on" % "1.81",
      "dev.profunktor" %% "http4s-jwt-auth" % JwtHttp4sVersion,
      "com.github.jwt-scala" %% "jwt-core" % JwtScalaVersion,
      "com.github.jwt-scala" %% "jwt-circe" % JwtScalaVersion,
      "org.scalatest" %% "scalatest" % scalaTestVersion % Test,
      "org.typelevel" %% "cats-effect-testing-scalatest" % scalaTestCatsEffectVersion % Test,
      "org.testcontainers" % "testcontainers" % testContainerVersion % Test,
      "org.testcontainers" % "postgresql" % testContainerVersion % Test
    ),
  )

assemblyMergeStrategy in assembly := {
  case PathList("META-INF", xs@_*) => MergeStrategy.discard
  case x => MergeStrategy.first
}

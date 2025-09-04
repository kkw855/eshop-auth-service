ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / scalaVersion := "3.7.0"

lazy val org = "com.eshop"

lazy val catsEffectVersion = "3.6.1"

lazy val pureConfigVersion = "0.17.9"

lazy val log4catsVersion = "2.7.0"
lazy val slf4jVersion = "2.0.17"

lazy val circeVersion = "0.14.1"
lazy val http4sVersion = "0.23.30"
lazy val doobieVersion = "1.0.0-RC9"

lazy val JwtHttp4sVersion = "1.2.0"
lazy val JwtScalaVersion = "9.3.0"

lazy val scalaTestVersion = "3.2.19"
lazy val scalaTestCatsEffectVersion = "1.6.0"
lazy val testContainerVersion = "1.21.0"
lazy val logbackVersion = "1.5.13"

resolvers += "Maven Central Server" at "https://repo1.maven.org/maven2"

lazy val server = (project in file("."))
  .settings(
    name := "eshop-auth-service",
    scalaVersion := scalaVersion.value,
    organization := org,
    assembly / mainClass := Some("com.eshop.auth.Application"), // Set your main class here
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-effect" % catsEffectVersion,
      "com.github.pureconfig" %% "pureconfig-core" % pureConfigVersion,
      "org.typelevel" %% "log4cats-slf4j" % log4catsVersion,
      "org.slf4j" % "slf4j-simple" % slf4jVersion,
      "io.circe" %% "circe-generic" % circeVersion,
      "io.circe" %% "circe-fs2" % circeVersion,
      "io.circe" %% "circe-parser" % circeVersion,
      "org.http4s" %% "http4s-dsl" % http4sVersion,
      "org.http4s" %% "http4s-ember-server" % http4sVersion,
      "org.http4s" %% "http4s-circe" % http4sVersion,
      "org.tpolecat" %% "doobie-core" % doobieVersion,
      "org.tpolecat" %% "doobie-hikari" % doobieVersion,
      "org.tpolecat" %% "doobie-postgres" % doobieVersion,
      "org.bouncycastle" % "bcpkix-jdk18on" % "1.81",
      "dev.profunktor" %% "http4s-jwt-auth" % JwtHttp4sVersion,
      "com.github.jwt-scala" %% "jwt-core" % JwtScalaVersion,
      "com.github.jwt-scala" %% "jwt-circe" % JwtScalaVersion,
      // "org.tpolecat"          %% "doobie-postgres-circe" % doobieVersion,
      "org.tpolecat" %% "doobie-scalatest" % doobieVersion % Test,
      "org.scalatest" %% "scalatest" % scalaTestVersion % Test,
      "org.typelevel" %% "cats-effect-testing-scalatest" % scalaTestCatsEffectVersion % Test,
      "org.testcontainers" % "testcontainers" % testContainerVersion % Test,
      "org.testcontainers" % "postgresql" % testContainerVersion % Test,
      "ch.qos.logback" % "logback-classic" % logbackVersion % Test
    ),
  )

assemblyMergeStrategy in assembly := {
  case PathList("META-INF", xs@_*) => MergeStrategy.discard
  case x => MergeStrategy.first
}

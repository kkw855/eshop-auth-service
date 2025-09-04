package com.eshop.auth

import cats.effect.{IO, IOApp}
import com.comcast.ip4s.{host, port}
import org.http4s.HttpRoutes
import org.http4s.ember.server.EmberServerBuilder
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import cats.effect.*
import org.http4s.*
import org.http4s.dsl.io.*

object Application extends IOApp.Simple {

  given logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  val helloWorldService: HttpRoutes[IO] = HttpRoutes.of[IO] {
    case GET -> Root / auth =>
      Ok(s"Hello, Auth")
  }

  override def run: IO[Unit] = EmberServerBuilder
    .default[IO]
    .withHost(host"0.0.0.0")
    .withPort(port"5001")
    .withHttpApp(helloWorldService.orNotFound)
    .build
    .use(_ => IO.println("Start Ecommerce Server") *> IO.never)
}

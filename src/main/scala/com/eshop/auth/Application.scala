package com.eshop.auth

import cats.effect.{IO, IOApp, Resource}
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import pureconfig.ConfigSource
import org.http4s.server.Server
import org.http4s.ember.server.EmberServerBuilder
import com.eshop.auth.config.AppConfig
import com.eshop.auth.config.syntax.*
import com.eshop.auth.modules.{Core, Database, HttpApi, LiveMailSender, Redis}

object Application extends IOApp.Simple {

  given logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  override def run: IO[Unit] =
    ConfigSource.default.loadF[IO, AppConfig].flatMap {
      case AppConfig(emberConfig, mongoConfig, redisConfig, smtpConfig) =>
        val appResource: Resource[IO, Server] = for {
          redisCommands <- Redis.makeRedisResource[IO](redisConfig)
          mongo         <- Database.makeMongoResource[IO](mongoConfig)
          mailSender    <- LiveMailSender[IO](smtpConfig)
          core          <- Core[IO](mongo, redisCommands, mailSender)
          httpApi       <- HttpApi[IO](core)
          // TODO: http4s request limit using client ip 검색
          server <- EmberServerBuilder
            .default[IO]
            .withHost(emberConfig.ipAddress)
            .withPort(emberConfig.port)
            .withHttpApp(httpApi.endPoints.orNotFound)
            .build
        } yield server

        appResource.use(_ => logger.info("Start EShop Auth Server") *> IO.never)
    }
}

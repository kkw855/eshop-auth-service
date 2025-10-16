package com.eshop.auth.modules

import cats.implicits.*
import cats.effect.{Async, Resource}

import io.lettuce.core.RedisClient
import io.lettuce.core.api.sync.RedisCommands

import com.eshop.auth.config.RedisConfig

object Redis {
  def makeRedisResource[F[_]: Async](
      config: RedisConfig
  ): Resource[F, RedisCommands[String, String]] =
    for {
      client <- Resource.make(Async[F].delay(RedisClient.create(config.uri)))(c =>
        Async[F].delay(c.shutdown().pure[F])
      )
      connection <- Resource.make(Async[F].delay(client.connect()))(c => Async[F].delay(c.close()))
      commands   <- Resource.eval(Async[F].delay(connection.sync()))
    } yield commands
}

package com.eshop.auth.modules

import cats.implicits.*
import cats.effect.{Async, Resource}

import org.typelevel.log4cats.Logger

import com.mongodb.client.MongoDatabase
import io.lettuce.core.api.sync.RedisCommands

import com.eshop.auth.core.{LiveUsers, Users}
import com.eshop.auth.modules.MailSender

final class Core[F[_]] private (val mailSender: MailSender[F], val users: Users[F])

object Core {
  def apply[F[_]: {Async, Logger}](
      database: MongoDatabase,
      redis: RedisCommands[String, String],
      mailSender: MailSender[F]
  ): Resource[F, Core[F]] = {
    val coreF = for {
      users <- LiveUsers[F](database, redis)
    } yield new Core(mailSender, users)

    Resource.eval(coreF)
  }
}

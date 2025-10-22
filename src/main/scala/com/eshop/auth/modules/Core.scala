package com.eshop.auth.modules

import cats.implicits.*
import cats.effect.{Async, Resource}

import org.typelevel.log4cats.Logger

import com.mongodb.client.MongoDatabase

import com.eshop.auth.core.{LiveUsers, Users}
import com.eshop.auth.modules.{MailSender, Redis}

final class Core[F[_]] private (
    val mailSender: MailSender[F],
    val redisClient: Redis[F],
    val users: Users[F]
)

object Core {
  def apply[F[_]: {Async, Logger}](
      database: MongoDatabase,
      redisClient: Redis[F],
      mailSender: MailSender[F]
  ): Resource[F, Core[F]] = {
    val coreF = for {
      users <- LiveUsers[F](database)
    } yield new Core(mailSender, redisClient, users)

    Resource.eval(coreF)
  }
}

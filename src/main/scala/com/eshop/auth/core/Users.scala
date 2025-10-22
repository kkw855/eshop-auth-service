package com.eshop.auth.core

import cats.implicits.*
import cats.effect.{Async, MonadCancelThrow}

import org.typelevel.log4cats.Logger

import io.lettuce.core.api.sync.RedisCommands
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Filters

import scala.reflect.ClassTag

import com.eshop.auth.modules.MailSender
import com.eshop.auth.domain.user

trait Users[F[_]] {
  def find(email: String): F[Option[User]]
}

final class LiveUsers[F[_]: {Async, Logger}] private (
    database: MongoDatabase
) extends Users[F] {
  override def find(email: String): F[Option[User]] = Async[F].delay {
//    val collection = database.getCollection("users")
//    val doc = collection.find(Filters.eq("email", email)).first()
//    val docs = collection.find()
//    println(s"find $doc $docs")
//    docs.forEach(d => {
//      println("docs")
//      println(d.getString("email"))
//    })
//    val user = Option(doc)
//      .map(d => User(d.getObjectId("_id"), d.getString("email")))
//
//    println(s"find $email $user")
//    user

    val userClassTag = implicitly[ClassTag[User]]

    val collection = database.getCollection("users", classOf[User])
    println(s"collection: $collection")
    val user = collection.find(Filters.eq("email", email)).first()
    val u    = new User()

    // println(s"find $email ${user.id} ${user.email}")
    Option(user)
  }
}

object LiveUsers {
  def apply[F[_]: {Async, Logger}](
      database: MongoDatabase
  ): F[LiveUsers[F]] =
    new LiveUsers[F](database).pure[F]
}

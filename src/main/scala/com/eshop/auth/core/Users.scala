package com.eshop.auth.core

import cats.implicits.*
import cats.effect.Async

import org.typelevel.log4cats.Logger

import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Filters

import scala.reflect.ClassTag

import java.util.Date

import com.eshop.auth.domain.user.*

trait Users[F[_]] {
  def find(email: String): F[Option[User]]
  def create(newUserInfo: User): F[String]
}

final class LiveUsers[F[_]: {Async, Logger}] private (
    database: MongoDatabase
) extends Users[F] {
  override def find(email: String): F[Option[User]] = Async[F].delay {
    val userClassTag = implicitly[ClassTag[User]]

    val collection = database.getCollection("users", classOf[UserDTO])

    val user = collection.find(Filters.eq("email", email)).first()

    Option(user).map(_.toUser)
  }

  override def create(user: User): F[String] = Async[F].delay {
    val collection = database.getCollection("users", classOf[UserDTO])

    val userDto = user.toUserDTO
    val now = new Date
    userDto.createdAt = now
    userDto.updatedAt = now

    // TODO: 저장 실패 예외 처리
    val result = collection.insertOne(userDto)

    user.email
  }
}

object LiveUsers {
  def apply[F[_]: {Async, Logger}](
      database: MongoDatabase
  ): F[LiveUsers[F]] =
    new LiveUsers[F](database).pure[F]
}

package com.eshop.auth.modules

import cats.implicits.*
import cats.effect.{Async, Resource}

import org.typelevel.log4cats.Logger

import io.lettuce.core.{RedisURI, RedisClient}
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.api.async.RedisAsyncCommands

import scala.jdk.FutureConverters.*

import com.eshop.auth.config.RedisConfig

// how to create class using redis lettuce in scala with cats effect io without redis4cat and lettucef

private trait RedisDeserializer[T] {
  def deserialize(value: String): T
}

object RedisDeserializer {
  given stringDeserializer: RedisDeserializer[String] = (value: String) => value
  given intDeserializer: RedisDeserializer[Int] = (value: String) => value.toInt
}

trait Redis[F[_]] {
  def set(key: String, value: String): F[Unit]
  def setex(key: String, seconds: Long, value: String): F[Unit]
  
  def get[T](key: String)(using deserializer: RedisDeserializer[T]): F[Option[T]]
  
  def del(keys: String*): F[Unit]
}

// TODO: Exception
final class LiveRedis[F[_]: {Logger, Async}] private (
    val client: RedisClient,
    val connection: StatefulRedisConnection[String, String]
) extends Redis[F] {
  private val asyncCommands: RedisAsyncCommands[String, String] = connection.async()

  // TODO: 1. Redis Commands 함수로 추가할지 2. ADT 로 추가할지 ??
  override def set(key: String, value: String): F[Unit] =
    // RedisFuture(Java Future) => Scala Future => IO
    Async[F].fromFuture(Async[F].delay(asyncCommands.set(key, value).asScala)).void

  override def setex(key: String, seconds: Long, value: String): F[Unit] =
    Async[F].fromFuture(Async[F].delay(asyncCommands.setex(key, seconds, value).asScala)).void

  override def get[T](key: String)(using deserializer: RedisDeserializer[T]): F[Option[T]] =
    Async[F]
      .fromFuture(Async[F].delay(asyncCommands.get(key).asScala))
      .map(
        Option(_).map(deserializer.deserialize)
      )

  override def del(keys: String*): F[Unit] =
    Async[F].fromFuture(Async[F].delay(asyncCommands.del(keys*).asScala)).void
}

object LiveRedis {
  def make[F[_]: {Logger, Async}](
      config: RedisConfig
  ): Resource[F, LiveRedis[F]] = {

    val acquire: F[LiveRedis[F]] = for {
      lettuceClient     <- Async[F].blocking {
        val redisUri: RedisURI = RedisURI.create(config.uri)
        // Disable sending CLIENT SETINFO lib-name and lib-version
        redisUri.setLibraryName(null)
        // Optionally, also disable setting the library version
        redisUri.setLibraryVersion(null)
        RedisClient.create(redisUri)
      }
      lettuceConnection <- Async[F].blocking(lettuceClient.connect())
      _                 <- Logger[F].info("Redis client connected.")
    } yield new LiveRedis(lettuceClient, lettuceConnection)

    val release: LiveRedis[F] => F[Unit] = client =>
      for {
        _ <- Logger[F].info("Closing Redis client...")
        _ <- Async[F].blocking(
          client.connection.closeAsync().get() // .get() blocks until close completes
        )
        _ <- Async[F].blocking(client.client.shutdown())
        _ <- Logger[F].info("Redis client closed.")
      } yield ()

    Resource.make(acquire)(release)
  }
}

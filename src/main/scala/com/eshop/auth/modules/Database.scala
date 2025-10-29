package com.eshop.auth.modules

import cats.effect.{IO, Resource, Sync}
import com.mongodb.{ConnectionString, MongoClientSettings}
import com.mongodb.client.{MongoClients, MongoDatabase}
import com.mongodb.MongoClientSettings.getDefaultCodecRegistry
import org.bson.codecs.pojo.{ClassModel, PojoCodecProvider}
import org.bson.codecs.configuration.CodecRegistries.{fromProviders, fromRegistries}
import com.eshop.auth.config.MongoConfig
//import com.eshop.auth.core.User
import com.eshop.auth.domain.user.*
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

object Database {
  given logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  def makeMongoResource[F[_]: Sync](conf: MongoConfig): Resource[F, MongoDatabase] =
    for {
      client <- Resource.make(
        Sync[F].delay {
          val settings = MongoClientSettings
            .builder()
            .applyConnectionString(ConnectionString(conf.uri))
            .build()
          
          MongoClients.create(settings)
        }
      )(client =>
        Sync[F].delay {
          client.close()
        }
      )
      database <- Resource.eval(Sync[F].delay {
        val pojoCodecProvider = PojoCodecProvider.builder.automatic(true).build
        val pojoCodecRegistry =
          fromRegistries(getDefaultCodecRegistry, fromProviders(pojoCodecProvider))

        client
          .getDatabase("development")
          .withCodecRegistry(pojoCodecRegistry)
      })
    } yield database
}

package com.eshop.auth.modules

import cats.effect.{IO, Resource, Sync}
import com.mongodb.{ConnectionString, MongoClientSettings}
import com.mongodb.client.{MongoClients, MongoDatabase}
import com.mongodb.MongoClientSettings.getDefaultCodecRegistry
import org.bson.codecs.pojo.{ClassModel, PojoCodecProvider}
import org.bson.codecs.configuration.CodecRegistries.{fromProviders, fromRegistries}
import com.eshop.auth.config.MongoConfig
import com.eshop.auth.core.User
import com.eshop.auth.core.User
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

object Database {
  given logger: Logger[IO] = Slf4jLogger.getLogger[IO]
  
  def makeMongoResource[F[_]: Sync](conf: MongoConfig): Resource[F, MongoDatabase] =
    for {
      client <- Resource.make(
        Sync[F].delay {
          // val url = s"mongodb://${conf.user}:${conf.pass}@${conf.url}"
          //      val credential =
          //        MongoCredential.createCredential(conf.user, conf.database, conf.pass.toCharArray)

          val settings = MongoClientSettings
            .builder()
            .applyConnectionString(ConnectionString(conf.uri))
            //        .credential(credential)
            //        .applyConnectionString(ConnectionString(s"mongodb://${conf.user}:${conf.pass}@localhost:27017/eshop?authSource=admin"))
            //        .applyToClusterSettings { builder =>
            //          builder.hosts(List(ServerAddress("localhost", 27017)).asJava)
            //        }
            .build()

          println("makeMongoResource")
          MongoClients.create(settings)
        }
      )(client =>
        Sync[F].delay {
          println("closeMongoResource")
          client.close()
        }
      )
      database <- Resource.eval(Sync[F].delay {
        // val classModel = ClassModel.builder(classOf[User2]).build
        val pojoCodecProvider = PojoCodecProvider.builder.automatic(true).build
//         val pojoCodecProvider = PojoCodecProvider.builder.register("com.eshop.auth.core.User2").build
        val pojoCodecRegistry =
          fromRegistries(getDefaultCodecRegistry, fromProviders(pojoCodecProvider))

        client
          .getDatabase("development")
          .withCodecRegistry(pojoCodecRegistry)
      })
    } yield database
}

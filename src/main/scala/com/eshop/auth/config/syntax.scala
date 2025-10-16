package com.eshop.auth.config

import cats.implicits.*
import cats.MonadThrow

import pureconfig.error.ConfigReaderException
import pureconfig.{ConfigReader, ConfigSource}

import scala.reflect.ClassTag

object syntax {
  extension (source: ConfigSource)
    def loadF[F[_], A](using
        reader: ConfigReader[A],
        F: MonadThrow[F],
        tag: ClassTag[A]
    ): F[A] =
      F.pure(source.load[A]).flatMap {
        case Left(errors)  => F.raiseError[A](ConfigReaderException(errors))
        case Right(config) => F.pure(config)
      }
}

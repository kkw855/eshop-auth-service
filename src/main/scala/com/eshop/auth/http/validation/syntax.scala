package com.eshop.auth.http.validation

import cats.implicits.*
import cats.MonadThrow
import cats.data.Validated.{Invalid, Valid}

import org.typelevel.log4cats.Logger

import io.circe.generic.auto.*
import org.http4s.circe.CirceEntityCodec.*

import org.http4s.dsl.Http4sDsl
import org.http4s.{EntityDecoder, Request, Response}

import com.eshop.auth.http.validation.validators.{ValidationResult, Validator}
import com.eshop.auth.http.responses.FailureResponse

object syntax {
  private def validateEntity[A](entity: A)(using validator: Validator[A]): ValidationResult[A] =
    validator.validate(entity)

  trait HttpValidationDsl[F[_]: {MonadThrow, Logger}] extends Http4sDsl[F] {

    extension(req: Request[F]) {
      def validate[A: Validator](serverLogicIfValid: A => F[Response[F]])(
          using EntityDecoder[F, A]
      ): F[Response[F]] =
        req
          .as[A]
          // TODO: logError
          .map(validateEntity) // F[ValidationResult[A]]
          .flatMap {
            case Valid(entity) =>
              serverLogicIfValid(entity) // F[Response[F]]
            case Invalid(errors) =>
              BadRequest(FailureResponse(errors.toList.map(_.errorMessage).mkString(", ")))
          }
    }
  }
}

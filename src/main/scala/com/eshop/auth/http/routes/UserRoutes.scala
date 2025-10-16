package com.eshop.auth.http.routes

import cats.implicits.*
import cats.effect.Async
import cats.data.Validated.{Invalid, Valid}

import org.typelevel.log4cats.Logger

import org.http4s.dsl.Http4sDsl
import org.http4s.HttpRoutes
import org.http4s.server.Router

import io.circe.generic.auto.*
import org.http4s.circe.CirceEntityCodec.*

import com.eshop.auth.modules.MailSender
import com.eshop.auth.core.Users
import com.eshop.auth.http.validation.validators.*

import com.eshop.auth.domain.user.*
import com.eshop.auth.http.responses.{SuccessResponse, FailureResponse}

final class UserRoutes[F[_]: {Async, Logger}] private (users: Users[F], mailSender: MailSender[F])
    extends Http4sDsl[F] {
  private def validateEntity[A](entity: A)(using validator: Validator[A]): ValidationResult[A] =
    validator.validate(entity)

  // TODO: extension for validation
  private val createUserRoute: HttpRoutes[F] = HttpRoutes.of[F] { case req @ POST -> Root =>
    for {
      resp <- req
        .as[NewUserInfo]
        .map(validateEntity)
        .flatMap {
          case Valid(entity) =>
            for {
              maybeUser <- users.find(entity.email)
              resp <- maybeUser match {
                case Some(user) =>
                  BadRequest(FailureResponse("User already exists with this email!"))
                case None => for {
                  // TODO: OPT CODE 만들기
                  _ <- mailSender.sendMail(
                    entity.email,
                    "Verify Your Email",
                    "user-activation-mail.html",
                    Map(s"$${name}" -> entity.name, s"$${otp}" -> "4125")
                  )
                  resp <- Created(SuccessResponse("OTP sent to email. Please verify your account."))
                } yield resp
              }
            } yield resp
          case Invalid(errors) =>
            BadRequest(FailureResponse(errors.toList.map(_.errorMessage).mkString(", ")))
        }
    } yield resp
  }

  val routes: HttpRoutes[F] = Router(
    "/users" -> createUserRoute
  )
}

object UserRoutes {
  def apply[F[_]: {Async, Logger}](users: Users[F], mailSender: MailSender[F]): UserRoutes[F] =
    new UserRoutes[F](users, mailSender)
}

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
import com.eshop.auth.http.validation.syntax.HttpValidationDsl
import com.eshop.auth.http.validation.validators.*

import com.eshop.auth.domain.user.*
import com.eshop.auth.http.responses.{SuccessResponse, FailureResponse}

final class UserRoutes[F[_]: {Async, Logger}] private (users: Users[F], mailSender: MailSender[F])
    extends HttpValidationDsl[F] {

  // TODO: extension for validation
  private val createUserRoute: HttpRoutes[F] = HttpRoutes.of[F] { case req @ POST -> Root =>
    req.validate[NewUserInfo] { newUserInfo =>
      for {
        maybeUser <- users.find(newUserInfo.email)
        resp <- maybeUser match {
          case Some(user) =>
            BadRequest(FailureResponse("User already exists with this email!"))
          case None => 
            // TODO: OPT CODE 만들기
            mailSender.sendMail(
              newUserInfo.email,
              "Verify Your Email",
              "user-activation-mail.html",
              Map(s"$${name}" -> newUserInfo.name, s"$${otp}" -> "4125")
            ) >> Created(
              SuccessResponse("OTP sent to email. Please verify your account.")
            )
        }
      } yield resp
    }
  }

  val routes: HttpRoutes[F] = Router(
    "/users" -> createUserRoute
  )
}

object UserRoutes {
  def apply[F[_]: {Async, Logger}](users: Users[F], mailSender: MailSender[F]): UserRoutes[F] =
    new UserRoutes[F](users, mailSender)
}

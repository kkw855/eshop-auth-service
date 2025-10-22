package com.eshop.auth.http.routes

import cats.implicits.*
import cats.data.EitherT
import cats.effect.Async

import org.typelevel.log4cats.Logger

import org.http4s.HttpRoutes
import org.http4s.server.Router

import io.circe.generic.auto.*
import org.http4s.circe.CirceEntityCodec.*

import scala.concurrent.duration.*

import com.eshop.auth.modules.{MailSender, LiveOTPGenerator, Redis}
import com.eshop.auth.core.Users
import com.eshop.auth.http.validation.syntax.HttpValidationDsl

import com.eshop.auth.domain.user.*
import com.eshop.auth.http.responses.{SuccessResponse, FailureResponse}

final class UserRoutes[F[_]: {Async, Logger}] private (
    mailSender: MailSender[F],
    redis: Redis[F],
    users: Users[F]
) extends HttpValidationDsl[F] {

  private val otpGenerator = LiveOTPGenerator[F]

  private def checkOtpRestrictions(email: String): F[Either[String, String]] =
    (
      redis.get[String](s"otp_lock:$email"),
      redis.get[String](s"otp_spam_lock:$email"),
      redis.get[String](s"otp_cooldown:$email")
    ).mapN {
      // 3번 잘못 입력하면 새로운 OTP 요청 거부 (30분 동안)
      case (Some(lock), _, _) =>
        Left("Account locked due to multiple failed attempts! Try again after 30 minutes")
      // 3번 넘게 OTP 요청하면 요청 거부 (1시간 동안)
      case (_, Some(spam), _) =>
        Left("Too many OTP requests, Please wait 1hour before sending requesting again.")
      // 1분 이내의 OTP 재발급 요청 거부
      case (_, _, Some(cooldown)) => Left("Please wait 1 minute before requesting a new OTP!")
      case _                      => Right("No restrictions")
    }

  private def trackOtpRequests(email: String): F[Either[String, String]] =
    redis
      .get[Int](s"otp_request_count:$email")
      .map(_.getOrElse(0))
      .map { requestCount =>
        Either.cond(
          requestCount < 3,
          (requestCount + 1).toString,
          "locked"
        )
      }
      .flatTap {
        case Left(locked) => redis.setex(s"otp_spam_lock:$email", 1.hour.toSeconds, locked)
        case Right(count) => redis.setex(s"otp_request_count:$email", 1.hour.toSeconds, count)
      }

  private def sendOtp(email: String, name: String): F[Either[String, String]] =
    for {
      otp <- otpGenerator.generate(4)
      send <- mailSender
        .sendMail(
          email,
          "Verify Your Email",
          "user-activation-mail.html",
          Map(
            s"$${name}" -> name,
            s"$${otp}"  -> otp
          )
        )
      result <- send match {
        case Left(error) =>
          Logger[F].error(s"Error sending email: $error") >>
            Async[F].delay(Left(s"Error sending email: $error"))
        case Right(_) =>
          redis.setex(s"otp:$email", 5.minutes.toSeconds, otp) >>
            // OTP 코드 발송하고 1분 이후에 재발송 가능
            redis.setex(s"otp_cooldown:$email", 1.minutes.toSeconds, "true") >>
            Async[F].delay(Right(otp))
      }
    } yield result

  // TODO: 모든 예외 테스트
  // 이메일 connect, sending, invalid email(not exist)
  private val createUserRoute: HttpRoutes[F] = HttpRoutes.of[F] { case req @ POST -> Root =>
    req.validate[NewUserInfo] { case NewUserInfo(name, email, password) =>
      users.find(email).flatMap {
        case Some(_) =>
          BadRequest(FailureResponse("User already exists with this email!"))
        case None =>
          (for {
            restrictions <- EitherT(checkOtpRestrictions(email))
            locked       <- EitherT(trackOtpRequests(email))
            send         <- EitherT(sendOtp(email, name))
          } yield send).value.flatMap {
            case Left(message) => BadRequest(FailureResponse(message))
            case Right(res) =>
              Created(SuccessResponse("OTP sent to email. Please verify your account."))
          }
      }
    }
  }
  
  val routes: HttpRoutes[F] = Router(
    "/users" -> createUserRoute
  )
}

object UserRoutes {
  def apply[F[_]: {Async, Logger}](
      mailSender: MailSender[F],
      redis: Redis[F],
      users: Users[F]
  ): UserRoutes[F] =
    new UserRoutes[F](mailSender, redis, users)
}

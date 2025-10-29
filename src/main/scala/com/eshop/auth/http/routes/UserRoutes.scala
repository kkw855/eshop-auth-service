package com.eshop.auth.http.routes

import cats.implicits.*
import cats.data.{OptionT, EitherT}
import cats.effect.Async

import org.typelevel.log4cats.Logger

import org.http4s.HttpRoutes
import org.http4s.server.Router
import org.http4s.ResponseCookie

import io.circe.generic.auto.*
import io.circe.syntax._ // For the .asJson extension method
import org.http4s.circe.CirceEntityCodec.*

import pdi.jwt.{JwtClaim, JwtAlgorithm, JwtCirce}

import java.time.Instant

import scala.concurrent.duration.*

import com.eshop.auth.modules.{MailSender, LiveOTPGenerator, Redis, Hashing}
import com.eshop.auth.core.Users
import com.eshop.auth.http.validation.syntax.HttpValidationDsl

import com.eshop.auth.http.responses.{SuccessResponse, FailureResponse}
import com.eshop.auth.domain.user.*
import com.eshop.auth.domain.token.*

final class UserRoutes[F[_]: {Async, Logger}] private (
    hashing: Hashing[F],
    mailSender: MailSender[F],
    redis: Redis[F],
    users: Users[F]
) extends HttpValidationDsl[F] {

  // Auth
  val claim = JwtClaim(
    content = """{"user":"John", "level":"basic"}""",
    expiration = Some(Instant.now.plusSeconds(157784760).getEpochSecond),
    issuedAt = Some(Instant.now.getEpochSecond)
  )

  val key = "secretKey"

  val algo = JwtAlgorithm.HS256

  val token = JwtCirce.encode(claim, key, algo)

  private val otpGenerator = LiveOTPGenerator[F]

  private def checkOtpRestrictions(email: String): F[Either[String, String]] =
    Logger[F].info("checkOtpRestrictions") >>
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
    Logger[F].info("trackOtpRequests") >>
      redis
        .get[Int](s"otp_request_count:$email")
        .map(_.getOrElse(0))
        .map { requestCount =>
          Either.cond(
            requestCount < 3,
            (requestCount + 1).toString,
            "Too many OTP requests. Please wait 1 hour before requesting again."
          )
        }
        .flatTap {
          case Left(locked) =>
            Logger[F].info(s"Left locked") >> redis.setex(
              s"otp_spam_lock:$email",
              1.hour.toSeconds,
              "locked"
            )
          case Right(count) =>
            Logger[F].info(s"Right $count") >> redis.setex(
              s"otp_request_count:$email",
              1.hour.toSeconds,
              count
            )
        }

  private def sendOtp(email: String, name: String): F[Either[String, String]] =
    for {
      otp <- otpGenerator.generate(4)
      _   <- Logger[F].info("sendOtp")
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
              Created(SuccessResponse("OTP sent to email. Please verify your account.")) // 201
          }
      }
    }
  }

  // TODO: Resolve nested
  private val verifyUserRoute: HttpRoutes[F] = HttpRoutes.of[F] {
    case req @ POST -> Root / "verify" =>
      req.validate[VerifyUserInfo] { case VerifyUserInfo(name, email, password, otp) =>
        users.find(email).flatMap {
          case Some(user) => BadRequest(FailureResponse("User already exists with this email!"))
          case None =>
            (
              redis.get[String](s"otp:$email"),
              redis.get[Int](s"otp_attempts:$email").map(_.getOrElse(0))
            ).flatMapN {
              case (None, _) => BadRequest(FailureResponse("Invalid or expired OTP!"))
              case (Some(storedOtp), failedAttempts) if storedOtp != otp =>
                if (failedAttempts >= 2) {
                  redis.setex(s"otp_lock:$email", 30.minutes.toSeconds, "locked") >> redis.del(
                    s"otp:$email"
                  ) >> BadRequest(
                    FailureResponse(
                      "Too many failed attempts. Your account is locked for 30 minutes!"
                    )
                  )
                } else
                  redis.setex(
                    s"otp_attempts:$email",
                    5.minutes.toSeconds,
                    (failedAttempts + 1).toString
                  ) >> BadRequest(
                    FailureResponse(s"Incorrect OTP. ${2 - failedAttempts} attempts left.")
                  )
              case _ =>
                Logger[F].info("Correct OTP") >>
                  redis.del(s"otp:$email", s"otp_attempts:$email")
                  >> hashing.hash(password).flatMap { hashedPassword =>
                    Logger[F].info("Create User") >>
                      users.create(User(name, email, hashedPassword, None, None))
                  } >> Created(SuccessResponse("User registered successfully!"))
            }
        }
      }
  }

  private val loginUserRoute: HttpRoutes[F] = HttpRoutes.of[F] {
    case req @ POST -> Root / "login" =>
      req.validate[LoginUserInfo] { case LoginUserInfo(email, password) =>
        users.find(email).flatMap {
          case None => BadRequest(FailureResponse("User doesn't exist!"))
          case Some(user) =>
            hashing.verify(password, user.hashedPassword).flatMap {
              case true =>
                val accessToken = JwtCirce.encode(
                  JwtClaim(
                    content = AccessToken(user.name, Role.USER).asJson.noSpaces,
                    expiration = Some(Instant.now.plusSeconds(15.minutes.toSeconds).getEpochSecond)
                  ),
                  key,
                  algo
                )
                val refreshToken = JwtCirce.encode(
                  JwtClaim(
                    content = AccessToken(user.name, Role.USER).asJson.noSpaces,
                    expiration = Some(Instant.now.plusSeconds(15.days.toSeconds).getEpochSecond)
                  ),
                  key,
                  algo
                )
                Ok(SuccessResponse("Login successful!"))
                  .map(
                    _.addCookie(
                      ResponseCookie(
                        "access_token",
                        accessToken,
                        httpOnly = true,
                        secure = true,
                        sameSite = None,
                        maxAge = Some(7.days.toMillis)
                      )
                    )
                  )
                  .map(
                    _.addCookie(
                      ResponseCookie(
                        "refresh_token",
                        refreshToken,
                        httpOnly = true,
                        secure = true,
                        sameSite = None,
                        maxAge = Some(7.days.toMillis)
                      )
                    )
                  )
              case false => BadRequest(FailureResponse("Invalid email or password"))
            }
        }
      }
  }

  val routes: HttpRoutes[F] = Router(
    "/users" -> (createUserRoute <+> verifyUserRoute <+> loginUserRoute)
  )
}

object UserRoutes {
  def apply[F[_]: {Async, Logger}](
      hashing: Hashing[F],
      mailSender: MailSender[F],
      redis: Redis[F],
      users: Users[F]
  ): UserRoutes[F] =
    new UserRoutes[F](hashing, mailSender, redis, users)
}

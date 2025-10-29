package com.eshop.auth.modules

import cats.implicits.*
import cats.effect.{Async, Resource}

import org.typelevel.log4cats.Logger

import org.http4s.HttpRoutes
import org.http4s.server.Router

import com.eshop.auth.http.routes.{HealthRoutes, UserRoutes}

final class HttpApi[F[_]: {Async, Logger}] private (core: Core[F]) {
  private val healthRoutes = HealthRoutes[F].routes
  private val userRoutes = UserRoutes[F](core.hashing, core.mailSender, core.redisClient, core.users).routes

  val endPoints: HttpRoutes[F] = Router(
    "/api" -> (healthRoutes <+> userRoutes)
  )
}

object HttpApi {
  def apply[F[_]: {Async, Logger}](core: Core[F]): Resource[F, HttpApi[F]] =
    Resource.pure(new HttpApi(core))
}

package com.eshop.auth.http.routes

import cats.Monad

import org.http4s.dsl.*
import org.http4s.HttpRoutes
import org.http4s.server.Router

final class HealthRoutes[F[_]: Monad] private extends Http4sDsl[F] {
  private val healthRoute: HttpRoutes[F] = HttpRoutes.of[F] { case GET -> Root =>
    Ok("All going great!")
  }

  val routes: HttpRoutes[F] = Router(
    "/health" -> healthRoute
  )
}

object HealthRoutes {
  def apply[F[_]: Monad]: HealthRoutes[F] = new HealthRoutes[F]
}

package com.eshop.auth.modules

import cats.effect.Sync

import java.security.SecureRandom

trait OTPGenerator[F[_]] {
  def generate(length: Int): F[String]
}

class LiveOTPGenerator[F[_]: Sync] extends OTPGenerator[F] {
  private val secureRandom = new SecureRandom()

  override def generate(length: Int): F[String] = Sync[F].delay {
    val random = math.abs(secureRandom.nextInt) + math.pow(10, length).toInt

    random.toString.take(length)
  }
}

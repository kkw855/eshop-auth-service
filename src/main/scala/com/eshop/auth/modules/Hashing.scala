package com.eshop.auth.modules

import cats.implicits.*
import cats.effect.{Resource, Sync}

import org.bouncycastle.crypto.generators.Argon2BytesGenerator
import org.bouncycastle.crypto.params.Argon2Parameters
import org.bouncycastle.util.encoders.Hex

import java.nio.charset.StandardCharsets
import java.security.SecureRandom

trait Hashing[F[_]] {
  def hash(value: String): F[String]
  def verify(value: String, hashed: String): F[Boolean]
}

final class LiveHashing[F[_]: Sync] private extends Hashing[F] {
  private val MEMORY = 15 * 1024 // 15 MiB
  private val ITERATIONS = 2 // 2 iterations
  private val PARALLELISM = 1 // 1 thread
  private val HASH_LENGTH = 64 // 64 bytes for a strong hash

  override def hash(value: String): F[String] = Sync[F].delay {
    // Generate a random salt for each password
    val random: SecureRandom = SecureRandom()
    val salt = new Array[Byte](16) // 16 bytes is a good salt size
    random.nextBytes(salt)

    // TODO: 중복 제거
    // Build the Argon2 parameters
    val params: Argon2Parameters = Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
      .withVersion(Argon2Parameters.ARGON2_VERSION_13)
      .withSalt(salt)
      .withMemoryAsKB(MEMORY)
      .withIterations(ITERATIONS)
      .withParallelism(PARALLELISM)
      .build()

    // Create the generator and initialize it
    val generator: Argon2BytesGenerator = Argon2BytesGenerator()
    generator.init(params)

    // Generate the hash
    val hash = new Array[Byte](HASH_LENGTH)
    generator.generateBytes(value.getBytes(StandardCharsets.UTF_8), hash, 0, hash.length)

    // TODO: Best practice argon2id 해싱 값을 DB 에 저장 검색
    // Return the salt and hash, typically encoded in Hex or Base64
    Hex.toHexString(salt) + ":" + Hex.toHexString(hash)
  }

  override def verify(value: String, hashed: String): F[Boolean] = Sync[F].delay {
    // Split the stored string into its components: salt and hash
    val parts = hashed.split(":")
    val salt = Hex.decode(parts(0))
    val storedHash = Hex.decode(parts(1))

    // TODO: 중복 제거
    // Build the Argon2 parameters using the stored salt
    val params: Argon2Parameters = Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
      .withVersion(Argon2Parameters.ARGON2_VERSION_13)
      .withSalt(salt)
      .withMemoryAsKB(MEMORY)
      .withIterations(ITERATIONS)
      .withParallelism(PARALLELISM)
      .build()

    // Create the generator and initialize it
    val generator: Argon2BytesGenerator = Argon2BytesGenerator()
    generator.init(params)

    // Generate a new hash from the user-provided password
    val newHash = new Array[Byte](HASH_LENGTH)
    generator.generateBytes(value.getBytes(StandardCharsets.UTF_8), newHash, 0, newHash.length)

    // Compare the newly generated hash with the stored hash
    newHash.sameElements(storedHash)
  }
}

object LiveHashing {
  def apply[F[_]: Sync]: Resource[F, LiveHashing[F]] =
    Resource.eval(new LiveHashing[F].pure[F])
}

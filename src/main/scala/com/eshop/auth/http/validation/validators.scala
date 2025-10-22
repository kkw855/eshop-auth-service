package com.eshop.auth.http.validation

import cats.implicits.*
import cats.data.ValidatedNec

import com.eshop.auth.domain.user.*

object validators {

  sealed trait ValidationFailure(val errorMessage: String)
  private final case class EmptyField(fieldName: String)
      extends ValidationFailure(s"'$fieldName' is empty")
  private final case class InvalidEmail(fieldName: String)
      extends ValidationFailure(s"'$fieldName' is not a valid email")
  private final case class InvalidOtp(fieldName: String)
    extends ValidationFailure(s"'$fieldName' is not a valid OTP")

  type ValidationResult[A] = ValidatedNec[ValidationFailure, A]

  trait Validator[A] {
    def validate(value: A): ValidationResult[A]
  }

  private val emailRegex =
    """^[a-zA-Z0-9.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$""".r

  private def validateRequired[A](field: A, fieldName: String)(
      required: A => Boolean
  ): ValidationResult[A] =
    if (required(field)) field.validNec
    else EmptyField(fieldName).invalidNec

  private def validateEmail(field: String, fieldName: String): ValidationResult[String] =
    if (emailRegex.findFirstMatchIn(field).isDefined) field.validNec
    else InvalidEmail(fieldName).invalidNec

  given newUserInfoValidator: Validator[NewUserInfo] = newUserInfo => {
    // TODO: 중복 제거
    val validName = validateRequired(newUserInfo.name, "name")(_.nonEmpty)
    val validUserEmail = validateRequired(newUserInfo.email, "email")(_.nonEmpty).andThen(e =>
      validateEmail(e, "email")
    )
    val validPassword = validateRequired(newUserInfo.password, "password")(_.nonEmpty)

    (
      validName,
      validUserEmail,
      validPassword
    ).mapN(
      NewUserInfo.apply
    )
  }
}

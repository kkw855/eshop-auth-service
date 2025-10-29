package com.eshop.auth.domain

import org.bson.types.ObjectId

// Java 에서만 getter setter 호출 가능 Scala 에서는 호출 불가능
import scala.beans.BeanProperty

import java.util.Date

object user {

  final case class UserDTO(
      @BeanProperty var id: ObjectId,
      @BeanProperty var name: String,
      @BeanProperty var email: String,
      @BeanProperty var hashedPassword: String,
      @BeanProperty var createdAt: Date,
      @BeanProperty var updatedAt: Date
  ) {
    def this() = this(ObjectId(), "", "", "", null, null)

    def toUser: User =
      User(
        this.name,
        this.email,
        this.hashedPassword,
        Option(this.createdAt),
        Option(this.updatedAt)
      )
  }

  final case class User(
      name: String,
      email: String,
      hashedPassword: String,
      createdAt: Option[Date],
      updatedAt: Option[Date]
  ) {
    def toUserDTO: UserDTO =
      UserDTO(ObjectId(), name, email, hashedPassword, createdAt.orNull, updatedAt.orNull)
  }

  final case class LoginUserInfo(email: String, password: String)

  // TODO: opaque type EMail
  final case class NewUserInfo(name: String, email: String, password: String)
  final case class VerifyUserInfo(name: String, email: String, password: String, otp: String)
}

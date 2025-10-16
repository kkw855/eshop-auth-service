package com.eshop.auth.domain

import org.bson.types.ObjectId

object user {
  // case class User(_id: ObjectId, email: String)
//  object User {
//    def apply(_id: ObjectId, email: String): User = new User(_id, email)
//  }
  // TODO: opaque type EMail
  final case class NewUserInfo(name: String, email: String, password: String)
}

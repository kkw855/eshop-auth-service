package com.eshop.auth.domain

object token {
  enum Role {
    case USER, RECRUITER
  }
  
  final case class AccessToken(name: String, role: Role)
}

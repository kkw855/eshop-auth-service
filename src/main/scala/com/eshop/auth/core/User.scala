package com.eshop.auth.core

import org.bson.types.ObjectId

class User {
  var id: ObjectId = new ObjectId()
  var email: String = ""

  def setId(id: ObjectId): Unit = this.id = id
  def setEmail(email: String): Unit = this.email = email
}

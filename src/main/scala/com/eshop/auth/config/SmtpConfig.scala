package com.eshop.auth.config

import pureconfig.ConfigReader

import com.comcast.ip4s.Port

import java.util.Properties

import com.eshop.auth.config

final case class SmtpConfig(
    host: String,
    port: Port,
    user: String,
    password: String
) derives ConfigReader {
  def toProperties: Properties = {
    val props = new Properties()
    
    props.put("mail.smtp.host", host)
    props.put("mail.smtp.port", port)
    props.put("mail.smtp.auth", "true")
    props.put("mail.smtp.ssl.enable", "true")
    props.put("mail.smtp.ssl.trust", host)

    props
  }
}

object SmtpConfig extends givens 
package com.eshop.auth.modules

import cats.implicits.*
import cats.effect.{Resource, Sync}

import org.typelevel.log4cats.Logger

import scala.util.Try
import scala.io.Source
import scala.util.matching.Regex

import javax.mail.{Authenticator, Message, PasswordAuthentication, Session, Transport}
import javax.mail.internet.{InternetAddress, MimeMessage}

import com.eshop.auth.config.SmtpConfig

// how to create email sender class using javamail in scala with cats effect io

trait MailSender[F[_]] {
  def sendMail(to: String, subject: String, templateFile: String, replacements: Map[String, String]): F[Either[Throwable, String]]
}

object MailResource {
  def make[F[_]: Sync](config: SmtpConfig): Resource[F, (Session, Transport)] = Resource.make {
    Sync[F].blocking {
      val session = Session.getInstance(config.toProperties, new Authenticator() {
        override def getPasswordAuthentication: PasswordAuthentication = {
          PasswordAuthentication(config.user, config.password)
        }
      })
      // TODO: 개발 환경일 때만
      session.setDebug(true)

      val transport = session.getTransport("smtp")
      transport.connect()
      
      (session, transport)
    }
  } { (session, transport) =>
    Sync[F].blocking(transport.close())
  }
}

final class LiveMailSender[F[_]: {Sync, Logger}] private (config: SmtpConfig) extends MailSender[F] {
  private val mailResource: Resource[F, (Session, Transport)] = MailResource.make[F](config)

  // TODO: Either 예외 처리
  override def sendMail(to: String, subject: String, templateFile: String, replacements: Map[String, String]): F[Either[Throwable, String]] =
    mailResource.use { (session, transport) =>
      Sync[F].blocking {
        Try {
          // TODO: Resource 사용
          val bufferedSource = Source.fromResource(templateFile)
          val template: String = bufferedSource.getLines().mkString("")

          val pattern: Regex = replacements.keys.map(Regex.quote).mkString("|").r
          val html = pattern.replaceAllIn(template, m => replacements(m.matched))

          bufferedSource.close()

          val message = MimeMessage(session)
          message.setFrom(InternetAddress(to))
          message.setRecipients(Message.RecipientType.TO, to)
          message.setSubject(subject)
          message.setContent(html, "text/html")

          transport.sendMessage(message, message.getAllRecipients)
          
          "success"
        }.toEither
      }
    }
}

object LiveMailSender {
  def apply[F[_]: {Sync, Logger}](config: SmtpConfig): Resource[F, LiveMailSender[F]] =
    Resource.eval(new LiveMailSender[F](config).pure[F])
}

package com.eshop.auth.modules

import cats.implicits.*
import cats.effect.{Resource, Sync}

import org.typelevel.log4cats.Logger

import scala.io.Source
import scala.util.matching.Regex

import javax.mail.{Authenticator, Message, PasswordAuthentication, Session, Transport}
import javax.mail.internet.{InternetAddress, MimeMessage}

import com.eshop.auth.config.SmtpConfig

trait MailSender[F[_]] {
  // TODO: Either
  def sendMail(to: String, subject: String, templateFile: String, replacements: Map[String, String]): F[Boolean]
}

object MailResource {
  def make[F[_]: Sync](config: SmtpConfig): Resource[F, (Session, Transport)] = Resource.make {
    Sync[F].blocking {
      println("makeMailResource")
      val session = Session.getInstance(config.toProperties, new Authenticator() {
        override def getPasswordAuthentication: PasswordAuthentication = {
          PasswordAuthentication("kkw855@gmail.com", "xkok oguj lfpv ffxb")
        }
      })
      // TODO: 개발 환경일 때만
      session.setDebug(true)

      val transport = session.getTransport("smtp")
      transport.connect()

      println("Session created...")
      (session, transport)
    }
  } { (session, transport) =>
    Sync[F].blocking(transport.close())
  }
}

final class LiveMailSender[F[_]: {Sync, Logger}] private (config: SmtpConfig) extends MailSender[F] {
  private val mailResource: Resource[F, (Session, Transport)] = MailResource.make[F](config)

  // TODO: Either 예외 처리
  override def sendMail(to: String, subject: String, templateFile: String, replacements: Map[String, String]): F[Boolean] =
    mailResource.use { (session, transport) =>
      Sync[F].blocking {
        println(s"Creating session: $to $subject $templateFile $replacements")
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
        println(s"Sending email: $to $subject $html")
        transport.sendMessage(message, message.getAllRecipients)
        true
      }
    }
}

object LiveMailSender {
  def apply[F[_]: {Sync, Logger}](config: SmtpConfig): Resource[F, LiveMailSender[F]] =
    Resource.eval(new LiveMailSender[F](config).pure[F])
}

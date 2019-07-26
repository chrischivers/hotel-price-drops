package hotelpricedrops.notifier

import cats.effect.IO
import cats.syntax.flatMap._
import java.util.Properties

import hotelpricedrops.Model.Screenshot
import hotelpricedrops.pricefetchers.PriceFetcher.ErrorString
import io.chrisdavenport.log4cats.Logger
import javax.activation.DataHandler
import javax.mail.internet.{
  InternetAddress,
  MimeBodyPart,
  MimeMessage,
  MimeMultipart
}
import javax.mail.util.ByteArrayDataSource
import javax.mail.{Session, _}

object EmailNotifier {

  case class EmailerConfig(fromAddress: String,
                           toAddress: String,
                           smtpHost: String,
                           smtpPort: Int,
                           smtpUsername: String,
                           smtpPassword: String)

  def apply(emailerConfig: EmailerConfig)(implicit logger: Logger[IO]) = IO {
    new Notifier {

      val properties = new Properties()
      properties.put("mail.transport.protocol", "smtp")
      properties.put("mail.smtp.auth", "true")
      properties.put("mail.smtp.starttls.enable", "true")
      properties.put("mail.smtp.user", emailerConfig.smtpUsername)
      properties.put("mail.smtp.password", emailerConfig.smtpPassword)
      properties.put("mail.smtp.host", emailerConfig.smtpHost)
      properties.put("mail.smtp.port", emailerConfig.smtpPort.toString)

      val session: Session = Session.getInstance(
        properties,
        new Authenticator() {
          override protected def getPasswordAuthentication =
            new PasswordAuthentication(emailerConfig.smtpUsername,
                                       emailerConfig.smtpPassword)
        }
      )
      session.setDebug(false)

      private def notify(subject: String,
                         bodyStr: String,
                         screenshot: Screenshot) = {
        val mimeMessage = new MimeMessage(session)

        mimeMessage.setFrom(new InternetAddress(emailerConfig.fromAddress))
        mimeMessage.setRecipient(javax.mail.Message.RecipientType.TO,
                                 new InternetAddress(emailerConfig.toAddress))
        mimeMessage.setSubject(subject)

        val body = new MimeBodyPart()
        body.setText(bodyStr, "utf-8", "html")

        val attachment = new MimeBodyPart()
        val bds = new ByteArrayDataSource(screenshot.value, "image/png")
        attachment.setDataHandler(new DataHandler(bds))
        attachment.setFileName("screenshot.png")

        val multipart = new MimeMultipart()
        multipart.addBodyPart(body)
        multipart.addBodyPart(attachment)

        mimeMessage.setContent(multipart)

        logger.info(s"Sending email to ${emailerConfig.toAddress}") >>
          IO(Transport.send(mimeMessage))
      }

      override def priceNotify(message: String,
                               screenshot: Screenshot): IO[Unit] =
        notify("Hotel Price Drop Notification", message, screenshot)

      override def errorNotify(error: ErrorString,
                               screenshot: Screenshot): IO[Unit] =
        notify("Hotel Price Drop: Error", error, screenshot)
    }
  }
}

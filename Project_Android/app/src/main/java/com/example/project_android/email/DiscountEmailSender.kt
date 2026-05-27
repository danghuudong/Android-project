package com.example.project_android.email

import com.example.project_android.BuildConfig
import com.example.project_android.model.entity.DiscountCode
import com.example.project_android.utils.CurrencyUtils
import java.util.Properties
import javax.mail.Authenticator
import javax.mail.Message
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

object DiscountEmailSender {

    // Admin bấm gửi -> Gmail SMTP xác thực bằng App Password -> khách nhận mã ưu đãi.
    fun send(
        recipientEmail: String,
        customerName: String,
        totalSpent: Double,
        discount: DiscountCode
    ): Result<Unit> = runCatching {
        val username = BuildConfig.MAIL_USERNAME.trim()
        val appPassword = BuildConfig.MAIL_APP_PASSWORD.replace(" ", "").trim()

        require(username.isNotBlank()) { "Chưa cấu hình email gửi." }
        require(appPassword.isNotBlank()) {
            "Chưa cấu hình MAIL_APP_PASSWORD trong local.properties."
        }

        val properties = Properties().apply {
            put("mail.smtp.auth", "true")
            put("mail.smtp.starttls.enable", "true")
            put("mail.smtp.host", "smtp.gmail.com")
            put("mail.smtp.port", "587")
            put("mail.smtp.connectiontimeout", "10000")
            put("mail.smtp.timeout", "10000")
            put("mail.smtp.writetimeout", "10000")
        }

        val session = Session.getInstance(properties, object : Authenticator() {
            override fun getPasswordAuthentication(): PasswordAuthentication {
                return PasswordAuthentication(username, appPassword)
            }
        })

        val message = MimeMessage(session).apply {
            setFrom(InternetAddress(username, "Digital Atelier"))
            setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipientEmail))
            setSubject(
                "[Digital Atelier] Mã ưu đãi dành cho khách hàng thân thiết",
                Charsets.UTF_8.name()
            )
            setText(
                buildBody(customerName, totalSpent, discount),
                Charsets.UTF_8.name()
            )
        }

        Transport.send(message)
    }

    private fun buildBody(
        customerName: String,
        totalSpent: Double,
        discount: DiscountCode
    ): String {
        val benefit = if (discount.discountType == "percent") {
            "Giảm ${discount.discountValue.toInt()}%, tối đa ${CurrencyUtils.formatVnd(discount.maximumDiscount)}"
        } else {
            "Giảm tối đa ${CurrencyUtils.formatVnd(discount.discountValue)}"
        }

        return """
            Xin chào $customerName,

            Cảm ơn bạn đã đồng hành cùng Digital Atelier.
            Tổng chi tiêu đã thanh toán của bạn: ${CurrencyUtils.formatVnd(totalSpent)}.

            Mã giảm giá của bạn: ${discount.code}
            Ưu đãi: $benefit
            Đơn thuê tối thiểu: ${CurrencyUtils.formatVnd(discount.minimumOrder)}
            Hạn sử dụng: ${discount.endDate}

            Vui lòng nhập mã khi thanh toán đơn thuê tiếp theo.

            Trân trọng,
            Digital Atelier
        """.trimIndent()
    }
}

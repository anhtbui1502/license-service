package com.vht.license.otp;

import com.vht.license.config.AppProperties;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

@Component
public class SmtpOtpNotifier implements OtpNotifier {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");

    private final JavaMailSender mailSender;
    private final AppProperties props;

    public SmtpOtpNotifier(JavaMailSender mailSender, AppProperties props) {
        this.mailSender = mailSender;
        this.props = props;
    }

    @Override
    public void send(OtpEmail email) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(props.mail().from());
            helper.setTo(email.to());
            helper.setSubject(subject(email));
            helper.setText(body(email), true);
            mailSender.send(message);
        } catch (MessagingException e) {
            throw new IllegalStateException("Không tạo được email OTP", e);
        }
    }

    String subject(OtpEmail e) {
        return "[vOCS License] " + e.requestCode() + " – " + e.marketName() + " (" + e.marketCode() + ")"
                + " – MaxSub " + num(e.newMaxSub()) + " / MaxActive " + num(e.newMaxActiveSub())
                + (e.resend() ? " (gửi lại)" : "");
    }

    String body(OtpEmail e) {
        String expires = TIME.format(e.expiresAt().atZone(props.timezone()));
        String currentLabel = e.currentVersion() == null ? "Chưa có" : "Hiện tại (v" + e.currentVersion() + ")";
        StringBuilder sb = new StringBuilder();
        sb.append("<div style=\"font-family:Arial,sans-serif;font-size:14px;color:#222\">");
        sb.append("<h3 style=\"margin:0 0 12px\">Yêu cầu cấp license vOCS</h3>");
        if (e.resend()) {
            sb.append("<p style=\"color:#b45309\"><b>Đây là OTP gửi lại. Mã OTP gửi trước đó cho yêu cầu này đã hết hiệu lực.</b></p>");
        }
        sb.append("<table cellpadding=\"4\" style=\"border-collapse:collapse\">")
                .append(row("Thị trường", esc(e.marketName()) + " (" + esc(e.marketCode()) + ")"))
                .append(row("Mã yêu cầu", esc(e.requestCode())))
                .append(row("Người yêu cầu", esc(e.requestedBy())))
                .append(row("Lý do", esc(e.reason())))
                .append("</table><br>");
        sb.append("<table cellpadding=\"6\" border=\"1\" style=\"border-collapse:collapse;border-color:#ccc\">")
                .append("<tr style=\"background:#f3f4f6\"><th></th><th>").append(currentLabel)
                .append("</th><th>Đề nghị</th><th>Chênh lệch</th></tr>")
                .append(compare("Max Sub", e.currentMaxSub(), e.newMaxSub()))
                .append(compare("Max Active Sub", e.currentMaxActiveSub(), e.newMaxActiveSub()))
                .append("</table>");
        sb.append("<p style=\"margin-top:16px\">Mã OTP: <b style=\"font-size:22px;letter-spacing:4px\">")
                .append(e.otp()).append("</b></p>");
        sb.append("<p>Hiệu lực đến: <b>").append(expires).append("</b></p>");
        sb.append("<p style=\"color:#b91c1c\">Nếu không đồng ý với yêu cầu này, vui lòng <b>KHÔNG</b> cung cấp mã OTP.</p>");
        sb.append("</div>");
        return sb.toString();
    }

    private static String row(String label, String value) {
        return "<tr><td style=\"color:#555\">" + label + ":</td><td><b>" + value + "</b></td></tr>";
    }

    private static String compare(String label, Long current, long proposed) {
        String cur = current == null ? "—" : num(current);
        String diff;
        if (current == null) {
            diff = "—";
        } else {
            long d = proposed - current;
            String color = d < 0 ? "#b91c1c" : "#15803d";
            diff = "<span style=\"color:" + color + "\">" + (d > 0 ? "+" : "") + num(d) + "</span>";
        }
        return "<tr><td>" + label + "</td><td align=\"right\">" + cur + "</td><td align=\"right\"><b>" + num(proposed)
                + "</b></td><td align=\"right\">" + diff + "</td></tr>";
    }

    private static String num(long n) {
        return NumberFormat.getIntegerInstance(Locale.forLanguageTag("vi-VN")).format(n);
    }

    private static String esc(String s) {
        return s == null ? "" : HtmlUtils.htmlEscape(s);
    }
}

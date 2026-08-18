package com.alantek.caja.shared.email;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    @Autowired(required = false)
    private JavaMailSender javaMailSender;

    @Value("${app.mail.enabled:false}")
    private boolean mailEnabled;

    @Value("${app.mail.from:noreply@caja-ahorros.com}")
    private String mailFrom;

    public void enviarEmail(String to, String subject, String htmlBody) {
        if (!mailEnabled || javaMailSender == null) {
            log.info("Email deshabilitado. Se omitio envio a: {}", to);
            return;
        }
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(mailFrom);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            javaMailSender.send(message);
            log.info("Email enviado exitosamente a: {}", to);
        } catch (MessagingException e) {
            log.error("Error al enviar email a {}: {}", to, e.getMessage(), e);
        }
    }

    public void enviarNotificacion(String to, String nombreUsuario, String tipo, String mensaje) {
        if (!mailEnabled || javaMailSender == null) {
            return;
        }
        String subject = "Notificacion - Caja de Ahorros";
        String htmlBody = "<html><body>"
                + "<div style='font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;'>"
                + "<div style='background-color: #1a237e; color: white; padding: 20px; text-align: center;'>"
                + "<h1 style='margin: 0;'>Caja de Ahorros</h1>"
                + "</div>"
                + "<div style='padding: 20px; border: 1px solid #ddd;'>"
                + "<h2 style='color: #333;'>Hola " + nombreUsuario + "</h2>"
                + "<p><strong>Tipo:</strong> " + tipo + "</p>"
                + "<p><strong>Mensaje:</strong> " + mensaje + "</p>"
                + "<hr style='border: none; border-top: 1px solid #eee;'>"
                + "<p style='color: #999; font-size: 12px;'>Este es un correo automatico, por favor no responder.</p>"
                + "</div>"
                + "</div>"
                + "</body></html>";
        enviarEmail(to, subject, htmlBody);
    }
}

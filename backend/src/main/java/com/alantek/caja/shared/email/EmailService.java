package com.alantek.caja.shared.email;

import com.alantek.caja.modulo.email.entity.EmailConfiguracion;
import com.alantek.caja.modulo.email.entity.EmailPlantilla;
import com.alantek.caja.modulo.email.repository.EmailConfiguracionRepository;
import com.alantek.caja.modulo.email.repository.EmailPlantillaRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.Properties;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final EmailConfiguracionRepository configRepo;
    private final EmailPlantillaRepository plantillaRepo;

    @Value("${app.mail.enabled:false}")
    private boolean mailEnabled;

    @Value("${app.mail.from:noreply@caja-ahorros.com}")
    private String mailFrom;

    public EmailService(EmailConfiguracionRepository configRepo, EmailPlantillaRepository plantillaRepo) {
        this.configRepo = configRepo;
        this.plantillaRepo = plantillaRepo;
    }

    public void enviarEmail(String to, String subject, String htmlBody) {
        if (!mailEnabled) {
            log.info("Email deshabilitado. Se omitio envio a: {}", to);
            return;
        }

        EmailConfiguracion config = configRepo.findFirstByActivoTrue().orElse(null);
        if (config == null) {
            log.warn("No hay configuracion de email activa. Se omitio envio a: {}", to);
            return;
        }

        String from = config.getFromEmail() != null && !config.getFromEmail().isBlank()
                ? config.getFromEmail() : mailFrom;

        switch (config.getMetodo().toUpperCase()) {
            case "SMTP", "GMAIL", "HOTMAIL" -> enviarPorSmtp(config, to, subject, htmlBody, from);
            case "API" -> enviarPorApi(config, to, subject, htmlBody, from);
            default -> log.error("Metodo de envio desconocido: {}", config.getMetodo());
        }
    }

    public void enviarEmailPlantilla(String to, String modulo, String nombrePlantilla, Map<String, String> variables) {
        if (!mailEnabled) return;

        EmailPlantilla plantilla = plantillaRepo.findByModuloAndNombre(modulo, nombrePlantilla).orElse(null);
        if (plantilla == null || !plantilla.getActivo()) {
            log.warn("Plantilla no encontrada o inactiva: {}/{}", modulo, nombrePlantilla);
            return;
        }

        String html = plantilla.getCuerpoHtml();
        String asunto = plantilla.getAsunto();
        if (variables != null) {
            for (Map.Entry<String, String> entry : variables.entrySet()) {
                html = html.replace("{{" + entry.getKey() + "}}", entry.getValue());
                asunto = asunto.replace("{{" + entry.getKey() + "}}", entry.getValue());
            }
        }
        enviarEmail(to, asunto, html);
    }

    public void enviarNotificacion(String to, String nombreUsuario, String tipo, String mensaje) {
        if (!mailEnabled) return;

        EmailPlantilla plantilla = plantillaRepo.findByModuloAndNombre("general", "notificacion").orElse(null);
        if (plantilla != null && plantilla.getActivo()) {
            Map<String, String> vars = Map.of(
                    "nombre_usuario", nombreUsuario,
                    "tipo", tipo,
                    "mensaje", mensaje
            );
            enviarEmailPlantilla(to, "general", "notificacion", vars);
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

    public List<String> listarModulosPlantillas() {
        return plantillaRepo.findAllByOrderByModuloAscNombreAsc().stream()
                .map(EmailPlantilla::getModulo).distinct().toList();
    }

    private void enviarPorSmtp(EmailConfiguracion config, String to, String subject, String htmlBody, String from) {
        try {
            JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
            mailSender.setHost(config.getSmtpHost());
            mailSender.setPort(config.getSmtpPort() != null ? config.getSmtpPort() : 587);
            if (config.getSmtpUsername() != null && !config.getSmtpUsername().isBlank()) {
                mailSender.setUsername(config.getSmtpUsername());
            }
            if (config.getSmtpPassword() != null && !config.getSmtpPassword().isBlank()) {
                mailSender.setPassword(config.getSmtpPassword());
            }
            Properties props = mailSender.getJavaMailProperties();
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", String.valueOf(config.getSmtpUseTls() != null && config.getSmtpUseTls()));
            if (config.getSmtpUseSsl() != null && config.getSmtpUseSsl()) {
                props.put("mail.smtp.socketFactory.port", String.valueOf(config.getSmtpPort()));
                props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            }

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(message);
            log.info("Email enviado via {} a: {}", config.getMetodo(), to);
        } catch (MessagingException e) {
            log.error("Error al enviar email via {} a {}: {}", config.getMetodo(), to, e.getMessage(), e);
        }
    }

    private void enviarPorApi(EmailConfiguracion config, String to, String subject, String htmlBody, String from) {
        try {
            RestClient client = RestClient.create();
            Map<String, Object> body = Map.of(
                    "from", from,
                    "to", List.of(to),
                    "subject", subject,
                    "html", htmlBody
            );
            String url = config.getApiUrl();
            if (url == null || url.isBlank()) {
                log.error("URL de API de email no configurada");
                return;
            }
            var requestSpec = client.post().uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body);
            if (config.getApiKey() != null && !config.getApiKey().isBlank()) {
                requestSpec.header("Authorization", "Bearer " + config.getApiKey());
            }
            String response = requestSpec.retrieve().body(String.class);
            log.info("Email enviado via API a: {} - Response: {}", to, response);
        } catch (Exception e) {
            log.error("Error al enviar email via API a {}: {}", to, e.getMessage(), e);
        }
    }
}

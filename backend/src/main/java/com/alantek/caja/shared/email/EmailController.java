package com.alantek.caja.shared.email;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/config/email")
public class EmailController {

    private final EmailService emailService;

    public EmailController(EmailService emailService) {
        this.emailService = emailService;
    }

    @GetMapping("/test")
    @PreAuthorize("hasAuthority('SEGURIDAD:EDITAR')")
    public ResponseEntity<Map<String, String>> testEmailGet(
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "admin@caja-ahorros.com") String to) {
        try {
            emailService.enviarEmail(to, "Test - Caja de Ahorros",
                    "<html><body><h1>Correo de prueba</h1><p>Si recibe este mensaje, la configuracion SMTP funciona correctamente.</p></body></html>");
            return ResponseEntity.ok(Map.of("message", "Correo de prueba enviado a " + to));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Error al enviar correo: " + e.getMessage()));
        }
    }

    @PostMapping("/test")
    @PreAuthorize("hasAuthority('SEGURIDAD:EDITAR')")
    public ResponseEntity<Map<String, String>> testEmailPost(@RequestBody Map<String, String> request) {
        String to = request.get("to");
        if (to == null || to.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "El campo 'to' es requerido"));
        }
        try {
            emailService.enviarEmail(to, "Test - Caja de Ahorros",
                    "<html><body><h1>Correo de prueba</h1><p>Si recibe este mensaje, la configuracion SMTP funciona correctamente.</p></body></html>");
            return ResponseEntity.ok(Map.of("message", "Correo de prueba enviado a " + to));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Error al enviar correo: " + e.getMessage()));
        }
    }
}

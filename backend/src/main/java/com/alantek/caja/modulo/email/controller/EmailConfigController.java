package com.alantek.caja.modulo.email.controller;

import com.alantek.caja.modulo.email.dto.EmailConfiguracionRequest;
import com.alantek.caja.modulo.email.dto.EmailConfiguracionResponse;
import com.alantek.caja.modulo.email.dto.EmailPlantillaRequest;
import com.alantek.caja.modulo.email.dto.EmailPlantillaResponse;
import com.alantek.caja.modulo.email.service.EmailConfigService;
import com.alantek.caja.shared.email.EmailService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/config/email")
public class EmailConfigController {

    private final EmailConfigService configService;
    private final EmailService emailService;

    public EmailConfigController(EmailConfigService configService, EmailService emailService) {
        this.configService = configService;
        this.emailService = emailService;
    }

    @GetMapping("/configuracion")
    @PreAuthorize("hasAuthority('SEGURIDAD:VER')")
    public ResponseEntity<EmailConfiguracionResponse> obtenerConfiguracion() {
        return configService.obtenerConfiguracion()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.ok(null));
    }

    @PostMapping("/configuracion")
    @PreAuthorize("hasAuthority('SEGURIDAD:EDITAR')")
    public ResponseEntity<EmailConfiguracionResponse> guardarConfiguracion(
            @Valid @RequestBody EmailConfiguracionRequest request) {
        return ResponseEntity.ok(configService.guardarConfiguracion(request));
    }

    @DeleteMapping("/configuracion/{id}")
    @PreAuthorize("hasAuthority('SEGURIDAD:EDITAR')")
    public ResponseEntity<Map<String, Object>> eliminarConfiguracion(@PathVariable Long id) {
        return ResponseEntity.ok(configService.eliminarConfiguracion(id));
    }

    @GetMapping("/plantillas")
    @PreAuthorize("hasAuthority('SEGURIDAD:VER')")
    public ResponseEntity<List<EmailPlantillaResponse>> listarPlantillas(
            @RequestParam(required = false) String modulo) {
        return ResponseEntity.ok(configService.listarPlantillas(modulo));
    }

    @GetMapping("/plantillas/{id}")
    @PreAuthorize("hasAuthority('SEGURIDAD:VER')")
    public ResponseEntity<EmailPlantillaResponse> obtenerPlantilla(@PathVariable Long id) {
        return configService.obtenerPlantilla(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/plantillas")
    @PreAuthorize("hasAuthority('SEGURIDAD:EDITAR')")
    public ResponseEntity<EmailPlantillaResponse> crearPlantilla(
            @Valid @RequestBody EmailPlantillaRequest request) {
        return ResponseEntity.ok(configService.guardarPlantilla(request));
    }

    @PutMapping("/plantillas/{id}")
    @PreAuthorize("hasAuthority('SEGURIDAD:EDITAR')")
    public ResponseEntity<EmailPlantillaResponse> actualizarPlantilla(
            @PathVariable Long id, @Valid @RequestBody EmailPlantillaRequest request) {
        return ResponseEntity.ok(configService.actualizarPlantilla(id, request));
    }

    @DeleteMapping("/plantillas/{id}")
    @PreAuthorize("hasAuthority('SEGURIDAD:EDITAR')")
    public ResponseEntity<Map<String, Object>> eliminarPlantilla(@PathVariable Long id) {
        return ResponseEntity.ok(configService.eliminarPlantilla(id));
    }

    @PutMapping("/plantillas/{id}/toggle")
    @PreAuthorize("hasAuthority('SEGURIDAD:EDITAR')")
    public ResponseEntity<EmailPlantillaResponse> togglePlantilla(@PathVariable Long id) {
        return ResponseEntity.ok(configService.togglePlantilla(id));
    }

    @PostMapping("/test")
    @PreAuthorize("hasAuthority('SEGURIDAD:EDITAR')")
    public ResponseEntity<Map<String, String>> testEmail(@RequestBody Map<String, String> request) {
        String to = request.get("to");
        if (to == null || to.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "El campo 'to' es requerido"));
        }
        try {
            emailService.enviarEmail(to, "Test - Caja de Ahorros",
                    "<html><body><h1>Correo de prueba</h1><p>Si recibe este mensaje, la configuracion de email funciona correctamente.</p></body></html>");
            return ResponseEntity.ok(Map.of("message", "Correo de prueba enviado a " + to));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Error al enviar correo: " + e.getMessage()));
        }
    }

    @GetMapping("/modulos")
    @PreAuthorize("hasAuthority('SEGURIDAD:VER')")
    public ResponseEntity<List<String>> listarModulos() {
        return ResponseEntity.ok(emailService.listarModulosPlantillas());
    }
}

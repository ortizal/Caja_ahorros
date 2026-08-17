package com.alantek.caja.modulo.notificaciones.controller;

import com.alantek.caja.modulo.notificaciones.dto.NotificacionResponse;
import com.alantek.caja.modulo.notificaciones.service.NotificacionService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notificaciones")
public class NotificacionController {

    private final NotificacionService notificacionService;

    public NotificacionController(NotificacionService notificacionService) {
        this.notificacionService = notificacionService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<NotificacionResponse> listarMias() {
        return notificacionService.listarMias();
    }

    @GetMapping("/no-leidas")
    @PreAuthorize("isAuthenticated()")
    public long contarNoLeidas() {
        return notificacionService.contarNoLeidas();
    }

    @PostMapping("/{id}/leida")
    @PreAuthorize("isAuthenticated()")
    public NotificacionResponse marcarLeida(@PathVariable Long id) {
        return notificacionService.marcarLeida(id);
    }

    @PostMapping("/leidas")
    @PreAuthorize("isAuthenticated()")
    public void marcarTodasLeidas() {
        notificacionService.marcarTodasLeidas();
    }

    @PostMapping("/generar")
    @PreAuthorize("hasRole('ADMIN')")
    public void generarAlertas() {
        notificacionService.generarAlertas();
    }
}

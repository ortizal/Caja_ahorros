package com.alantek.caja.modulo.seguridad.controller;

import com.alantek.caja.modulo.seguridad.dto.AuditoriaResponse;
import com.alantek.caja.modulo.seguridad.dto.PermisoResponse;
import com.alantek.caja.modulo.seguridad.dto.RolResponse;
import com.alantek.caja.modulo.seguridad.service.RolService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/v1")
public class RolController {

    private final RolService rolService;

    public RolController(RolService rolService) {
        this.rolService = rolService;
    }

    @GetMapping("/roles")
    @PreAuthorize("hasAuthority('SEGURIDAD:VER')")
    public List<RolResponse> listarRoles() {
        return rolService.listar();
    }

    @GetMapping("/permisos")
    @PreAuthorize("hasAuthority('SEGURIDAD:VER')")
    public List<PermisoResponse> listarPermisos() {
        return rolService.listarPermisos();
    }

    @PostMapping("/roles/{id}/permisos")
    @PreAuthorize("hasAuthority('SEGURIDAD:EDITAR')")
    public RolResponse asignarPermisos(@PathVariable Long id, @RequestBody Set<Long> permisoIds) {
        return rolService.asignarPermisos(id, permisoIds);
    }

    @GetMapping("/auditoria")
    @PreAuthorize("hasAuthority('SEGURIDAD:VER')")
    public List<AuditoriaResponse> listarAuditoria(
            @RequestParam(required = false) String tabla,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant hasta) {
        return rolService.listarAuditoria(tabla, desde, hasta);
    }
}

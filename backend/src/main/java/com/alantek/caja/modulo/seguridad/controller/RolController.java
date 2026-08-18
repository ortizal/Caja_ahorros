package com.alantek.caja.modulo.seguridad.controller;

import com.alantek.caja.modulo.seguridad.dto.AuditoriaResponse;
import com.alantek.caja.modulo.seguridad.dto.PermisoResponse;
import com.alantek.caja.modulo.seguridad.dto.RolResponse;
import com.alantek.caja.modulo.seguridad.service.RolService;
import com.alantek.caja.shared.PageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
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
    public PageResponse<RolResponse> listarRoles(@PageableDefault(size = 10) Pageable pageable) {
        return rolService.listar(pageable);
    }

    @GetMapping("/permisos")
    @PreAuthorize("hasAuthority('SEGURIDAD:VER')")
    public PageResponse<PermisoResponse> listarPermisos(@PageableDefault(size = 10) Pageable pageable) {
        return rolService.listarPermisos(pageable);
    }

    @PostMapping("/roles/{id}/permisos")
    @PreAuthorize("hasAuthority('SEGURIDAD:EDITAR')")
    public RolResponse asignarPermisos(@PathVariable Long id, @RequestBody Set<Long> permisoIds) {
        return rolService.asignarPermisos(id, permisoIds);
    }

    @GetMapping("/auditoria")
    @PreAuthorize("hasAuthority('SEGURIDAD:VER')")
    public PageResponse<AuditoriaResponse> listarAuditoria(
            @RequestParam(required = false) String tabla,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant hasta,
            @PageableDefault(size = 10) Pageable pageable) {
        return rolService.listarAuditoria(tabla, desde, hasta, pageable);
    }
}

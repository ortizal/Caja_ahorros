package com.alantek.caja.modulo.socios.controller;

import com.alantek.caja.modulo.socios.dto.EstadoCuentaResponse;
import com.alantek.caja.modulo.socios.dto.SocioRequest;
import com.alantek.caja.modulo.socios.dto.SocioResponse;
import com.alantek.caja.modulo.socios.service.SocioService;
import com.alantek.caja.shared.PageResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/socios")
public class SocioController {

    private final SocioService socioService;

    public SocioController(SocioService socioService) {
        this.socioService = socioService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('SOCIOS:VER')")
    public PageResponse<SocioResponse> listar(@RequestParam(required = false) String estado, @PageableDefault(size = 10) Pageable pageable) {
        return socioService.listar(estado, pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('SOCIOS:VER')")
    public SocioResponse obtener(@PathVariable Long id) {
        return socioService.obtener(id);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('SOCIOS:CREAR')")
    public ResponseEntity<SocioResponse> crear(@Valid @RequestBody SocioRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(socioService.crear(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('SOCIOS:EDITAR')")
    public SocioResponse actualizar(@PathVariable Long id, @Valid @RequestBody SocioRequest request) {
        return socioService.actualizar(id, request);
    }

    @PutMapping("/{id}/estado")
    @PreAuthorize("hasAuthority('SOCIOS:EDITAR')")
    public ResponseEntity<Void> cambiarEstado(@PathVariable Long id, @RequestParam String estado) {
        socioService.cambiarEstado(id, estado);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/estado-cuenta")
    @PreAuthorize("hasAuthority('SOCIOS:VER')")
    public EstadoCuentaResponse estadoCuenta(@PathVariable Long id) {
        return socioService.estadoCuenta(id);
    }
}

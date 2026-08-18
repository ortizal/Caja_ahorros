package com.alantek.caja.modulo.contabilidad.controller;

import com.alantek.caja.modulo.contabilidad.dto.AsientoManualRequest;
import com.alantek.caja.modulo.contabilidad.dto.AsientoResponse;
import com.alantek.caja.modulo.contabilidad.dto.BalanceLinea;
import com.alantek.caja.modulo.contabilidad.dto.MayorLinea;
import com.alantek.caja.modulo.contabilidad.service.AsientoContableService;
import com.alantek.caja.shared.PageResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1")
public class AsientoController {

    private final AsientoContableService asientoService;

    public AsientoController(AsientoContableService asientoService) {
        this.asientoService = asientoService;
    }

    @PostMapping("/asientos-contables")
    @PreAuthorize("hasAuthority('CONTABILIDAD:CREAR')")
    public ResponseEntity<AsientoResponse> registrarManual(@Valid @RequestBody AsientoManualRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(asientoService.registrarManual(request));
    }

    @GetMapping("/libro-diario")
    @PreAuthorize("hasAuthority('CONTABILIDAD:VER')")
    public PageResponse<AsientoResponse> libroDiario(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @PageableDefault(size = 10) Pageable pageable) {
        return asientoService.libroDiario(desde, hasta, pageable);
    }

    @GetMapping("/libro-mayor")
    @PreAuthorize("hasAuthority('CONTABILIDAD:VER')")
    public PageResponse<MayorLinea> libroMayor(
            @RequestParam Long cuentaId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @PageableDefault(size = 10) Pageable pageable) {
        return asientoService.libroMayor(cuentaId, desde, hasta, pageable);
    }

    @GetMapping("/balance-comprobacion")
    @PreAuthorize("hasAuthority('CONTABILIDAD:VER')")
    public PageResponse<BalanceLinea> balanceComprobacion(
            @RequestParam Integer anio,
            @RequestParam Integer mes,
            @PageableDefault(size = 10) Pageable pageable) {
        return asientoService.balanceComprobacion(anio, mes, pageable);
    }
}

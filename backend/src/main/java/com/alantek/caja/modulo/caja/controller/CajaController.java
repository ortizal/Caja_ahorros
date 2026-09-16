package com.alantek.caja.modulo.caja.controller;

import com.alantek.caja.modulo.caja.dto.CajaAperturaRequest;
import com.alantek.caja.modulo.caja.dto.CajaAperturaResponse;
import com.alantek.caja.modulo.caja.dto.CajaArqueoRequest;
import com.alantek.caja.modulo.caja.dto.CajaArqueoResponse;
import com.alantek.caja.modulo.caja.dto.CajaMovimientoRequest;
import com.alantek.caja.modulo.caja.dto.CajaMovimientoResponse;
import com.alantek.caja.modulo.caja.dto.CajeroResponse;
import com.alantek.caja.modulo.caja.dto.SaldoCajaResponse;
import com.alantek.caja.modulo.caja.service.CajaService;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/caja")
public class CajaController {

    private final CajaService cajaService;

    public CajaController(CajaService cajaService) {
        this.cajaService = cajaService;
    }

    @PostMapping("/apertura")
    @PreAuthorize("hasAuthority('CAJA:CREAR')")
    public ResponseEntity<CajaAperturaResponse> apertura(@Valid @RequestBody CajaAperturaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cajaService.apertura(request));
    }

    @PostMapping("/{id}/cierre")
    @PreAuthorize("hasAuthority('CAJA:CREAR')")
    public CajaAperturaResponse cierre(@PathVariable Long id) {
        return cajaService.cerrar(id);
    }

    @PostMapping("/{id}/movimientos")
    @PreAuthorize("hasAuthority('CAJA:CREAR')")
    public ResponseEntity<CajaMovimientoResponse> registrarMovimiento(@PathVariable Long id,
                                                                      @Valid @RequestBody CajaMovimientoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cajaService.registrarMovimiento(request));
    }

    @GetMapping("/mias")
    @PreAuthorize("hasAuthority('CAJA:VER')")
    public PageResponse<CajaAperturaResponse> misCajas(@PageableDefault(size = 10) Pageable pageable) {
        return cajaService.misCajas(pageable);
    }

    @GetMapping("/listado")
    @PreAuthorize("hasAuthority('CAJA:APROBAR')")
    public PageResponse<CajaAperturaResponse> listado(@RequestParam(required = false) String estado,
                                                      @PageableDefault(size = 10) Pageable pageable) {
        return cajaService.listarTodas(estado, pageable);
    }

    @GetMapping("/cajeros")
    @PreAuthorize("hasAuthority('CAJA:APROBAR')")
    public List<CajeroResponse> cajeros() {
        return cajaService.listarCajeros();
    }

    @GetMapping("/{id}/movimientos")
    @PreAuthorize("hasAuthority('CAJA:VER')")
    public PageResponse<CajaMovimientoResponse> listarMovimientos(@PathVariable Long id,
                                                                   @PageableDefault(size = 10) Pageable pageable) {
        return cajaService.listarMovimientos(id, pageable);
    }

    @GetMapping("/{id}/saldo")
    @PreAuthorize("hasAuthority('CAJA:VER')")
    public SaldoCajaResponse saldo(@PathVariable Long id) {
        return cajaService.saldoCaja(id);
    }

    @PostMapping("/{id}/arqueo")
    @PreAuthorize("hasAuthority('CAJA:CREAR')")
    public ResponseEntity<CajaArqueoResponse> arqueo(@PathVariable Long id,
                                                     @Valid @RequestBody CajaArqueoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cajaService.arqueo(id, request));
    }
}

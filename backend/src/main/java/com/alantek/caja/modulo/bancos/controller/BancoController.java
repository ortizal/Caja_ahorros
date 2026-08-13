package com.alantek.caja.modulo.bancos.controller;

import com.alantek.caja.modulo.bancos.dto.BancoMovimientoRequest;
import com.alantek.caja.modulo.bancos.dto.BancoMovimientoResponse;
import com.alantek.caja.modulo.bancos.dto.ConciliacionRequest;
import com.alantek.caja.modulo.bancos.dto.ConciliacionResponse;
import com.alantek.caja.modulo.bancos.dto.CuentaBancariaRequest;
import com.alantek.caja.modulo.bancos.dto.CuentaBancariaResponse;
import com.alantek.caja.modulo.bancos.service.BancoService;
import jakarta.validation.Valid;
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
@RequestMapping("/api/v1")
public class BancoController {

    private final BancoService bancoService;

    public BancoController(BancoService bancoService) {
        this.bancoService = bancoService;
    }

    @GetMapping("/cuentas-bancarias")
    @PreAuthorize("hasAuthority('BANCOS:VER')")
    public List<CuentaBancariaResponse> listarCuentas() {
        return bancoService.listarCuentas();
    }

    @PostMapping("/cuentas-bancarias")
    @PreAuthorize("hasAuthority('BANCOS:CREAR')")
    public ResponseEntity<CuentaBancariaResponse> crearCuenta(@Valid @RequestBody CuentaBancariaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(bancoService.crearCuenta(request));
    }

    @GetMapping("/cuentas-bancarias/{id}/movimientos")
    @PreAuthorize("hasAuthority('BANCOS:VER')")
    public List<BancoMovimientoResponse> listarMovimientos(@PathVariable Long id) {
        return bancoService.listarMovimientos(id);
    }

    @PostMapping("/cuentas-bancarias/{id}/movimientos")
    @PreAuthorize("hasAuthority('BANCOS:CREAR')")
    public ResponseEntity<BancoMovimientoResponse> registrarMovimiento(@PathVariable Long id,
                                                                       @Valid @RequestBody BancoMovimientoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(bancoService.registrarMovimiento(id, request));
    }

    @PostMapping("/conciliacion-bancaria")
    @PreAuthorize("hasAuthority('BANCOS:CREAR')")
    public ResponseEntity<ConciliacionResponse> conciliar(@RequestParam Long cuentaId,
                                                          @Valid @RequestBody ConciliacionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(bancoService.conciliar(cuentaId, request));
    }
}

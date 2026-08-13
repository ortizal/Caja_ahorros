package com.alantek.caja.modulo.ahorros.controller;

import com.alantek.caja.modulo.ahorros.dto.CapitalizacionResponse;
import com.alantek.caja.modulo.ahorros.dto.CuentaAhorroRequest;
import com.alantek.caja.modulo.ahorros.dto.CuentaAhorroResponse;
import com.alantek.caja.modulo.ahorros.dto.MovimientoAhorroRequest;
import com.alantek.caja.modulo.ahorros.dto.MovimientoAhorroResponse;
import com.alantek.caja.modulo.ahorros.dto.ProductoAhorroRequest;
import com.alantek.caja.modulo.ahorros.dto.ProductoAhorroResponse;
import com.alantek.caja.modulo.ahorros.service.AhorroService;
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
public class AhorroController {

    private final AhorroService ahorroService;

    public AhorroController(AhorroService ahorroService) {
        this.ahorroService = ahorroService;
    }

    @GetMapping("/productos-ahorro")
    @PreAuthorize("hasAuthority('AHORROS:VER')")
    public List<ProductoAhorroResponse> listarProductos() {
        return ahorroService.listarProductos();
    }

    @PostMapping("/productos-ahorro")
    @PreAuthorize("hasAuthority('AHORROS:CREAR')")
    public ResponseEntity<ProductoAhorroResponse> crearProducto(
            @Valid @RequestBody ProductoAhorroRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ahorroService.crearProducto(request));
    }

    @GetMapping("/cuentas-ahorro")
    @PreAuthorize("hasAuthority('AHORROS:VER')")
    public List<CuentaAhorroResponse> listarCuentas(@RequestParam(required = false) Long socioId) {
        return ahorroService.listarCuentas(socioId);
    }

    @PostMapping("/cuentas-ahorro")
    @PreAuthorize("hasAuthority('AHORROS:CREAR')")
    public ResponseEntity<CuentaAhorroResponse> aperturar(@Valid @RequestBody CuentaAhorroRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ahorroService.aperturar(request));
    }

    @GetMapping("/cuentas-ahorro/{id}/movimientos")
    @PreAuthorize("hasAuthority('AHORROS:VER')")
    public List<MovimientoAhorroResponse> listarMovimientos(@PathVariable Long id) {
        return ahorroService.listarMovimientos(id);
    }

    @PostMapping("/cuentas-ahorro/{id}/depositos")
    @PreAuthorize("hasAuthority('AHORROS:CREAR')")
    public ResponseEntity<MovimientoAhorroResponse> depositar(@PathVariable Long id,
                                                              @Valid @RequestBody MovimientoAhorroRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ahorroService.depositar(id, request));
    }

    @PostMapping("/cuentas-ahorro/{id}/retiros")
    @PreAuthorize("hasAuthority('AHORROS:CREAR')")
    public ResponseEntity<MovimientoAhorroResponse> retirar(@PathVariable Long id,
                                                            @Valid @RequestBody MovimientoAhorroRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ahorroService.retirar(id, request));
    }

    @PostMapping("/ahorros/capitalizar")
    @PreAuthorize("hasAuthority('AHORROS:CREAR')")
    public CapitalizacionResponse capitalizar(@RequestParam int anio, @RequestParam int mes) {
        return ahorroService.capitalizar(anio, mes);
    }
}

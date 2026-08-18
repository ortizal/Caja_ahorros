package com.alantek.caja.modulo.ahorros.controller;

import com.alantek.caja.modulo.ahorros.dto.CapitalizacionResponse;
import com.alantek.caja.modulo.ahorros.dto.CuentaAhorroRequest;
import com.alantek.caja.modulo.ahorros.dto.CuentaAhorroResponse;
import com.alantek.caja.modulo.ahorros.dto.MovimientoAhorroRequest;
import com.alantek.caja.modulo.ahorros.dto.MovimientoAhorroResponse;
import com.alantek.caja.modulo.ahorros.dto.ProductoAhorroRequest;
import com.alantek.caja.modulo.ahorros.dto.ProductoAhorroResponse;
import com.alantek.caja.modulo.ahorros.service.AhorroService;
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

@RestController
@RequestMapping("/api/v1")
public class AhorroController {

    private final AhorroService ahorroService;

    public AhorroController(AhorroService ahorroService) {
        this.ahorroService = ahorroService;
    }

    @GetMapping("/productos-ahorro")
    @PreAuthorize("hasAuthority('AHORROS:VER')")
    public PageResponse<ProductoAhorroResponse> listarProductos(@PageableDefault(size = 10) Pageable pageable) {
        return ahorroService.listarProductos(pageable);
    }

    @PostMapping("/productos-ahorro")
    @PreAuthorize("hasAuthority('AHORROS:CREAR')")
    public ResponseEntity<ProductoAhorroResponse> crearProducto(
            @Valid @RequestBody ProductoAhorroRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ahorroService.crearProducto(request));
    }

    @GetMapping("/cuentas-ahorro")
    @PreAuthorize("hasAuthority('AHORROS:VER')")
    public PageResponse<CuentaAhorroResponse> listarCuentas(@RequestParam(required = false) Long socioId,
                                                             @PageableDefault(size = 10) Pageable pageable) {
        return ahorroService.listarCuentas(socioId, pageable);
    }

    @PostMapping("/cuentas-ahorro")
    @PreAuthorize("hasAuthority('AHORROS:CREAR')")
    public ResponseEntity<CuentaAhorroResponse> aperturar(@Valid @RequestBody CuentaAhorroRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ahorroService.aperturar(request));
    }

    @GetMapping("/cuentas-ahorro/{id}/movimientos")
    @PreAuthorize("hasAuthority('AHORROS:VER')")
    public PageResponse<MovimientoAhorroResponse> listarMovimientos(@PathVariable Long id,
                                                                     @PageableDefault(size = 10) Pageable pageable) {
        return ahorroService.listarMovimientos(id, pageable);
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

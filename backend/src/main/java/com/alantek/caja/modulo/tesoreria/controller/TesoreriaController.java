package com.alantek.caja.modulo.tesoreria.controller;

import com.alantek.caja.modulo.tesoreria.dto.AprobacionGastoRequest;
import com.alantek.caja.modulo.tesoreria.dto.CuentaPorCobrarRequest;
import com.alantek.caja.modulo.tesoreria.dto.CuentaPorCobrarResponse;
import com.alantek.caja.modulo.tesoreria.dto.CuentaPorPagarRequest;
import com.alantek.caja.modulo.tesoreria.dto.CuentaPorPagarResponse;
import com.alantek.caja.modulo.tesoreria.dto.GastoRequest;
import com.alantek.caja.modulo.tesoreria.dto.GastoResponse;
import com.alantek.caja.modulo.tesoreria.dto.PresupuestoPartidaRequest;
import com.alantek.caja.modulo.tesoreria.dto.PresupuestoPartidaResponse;
import com.alantek.caja.modulo.tesoreria.dto.PresupuestoResumenResponse;
import com.alantek.caja.modulo.tesoreria.service.TesoreriaService;
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
public class TesoreriaController {

    private final TesoreriaService tesoreriaService;

    public TesoreriaController(TesoreriaService tesoreriaService) {
        this.tesoreriaService = tesoreriaService;
    }

    @GetMapping("/gastos")
    @PreAuthorize("hasAuthority('TESORERIA:VER')")
    public List<GastoResponse> listarGastos(@RequestParam(required = false) String estado) {
        return tesoreriaService.listarGastos(estado);
    }

    @PostMapping("/gastos")
    @PreAuthorize("hasAuthority('TESORERIA:CREAR')")
    public ResponseEntity<GastoResponse> crearGasto(@Valid @RequestBody GastoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(tesoreriaService.crearGasto(request));
    }

    @PostMapping("/gastos/{id}/aprobar")
    @PreAuthorize("hasAuthority('TESORERIA:APROBAR')")
    public GastoResponse aprobarGasto(@PathVariable Long id, @Valid @RequestBody AprobacionGastoRequest request) {
        return tesoreriaService.aprobarGasto(id, request);
    }

    @PostMapping("/gastos/{id}/pagar")
    @PreAuthorize("hasAuthority('TESORERIA:EDITAR')")
    public GastoResponse pagarGasto(@PathVariable Long id) {
        return tesoreriaService.pagarGasto(id);
    }

    @PostMapping("/gastos/{id}/anular")
    @PreAuthorize("hasAuthority('TESORERIA:ANULAR')")
    public GastoResponse anularGasto(@PathVariable Long id) {
        return tesoreriaService.anularGasto(id);
    }

    @GetMapping("/cuentas-por-pagar")
    @PreAuthorize("hasAuthority('TESORERIA:VER')")
    public List<CuentaPorPagarResponse> listarCuentasPorPagar(@RequestParam(required = false) String estado) {
        return tesoreriaService.listarCuentasPorPagar(estado);
    }

    @PostMapping("/cuentas-por-pagar")
    @PreAuthorize("hasAuthority('TESORERIA:CREAR')")
    public ResponseEntity<CuentaPorPagarResponse> crearCuentaPorPagar(@Valid @RequestBody CuentaPorPagarRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(tesoreriaService.crearCuentaPorPagar(request));
    }

    @PostMapping("/cuentas-por-pagar/{id}/pagar")
    @PreAuthorize("hasAuthority('TESORERIA:EDITAR')")
    public CuentaPorPagarResponse pagarCuentaPorPagar(@PathVariable Long id) {
        return tesoreriaService.pagarCuentaPorPagar(id);
    }

    @GetMapping("/cuentas-por-cobrar")
    @PreAuthorize("hasAuthority('TESORERIA:VER')")
    public List<CuentaPorCobrarResponse> listarCuentasPorCobrar(@RequestParam(required = false) String estado) {
        return tesoreriaService.listarCuentasPorCobrar(estado);
    }

    @PostMapping("/cuentas-por-cobrar")
    @PreAuthorize("hasAuthority('TESORERIA:CREAR')")
    public ResponseEntity<CuentaPorCobrarResponse> crearCuentaPorCobrar(@Valid @RequestBody CuentaPorCobrarRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(tesoreriaService.crearCuentaPorCobrar(request));
    }

    @PostMapping("/cuentas-por-cobrar/{id}/cobrar")
    @PreAuthorize("hasAuthority('TESORERIA:EDITAR')")
    public CuentaPorCobrarResponse cobrarCuentaPorCobrar(@PathVariable Long id) {
        return tesoreriaService.cobrarCuentaPorCobrar(id);
    }

    @GetMapping("/presupuesto")
    @PreAuthorize("hasAuthority('TESORERIA:VER')")
    public PresupuestoResumenResponse resumenPresupuesto(@RequestParam(required = false) Integer anio) {
        return tesoreriaService.resumenPresupuesto(anio);
    }

    @PostMapping("/presupuesto/partidas")
    @PreAuthorize("hasAuthority('TESORERIA:CREAR')")
    public ResponseEntity<PresupuestoPartidaResponse> crearPartidaPresupuesto(@Valid @RequestBody PresupuestoPartidaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(tesoreriaService.crearPartidaPresupuesto(request));
    }
}

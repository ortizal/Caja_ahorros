package com.alantek.caja.modulo.creditos.controller;

import com.alantek.caja.modulo.creditos.dto.AprobarSolicitudRequest;
import com.alantek.caja.modulo.creditos.dto.CreditoResponse;
import com.alantek.caja.modulo.creditos.dto.CuotaCreditoResponse;
import com.alantek.caja.modulo.creditos.dto.MoraResponse;
import com.alantek.caja.modulo.creditos.dto.PagoCuotaRequest;
import com.alantek.caja.modulo.creditos.dto.PagoCuotaResponse;
import com.alantek.caja.modulo.creditos.dto.ProductoCreditoRequest;
import com.alantek.caja.modulo.creditos.dto.ProductoCreditoResponse;
import com.alantek.caja.modulo.creditos.dto.RefinanciarRequest;
import com.alantek.caja.modulo.creditos.dto.SimulacionCreditoRequest;
import com.alantek.caja.modulo.creditos.dto.SimulacionCreditoResponse;
import com.alantek.caja.modulo.creditos.dto.SolicitudCreditoRequest;
import com.alantek.caja.modulo.creditos.dto.SolicitudCreditoResponse;
import com.alantek.caja.modulo.creditos.service.CreditoService;
import jakarta.validation.Valid;
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

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class CreditoController {

    private final CreditoService creditoService;

    public CreditoController(CreditoService creditoService) {
        this.creditoService = creditoService;
    }

    @GetMapping("/productos-credito")
    @PreAuthorize("hasAuthority('CREDITOS:VER')")
    public List<ProductoCreditoResponse> listarProductos() {
        return creditoService.listarProductos();
    }

    @GetMapping("/productos-credito/activos")
    @PreAuthorize("hasAuthority('CREDITOS:VER')")
    public List<ProductoCreditoResponse> listarProductosActivos() {
        return creditoService.listarProductosActivos();
    }

    @PostMapping("/productos-credito")
    @PreAuthorize("hasAuthority('CREDITOS:CREAR')")
    public ResponseEntity<ProductoCreditoResponse> crearProducto(
            @Valid @RequestBody ProductoCreditoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(creditoService.crearProducto(request));
    }

    @GetMapping("/solicitudes-credito")
    @PreAuthorize("hasAuthority('CREDITOS:VER')")
    public List<SolicitudCreditoResponse> listarSolicitudes(@RequestParam(required = false) String estado) {
        return creditoService.listarSolicitudes(estado);
    }

    @PostMapping("/solicitudes-credito")
    @PreAuthorize("hasAuthority('CREDITOS:CREAR')")
    public ResponseEntity<SolicitudCreditoResponse> crearSolicitud(
            @Valid @RequestBody SolicitudCreditoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(creditoService.crearSolicitud(request));
    }

    @PutMapping("/solicitudes-credito/{id}/evaluar")
    @PreAuthorize("hasAuthority('CREDITOS:EDITAR')")
    public SolicitudCreditoResponse evaluar(@PathVariable Long id) {
        return creditoService.evaluar(id);
    }

    @PutMapping("/solicitudes-credito/{id}/aprobar")
    @PreAuthorize("hasAuthority('CREDITOS:APROBAR')")
    public SolicitudCreditoResponse aprobar(@PathVariable Long id,
                                            @Valid @RequestBody AprobarSolicitudRequest request) {
        return creditoService.aprobar(id, request);
    }

    @GetMapping("/creditos")
    @PreAuthorize("hasAuthority('CREDITOS:VER')")
    public List<CreditoResponse> listarCreditos(@RequestParam(required = false) Long socioId) {
        return creditoService.listarCreditos(socioId);
    }

    @GetMapping("/creditos/{id}")
    @PreAuthorize("hasAuthority('CREDITOS:VER')")
    public CreditoResponse obtenerCredito(@PathVariable Long id) {
        return creditoService.obtenerCredito(id);
    }

    @PostMapping("/creditos/{id}/desembolsar")
    @PreAuthorize("hasAuthority('CREDITOS:APROBAR')")
    public CreditoResponse desembolsar(@PathVariable Long id) {
        return creditoService.desembolsar(id);
    }

    @GetMapping("/creditos/{id}/amortizacion")
    @PreAuthorize("hasAuthority('CREDITOS:VER')")
    public List<CuotaCreditoResponse> listarCuotas(@PathVariable Long id) {
        return creditoService.listarCuotas(id);
    }

    @GetMapping("/creditos/{id}/pagos")
    @PreAuthorize("hasAuthority('CREDITOS:VER')")
    public List<PagoCuotaResponse> listarPagos(@PathVariable Long id) {
        return creditoService.listarPagos(id);
    }

    @PostMapping("/creditos/{id}/pagos")
    @PreAuthorize("hasAuthority('CREDITOS:CREAR')")
    public ResponseEntity<PagoCuotaResponse> pagarCuota(@PathVariable Long id,
                                                        @Valid @RequestBody PagoCuotaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(creditoService.pagarCuota(request));
    }

    @PostMapping("/creditos/{id}/refinanciar")
    @PreAuthorize("hasAuthority('CREDITOS:APROBAR')")
    public CreditoResponse refinanciar(@PathVariable Long id,
                                       @Valid @RequestBody RefinanciarRequest request) {
        return creditoService.refinanciar(id, request);
    }

    @PostMapping("/creditos/procesar-vencidas")
    @PreAuthorize("hasAuthority('CREDITOS:EDITAR')")
    public MoraResponse procesarVencidas() {
        return creditoService.procesarVencidas();
    }

    @PostMapping("/simulador-credito")
    @PreAuthorize("hasAuthority('CREDITOS:VER')")
    public SimulacionCreditoResponse simular(@Valid @RequestBody SimulacionCreditoRequest request) {
        return creditoService.simular(request);
    }
}

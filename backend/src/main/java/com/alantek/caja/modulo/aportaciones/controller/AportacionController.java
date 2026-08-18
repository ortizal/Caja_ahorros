package com.alantek.caja.modulo.aportaciones.controller;

import com.alantek.caja.modulo.aportaciones.dto.AportacionConfigRequest;
import com.alantek.caja.modulo.aportaciones.dto.AportacionConfigResponse;
import com.alantek.caja.modulo.aportaciones.dto.AportacionPagoRequest;
import com.alantek.caja.modulo.aportaciones.dto.AportacionPagoResponse;
import com.alantek.caja.modulo.aportaciones.dto.AportacionResponse;
import com.alantek.caja.modulo.aportaciones.dto.GenerarAportacionesResponse;
import com.alantek.caja.modulo.aportaciones.service.AportacionService;
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
@RequestMapping("/api/v1/aportaciones")
public class AportacionController {

    private final AportacionService aportacionService;

    public AportacionController(AportacionService aportacionService) {
        this.aportacionService = aportacionService;
    }

    @GetMapping("/config")
    @PreAuthorize("hasAuthority('APORTACIONES:VER')")
    public PageResponse<AportacionConfigResponse> listarConfigs(@PageableDefault(size = 10) Pageable pageable) {
        return aportacionService.listarConfigs(pageable);
    }

    @PostMapping("/config")
    @PreAuthorize("hasAuthority('APORTACIONES:CREAR')")
    public ResponseEntity<AportacionConfigResponse> crearConfig(
            @Valid @RequestBody AportacionConfigRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(aportacionService.crearConfig(request));
    }

    @PostMapping("/generar")
    @PreAuthorize("hasAuthority('APORTACIONES:CREAR')")
    public GenerarAportacionesResponse generarPeriodo(@RequestParam String periodo) {
        return aportacionService.generarPeriodo(periodo);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('APORTACIONES:VER')")
    public PageResponse<AportacionResponse> listar(@RequestParam(required = false) String periodo,
                                                   @RequestParam(required = false) Long socioId,
                                                   @PageableDefault(size = 10) Pageable pageable) {
        return aportacionService.listarAportaciones(periodo, socioId, pageable);
    }

    @GetMapping("/{id}/pagos")
    @PreAuthorize("hasAuthority('APORTACIONES:VER')")
    public PageResponse<AportacionPagoResponse> listarPagos(@PathVariable Long id,
                                                            @PageableDefault(size = 10) Pageable pageable) {
        return aportacionService.listarPagos(id, pageable);
    }

    @PostMapping("/{id}/pagos")
    @PreAuthorize("hasAuthority('APORTACIONES:CREAR')")
    public ResponseEntity<AportacionPagoResponse> pagar(@PathVariable Long id,
                                                        @Valid @RequestBody AportacionPagoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(aportacionService.pagar(id, request));
    }
}

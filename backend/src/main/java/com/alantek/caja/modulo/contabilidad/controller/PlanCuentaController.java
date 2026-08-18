package com.alantek.caja.modulo.contabilidad.controller;

import com.alantek.caja.modulo.contabilidad.dto.PlanCuentaRequest;
import com.alantek.caja.modulo.contabilidad.dto.PlanCuentaResponse;
import com.alantek.caja.modulo.contabilidad.service.PlanCuentaService;
import com.alantek.caja.shared.PageResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/plan-cuentas")
public class PlanCuentaController {

    private final PlanCuentaService planCuentaService;

    public PlanCuentaController(PlanCuentaService planCuentaService) {
        this.planCuentaService = planCuentaService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('CONTABILIDAD:VER')")
    public PageResponse<PlanCuentaResponse> listar(@PageableDefault(size = 10) Pageable pageable) {
        return planCuentaService.listar(pageable);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('CONTABILIDAD:CREAR')")
    public ResponseEntity<PlanCuentaResponse> crear(@Valid @RequestBody PlanCuentaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(planCuentaService.crear(request));
    }
}

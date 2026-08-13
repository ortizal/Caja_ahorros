package com.alantek.caja.modulo.contabilidad.controller;

import com.alantek.caja.modulo.contabilidad.dto.PlanCuentaRequest;
import com.alantek.caja.modulo.contabilidad.dto.PlanCuentaResponse;
import com.alantek.caja.modulo.contabilidad.service.PlanCuentaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/plan-cuentas")
public class PlanCuentaController {

    private final PlanCuentaService planCuentaService;

    public PlanCuentaController(PlanCuentaService planCuentaService) {
        this.planCuentaService = planCuentaService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('CONTABILIDAD:VER')")
    public List<PlanCuentaResponse> listar() {
        return planCuentaService.listar();
    }

    @PostMapping
    @PreAuthorize("hasAuthority('CONTABILIDAD:CREAR')")
    public ResponseEntity<PlanCuentaResponse> crear(@Valid @RequestBody PlanCuentaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(planCuentaService.crear(request));
    }
}

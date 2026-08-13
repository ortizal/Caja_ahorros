package com.alantek.caja.modulo.contabilidad.service;

import com.alantek.caja.modulo.contabilidad.dto.PlanCuentaRequest;
import com.alantek.caja.modulo.contabilidad.dto.PlanCuentaResponse;
import com.alantek.caja.modulo.contabilidad.entity.PlanCuenta;
import com.alantek.caja.modulo.contabilidad.repository.PlanCuentaRepository;
import com.alantek.caja.shared.audit.AuditService;
import com.alantek.caja.shared.exception.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PlanCuentaService {

    private final PlanCuentaRepository planCuentaRepository;
    private final AuditService auditService;

    public PlanCuentaService(PlanCuentaRepository planCuentaRepository, AuditService auditService) {
        this.planCuentaRepository = planCuentaRepository;
        this.auditService = auditService;
    }

    public List<PlanCuentaResponse> listar() {
        return planCuentaRepository.findAll().stream()
                .sorted(java.util.Comparator.comparing(PlanCuenta::getCodigo))
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public PlanCuentaResponse crear(PlanCuentaRequest request) {
        if (planCuentaRepository.findByCodigo(request.codigo()).isPresent()) {
            throw new BusinessException("Ya existe una cuenta con el código: " + request.codigo());
        }
        PlanCuenta cuenta = new PlanCuenta();
        cuenta.setCodigo(request.codigo());
        cuenta.setNombre(request.nombre());
        cuenta.setTipo(request.tipo().toUpperCase());
        cuenta.setCuentaPadreId(request.cuentaPadreId());
        cuenta.setNivel(request.nivel());
        cuenta.setAceptaMovimiento(request.aceptaMovimiento() == null || request.aceptaMovimiento());
        PlanCuenta saved = planCuentaRepository.save(cuenta);
        auditService.registrar("plan_cuentas", saved.getId(), "CREAR", null, request);
        return toResponse(saved);
    }

    private PlanCuentaResponse toResponse(PlanCuenta cuenta) {
        return new PlanCuentaResponse(
                cuenta.getId(), cuenta.getCodigo(), cuenta.getNombre(), cuenta.getTipo(),
                cuenta.getCuentaPadreId(), cuenta.getNivel(), cuenta.getAceptaMovimiento());
    }
}

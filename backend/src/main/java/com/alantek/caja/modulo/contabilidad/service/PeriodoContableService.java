package com.alantek.caja.modulo.contabilidad.service;

import com.alantek.caja.modulo.contabilidad.entity.PeriodoContable;
import com.alantek.caja.modulo.contabilidad.repository.PeriodoContableRepository;
import com.alantek.caja.shared.audit.AuditService;
import com.alantek.caja.shared.exception.BusinessException;
import com.alantek.caja.shared.security.CurrentUserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class PeriodoContableService {

    private final PeriodoContableRepository periodoRepository;
    private final CurrentUserService currentUserService;
    private final AuditService auditService;

    public PeriodoContableService(PeriodoContableRepository periodoRepository,
                                  CurrentUserService currentUserService,
                                  AuditService auditService) {
        this.periodoRepository = periodoRepository;
        this.currentUserService = currentUserService;
        this.auditService = auditService;
    }

    public List<PeriodoContable> listar() {
        return periodoRepository.findAll().stream()
                .sorted(java.util.Comparator.comparing(PeriodoContable::getAnio).reversed()
                        .thenComparing(PeriodoContable::getMes).reversed())
                .toList();
    }

    @Transactional
    public PeriodoContable cerrar(Integer anio, Integer mes) {
        PeriodoContable periodo = obtener(anio, mes);
        if ("CERRADO".equals(periodo.getEstado())) {
            throw new BusinessException("El período " + anio + "-" + mes + " ya está cerrado");
        }
        periodo.setEstado("CERRADO");
        periodo.setCerradoPor(currentUserService.requireUserId());
        periodo.setCerradoAt(Instant.now());
        periodoRepository.save(periodo);
        auditService.registrar("periodo_contable", periodo.getId(), "CERRAR", "ABIERTO", "CERRADO");
        return periodo;
    }

    @Transactional
    public PeriodoContable reabrir(Integer anio, Integer mes) {
        PeriodoContable periodo = obtener(anio, mes);
        if (!"CERRADO".equals(periodo.getEstado())) {
            throw new BusinessException("El período " + anio + "-" + mes + " no está cerrado");
        }
        periodo.setEstado("ABIERTO");
        periodo.setCerradoPor(null);
        periodo.setCerradoAt(null);
        periodoRepository.save(periodo);
        auditService.registrar("periodo_contable", periodo.getId(), "REABRIR", "CERRADO", "ABIERTO");
        return periodo;
    }

    private PeriodoContable obtener(Integer anio, Integer mes) {
        return periodoRepository.findByAnioAndMes(anio, mes)
                .orElseThrow(() -> new BusinessException("Período contable no encontrado: " + anio + "-" + mes));
    }
}

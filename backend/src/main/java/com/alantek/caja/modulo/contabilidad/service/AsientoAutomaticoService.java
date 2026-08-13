package com.alantek.caja.modulo.contabilidad.service;

import com.alantek.caja.modulo.contabilidad.entity.AsientoContable;
import com.alantek.caja.modulo.contabilidad.entity.AsientoDetalle;
import com.alantek.caja.modulo.contabilidad.entity.PeriodoContable;
import com.alantek.caja.modulo.contabilidad.entity.ReglaContable;
import com.alantek.caja.modulo.contabilidad.repository.AsientoContableRepository;
import com.alantek.caja.modulo.contabilidad.repository.AsientoDetalleRepository;
import com.alantek.caja.modulo.contabilidad.repository.PeriodoContableRepository;
import com.alantek.caja.modulo.contabilidad.repository.ReglaContableRepository;
import com.alantek.caja.shared.audit.AuditService;
import com.alantek.caja.shared.exception.BusinessException;
import com.alantek.caja.shared.security.CurrentUserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class AsientoAutomaticoService {

    public record ReglaAplicada(String operacion, BigDecimal monto) {
    }

    private final ReglaContableRepository reglaRepository;
    private final PeriodoContableRepository periodoRepository;
    private final AsientoContableRepository asientoRepository;
    private final CurrentUserService currentUserService;
    private final AuditService auditService;

    public AsientoAutomaticoService(ReglaContableRepository reglaRepository,
                                    PeriodoContableRepository periodoRepository,
                                    AsientoContableRepository asientoRepository,
                                    CurrentUserService currentUserService,
                                    AuditService auditService) {
        this.reglaRepository = reglaRepository;
        this.periodoRepository = periodoRepository;
        this.asientoRepository = asientoRepository;
        this.currentUserService = currentUserService;
        this.auditService = auditService;
    }

    @Transactional
    public AsientoContable generarAsiento(String descripcion, Long comprobanteId, LocalDate fecha,
                                          List<ReglaAplicada> reglas) {
        if (reglas == null || reglas.isEmpty()) {
            throw new BusinessException("Debe indicar al menos una regla contable");
        }

        PeriodoContable periodo = obtenerPeriodoAbierto(fecha);

        BigDecimal totalDebe = BigDecimal.ZERO;
        BigDecimal totalHaber = BigDecimal.ZERO;
        List<AsientoDetalle> detalles = new ArrayList<>();

        for (ReglaAplicada regla : reglas) {
            if (regla.monto() == null || regla.monto().signum() == 0) {
                continue;
            }
            ReglaContable reglaVigente = reglaRepository.findVigente(regla.operacion(), fecha)
                    .orElseThrow(() -> new BusinessException(
                            "No existe regla contable vigente para la operación '" + regla.operacion()
                                    + "' en la fecha " + fecha));

            AsientoDetalle detalleDebe = new AsientoDetalle();
            detalleDebe.setCuentaId(reglaVigente.getCuentaDebeId());
            detalleDebe.setDebe(regla.monto());
            detalles.add(detalleDebe);

            AsientoDetalle detalleHaber = new AsientoDetalle();
            detalleHaber.setCuentaId(reglaVigente.getCuentaHaberId());
            detalleHaber.setHaber(regla.monto());
            detalles.add(detalleHaber);

            totalDebe = totalDebe.add(regla.monto());
            totalHaber = totalHaber.add(regla.monto());
        }

        if (detalles.isEmpty()) {
            throw new BusinessException("No hay montos que registrar en el asiento");
        }
        if (totalDebe.compareTo(totalHaber) != 0) {
            throw new BusinessException("Cuadre contable no válido: Debe=" + totalDebe + " vs Haber=" + totalHaber);
        }

        AsientoContable asiento = new AsientoContable();
        asiento.setPeriodoId(periodo.getId());
        asiento.setComprobanteId(comprobanteId);
        asiento.setFecha(fecha);
        asiento.setDescripcion(descripcion);
        asiento.setOrigen("AUTOMATICO");
        asiento.setCreatedBy(currentUserService.getCurrentUser().map(u -> u.id()).orElse(null));
        asiento.setDetalles(detalles);
        detalles.forEach(detalle -> detalle.setAsiento(asiento));

        AsientoContable saved = asientoRepository.save(asiento);
        auditService.registrar("asiento_contable", saved.getId(), "CREAR", null, descripcion);
        return saved;
    }

    public AsientoContable generarAsientoSimple(String operacion, Long comprobanteId, LocalDate fecha,
                                                BigDecimal monto, String descripcion) {
        return generarAsiento(descripcion, comprobanteId, fecha, List.of(new ReglaAplicada(operacion, monto)));
    }

    @Transactional
    public PeriodoContable obtenerPeriodoAbierto(LocalDate fecha) {
        int anio = fecha.getYear();
        int mes = fecha.getMonthValue();
        PeriodoContable periodo = periodoRepository.findByAnioAndMes(anio, mes).orElseGet(() -> {
            PeriodoContable nuevo = new PeriodoContable();
            nuevo.setAnio(anio);
            nuevo.setMes(mes);
            return periodoRepository.save(nuevo);
        });
        if (!"ABIERTO".equals(periodo.getEstado())) {
            throw new BusinessException("El período contable " + anio + "-" + mes
                    + " está CERRADO; no se pueden registrar movimientos");
        }
        return periodo;
    }
}

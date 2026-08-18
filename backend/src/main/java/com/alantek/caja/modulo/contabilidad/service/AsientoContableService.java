package com.alantek.caja.modulo.contabilidad.service;

import com.alantek.caja.modulo.contabilidad.dto.AsientoManualRequest;
import com.alantek.caja.modulo.contabilidad.dto.AsientoResponse;
import com.alantek.caja.modulo.contabilidad.dto.BalanceLinea;
import com.alantek.caja.modulo.contabilidad.dto.MayorLinea;
import com.alantek.caja.modulo.contabilidad.entity.AsientoContable;
import com.alantek.caja.modulo.contabilidad.entity.AsientoDetalle;
import com.alantek.caja.modulo.contabilidad.entity.PeriodoContable;
import com.alantek.caja.modulo.contabilidad.entity.PlanCuenta;
import com.alantek.caja.modulo.contabilidad.repository.AsientoContableRepository;
import com.alantek.caja.modulo.contabilidad.repository.AsientoDetalleRepository;
import com.alantek.caja.modulo.contabilidad.repository.PeriodoContableRepository;
import com.alantek.caja.modulo.contabilidad.repository.PlanCuentaRepository;
import com.alantek.caja.shared.PageResponse;
import com.alantek.caja.shared.audit.AuditService;
import com.alantek.caja.shared.exception.BusinessException;
import com.alantek.caja.shared.security.CurrentUserService;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AsientoContableService {

    private final AsientoContableRepository asientoRepository;
    private final AsientoDetalleRepository detalleRepository;
    private final PlanCuentaRepository planCuentaRepository;
    private final PeriodoContableRepository periodoRepository;
    private final AsientoAutomaticoService asientoAutomaticoService;
    private final CurrentUserService currentUserService;
    private final AuditService auditService;

    public AsientoContableService(AsientoContableRepository asientoRepository,
                                  AsientoDetalleRepository detalleRepository,
                                  PlanCuentaRepository planCuentaRepository,
                                  PeriodoContableRepository periodoRepository,
                                  AsientoAutomaticoService asientoAutomaticoService,
                                  CurrentUserService currentUserService,
                                  AuditService auditService) {
        this.asientoRepository = asientoRepository;
        this.detalleRepository = detalleRepository;
        this.planCuentaRepository = planCuentaRepository;
        this.periodoRepository = periodoRepository;
        this.asientoAutomaticoService = asientoAutomaticoService;
        this.currentUserService = currentUserService;
        this.auditService = auditService;
    }

    @Transactional
    public AsientoResponse registrarManual(AsientoManualRequest request) {
        BigDecimal totalDebe = BigDecimal.ZERO;
        BigDecimal totalHaber = BigDecimal.ZERO;

        List<AsientoDetalle> detalles = new ArrayList<>();
        for (AsientoManualRequest.DetalleRequest detalleRequest : request.detalles()) {
            BigDecimal debe = detalleRequest.debe() == null ? BigDecimal.ZERO : detalleRequest.debe();
            BigDecimal haber = detalleRequest.haber() == null ? BigDecimal.ZERO : detalleRequest.haber();
            if (debe.signum() == 0 && haber.signum() == 0) {
                continue;
            }
            PlanCuenta cuenta = planCuentaRepository.findById(detalleRequest.cuentaId())
                    .orElseThrow(() -> new BusinessException("Cuenta contable no encontrada: " + detalleRequest.cuentaId()));
            if (Boolean.FALSE.equals(cuenta.getAceptaMovimiento())) {
                throw new BusinessException("La cuenta " + cuenta.getCodigo() + " no acepta movimiento");
            }
            AsientoDetalle detalle = new AsientoDetalle();
            detalle.setCuentaId(detalleRequest.cuentaId());
            detalle.setDebe(debe);
            detalle.setHaber(haber);
            detalles.add(detalle);
            totalDebe = totalDebe.add(debe);
            totalHaber = totalHaber.add(haber);
        }

        if (detalles.isEmpty()) {
            throw new BusinessException("Debe indicar al menos un detalle con monto");
        }
        if (totalDebe.compareTo(totalHaber) != 0) {
            throw new BusinessException("Cuadre contable no válido: Debe=" + totalDebe + " vs Haber=" + totalHaber);
        }

        PeriodoContable periodo = asientoAutomaticoService.obtenerPeriodoAbierto(request.fecha());

        AsientoContable asiento = new AsientoContable();
        asiento.setPeriodoId(periodo.getId());
        asiento.setFecha(request.fecha());
        asiento.setDescripcion(request.descripcion());
        asiento.setOrigen("MANUAL");
        asiento.setCreatedBy(currentUserService.getCurrentUser().map(u -> u.id()).orElse(null));
        asiento.setDetalles(detalles);
        detalles.forEach(detalle -> detalle.setAsiento(asiento));

        AsientoContable saved = asientoRepository.save(asiento);
        auditService.registrar("asiento_contable", saved.getId(), "CREAR", null, request);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public PageResponse<AsientoResponse> libroDiario(LocalDate desde, LocalDate hasta, Pageable pageable) {
        List<AsientoResponse> list = asientoRepository.findByFechaBetweenOrderByFechaAsc(desde, hasta).stream()
                .map(this::toResponse)
                .toList();
        return PageResponse.ofList(list, pageable.getPageNumber(), pageable.getPageSize(),
                Comparator.comparing(AsientoResponse::fecha).thenComparing(AsientoResponse::id));
    }

    @Transactional(readOnly = true)
    public PageResponse<MayorLinea> libroMayor(Long cuentaId, LocalDate desde, LocalDate hasta, Pageable pageable) {
        planCuentaRepository.findById(cuentaId)
                .orElseThrow(() -> new BusinessException("Cuenta contable no encontrada: " + cuentaId));

        List<AsientoContable> asientos = asientoRepository.findByFechaBetweenOrderByFechaAsc(desde, hasta);
        List<MayorLinea> lineas = new ArrayList<>();
        BigDecimal saldo = BigDecimal.ZERO;
        for (AsientoContable asiento : asientos) {
            for (AsientoDetalle detalle : detalleRepository.findByAsiento_Id(asiento.getId())) {
                if (cuentaId.equals(detalle.getCuentaId())) {
                    saldo = saldo.add(detalle.getDebe()).subtract(detalle.getHaber());
                    lineas.add(new MayorLinea(
                            asiento.getFecha(), asiento.getId(), asiento.getDescripcion(),
                            detalle.getDebe(), detalle.getHaber(), saldo));
                }
            }
        }
        return PageResponse.ofList(lineas, pageable.getPageNumber(), pageable.getPageSize(), null);
    }

    @Transactional(readOnly = true)
    public PageResponse<BalanceLinea> balanceComprobacion(Integer anio, Integer mes, Pageable pageable) {
        PeriodoContable periodo = periodoRepository.findByAnioAndMes(anio, mes)
                .orElseThrow(() -> new BusinessException("Período contable no encontrado: " + anio + "-" + mes));

        Map<Long, BigDecimal[]> porCuenta = new LinkedHashMap<>();
        for (AsientoContable asiento : asientoRepository.findByPeriodoIdOrderByFechaAsc(periodo.getId())) {
            for (AsientoDetalle detalle : detalleRepository.findByAsiento_Id(asiento.getId())) {
                BigDecimal[] acum = porCuenta.computeIfAbsent(detalle.getCuentaId(),
                        k -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
                acum[0] = acum[0].add(detalle.getDebe());
                acum[1] = acum[1].add(detalle.getHaber());
            }
        }

        List<BalanceLinea> lineas = porCuenta.entrySet().stream()
                .map(entry -> {
                    PlanCuenta cuenta = planCuentaRepository.findById(entry.getKey()).orElseThrow();
                    return new BalanceLinea(cuenta.getCodigo(), cuenta.getNombre(),
                            entry.getValue()[0], entry.getValue()[1]);
                })
                .sorted(Comparator.comparing(BalanceLinea::cuentaCodigo))
                .toList();
        return PageResponse.ofList(lineas, pageable.getPageNumber(), pageable.getPageSize(), null);
    }

    private AsientoResponse toResponse(AsientoContable asiento) {
        List<AsientoResponse.DetalleResponse> detalles = asiento.getDetalles().stream()
                .map(d -> {
                    PlanCuenta cuenta = planCuentaRepository.findById(d.getCuentaId()).orElse(null);
                    return new AsientoResponse.DetalleResponse(
                            d.getCuentaId(),
                            cuenta != null ? cuenta.getCodigo() : "?",
                            cuenta != null ? cuenta.getNombre() : "?",
                            d.getDebe(), d.getHaber());
                })
                .toList();
        return new AsientoResponse(
                asiento.getId(), asiento.getPeriodoId(), asiento.getComprobanteId(),
                asiento.getFecha(), asiento.getDescripcion(), asiento.getOrigen(),
                asiento.getEstado(), asiento.getCreatedAt(), asiento.getCreatedBy(), detalles);
    }
}

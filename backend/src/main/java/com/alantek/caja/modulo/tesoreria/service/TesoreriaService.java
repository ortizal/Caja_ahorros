package com.alantek.caja.modulo.tesoreria.service;

import com.alantek.caja.modulo.caja.dto.CajaMovimientoRequest;
import com.alantek.caja.modulo.caja.dto.CajaMovimientoResponse;
import com.alantek.caja.modulo.caja.service.CajaService;
import com.alantek.caja.modulo.contabilidad.entity.PlanCuenta;
import com.alantek.caja.modulo.contabilidad.repository.AsientoDetalleRepository;
import com.alantek.caja.modulo.contabilidad.repository.PlanCuentaRepository;
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
import com.alantek.caja.modulo.tesoreria.entity.CuentaPorCobrar;
import com.alantek.caja.modulo.tesoreria.entity.CuentaPorPagar;
import com.alantek.caja.modulo.tesoreria.entity.Gasto;
import com.alantek.caja.modulo.tesoreria.entity.PresupuestoPartida;
import com.alantek.caja.modulo.tesoreria.repository.CuentaPorCobrarRepository;
import com.alantek.caja.modulo.tesoreria.repository.CuentaPorPagarRepository;
import com.alantek.caja.modulo.tesoreria.repository.GastoRepository;
import com.alantek.caja.modulo.tesoreria.repository.PresupuestoPartidaRepository;
import com.alantek.caja.shared.audit.AuditService;
import com.alantek.caja.shared.exception.BusinessException;
import com.alantek.caja.shared.security.CurrentUserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Service
public class TesoreriaService {

    private final GastoRepository gastoRepository;
    private final CuentaPorPagarRepository cxpRepository;
    private final CuentaPorCobrarRepository cxcRepository;
    private final PresupuestoPartidaRepository presupuestoRepository;
    private final PlanCuentaRepository planCuentaRepository;
    private final AsientoDetalleRepository asientoDetalleRepository;
    private final CajaService cajaService;
    private final CurrentUserService currentUserService;
    private final AuditService auditService;

    public TesoreriaService(GastoRepository gastoRepository,
                            CuentaPorPagarRepository cxpRepository,
                            CuentaPorCobrarRepository cxcRepository,
                            PresupuestoPartidaRepository presupuestoRepository,
                            PlanCuentaRepository planCuentaRepository,
                            AsientoDetalleRepository asientoDetalleRepository,
                            CajaService cajaService,
                            CurrentUserService currentUserService,
                            AuditService auditService) {
        this.gastoRepository = gastoRepository;
        this.cxpRepository = cxpRepository;
        this.cxcRepository = cxcRepository;
        this.presupuestoRepository = presupuestoRepository;
        this.planCuentaRepository = planCuentaRepository;
        this.asientoDetalleRepository = asientoDetalleRepository;
        this.cajaService = cajaService;
        this.currentUserService = currentUserService;
        this.auditService = auditService;
    }

    @Transactional
    public GastoResponse crearGasto(GastoRequest request) {
        validarCuenta(request.cuentaContableId());
        Gasto gasto = new Gasto();
        gasto.setConcepto(request.concepto());
        gasto.setDescripcion(request.descripcion());
        gasto.setMonto(request.monto());
        gasto.setCuentaContableId(request.cuentaContableId());
        gasto.setFechaSolicitud(request.fechaSolicitud() != null ? request.fechaSolicitud() : LocalDate.now());
        gasto.setSolicitadoPor(currentUserService.requireUserId());
        gasto.setEstado("PENDIENTE");
        Gasto saved = gastoRepository.save(gasto);
        auditService.registrar("gasto", saved.getId(), "CREAR", null, request);
        return toResponse(saved);
    }

    @Transactional
    public GastoResponse aprobarGasto(Long id, AprobacionGastoRequest request) {
        Gasto gasto = requireGasto(id);
        if (!"PENDIENTE".equals(gasto.getEstado())) {
            throw new BusinessException("Solo un gasto PENDIENTE puede ser evaluado");
        }
        if (Boolean.TRUE.equals(request.aprobar())) {
            gasto.setEstado("APROBADO");
            gasto.setAprobadoPor(currentUserService.requireUserId());
            gasto.setFechaAprobacion(Instant.now());
            gasto.setMotivoRechazo(null);
        } else {
            gasto.setEstado("RECHAZADO");
            gasto.setMotivoRechazo(request.motivoRechazo());
        }
        gasto.setUpdatedAt(Instant.now());
        Gasto saved = gastoRepository.save(gasto);
        auditService.registrar("gasto", id, "EDITAR", "PENDIENTE", gasto.getEstado());
        return toResponse(saved);
    }

    @Transactional
    public GastoResponse pagarGasto(Long id) {
        Gasto gasto = requireGasto(id);
        if (!"APROBADO".equals(gasto.getEstado())) {
            throw new BusinessException("Solo un gasto APROBADO puede ser pagado");
        }
        CajaMovimientoResponse movimiento = registrarMovimientoCaja("GASTO", gasto.getMonto(), "gastos", gasto.getId(), gasto.getConcepto());
        gasto.setEstado("PAGADO");
        gasto.setComprobanteId(movimiento.comprobanteId());
        gasto.setCajaMovimientoId(movimiento.id());
        gasto.setUpdatedAt(Instant.now());
        Gasto saved = gastoRepository.save(gasto);
        auditService.registrar("gasto", id, "EDITAR", "APROBADO", "PAGADO");
        return toResponse(saved);
    }

    @Transactional
    public GastoResponse anularGasto(Long id) {
        Gasto gasto = requireGasto(id);
        if (!"PENDIENTE".equals(gasto.getEstado())) {
            throw new BusinessException("Solo un gasto PENDIENTE puede ser anulado");
        }
        gasto.setEstado("ANULADO");
        gasto.setUpdatedAt(Instant.now());
        Gasto saved = gastoRepository.save(gasto);
        auditService.registrar("gasto", id, "EDITAR", "PENDIENTE", "ANULADO");
        return toResponse(saved);
    }

    public List<GastoResponse> listarGastos(String estado) {
        List<Gasto> gastos = estado == null || estado.isBlank()
                ? gastoRepository.findAllByOrderByCreatedAtDesc()
                : gastoRepository.findByEstadoOrderByCreatedAtDesc(estado.toUpperCase());
        return gastos.stream().map(this::toResponse).toList();
    }

    @Transactional
    public CuentaPorPagarResponse crearCuentaPorPagar(CuentaPorPagarRequest request) {
        validarCuenta(request.cuentaContableId());
        CuentaPorPagar cxp = new CuentaPorPagar();
        cxp.setProveedor(request.proveedor());
        cxp.setConcepto(request.concepto());
        cxp.setMonto(request.monto());
        cxp.setCuentaContableId(request.cuentaContableId());
        cxp.setFechaEmision(request.fechaEmision() != null ? request.fechaEmision() : LocalDate.now());
        cxp.setFechaVencimiento(request.fechaVencimiento());
        cxp.setEstado("PENDIENTE");
        CuentaPorPagar saved = cxpRepository.save(cxp);
        auditService.registrar("cuenta_por_pagar", saved.getId(), "CREAR", null, request);
        return toResponse(saved);
    }

    @Transactional
    public CuentaPorPagarResponse pagarCuentaPorPagar(Long id) {
        CuentaPorPagar cxp = requireCxp(id);
        if (!"PENDIENTE".equals(cxp.getEstado())) {
            throw new BusinessException("Solo una cuenta por pagar PENDIENTE puede ser pagada");
        }
        CajaMovimientoResponse movimiento = registrarMovimientoCaja("GASTO", cxp.getMonto(), "cuentas_por_pagar", cxp.getId(), "Pago a " + cxp.getProveedor());
        cxp.setEstado("PAGADA");
        cxp.setComprobanteId(movimiento.comprobanteId());
        cxp.setCajaMovimientoId(movimiento.id());
        cxp.setUpdatedAt(Instant.now());
        CuentaPorPagar saved = cxpRepository.save(cxp);
        auditService.registrar("cuenta_por_pagar", id, "EDITAR", "PENDIENTE", "PAGADA");
        return toResponse(saved);
    }

    public List<CuentaPorPagarResponse> listarCuentasPorPagar(String estado) {
        List<CuentaPorPagar> cuentas = estado == null || estado.isBlank()
                ? cxpRepository.findAllByOrderByCreatedAtDesc()
                : cxpRepository.findByEstadoOrderByCreatedAtDesc(estado.toUpperCase());
        return cuentas.stream().map(this::toResponse).toList();
    }

    @Transactional
    public CuentaPorCobrarResponse crearCuentaPorCobrar(CuentaPorCobrarRequest request) {
        validarCuenta(request.cuentaContableId());
        CuentaPorCobrar cxc = new CuentaPorCobrar();
        cxc.setSocioId(request.socioId());
        cxc.setDeudor(request.deudor());
        cxc.setConcepto(request.concepto());
        cxc.setMonto(request.monto());
        cxc.setCuentaContableId(request.cuentaContableId());
        cxc.setFechaEmision(request.fechaEmision() != null ? request.fechaEmision() : LocalDate.now());
        cxc.setFechaVencimiento(request.fechaVencimiento());
        cxc.setEstado("PENDIENTE");
        CuentaPorCobrar saved = cxcRepository.save(cxc);
        auditService.registrar("cuenta_por_cobrar", saved.getId(), "CREAR", null, request);
        return toResponse(saved);
    }

    @Transactional
    public CuentaPorCobrarResponse cobrarCuentaPorCobrar(Long id) {
        CuentaPorCobrar cxc = requireCxc(id);
        if (!"PENDIENTE".equals(cxc.getEstado())) {
            throw new BusinessException("Solo una cuenta por cobrar PENDIENTE puede ser cobrada");
        }
        CajaMovimientoResponse movimiento = registrarMovimientoCaja("COBRO_CXC", cxc.getMonto(), "cuentas_por_cobrar", cxc.getId(), "Cobro de " + cxc.getDeudor());
        cxc.setEstado("COBRADA");
        cxc.setComprobanteId(movimiento.comprobanteId());
        cxc.setCajaMovimientoId(movimiento.id());
        cxc.setUpdatedAt(Instant.now());
        CuentaPorCobrar saved = cxcRepository.save(cxc);
        auditService.registrar("cuenta_por_cobrar", id, "EDITAR", "PENDIENTE", "COBRADA");
        return toResponse(saved);
    }

    public List<CuentaPorCobrarResponse> listarCuentasPorCobrar(String estado) {
        List<CuentaPorCobrar> cuentas = estado == null || estado.isBlank()
                ? cxcRepository.findAllByOrderByCreatedAtDesc()
                : cxcRepository.findByEstadoOrderByCreatedAtDesc(estado.toUpperCase());
        return cuentas.stream().map(this::toResponse).toList();
    }

    @Transactional
    public PresupuestoPartidaResponse crearPartidaPresupuesto(PresupuestoPartidaRequest request) {
        validarCuenta(request.cuentaContableId());
        presupuestoRepository.findByAnioAndConcepto(request.anio(), request.concepto())
                .ifPresent(p -> {
                    throw new BusinessException("Ya existe una partida con ese concepto para el anio " + request.anio());
                });
        PresupuestoPartida partida = new PresupuestoPartida();
        partida.setAnio(request.anio());
        partida.setConcepto(request.concepto());
        partida.setCuentaContableId(request.cuentaContableId());
        partida.setMontoPresupuestado(request.montoPresupuestado());
        PresupuestoPartida saved = presupuestoRepository.save(partida);
        auditService.registrar("presupuesto_partida", saved.getId(), "CREAR", null, request);
        return toPartidaResponse(saved);
    }

    public PresupuestoResumenResponse resumenPresupuesto(Integer anio) {
        int anioResumen = anio != null ? anio : LocalDate.now().getYear();
        List<PresupuestoPartida> partidas = presupuestoRepository.findByAnioOrderByConceptoAsc(anioResumen);
        List<PresupuestoPartidaResponse> respuestas = partidas.stream().map(this::toPartidaResponse).toList();
        BigDecimal totalPresupuestado = respuestas.stream()
                .map(PresupuestoPartidaResponse::montoPresupuestado)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalEjecutado = respuestas.stream()
                .map(PresupuestoPartidaResponse::montoEjecutado)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal porcentaje = totalPresupuestado.compareTo(BigDecimal.ZERO) > 0
                ? totalEjecutado.multiply(new BigDecimal("100")).divide(totalPresupuestado, 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        return new PresupuestoResumenResponse(anioResumen, respuestas, totalPresupuestado, totalEjecutado, porcentaje);
    }

    private CajaMovimientoResponse registrarMovimientoCaja(String tipo, BigDecimal monto, String referenciaTabla,
                                                           Long referenciaId, String descripcion) {
        CajaMovimientoRequest request = new CajaMovimientoRequest(tipo, monto, referenciaTabla, referenciaId,
                descripcion, null, null, null);
        return cajaService.registrarMovimiento(request);
    }

    private void validarCuenta(Long cuentaId) {
        PlanCuenta cuenta = planCuentaRepository.findById(cuentaId)
                .orElseThrow(() -> new BusinessException("Cuenta contable no encontrada: " + cuentaId));
        if (!Boolean.TRUE.equals(cuenta.getAceptaMovimiento())) {
            throw new BusinessException("La cuenta contable no acepta movimiento: " + cuentaId);
        }
    }

    private Gasto requireGasto(Long id) {
        return gastoRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Gasto no encontrado: " + id));
    }

    private CuentaPorPagar requireCxp(Long id) {
        return cxpRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Cuenta por pagar no encontrada: " + id));
    }

    private CuentaPorCobrar requireCxc(Long id) {
        return cxcRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Cuenta por cobrar no encontrada: " + id));
    }

    private String codigoCuenta(Long cuentaId) {
        return planCuentaRepository.findById(cuentaId).map(pc -> pc.getCodigo()).orElse(null);
    }

    private GastoResponse toResponse(Gasto gasto) {
        return new GastoResponse(gasto.getId(), gasto.getConcepto(), gasto.getDescripcion(), gasto.getMonto(),
                gasto.getCuentaContableId(), codigoCuenta(gasto.getCuentaContableId()), gasto.getFechaSolicitud(),
                gasto.getSolicitadoPor(), gasto.getEstado(), gasto.getAprobadoPor(), gasto.getFechaAprobacion(),
                gasto.getMotivoRechazo(), gasto.getComprobanteId(), gasto.getCajaMovimientoId());
    }

    private CuentaPorPagarResponse toResponse(CuentaPorPagar cxp) {
        return new CuentaPorPagarResponse(cxp.getId(), cxp.getProveedor(), cxp.getConcepto(), cxp.getMonto(),
                cxp.getCuentaContableId(), codigoCuenta(cxp.getCuentaContableId()), cxp.getFechaEmision(),
                cxp.getFechaVencimiento(), cxp.getEstado(), cxp.getComprobanteId(), cxp.getCajaMovimientoId());
    }

    private CuentaPorCobrarResponse toResponse(CuentaPorCobrar cxc) {
        return new CuentaPorCobrarResponse(cxc.getId(), cxc.getSocioId(), cxc.getDeudor(), cxc.getConcepto(),
                cxc.getMonto(), cxc.getCuentaContableId(), codigoCuenta(cxc.getCuentaContableId()),
                cxc.getFechaEmision(), cxc.getFechaVencimiento(), cxc.getEstado(),
                cxc.getComprobanteId(), cxc.getCajaMovimientoId());
    }

    private PresupuestoPartidaResponse toPartidaResponse(PresupuestoPartida partida) {
        BigDecimal ejecutado = asientoDetalleRepository.sumDebePorCuentaYAnio(partida.getCuentaContableId(), partida.getAnio());
        BigDecimal porcentaje = partida.getMontoPresupuestado().compareTo(BigDecimal.ZERO) > 0
                ? ejecutado.multiply(new BigDecimal("100")).divide(partida.getMontoPresupuestado(), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        return new PresupuestoPartidaResponse(partida.getId(), partida.getAnio(), partida.getConcepto(),
                partida.getCuentaContableId(), codigoCuenta(partida.getCuentaContableId()),
                partida.getMontoPresupuestado(), ejecutado, porcentaje);
    }
}

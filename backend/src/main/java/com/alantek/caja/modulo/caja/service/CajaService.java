package com.alantek.caja.modulo.caja.service;

import com.alantek.caja.modulo.caja.dto.CajaAperturaRequest;
import com.alantek.caja.modulo.caja.dto.CajaAperturaResponse;
import com.alantek.caja.modulo.caja.dto.CajaArqueoRequest;
import com.alantek.caja.modulo.caja.dto.CajaArqueoResponse;
import com.alantek.caja.modulo.caja.dto.CajaMovimientoRequest;
import com.alantek.caja.modulo.caja.dto.CajaMovimientoResponse;
import com.alantek.caja.modulo.caja.dto.SaldoCajaResponse;
import com.alantek.caja.modulo.caja.entity.CajaApertura;
import com.alantek.caja.modulo.caja.entity.CajaArqueo;
import com.alantek.caja.modulo.caja.entity.CajaMovimiento;
import com.alantek.caja.modulo.caja.entity.Comprobante;
import com.alantek.caja.modulo.caja.repository.CajaAperturaRepository;
import com.alantek.caja.modulo.caja.repository.CajaArqueoRepository;
import com.alantek.caja.modulo.caja.repository.CajaMovimientoRepository;
import com.alantek.caja.modulo.caja.repository.ComprobanteRepository;
import com.alantek.caja.modulo.contabilidad.service.AsientoAutomaticoService;
import com.alantek.caja.shared.audit.AuditService;
import com.alantek.caja.shared.exception.BusinessException;
import com.alantek.caja.shared.security.CurrentUserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Service
public class CajaService {

    private final CajaAperturaRepository aperturaRepository;
    private final ComprobanteRepository comprobanteRepository;
    private final CajaMovimientoRepository movimientoRepository;
    private final CajaArqueoRepository arqueoRepository;
    private final AsientoAutomaticoService asientoService;
    private final CurrentUserService currentUserService;
    private final AuditService auditService;

    public CajaService(CajaAperturaRepository aperturaRepository,
                       ComprobanteRepository comprobanteRepository,
                       CajaMovimientoRepository movimientoRepository,
                       CajaArqueoRepository arqueoRepository,
                       AsientoAutomaticoService asientoService,
                       CurrentUserService currentUserService,
                       AuditService auditService) {
        this.aperturaRepository = aperturaRepository;
        this.comprobanteRepository = comprobanteRepository;
        this.movimientoRepository = movimientoRepository;
        this.arqueoRepository = arqueoRepository;
        this.asientoService = asientoService;
        this.currentUserService = currentUserService;
        this.auditService = auditService;
    }

    @Transactional
    public CajaAperturaResponse apertura(CajaAperturaRequest request) {
        Long cajeroId = currentUserService.requireUserId();
        LocalDate fecha = request.fecha() != null ? request.fecha() : LocalDate.now();

        aperturaRepository.findFirstByCajeroIdAndFechaAndEstado(cajeroId, fecha, "ABIERTA")
                .ifPresent(apertura -> {
                    throw new BusinessException("Ya existe una caja ABIERTA para el día de hoy");
                });

        CajaApertura apertura = new CajaApertura();
        apertura.setCajeroId(cajeroId);
        apertura.setFecha(fecha);
        apertura.setSaldoInicial(request.saldoInicial() == null ? BigDecimal.ZERO : request.saldoInicial());

        CajaApertura saved = aperturaRepository.save(apertura);
        auditService.registrar("caja_apertura", saved.getId(), "CREAR", null, request);
        return toResponse(saved);
    }

    @Transactional
    public CajaAperturaResponse cerrar(Long id) {
        CajaApertura apertura = aperturaRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Caja no encontrada: " + id));
        if ("CERRADA".equals(apertura.getEstado())) {
            throw new BusinessException("La caja ya está cerrada");
        }
        apertura.setEstado("CERRADA");
        apertura.setClosedAt(Instant.now());
        aperturaRepository.save(apertura);
        auditService.registrar("caja_apertura", id, "EDITAR", "ABIERTA", "CERRADA");
        return toResponse(apertura);
    }

    @Transactional
    public CajaMovimientoResponse registrarMovimiento(CajaMovimientoRequest request) {
        Long cajeroId = currentUserService.requireUserId();
        LocalDate fecha = LocalDate.now();

        CajaApertura apertura = aperturaRepository
                .findFirstByCajeroIdAndFechaAndEstado(cajeroId, fecha, "ABIERTA")
                .orElseThrow(() -> new BusinessException(
                        "No existe una caja ABIERTA para el día de hoy; realice la apertura primero"));

        TipoMovimiento tipo = TipoMovimiento.from(request.tipo());
        if (tipo == null) {
            throw new BusinessException("Tipo de movimiento no soportado: " + request.tipo());
        }

        Comprobante comprobante = crearComprobante(tipo, request.descripcion());

        CajaMovimiento movimiento = new CajaMovimiento();
        movimiento.setCajaAperturaId(apertura.getId());
        movimiento.setComprobanteId(comprobante.getId());
        movimiento.setTipo(tipo.name());
        movimiento.setMonto(request.monto());
        movimiento.setReferenciaTabla(request.referenciaTabla());
        movimiento.setReferenciaId(request.referenciaId());
        movimiento.setCreatedBy(cajeroId);

        CajaMovimiento saved = movimientoRepository.save(movimiento);

        generarAsiento(tipo, comprobante.getId(), fecha, request);

        auditService.registrar("caja_movimiento", saved.getId(), "CREAR", null, request);
        return toResponse(saved, comprobante);
    }

    public List<CajaMovimientoResponse> listarMovimientos(Long cajaAperturaId) {
        return movimientoRepository.findByCajaAperturaId(cajaAperturaId).stream()
                .map(m -> toResponse(m, comprobanteRepository.findById(m.getComprobanteId()).orElse(null)))
                .toList();
    }

    public List<CajaAperturaResponse> misCajas() {
        return aperturaRepository.findByCajeroIdOrderByOpenedAtDesc(currentUserService.requireUserId()).stream()
                .map(this::toResponse)
                .toList();
    }

    public SaldoCajaResponse saldoCaja(Long cajaAperturaId) {        CajaApertura apertura = aperturaRepository.findById(cajaAperturaId)
                .orElseThrow(() -> new BusinessException("Caja no encontrada: " + cajaAperturaId));

        BigDecimal ingresos = BigDecimal.ZERO;
        BigDecimal egresos = BigDecimal.ZERO;
        for (CajaMovimiento movimiento : movimientoRepository.findByCajaAperturaId(cajaAperturaId)) {
            if (esIngreso(movimiento.getTipo())) {
                ingresos = ingresos.add(movimiento.getMonto());
            } else {
                egresos = egresos.add(movimiento.getMonto());
            }
        }
        BigDecimal saldoActual = apertura.getSaldoInicial().add(ingresos).subtract(egresos);
        return new SaldoCajaResponse(apertura.getId(), apertura.getSaldoInicial(), ingresos, egresos, saldoActual);
    }

    @Transactional
    public CajaArqueoResponse arqueo(Long cajaAperturaId, CajaArqueoRequest request) {
        SaldoCajaResponse saldo = saldoCaja(cajaAperturaId);
        BigDecimal saldoFisico = request.saldoFisico();
        BigDecimal diferencia = saldo.saldoActual().subtract(saldoFisico);

        CajaArqueo arqueo = new CajaArqueo();
        arqueo.setCajaAperturaId(cajaAperturaId);
        arqueo.setSaldoSistema(saldo.saldoActual());
        arqueo.setSaldoFisico(saldoFisico);
        arqueo.setDiferencia(diferencia);
        arqueo.setObservacion(request.observacion());
        arqueo.setRealizadoPor(currentUserService.requireUserId());

        CajaArqueo saved = arqueoRepository.save(arqueo);
        auditService.registrar("caja_arqueo", saved.getId(), "CREAR", null, request);
        return new CajaArqueoResponse(saved.getId(), cajaAperturaId, saldo.saldoActual(),
                saldoFisico, diferencia, request.observacion());
    }

    private void generarAsiento(TipoMovimiento tipo, Long comprobanteId, LocalDate fecha,
                                CajaMovimientoRequest request) {
        String descripcion = request.descripcion() != null ? request.descripcion() : tipo.name();
        switch (tipo) {
            case APORTACION -> asientoService.generarAsientoSimple(
                    "APORTACION", comprobanteId, fecha, request.monto(), descripcion);
            case DEPOSITO -> asientoService.generarAsientoSimple(
                    "DEPOSITO_AHORRO", comprobanteId, fecha, request.monto(), descripcion);
            case RETIRO -> asientoService.generarAsientoSimple(
                    "RETIRO_AHORRO", comprobanteId, fecha, request.monto(), descripcion);
            case DESEMBOLSO -> asientoService.generarAsientoSimple(
                    "DESEMBOLSO_CREDITO", comprobanteId, fecha, request.monto(), descripcion);
            case GASTO -> asientoService.generarAsientoSimple(
                    "GASTO_PAGADO", comprobanteId, fecha, request.monto(), descripcion);
            case COBRO_CXC -> asientoService.generarAsientoSimple(
                    "COBRO_CUENTA", comprobanteId, fecha, request.monto(), descripcion);
            case COBRO_CREDITO -> {
                BigDecimal capital = request.montoCapital() == null ? BigDecimal.ZERO : request.montoCapital();
                BigDecimal interes = request.montoInteres() == null ? BigDecimal.ZERO : request.montoInteres();
                BigDecimal mora = request.montoMora() == null ? BigDecimal.ZERO : request.montoMora();
                if (capital.add(interes).add(mora).compareTo(request.monto()) != 0) {
                    throw new BusinessException("El monto del cobro no coincide con la suma capital+interés+mora");
                }
                asientoService.generarAsiento(descripcion, comprobanteId, fecha, List.of(
                        new AsientoAutomaticoService.ReglaAplicada("PAGO_CAPITAL", capital),
                        new AsientoAutomaticoService.ReglaAplicada("PAGO_INTERES", interes),
                        new AsientoAutomaticoService.ReglaAplicada("PAGO_MORA", mora)));
            }
        }
    }

    private Comprobante crearComprobante(TipoMovimiento tipo, String descripcion) {
        long next = comprobanteRepository.maxId() + 1;
        Comprobante comprobante = new Comprobante();
        comprobante.setNumero("CMP-" + String.format("%08d", next));
        comprobante.setTipo(tipo.isEgreso() ? "EGRESO" : "INGRESO");
        comprobante.setDescripcion(descripcion);
        comprobante.setCreatedBy(currentUserService.getCurrentUser().map(u -> u.id()).orElse(null));
        return comprobanteRepository.save(comprobante);
    }

    private boolean esIngreso(String tipo) {
        TipoMovimiento movimiento = TipoMovimiento.from(tipo);
        return movimiento != null && !movimiento.isEgreso();
    }

    private CajaMovimientoResponse toResponse(CajaMovimiento movimiento, Comprobante comprobante) {
        return new CajaMovimientoResponse(
                movimiento.getId(),
                movimiento.getCajaAperturaId(),
                movimiento.getComprobanteId(),
                comprobante != null ? comprobante.getNumero() : null,
                movimiento.getTipo(),
                movimiento.getMonto(),
                movimiento.getReferenciaTabla(),
                movimiento.getReferenciaId(),
                movimiento.getCreatedAt());
    }

    private CajaAperturaResponse toResponse(CajaApertura apertura) {
        return new CajaAperturaResponse(
                apertura.getId(), apertura.getCajeroId(), apertura.getFecha(),
                apertura.getSaldoInicial(), apertura.getEstado(),
                apertura.getOpenedAt(), apertura.getClosedAt());
    }
}

package com.alantek.caja.modulo.aportaciones.service;

import com.alantek.caja.modulo.aportaciones.dto.AportacionConfigRequest;
import com.alantek.caja.modulo.aportaciones.dto.AportacionConfigResponse;
import com.alantek.caja.modulo.aportaciones.dto.AportacionPagoRequest;
import com.alantek.caja.modulo.aportaciones.dto.AportacionPagoResponse;
import com.alantek.caja.modulo.aportaciones.dto.AportacionResponse;
import com.alantek.caja.modulo.aportaciones.dto.GenerarAportacionesResponse;
import com.alantek.caja.modulo.aportaciones.entity.Aportacion;
import com.alantek.caja.modulo.aportaciones.entity.AportacionConfig;
import com.alantek.caja.modulo.aportaciones.entity.AportacionPago;
import com.alantek.caja.modulo.aportaciones.repository.AportacionConfigRepository;
import com.alantek.caja.modulo.aportaciones.repository.AportacionPagoRepository;
import com.alantek.caja.modulo.aportaciones.repository.AportacionRepository;
import com.alantek.caja.modulo.caja.dto.CajaMovimientoRequest;
import com.alantek.caja.modulo.caja.dto.CajaMovimientoResponse;
import com.alantek.caja.modulo.caja.service.CajaService;
import com.alantek.caja.modulo.socios.entity.Socio;
import com.alantek.caja.modulo.socios.repository.SocioRepository;
import com.alantek.caja.shared.PageResponse;
import com.alantek.caja.shared.audit.AuditService;
import com.alantek.caja.shared.exception.BusinessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class AportacionService {

    private static final Pattern PERIODO = Pattern.compile("\\d{4}-(0[1-9]|1[0-2])");

    private final AportacionConfigRepository configRepository;
    private final AportacionRepository aportacionRepository;
    private final AportacionPagoRepository pagoRepository;
    private final SocioRepository socioRepository;
    private final CajaService cajaService;
    private final AuditService auditService;

    public AportacionService(AportacionConfigRepository configRepository,
                             AportacionRepository aportacionRepository,
                             AportacionPagoRepository pagoRepository,
                             SocioRepository socioRepository,
                             CajaService cajaService,
                             AuditService auditService) {
        this.configRepository = configRepository;
        this.aportacionRepository = aportacionRepository;
        this.pagoRepository = pagoRepository;
        this.socioRepository = socioRepository;
        this.cajaService = cajaService;
        this.auditService = auditService;
    }

    @Transactional
    public AportacionConfigResponse crearConfig(AportacionConfigRequest request) {
        if (request.vigenteHasta() != null && request.vigenteHasta().isBefore(request.vigenteDesde())) {
            throw new BusinessException("La fecha de fin de vigencia no puede ser anterior al inicio");
        }
        AportacionConfig config = new AportacionConfig();
        config.setTipo(request.tipo().toUpperCase());
        config.setModoCalculo(request.modoCalculo().toUpperCase());
        config.setValor(request.valor());
        config.setPeriodicidad(request.periodicidad().toUpperCase());
        config.setMontoMinimo(request.montoMinimo());
        config.setMontoMaximo(request.montoMaximo());
        config.setVigenteDesde(request.vigenteDesde());
        config.setVigenteHasta(request.vigenteHasta());

        AportacionConfig saved = configRepository.save(config);
        auditService.registrar("aportacion_config", saved.getId(), "CREAR", null, request);
        return toResponse(saved);
    }

    public PageResponse<AportacionConfigResponse> listarConfigs(Pageable pageable) {
        Page<AportacionConfig> page = configRepository.findAll(pageable);
        return PageResponse.of(page, this::toResponse);
    }

    @Transactional
    public GenerarAportacionesResponse generarPeriodo(String periodo) {
        if (periodo == null || !PERIODO.matcher(periodo).matches()) {
            throw new BusinessException("El periodo debe tener el formato AAAA-MM");
        }
        AportacionConfig config = configRepository.findTopByOrderByIdDesc()
                .orElseThrow(() -> new BusinessException("No existe configuracion de aportaciones"));
        if (!"FIJO".equals(config.getModoCalculo())) {
            throw new BusinessException("El modo de calculo PORCENTAJE no esta soportado en esta fase");
        }

        List<Socio> socios = socioRepository.findByEstado("ACTIVO");
        int generadas = 0;
        for (Socio socio : socios) {
            if (aportacionRepository.existsBySocioIdAndPeriodo(socio.getId(), periodo)) {
                continue;
            }
            Aportacion aportacion = new Aportacion();
            aportacion.setSocioId(socio.getId());
            aportacion.setConfigId(config.getId());
            aportacion.setPeriodo(periodo);
            aportacion.setMontoEsperado(config.getValor());
            aportacion.setMontoPagado(BigDecimal.ZERO);
            aportacion.setMora(BigDecimal.ZERO);
            aportacion.setEstado("PENDIENTE");
            aportacionRepository.save(aportacion);
            generadas++;
        }
        auditService.registrar("aportaciones", 0L, "GENERAR",
                null, periodo + "|generadas=" + generadas);
        return new GenerarAportacionesResponse(generadas, periodo);
    }

    public PageResponse<AportacionResponse> listarAportaciones(String periodo, Long socioId, Pageable pageable) {
        Page<Aportacion> page;
        if (socioId != null && periodo != null && !periodo.isBlank()) {
            page = aportacionRepository.findByPeriodoAndSocioId(periodo, socioId, pageable);
        } else if (socioId != null) {
            page = aportacionRepository.findBySocioId(socioId, pageable);
        } else if (periodo != null && !periodo.isBlank()) {
            page = aportacionRepository.findByPeriodo(periodo, pageable);
        } else {
            page = aportacionRepository.findAll(pageable);
        }
        return PageResponse.of(page, this::toResponse);
    }

    public PageResponse<AportacionPagoResponse> listarPagos(Long aportacionId, Pageable pageable) {
        Page<AportacionPago> page = pagoRepository.findByAportacionId(aportacionId, pageable);
        return PageResponse.of(page, this::toResponse);
    }

    @Transactional
    public AportacionPagoResponse pagar(Long aportacionId, AportacionPagoRequest request) {
        Aportacion aportacion = aportacionRepository.findById(aportacionId)
                .orElseThrow(() -> new BusinessException("Aportacion no encontrada: " + aportacionId));
        if ("PAGADA".equals(aportacion.getEstado())) {
            throw new BusinessException("La aportacion ya esta pagada");
        }
        Socio socio = socioRepository.findById(aportacion.getSocioId())
                .orElseThrow(() -> new BusinessException("Socio no encontrado: " + aportacion.getSocioId()));
        if (!"ACTIVO".equals(socio.getEstado())) {
            throw new BusinessException("El socio no se encuentra ACTIVO; no se puede registrar el pago");
        }

        AportacionPago pago = new AportacionPago();
        pago.setAportacionId(aportacionId);
        pago.setMonto(request.monto());
        AportacionPago saved = pagoRepository.save(pago);

        CajaMovimientoResponse movimiento = cajaService.registrarMovimiento(new CajaMovimientoRequest(
                "APORTACION", request.monto(), "aportacion_pagos", saved.getId(),
                "Pago de aportacion periodo " + aportacion.getPeriodo(), null, null, null));
        saved.setCajaMovimientoId(movimiento.id());
        pagoRepository.save(saved);

        BigDecimal nuevoPagado = aportacion.getMontoPagado().add(request.monto());
        aportacion.setMontoPagado(nuevoPagado);
        aportacion.setEstado(nuevoPagado.compareTo(aportacion.getMontoEsperado()) >= 0
                ? "PAGADA" : "PARCIAL");
        aportacionRepository.save(aportacion);

        auditService.registrar("aportacion_pagos", saved.getId(), "CREAR", null, request);
        return new AportacionPagoResponse(saved.getId(), aportacionId, saved.getMonto(),
                saved.getCajaMovimientoId(), movimiento.comprobanteNumero(), saved.getPagadoAt());
    }

    private AportacionConfigResponse toResponse(AportacionConfig config) {
        return new AportacionConfigResponse(config.getId(), config.getTipo(), config.getModoCalculo(),
                config.getValor(), config.getPeriodicidad(), config.getMontoMinimo(), config.getMontoMaximo(),
                config.getVigenteDesde(), config.getVigenteHasta());
    }

    private AportacionResponse toResponse(Aportacion aportacion) {
        Socio socio = socioRepository.findById(aportacion.getSocioId()).orElse(null);
        String socioCodigo = socio != null ? socio.getCodigo() : null;
        String socioNombre = socio != null ? socio.getNombres() + " " + socio.getApellidos() : null;
        return new AportacionResponse(aportacion.getId(), aportacion.getSocioId(), socioCodigo, socioNombre,
                aportacion.getConfigId(), aportacion.getPeriodo(), aportacion.getMontoEsperado(),
                aportacion.getMontoPagado(), aportacion.getMora(), aportacion.getEstado());
    }

    private AportacionPagoResponse toResponse(AportacionPago pago) {
        return new AportacionPagoResponse(pago.getId(), pago.getAportacionId(), pago.getMonto(),
                pago.getCajaMovimientoId(), null, pago.getPagadoAt());
    }
}

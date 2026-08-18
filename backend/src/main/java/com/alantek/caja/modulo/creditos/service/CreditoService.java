package com.alantek.caja.modulo.creditos.service;

import com.alantek.caja.modulo.caja.dto.CajaMovimientoRequest;
import com.alantek.caja.modulo.caja.dto.CajaMovimientoResponse;
import com.alantek.caja.modulo.caja.entity.Comprobante;
import com.alantek.caja.modulo.caja.repository.ComprobanteRepository;
import com.alantek.caja.modulo.caja.service.CajaService;
import com.alantek.caja.modulo.creditos.dto.AprobarSolicitudRequest;
import com.alantek.caja.modulo.creditos.dto.CreditoResponse;
import com.alantek.caja.modulo.creditos.dto.CuotaCreditoResponse;
import com.alantek.caja.modulo.creditos.dto.MoraResponse;
import com.alantek.caja.modulo.creditos.dto.PagoCuotaRequest;
import com.alantek.caja.modulo.creditos.dto.PagoCuotaResponse;
import com.alantek.caja.modulo.creditos.dto.ProductoCreditoRequest;
import com.alantek.caja.modulo.creditos.dto.ProductoCreditoResponse;
import com.alantek.caja.modulo.creditos.dto.RefinanciarRequest;
import com.alantek.caja.modulo.creditos.dto.SimulacionCreditoRequest;
import com.alantek.caja.modulo.creditos.dto.SimulacionCreditoResponse;
import com.alantek.caja.modulo.creditos.dto.SolicitudCreditoRequest;
import com.alantek.caja.modulo.creditos.dto.SolicitudCreditoResponse;
import com.alantek.caja.modulo.creditos.entity.Credito;
import com.alantek.caja.modulo.creditos.entity.CreditoEstadoHistorial;
import com.alantek.caja.modulo.creditos.entity.CuotaCredito;
import com.alantek.caja.modulo.creditos.entity.PagoCuota;
import com.alantek.caja.modulo.creditos.entity.ProductoCredito;
import com.alantek.caja.modulo.creditos.entity.SolicitudCredito;
import com.alantek.caja.modulo.creditos.repository.CreditoEstadoHistorialRepository;
import com.alantek.caja.modulo.creditos.repository.CreditoRepository;
import com.alantek.caja.modulo.creditos.repository.CuotaCreditoRepository;
import com.alantek.caja.modulo.creditos.repository.PagoCuotaRepository;
import com.alantek.caja.modulo.creditos.repository.ProductoCreditoRepository;
import com.alantek.caja.modulo.creditos.repository.SolicitudCreditoRepository;
import com.alantek.caja.modulo.socios.entity.Socio;
import com.alantek.caja.modulo.socios.repository.SocioRepository;
import com.alantek.caja.shared.PageResponse;
import com.alantek.caja.shared.audit.AuditService;
import com.alantek.caja.shared.exception.BusinessException;
import com.alantek.caja.shared.security.CurrentUserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class CreditoService {

    private static final BigDecimal CIEN = new BigDecimal("100");
    private static final BigDecimal BASE_ANUAL = new BigDecimal("360");
    private static final int PRECISION = 10;
    private static final List<String> ESTADOS_SOLICITUD_ABIERTA = List.of("PENDIENTE", "EVALUACION", "APROBADA");
    private static final List<String> ESTADOS_CREDITO_VIGENTE = List.of("VIGENTE", "EN_MORA");

    private final ProductoCreditoRepository productoRepository;
    private final SolicitudCreditoRepository solicitudRepository;
    private final CreditoRepository creditoRepository;
    private final CuotaCreditoRepository cuotaRepository;
    private final PagoCuotaRepository pagoRepository;
    private final CreditoEstadoHistorialRepository historialRepository;
    private final SocioRepository socioRepository;
    private final ComprobanteRepository comprobanteRepository;
    private final CajaService cajaService;
    private final AmortizacionService amortizacionService;
    private final CurrentUserService currentUserService;
    private final AuditService auditService;

    public CreditoService(ProductoCreditoRepository productoRepository,
                          SolicitudCreditoRepository solicitudRepository,
                          CreditoRepository creditoRepository,
                          CuotaCreditoRepository cuotaRepository,
                          PagoCuotaRepository pagoRepository,
                          CreditoEstadoHistorialRepository historialRepository,
                          SocioRepository socioRepository,
                          ComprobanteRepository comprobanteRepository,
                          CajaService cajaService,
                          AmortizacionService amortizacionService,
                          CurrentUserService currentUserService,
                          AuditService auditService) {
        this.productoRepository = productoRepository;
        this.solicitudRepository = solicitudRepository;
        this.creditoRepository = creditoRepository;
        this.cuotaRepository = cuotaRepository;
        this.pagoRepository = pagoRepository;
        this.historialRepository = historialRepository;
        this.socioRepository = socioRepository;
        this.comprobanteRepository = comprobanteRepository;
        this.cajaService = cajaService;
        this.amortizacionService = amortizacionService;
        this.currentUserService = currentUserService;
        this.auditService = auditService;
    }

    // ---------------- Productos ----------------

    @Transactional
    public ProductoCreditoResponse crearProducto(ProductoCreditoRequest request) {
        if (request.vigenteHasta() != null && request.vigenteHasta().isBefore(request.vigenteDesde())) {
            throw new BusinessException("La fecha de fin de vigencia no puede ser anterior al inicio");
        }
        if (request.montoMax() != null && request.montoMin() != null
                && request.montoMax().compareTo(request.montoMin()) < 0) {
            throw new BusinessException("El monto maximo no puede ser menor al minimo");
        }
        ProductoCredito producto = new ProductoCredito();
        producto.setNombre(request.nombre());
        producto.setTasaInteres(request.tasaInteres());
        producto.setTasaMora(request.tasaMora() == null ? BigDecimal.ONE : request.tasaMora());
        producto.setSistemaAmortizacion(request.sistemaAmortizacion() == null
                ? "FRANCES" : request.sistemaAmortizacion().toUpperCase());
        producto.setPlazoMaxMeses(request.plazoMaxMeses());
        producto.setMontoMin(request.montoMin() == null ? BigDecimal.ZERO : request.montoMin());
        producto.setMontoMax(request.montoMax());
        producto.setRequiereGarante(request.requiereGarante() != null && request.requiereGarante());
        producto.setVigenteDesde(request.vigenteDesde());
        producto.setVigenteHasta(request.vigenteHasta());
        producto.setActivo(true);

        ProductoCredito saved = productoRepository.save(producto);
        auditService.registrar("producto_credito", saved.getId(), "CREAR", null, request);
        return toResponse(saved);
    }

    public PageResponse<ProductoCreditoResponse> listarProductos(Pageable pageable) {
        Page<ProductoCredito> page = productoRepository.findAllByOrderByActivoDescIdDesc(pageable);
        return PageResponse.of(page, this::toResponse);
    }

    public PageResponse<ProductoCreditoResponse> listarProductosActivos(Pageable pageable) {
        Page<ProductoCredito> page = productoRepository.findByActivoTrueOrderByIdDesc(pageable);
        return PageResponse.of(page, this::toResponse);
    }

    // ---------------- Solicitudes ----------------

    @Transactional
    public SolicitudCreditoResponse crearSolicitud(SolicitudCreditoRequest request) {
        Socio socio = socioRepository.findById(request.socioId())
                .orElseThrow(() -> new BusinessException("Socio no encontrado: " + request.socioId()));
        if (!"ACTIVO".equals(socio.getEstado())) {
            throw new BusinessException("El socio no se encuentra ACTIVO");
        }
        ProductoCredito producto = productoRepository.findById(request.productoId())
                .orElseThrow(() -> new BusinessException("Producto de credito no encontrado: " + request.productoId()));
        if (!producto.isActivo()) {
            throw new BusinessException("El producto de credito no esta activo");
        }
        if (producto.getMontoMax() != null && request.montoSolicitado().compareTo(producto.getMontoMax()) > 0) {
            throw new BusinessException("El monto supera el maximo del producto (" + producto.getMontoMax() + ")");
        }
        if (request.montoSolicitado().compareTo(producto.getMontoMin()) < 0) {
            throw new BusinessException("El monto es menor al minimo del producto (" + producto.getMontoMin() + ")");
        }
        if (request.plazoMeses() > producto.getPlazoMaxMeses()) {
            throw new BusinessException("El plazo supera el maximo del producto ("
                    + producto.getPlazoMaxMeses() + " meses)");
        }
        if (solicitudRepository.existsBySocioIdAndEstadoIn(socio.getId(), ESTADOS_SOLICITUD_ABIERTA)) {
            throw new BusinessException("El socio ya tiene una solicitud en tramite");
        }
        if (creditoRepository.existsBySocioIdAndEstadoIn(socio.getId(), ESTADOS_CREDITO_VIGENTE)) {
            throw new BusinessException("El socio ya tiene un credito vigente");
        }

        SolicitudCredito solicitud = new SolicitudCredito();
        solicitud.setSocioId(socio.getId());
        solicitud.setProductoId(producto.getId());
        solicitud.setMontoSolicitado(request.montoSolicitado());
        solicitud.setPlazoMeses(request.plazoMeses());
        solicitud.setDestino(request.destino());
        solicitud.setEstado("PENDIENTE");
        solicitud.setSolicitadoPor(currentUserService.requireUserId());

        SolicitudCredito saved = solicitudRepository.save(solicitud);
        auditService.registrar("solicitud_credito", saved.getId(), "CREAR", null, request);
        return toResponse(saved);
    }

    public PageResponse<SolicitudCreditoResponse> listarSolicitudes(String estado, Pageable pageable) {
        Page<SolicitudCredito> page = (estado == null || estado.isBlank())
                ? solicitudRepository.findAllByOrderByCreatedAtDesc(pageable)
                : solicitudRepository.findByEstado(estado.toUpperCase(), pageable);
        return PageResponse.of(page, this::toResponse);
    }

    @Transactional
    public SolicitudCreditoResponse evaluar(Long id) {
        SolicitudCredito solicitud = solicitudRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Solicitud no encontrada: " + id));
        if (!"PENDIENTE".equals(solicitud.getEstado())) {
            throw new BusinessException("La solicitud debe estar en estado PENDIENTE para evaluarse");
        }
        if (solicitud.getSolicitadoPor() != null
                && solicitud.getSolicitadoPor().equals(currentUserService.requireUserId())) {
            throw new BusinessException("El solicitante no puede evaluar su propia solicitud");
        }
        solicitud.setEstado("EVALUACION");
        solicitud.setEvaluadoPor(currentUserService.requireUserId());
        solicitud.setUpdatedAt(LocalDateTime.now());
        SolicitudCredito saved = solicitudRepository.save(solicitud);
        auditService.registrar("solicitud_credito", saved.getId(), "EVALUAR", "PENDIENTE", "EVALUACION");
        return toResponse(saved);
    }

    @Transactional
    public SolicitudCreditoResponse aprobar(Long id, AprobarSolicitudRequest request) {
        SolicitudCredito solicitud = solicitudRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Solicitud no encontrada: " + id));
        if (!"EVALUACION".equals(solicitud.getEstado())) {
            throw new BusinessException("La solicitud debe estar en estado EVALUACION para aprobarse o rechazarse");
        }
        if (solicitud.getSolicitadoPor() != null
                && solicitud.getSolicitadoPor().equals(currentUserService.requireUserId())) {
            throw new BusinessException("El solicitante no puede aprobar su propia solicitud");
        }
        if (Boolean.TRUE.equals(request.aprobar())) {
            solicitud.setEstado("APROBADA");
            solicitud.setAprobadoPor(currentUserService.requireUserId());
            solicitud.setMotivoRechazo(null);
            solicitudRepository.save(solicitud);
            crearCredito(solicitud);
        } else {
            if (request.motivoRechazo() == null || request.motivoRechazo().isBlank()) {
                throw new BusinessException("Debe indicar el motivo del rechazo");
            }
            solicitud.setEstado("RECHAZADA");
            solicitud.setMotivoRechazo(request.motivoRechazo());
            solicitudRepository.save(solicitud);
        }
        solicitud.setUpdatedAt(LocalDateTime.now());
        SolicitudCredito saved = solicitudRepository.save(solicitud);
        auditService.registrar("solicitud_credito", saved.getId(), "APROBAR",
                "EVALUACION", saved.getEstado());
        return toResponse(saved);
    }

    private void crearCredito(SolicitudCredito solicitud) {
        ProductoCredito producto = productoRepository.findById(solicitud.getProductoId()).orElseThrow();
        Credito credito = new Credito();
        credito.setSolicitudId(solicitud.getId());
        credito.setSocioId(solicitud.getSocioId());
        credito.setProductoId(producto.getId());
        credito.setMontoDesembolsado(solicitud.getMontoSolicitado());
        credito.setTasaInteres(producto.getTasaInteres());
        credito.setPlazoMeses(solicitud.getPlazoMeses());
        credito.setSaldoCapital(solicitud.getMontoSolicitado());
        credito.setEstado("APROBADA");
        credito.setCreatedBy(currentUserService.requireUserId());
        Credito saved = creditoRepository.save(credito);
        registrarHistorial(saved, null, "APROBADA", "Solicitud aprobada");
    }

    // ---------------- Creditos ----------------

    public PageResponse<CreditoResponse> listarCreditos(Long socioId, Pageable pageable) {
        Page<Credito> page = socioId != null
                ? creditoRepository.findBySocioId(socioId, pageable)
                : creditoRepository.findAllByOrderByCreatedAtDesc(pageable);
        return PageResponse.of(page, this::toResponse);
    }

    public CreditoResponse obtenerCredito(Long id) {
        Credito credito = creditoRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Credito no encontrado: " + id));
        return toResponse(credito);
    }

    @Transactional
    public CreditoResponse desembolsar(Long id) {
        Credito credito = creditoRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Credito no encontrado: " + id));
        if (!"APROBADA".equals(credito.getEstado())) {
            throw new BusinessException("El credito debe estar en estado APROBADA para desembolsarse");
        }
        ProductoCredito producto = productoRepository.findById(credito.getProductoId()).orElseThrow();

        LocalDate fechaPrimeraCuota = LocalDate.now().plusMonths(1);
        List<CuotaCredito> cuotas = new ArrayList<>();
        amortizacionService.generarTabla(credito.getMontoDesembolsado(), credito.getTasaInteres(),
                credito.getPlazoMeses(), producto.getSistemaAmortizacion(), fechaPrimeraCuota)
                .forEach(cuota -> {
                    CuotaCredito fila = new CuotaCredito();
                    fila.setCreditoId(credito.getId());
                    fila.setNumeroCuota(cuota.numero());
                    fila.setFechaVencimiento(cuota.fechaVencimiento());
                    fila.setCapital(cuota.capital());
                    fila.setInteres(cuota.interes());
                    fila.setCuotaTotal(cuota.cuota());
                    fila.setSaldoCapital(cuota.saldo());
                    fila.setMora(BigDecimal.ZERO);
                    fila.setEstado("PENDIENTE");
                    cuotas.add(fila);
                });
        cuotaRepository.saveAll(cuotas);

        CajaMovimientoResponse cajaMovimiento = cajaService.registrarMovimiento(new CajaMovimientoRequest(
                "DESEMBOLSO", credito.getMontoDesembolsado(), "credito", credito.getId(),
                "Desembolso credito #" + credito.getId(), null, null, null));

        credito.setFechaDesembolso(LocalDate.now());
        credito.setEstado("VIGENTE");
        credito.setSaldoCapital(credito.getMontoDesembolsado());
        creditoRepository.save(credito);
        registrarHistorial(credito, "APROBADA", "VIGENTE",
                "Desembolso comprobante " + cajaMovimiento.comprobanteNumero());
        auditService.registrar("credito", credito.getId(), "DESEMBOLSAR",
                "APROBADA", "VIGENTE");
        return toResponse(credito);
    }

    public PageResponse<CuotaCreditoResponse> listarCuotas(Long creditoId, Pageable pageable) {
        Page<CuotaCredito> page = cuotaRepository.findByCreditoIdAndEstadoNot(creditoId, "REFINANCIADA", pageable);
        return PageResponse.of(page, this::toResponse);
    }

    public PageResponse<PagoCuotaResponse> listarPagos(Long creditoId, Pageable pageable) {
        Page<PagoCuota> page = pagoRepository.findByCreditoId(creditoId, pageable);
        return PageResponse.of(page, this::toResponse);
    }

    @Transactional
    public PagoCuotaResponse pagarCuota(PagoCuotaRequest request) {
        CuotaCredito cuota = cuotaRepository.findById(request.cuotaId())
                .orElseThrow(() -> new BusinessException("Cuota no encontrada: " + request.cuotaId()));
        if (!"PENDIENTE".equals(cuota.getEstado()) && !"VENCIDA".equals(cuota.getEstado())) {
            throw new BusinessException("La cuota ya fue pagada o no esta vigente");
        }
        Credito credito = creditoRepository.findById(cuota.getCreditoId())
                .orElseThrow(() -> new BusinessException("Credito no encontrado: " + cuota.getCreditoId()));
        if (!ESTADOS_CREDITO_VIGENTE.contains(credito.getEstado())) {
            throw new BusinessException("El credito no se encuentra vigente");
        }

        BigDecimal capital = request.montoCapital() == null ? cuota.getCapital() : request.montoCapital();
        BigDecimal interes = request.montoInteres() == null ? cuota.getInteres() : request.montoInteres();
        BigDecimal mora = request.montoMora() == null ? cuota.getMora() : request.montoMora();
        if (capital.signum() < 0 || interes.signum() < 0 || mora.signum() < 0) {
            throw new BusinessException("Los montos del pago no pueden ser negativos");
        }
        if (capital.compareTo(cuota.getCapital()) != 0
                || interes.compareTo(cuota.getInteres()) != 0
                || mora.compareTo(cuota.getMora()) != 0) {
            throw new BusinessException("El pago debe cubrir la cuota completa (capital + interes + mora)");
        }

        BigDecimal total = capital.add(interes).add(mora);
        CajaMovimientoResponse cajaMovimiento = cajaService.registrarMovimiento(new CajaMovimientoRequest(
                "COBRO_CREDITO", total, "credito", credito.getId(),
                "Pago cuota " + cuota.getNumeroCuota() + " credito #" + credito.getId(),
                capital, interes, mora));

        PagoCuota pago = new PagoCuota();
        pago.setCuotaId(cuota.getId());
        pago.setCreditoId(credito.getId());
        pago.setMontoCapital(capital);
        pago.setMontoInteres(interes);
        pago.setMontoMora(mora);
        pago.setComprobanteId(cajaMovimiento.comprobanteId());
        pago.setRegistradoPor(currentUserService.requireUserId());
        PagoCuota saved = pagoRepository.save(pago);

        cuota.setEstado("PAGADA");
        cuotaRepository.save(cuota);

        BigDecimal nuevoSaldo = credito.getSaldoCapital().subtract(capital);
        credito.setSaldoCapital(nuevoSaldo);
        long pendientes = cuotaRepository.countByCreditoIdAndEstado(credito.getId(), "PENDIENTE")
                + cuotaRepository.countByCreditoIdAndEstado(credito.getId(), "VENCIDA");
        if (pendientes == 0) {
            credito.setEstado("CANCELADO");
            registrarHistorial(credito, "VIGENTE", "CANCELADO", "Todas las cuotas pagadas");
        }
        creditoRepository.save(credito);
        auditService.registrar("pago_cuota", saved.getId(), "CREAR", null, request);
        return toResponse(saved);
    }

    @Transactional
    public MoraResponse procesarVencidas() {
        int cuotasMarcadas = 0;
        BigDecimal moraTotal = BigDecimal.ZERO;
        int creditosEnMora = 0;
        LocalDate hoy = LocalDate.now();
        List<Credito> creditos = creditoRepository.findByEstadoOrderByCreatedAtDesc("VIGENTE");
        for (Credito credito : creditos) {
            ProductoCredito producto = productoRepository.findById(credito.getProductoId()).orElse(null);
            if (producto == null) {
                continue;
            }
            boolean marco = false;
            for (CuotaCredito cuota : cuotaRepository.findByCreditoIdOrderByNumeroCuotaAsc(credito.getId())) {
                if ("PENDIENTE".equals(cuota.getEstado())
                        && cuota.getFechaVencimiento().isBefore(hoy)) {
                    long dias = java.time.temporal.ChronoUnit.DAYS.between(cuota.getFechaVencimiento(), hoy);
                    BigDecimal mora = cuota.getCapital()
                            .multiply(producto.getTasaMora())
                            .divide(CIEN, PRECISION, RoundingMode.HALF_UP)
                            .multiply(new BigDecimal(dias))
                            .divide(BASE_ANUAL, 2, RoundingMode.HALF_UP);
                    cuota.setEstado("VENCIDA");
                    cuota.setMora(cuota.getMora().add(mora));
                    cuotaRepository.save(cuota);
                    cuotasMarcadas++;
                    moraTotal = moraTotal.add(mora);
                    marco = true;
                }
            }
            if (marco) {
                credito.setEstado("EN_MORA");
                creditoRepository.save(credito);
                registrarHistorial(credito, "VIGENTE", "EN_MORA", "Cuotas vencidas");
                creditosEnMora++;
            }
        }
        return new MoraResponse(cuotasMarcadas, moraTotal, creditosEnMora);
    }

    @Transactional
    public CreditoResponse refinanciar(Long id, RefinanciarRequest request) {
        Credito credito = creditoRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Credito no encontrado: " + id));
        if (!ESTADOS_CREDITO_VIGENTE.contains(credito.getEstado())) {
            throw new BusinessException("El credito debe estar VIGENTE o EN_MORA para refinanciarse");
        }
        ProductoCredito producto = productoRepository.findById(credito.getProductoId()).orElseThrow();

        List<CuotaCredito> cuotasExistentes = cuotaRepository.findByCreditoIdOrderByNumeroCuotaAsc(credito.getId());
        int maxNumero = cuotasExistentes.stream()
                .mapToInt(CuotaCredito::getNumeroCuota)
                .max()
                .orElse(0);
        for (CuotaCredito cuota : cuotasExistentes) {
            if ("PENDIENTE".equals(cuota.getEstado()) || "VENCIDA".equals(cuota.getEstado())) {
                cuota.setEstado("REFINANCIADA");
                cuotaRepository.save(cuota);
            }
        }

        BigDecimal saldo = credito.getSaldoCapital();
        LocalDate fechaPrimeraCuota = LocalDate.now().plusMonths(1);
        amortizacionService.generarTabla(saldo, request.tasaInteres(), request.plazoMeses(),
                producto.getSistemaAmortizacion(), fechaPrimeraCuota)
                .forEach(cuota -> {
                    CuotaCredito fila = new CuotaCredito();
                    fila.setCreditoId(credito.getId());
                    fila.setNumeroCuota(maxNumero + cuota.numero());
                    fila.setFechaVencimiento(cuota.fechaVencimiento());
                    fila.setCapital(cuota.capital());
                    fila.setInteres(cuota.interes());
                    fila.setCuotaTotal(cuota.cuota());
                    fila.setSaldoCapital(cuota.saldo());
                    fila.setMora(BigDecimal.ZERO);
                    fila.setEstado("PENDIENTE");
                    cuotaRepository.save(fila);
                });

        credito.setTasaInteres(request.tasaInteres());
        credito.setPlazoMeses(request.plazoMeses());
        credito.setSaldoCapital(saldo);
        credito.setEstado("VIGENTE");
        creditoRepository.save(credito);
        registrarHistorial(credito, "EN_MORA", "VIGENTE", "Refinanciamiento a " + request.plazoMeses() + " meses");
        auditService.registrar("credito", credito.getId(), "REFINANCIAR", null, request);
        return toResponse(credito);
    }

    // ---------------- Simulador ----------------

    public SimulacionCreditoResponse simular(SimulacionCreditoRequest request) {
        return amortizacionService.simular(request.monto(), request.tasaInteres(),
                request.plazoMeses(), request.sistemaAmortizacion());
    }

    // ---------------- Helpers ----------------

    private void registrarHistorial(Credito credito, String anterior, String nuevo, String motivo) {
        CreditoEstadoHistorial historial = new CreditoEstadoHistorial();
        historial.setCreditoId(credito.getId());
        historial.setEstadoAnterior(anterior);
        historial.setEstadoNuevo(nuevo);
        historial.setMotivo(motivo);
        historial.setChangedBy(currentUserService.getCurrentUser().map(u -> u.id()).orElse(null));
        historialRepository.save(historial);
    }

    private ProductoCreditoResponse toResponse(ProductoCredito producto) {
        return new ProductoCreditoResponse(producto.getId(), producto.getNombre(), producto.getTasaInteres(),
                producto.getTasaMora(), producto.getSistemaAmortizacion(), producto.getPlazoMaxMeses(),
                producto.getMontoMin(), producto.getMontoMax(), producto.isRequiereGarante(),
                producto.getVigenteDesde(), producto.getVigenteHasta(), producto.isActivo());
    }

    private SolicitudCreditoResponse toResponse(SolicitudCredito solicitud) {
        Socio socio = socioRepository.findById(solicitud.getSocioId()).orElse(null);
        ProductoCredito producto = productoRepository.findById(solicitud.getProductoId()).orElse(null);
        return new SolicitudCreditoResponse(solicitud.getId(), solicitud.getSocioId(),
                socio != null ? socio.getCodigo() : null,
                socio != null ? socio.getNombres() + " " + socio.getApellidos() : null,
                solicitud.getProductoId(),
                producto != null ? producto.getNombre() : null,
                solicitud.getMontoSolicitado(), solicitud.getPlazoMeses(), solicitud.getDestino(),
                solicitud.getEstado(), solicitud.getSolicitadoPor(), solicitud.getEvaluadoPor(),
                solicitud.getAprobadoPor(), solicitud.getMotivoRechazo(), solicitud.getCreatedAt());
    }

    private CreditoResponse toResponse(Credito credito) {
        Socio socio = socioRepository.findById(credito.getSocioId()).orElse(null);
        ProductoCredito producto = productoRepository.findById(credito.getProductoId()).orElse(null);
        long pendientes = cuotaRepository.countByCreditoIdAndEstado(credito.getId(), "PENDIENTE")
                + cuotaRepository.countByCreditoIdAndEstado(credito.getId(), "VENCIDA");
        return new CreditoResponse(credito.getId(), credito.getSolicitudId(), credito.getSocioId(),
                socio != null ? socio.getCodigo() : null,
                socio != null ? socio.getNombres() + " " + socio.getApellidos() : null,
                credito.getProductoId(),
                producto != null ? producto.getNombre() : null,
                credito.getMontoDesembolsado(), credito.getTasaInteres(), credito.getPlazoMeses(),
                credito.getFechaDesembolso(), credito.getSaldoCapital(), credito.getEstado(),
                (int) pendientes, credito.getCreatedAt());
    }

    private CuotaCreditoResponse toResponse(CuotaCredito cuota) {
        return new CuotaCreditoResponse(cuota.getId(), cuota.getCreditoId(), cuota.getNumeroCuota(),
                cuota.getFechaVencimiento(), cuota.getCapital(), cuota.getInteres(), cuota.getCuotaTotal(),
                cuota.getSaldoCapital(), cuota.getMora(), cuota.getEstado());
    }

    private PagoCuotaResponse toResponse(PagoCuota pago) {
        Comprobante comprobante = pago.getComprobanteId() != null
                ? comprobanteRepository.findById(pago.getComprobanteId()).orElse(null)
                : null;
        return new PagoCuotaResponse(pago.getId(), pago.getCuotaId(), pago.getCreditoId(),
                pago.getMontoCapital(), pago.getMontoInteres(), pago.getMontoMora(),
                pago.getComprobanteId(),
                comprobante != null ? comprobante.getNumero() : null,
                pago.getPagadoAt());
    }
}

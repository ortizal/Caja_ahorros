package com.alantek.caja.modulo.ahorros.service;

import com.alantek.caja.modulo.ahorros.dto.CapitalizacionResponse;
import com.alantek.caja.modulo.ahorros.dto.CuentaAhorroRequest;
import com.alantek.caja.modulo.ahorros.dto.CuentaAhorroResponse;
import com.alantek.caja.modulo.ahorros.dto.MovimientoAhorroRequest;
import com.alantek.caja.modulo.ahorros.dto.MovimientoAhorroResponse;
import com.alantek.caja.modulo.ahorros.dto.ProductoAhorroRequest;
import com.alantek.caja.modulo.ahorros.dto.ProductoAhorroResponse;
import com.alantek.caja.modulo.ahorros.entity.CuentaAhorro;
import com.alantek.caja.modulo.ahorros.entity.MovimientoAhorro;
import com.alantek.caja.modulo.ahorros.entity.ProductoAhorro;
import com.alantek.caja.modulo.ahorros.repository.CuentaAhorroRepository;
import com.alantek.caja.modulo.ahorros.repository.MovimientoAhorroRepository;
import com.alantek.caja.modulo.ahorros.repository.ProductoAhorroRepository;
import com.alantek.caja.modulo.caja.dto.CajaMovimientoRequest;
import com.alantek.caja.modulo.caja.dto.CajaMovimientoResponse;
import com.alantek.caja.modulo.caja.entity.Comprobante;
import com.alantek.caja.modulo.caja.repository.ComprobanteRepository;
import com.alantek.caja.modulo.caja.service.CajaService;
import com.alantek.caja.modulo.contabilidad.service.AsientoAutomaticoService;
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
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Service
public class AhorroService {

    private static final BigDecimal BASE_ANUAL = new BigDecimal("360");

    private final ProductoAhorroRepository productoRepository;
    private final CuentaAhorroRepository cuentaRepository;
    private final MovimientoAhorroRepository movimientoRepository;
    private final SocioRepository socioRepository;
    private final ComprobanteRepository comprobanteRepository;
    private final CajaService cajaService;
    private final AsientoAutomaticoService asientoService;
    private final AuditService auditService;

    public AhorroService(ProductoAhorroRepository productoRepository,
                         CuentaAhorroRepository cuentaRepository,
                         MovimientoAhorroRepository movimientoRepository,
                         SocioRepository socioRepository,
                         ComprobanteRepository comprobanteRepository,
                         CajaService cajaService,
                         AsientoAutomaticoService asientoService,
                         AuditService auditService) {
        this.productoRepository = productoRepository;
        this.cuentaRepository = cuentaRepository;
        this.movimientoRepository = movimientoRepository;
        this.socioRepository = socioRepository;
        this.comprobanteRepository = comprobanteRepository;
        this.cajaService = cajaService;
        this.asientoService = asientoService;
        this.auditService = auditService;
    }

    @Transactional
    public ProductoAhorroResponse crearProducto(ProductoAhorroRequest request) {
        if (request.vigenteHasta() != null && request.vigenteHasta().isBefore(request.vigenteDesde())) {
            throw new BusinessException("La fecha de fin de vigencia no puede ser anterior al inicio");
        }
        ProductoAhorro producto = new ProductoAhorro();
        producto.setNombre(request.nombre());
        producto.setTasaInteres(request.tasaInteres());
        producto.setPeriodicidadCapitalizacion(request.periodicidadCapitalizacion().toUpperCase());
        producto.setSaldoMinimo(request.saldoMinimo() == null ? BigDecimal.ZERO : request.saldoMinimo());
        producto.setLimiteRetirosMes(request.limiteRetirosMes());
        producto.setVigenteDesde(request.vigenteDesde());
        producto.setVigenteHasta(request.vigenteHasta());
        producto.setActivo(true);

        ProductoAhorro saved = productoRepository.save(producto);
        auditService.registrar("producto_ahorro", saved.getId(), "CREAR", null, request);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductoAhorroResponse> listarProductos(Pageable pageable) {
        Page<ProductoAhorro> page = productoRepository.findAllByOrderByActivoDescIdDesc(pageable);
        return PageResponse.of(page, this::toResponse);
    }

    @Transactional(readOnly = true)
    public PageResponse<CuentaAhorroResponse> listarCuentas(Long socioId, Pageable pageable) {
        Page<CuentaAhorro> page = (socioId != null)
                ? cuentaRepository.findBySocioIdOrderByFechaAperturaDesc(socioId, pageable)
                : cuentaRepository.findAllByOrderByFechaAperturaDesc(pageable);
        return PageResponse.of(page, this::toResponse);
    }

    @Transactional
    public CuentaAhorroResponse aperturar(CuentaAhorroRequest request) {
        Socio socio = socioRepository.findById(request.socioId())
                .orElseThrow(() -> new BusinessException("Socio no encontrado: " + request.socioId()));
        if (!"ACTIVO".equals(socio.getEstado())) {
            throw new BusinessException("El socio no se encuentra ACTIVO");
        }
        ProductoAhorro producto = productoRepository.findById(request.productoId())
                .orElseThrow(() -> new BusinessException("Producto de ahorro no encontrado: " + request.productoId()));
        if (!producto.isActivo()) {
            throw new BusinessException("El producto de ahorro no esta activo");
        }

        CuentaAhorro cuenta = new CuentaAhorro();
        cuenta.setSocioId(socio.getId());
        cuenta.setProductoId(producto.getId());
        cuenta.setNumeroCuenta("AH-" + String.format("%07d", cuentaRepository.maxId() + 1));
        cuenta.setSaldo(BigDecimal.ZERO);
        cuenta.setEstado("ACTIVA");
        cuenta.setFechaApertura(LocalDate.now());

        CuentaAhorro saved = cuentaRepository.save(cuenta);
        auditService.registrar("cuenta_ahorro", saved.getId(), "CREAR", null, request);
        return toResponse(saved);
    }

    @Transactional
    public MovimientoAhorroResponse depositar(Long cuentaId, MovimientoAhorroRequest request) {
        CuentaAhorro cuenta = cuentaActiva(cuentaId);
        BigDecimal nuevoSaldo = cuenta.getSaldo().add(request.monto());

        CajaMovimientoResponse cajaMovimiento = cajaService.registrarMovimiento(new CajaMovimientoRequest(
                "DEPOSITO", request.monto(), "cuenta_ahorro", cuentaId,
                "Deposito ahorro " + cuenta.getNumeroCuenta(), null, null, null));

        MovimientoAhorro movimiento = registrarMovimiento(cuenta, "DEPOSITO", request.monto(),
                nuevoSaldo, cajaMovimiento.comprobanteId(), YearMonth.now().toString());
        cuenta.setSaldo(nuevoSaldo);
        cuentaRepository.save(cuenta);
        return toResponse(movimiento);
    }

    @Transactional
    public MovimientoAhorroResponse retirar(Long cuentaId, MovimientoAhorroRequest request) {
        CuentaAhorro cuenta = cuentaActiva(cuentaId);
        ProductoAhorro producto = productoRepository.findById(cuenta.getProductoId()).orElseThrow();

        BigDecimal nuevoSaldo = cuenta.getSaldo().subtract(request.monto());
        if (nuevoSaldo.compareTo(producto.getSaldoMinimo()) < 0) {
            throw new BusinessException("El retiro no puede dejar un saldo menor al minimo del producto ("
                    + producto.getSaldoMinimo() + ")");
        }
        if (producto.getLimiteRetirosMes() != null) {
            String mesActual = YearMonth.now().toString();
            long retirosMes = movimientoRepository.findByCuentaIdOrderByCreatedAtAsc(cuentaId).stream()
                    .filter(m -> "RETIRO".equals(m.getTipo()))
                    .filter(m -> "EJECUTADO".equals(m.getEstado()))
                    .filter(m -> mesActual.equals(m.getPeriodo()))
                    .count();
            if (retirosMes >= producto.getLimiteRetirosMes()) {
                throw new BusinessException("Se alcanzo el limite mensual de retiros del producto");
            }
        }

        CajaMovimientoResponse cajaMovimiento = cajaService.registrarMovimiento(new CajaMovimientoRequest(
                "RETIRO", request.monto(), "cuenta_ahorro", cuentaId,
                "Retiro ahorro " + cuenta.getNumeroCuenta(), null, null, null));

        MovimientoAhorro movimiento = registrarMovimiento(cuenta, "RETIRO", request.monto(),
                nuevoSaldo, cajaMovimiento.comprobanteId(), YearMonth.now().toString());
        cuenta.setSaldo(nuevoSaldo);
        cuentaRepository.save(cuenta);
        return toResponse(movimiento);
    }

    @Transactional(readOnly = true)
    public PageResponse<MovimientoAhorroResponse> listarMovimientos(Long cuentaId, Pageable pageable) {
        Page<MovimientoAhorro> page = movimientoRepository.findByCuentaIdOrderByCreatedAtAsc(cuentaId, pageable);
        return PageResponse.of(page, this::toResponse);
    }

    @Transactional
    public CapitalizacionResponse capitalizar(int anio, int mes) {
        YearMonth periodo = YearMonth.of(anio, mes);
        String periodoStr = periodo.toString();
        if (movimientoRepository.existsByTipoAndEstadoAndPeriodo("INTERES", "EJECUTADO", periodoStr)) {
            throw new BusinessException("La capitalizacion de " + periodoStr + " ya fue realizada");
        }

        LocalDate fechaCapitalizacion = periodo.atEndOfMonth();
        int dias = periodo.lengthOfMonth();
        List<CuentaAhorro> cuentas = cuentaRepository.findByEstadoOrderByFechaAperturaDesc("ACTIVA");

        int capitalizadas = 0;
        BigDecimal totalInteres = BigDecimal.ZERO;
        for (CuentaAhorro cuenta : cuentas) {
            ProductoAhorro producto = productoRepository.findById(cuenta.getProductoId()).orElse(null);
            if (producto == null || !producto.isActivo() || cuenta.getSaldo().signum() <= 0) {
                continue;
            }
            BigDecimal interes = cuenta.getSaldo()
                    .multiply(producto.getTasaInteres())
                    .divide(new BigDecimal("100"), 10, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal(dias))
                    .divide(BASE_ANUAL, 2, RoundingMode.HALF_UP);
            if (interes.signum() == 0) {
                continue;
            }
            BigDecimal nuevoSaldo = cuenta.getSaldo().add(interes);
            MovimientoAhorro movimiento = registrarMovimiento(cuenta, "INTERES", interes,
                    nuevoSaldo, null, periodoStr);
            cuenta.setSaldo(nuevoSaldo);
            cuentaRepository.save(cuenta);

            asientoService.generarAsientoSimple("INTERES_AHORRO", null, fechaCapitalizacion, interes,
                    "Capitalizacion de intereses " + periodoStr + " cuenta " + cuenta.getNumeroCuenta());
            auditService.registrar("movimiento_ahorro", movimiento.getId(), "CAPITALIZAR", null, interes);
            capitalizadas++;
            totalInteres = totalInteres.add(interes);
        }

        return new CapitalizacionResponse(anio, mes, capitalizadas, totalInteres);
    }

    private CuentaAhorro cuentaActiva(Long cuentaId) {
        CuentaAhorro cuenta = cuentaRepository.findById(cuentaId)
                .orElseThrow(() -> new BusinessException("Cuenta de ahorro no encontrada: " + cuentaId));
        if (!"ACTIVA".equals(cuenta.getEstado())) {
            throw new BusinessException("La cuenta de ahorro no esta ACTIVA");
        }
        return cuenta;
    }

    private MovimientoAhorro registrarMovimiento(CuentaAhorro cuenta, String tipo, BigDecimal monto,
                                                 BigDecimal saldoResultante, Long comprobanteId) {
        return registrarMovimiento(cuenta, tipo, monto, saldoResultante, comprobanteId, null);
    }

    private MovimientoAhorro registrarMovimiento(CuentaAhorro cuenta, String tipo, BigDecimal monto,
                                                 BigDecimal saldoResultante, Long comprobanteId,
                                                 String periodo) {
        MovimientoAhorro movimiento = new MovimientoAhorro();
        movimiento.setCuentaId(cuenta.getId());
        movimiento.setTipo(tipo);
        movimiento.setMonto(monto);
        movimiento.setSaldoResultante(saldoResultante);
        movimiento.setComprobanteId(comprobanteId);
        movimiento.setPeriodo(periodo);
        movimiento.setEstado("EJECUTADO");
        return movimientoRepository.save(movimiento);
    }

    private ProductoAhorroResponse toResponse(ProductoAhorro producto) {
        return new ProductoAhorroResponse(producto.getId(), producto.getNombre(), producto.getTasaInteres(),
                producto.getPeriodicidadCapitalizacion(), producto.getSaldoMinimo(), producto.getLimiteRetirosMes(),
                producto.getVigenteDesde(), producto.getVigenteHasta(), producto.isActivo());
    }

    private CuentaAhorroResponse toResponse(CuentaAhorro cuenta) {
        Socio socio = socioRepository.findById(cuenta.getSocioId()).orElse(null);
        ProductoAhorro producto = productoRepository.findById(cuenta.getProductoId()).orElse(null);
        return new CuentaAhorroResponse(cuenta.getId(), cuenta.getSocioId(),
                socio != null ? socio.getCodigo() : null,
                socio != null ? socio.getNombres() + " " + socio.getApellidos() : null,
                cuenta.getProductoId(),
                producto != null ? producto.getNombre() : null,
                cuenta.getNumeroCuenta(), cuenta.getSaldo(), cuenta.getEstado(),
                cuenta.getFechaApertura(), cuenta.getFechaCierre());
    }

    private MovimientoAhorroResponse toResponse(MovimientoAhorro movimiento) {
        Comprobante comprobante = movimiento.getComprobanteId() != null
                ? comprobanteRepository.findById(movimiento.getComprobanteId()).orElse(null)
                : null;
        return new MovimientoAhorroResponse(movimiento.getId(), movimiento.getCuentaId(),
                movimiento.getTipo(), movimiento.getMonto(), movimiento.getSaldoResultante(),
                movimiento.getComprobanteId(),
                comprobante != null ? comprobante.getNumero() : null,
                movimiento.getEstado(), movimiento.getCreatedAt());
    }
}

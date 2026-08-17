package com.alantek.caja.modulo.cartera.service;

import com.alantek.caja.modulo.bancos.repository.CuentaBancariaRepository;
import com.alantek.caja.modulo.caja.entity.CajaApertura;
import com.alantek.caja.modulo.caja.entity.CajaMovimiento;
import com.alantek.caja.modulo.caja.repository.CajaAperturaRepository;
import com.alantek.caja.modulo.caja.repository.CajaMovimientoRepository;
import com.alantek.caja.modulo.caja.service.TipoMovimiento;
import com.alantek.caja.modulo.cartera.dto.CarteraItemResponse;
import com.alantek.caja.modulo.cartera.dto.DashboardGraficosResponse;
import com.alantek.caja.modulo.cartera.dto.DashboardResumenResponse;
import com.alantek.caja.modulo.cartera.dto.MorosidadResponse;
import com.alantek.caja.modulo.creditos.entity.Credito;
import com.alantek.caja.modulo.creditos.entity.CuotaCredito;
import com.alantek.caja.modulo.creditos.entity.PagoCuota;
import com.alantek.caja.modulo.creditos.entity.ProductoCredito;
import com.alantek.caja.modulo.creditos.repository.CreditoRepository;
import com.alantek.caja.modulo.creditos.repository.CuotaCreditoRepository;
import com.alantek.caja.modulo.creditos.repository.PagoCuotaRepository;
import com.alantek.caja.modulo.creditos.repository.ProductoCreditoRepository;
import com.alantek.caja.modulo.socios.entity.Socio;
import com.alantek.caja.modulo.socios.repository.SocioRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class CarteraService {

    private static final List<String> ESTADOS_EN_CARTERA = List.of("PENDIENTE", "VENCIDA");

    private final CuotaCreditoRepository cuotaRepository;
    private final CreditoRepository creditoRepository;
    private final ProductoCreditoRepository productoRepository;
    private final PagoCuotaRepository pagoCuotaRepository;
    private final SocioRepository socioRepository;
    private final CuentaBancariaRepository cuentaBancariaRepository;
    private final CajaAperturaRepository cajaAperturaRepository;
    private final CajaMovimientoRepository cajaMovimientoRepository;

    public CarteraService(CuotaCreditoRepository cuotaRepository,
                          CreditoRepository creditoRepository,
                          ProductoCreditoRepository productoRepository,
                          PagoCuotaRepository pagoCuotaRepository,
                          SocioRepository socioRepository,
                          CuentaBancariaRepository cuentaBancariaRepository,
                          CajaAperturaRepository cajaAperturaRepository,
                          CajaMovimientoRepository cajaMovimientoRepository) {
        this.cuotaRepository = cuotaRepository;
        this.creditoRepository = creditoRepository;
        this.productoRepository = productoRepository;
        this.pagoCuotaRepository = pagoCuotaRepository;
        this.socioRepository = socioRepository;
        this.cuentaBancariaRepository = cuentaBancariaRepository;
        this.cajaAperturaRepository = cajaAperturaRepository;
        this.cajaMovimientoRepository = cajaMovimientoRepository;
    }

    public List<CarteraItemResponse> listarCartera(String estado, Long socioId) {
        List<CuotaCredito> cuotas = cuotaRepository.findByEstadoInOrderByFechaVencimientoAsc(ESTADOS_EN_CARTERA);
        if (estado != null && !estado.isBlank()) {
            cuotas = cuotas.stream().filter(c -> estado.equalsIgnoreCase(c.getEstado())).collect(Collectors.toList());
        }

        Set<Long> creditoIds = cuotas.stream().map(CuotaCredito::getCreditoId).collect(Collectors.toSet());
        Map<Long, Credito> creditos = creditoRepository.findAllById(creditoIds).stream()
                .collect(Collectors.toMap(Credito::getId, Function.identity()));
        Map<Long, ProductoCredito> productos = productoRepository.findAllById(
                        creditos.values().stream().map(Credito::getProductoId).collect(Collectors.toSet())).stream()
                .collect(Collectors.toMap(ProductoCredito::getId, Function.identity()));
        Map<Long, Socio> socios = socioRepository.findAllById(
                        creditos.values().stream().map(Credito::getSocioId).collect(Collectors.toSet())).stream()
                .collect(Collectors.toMap(Socio::getId, Function.identity()));

        LocalDate hoy = LocalDate.now();
        return cuotas.stream()
                .filter(c -> socioId == null || Objects.equals(creditos.get(c.getCreditoId()).getSocioId(), socioId))
                .map(cuota -> {
                    Credito credito = creditos.get(cuota.getCreditoId());
                    Socio socio = socios.get(credito.getSocioId());
                    ProductoCredito producto = productos.get(credito.getProductoId());
                    long diasVencido = calcularDiasVencido(cuota, hoy);
                    return new CarteraItemResponse(
                            cuota.getId(),
                            cuota.getCreditoId(),
                            credito.getSocioId(),
                            socio.getCodigo(),
                            socio.getNombres() + " " + socio.getApellidos(),
                            producto != null ? producto.getNombre() : null,
                            cuota.getSaldoCapital(),
                            cuota.getNumeroCuota(),
                            cuota.getFechaVencimiento(),
                            cuota.getCuotaTotal(),
                            cuota.getMora(),
                            cuota.getCuotaTotal().add(cuota.getMora()),
                            cuota.getEstado(),
                            diasVencido);
                })
                .collect(Collectors.toList());
    }

    public MorosidadResponse morosidad() {
        LocalDate hoy = LocalDate.now();
        List<CuotaCredito> vencidas = cuotaRepository.findByEstadoInOrderByFechaVencimientoAsc(ESTADOS_EN_CARTERA)
                .stream().filter(c -> c.getFechaVencimiento().isBefore(hoy)).collect(Collectors.toList());

        BigDecimal saldoVencido = vencidas.stream()
                .map(c -> c.getCuotaTotal().add(c.getMora()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        int creditosEnMora = vencidas.stream().map(CuotaCredito::getCreditoId)
                .collect(Collectors.toSet()).size();
        BigDecimal carteraColocada = creditoRepository.sumSaldoVigente();
        BigDecimal porcentaje = carteraColocada.compareTo(BigDecimal.ZERO) > 0
                ? saldoVencido.multiply(BigDecimal.valueOf(100)).divide(carteraColocada, 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        return new MorosidadResponse(vencidas.size(), saldoVencido, carteraColocada, porcentaje, creditosEnMora);
    }

    public DashboardGraficosResponse graficos() {
        return new DashboardGraficosResponse(
                colocacionPorMes(),
                cobranzaPorMes(),
                flujoCajaPorMes(),
                carteraPorEstado());
    }

    private List<DashboardGraficosResponse.SerieMensual> colocacionPorMes() {
        Map<String, BigDecimal> porMes = new HashMap<>();
        for (Credito credito : creditoRepository.findAll()) {
            if (credito.getFechaDesembolso() != null) {
                String mes = mesDe(credito.getFechaDesembolso());
                porMes.merge(mes, credito.getMontoDesembolsado(), BigDecimal::add);
            }
        }
        return ultimosMeses(12).stream()
                .map(mes -> new DashboardGraficosResponse.SerieMensual(mes, porMes.getOrDefault(mes, BigDecimal.ZERO)))
                .toList();
    }

    private List<DashboardGraficosResponse.SerieMensual> cobranzaPorMes() {
        Map<String, BigDecimal> porMes = new HashMap<>();
        for (PagoCuota pago : pagoCuotaRepository.findAll()) {
            String mes = mesDe(pago.getPagadoAt());
            BigDecimal total = pago.getMontoCapital().add(pago.getMontoInteres()).add(pago.getMontoMora());
            porMes.merge(mes, total, BigDecimal::add);
        }
        return ultimosMeses(12).stream()
                .map(mes -> new DashboardGraficosResponse.SerieMensual(mes, porMes.getOrDefault(mes, BigDecimal.ZERO)))
                .toList();
    }

    private List<DashboardGraficosResponse.FlujoMensual> flujoCajaPorMes() {
        Map<String, BigDecimal> ingresos = new HashMap<>();
        Map<String, BigDecimal> egresos = new HashMap<>();
        for (CajaMovimiento movimiento : cajaMovimientoRepository.findAll()) {
            String mes = mesDe(movimiento.getCreatedAt());
            TipoMovimiento tipo = TipoMovimiento.from(movimiento.getTipo());
            boolean esEgreso = tipo != null && tipo.isEgreso();
            (esEgreso ? egresos : ingresos).merge(mes, movimiento.getMonto(), BigDecimal::add);
        }
        return ultimosMeses(6).stream()
                .map(mes -> new DashboardGraficosResponse.FlujoMensual(
                        mes,
                        ingresos.getOrDefault(mes, BigDecimal.ZERO),
                        egresos.getOrDefault(mes, BigDecimal.ZERO)))
                .toList();
    }

    private List<DashboardGraficosResponse.PorEstado> carteraPorEstado() {
        Map<String, BigDecimal> saldos = new HashMap<>();
        Map<String, Long> cantidades = new HashMap<>();
        for (Credito credito : creditoRepository.findAll()) {
            saldos.merge(credito.getEstado(), credito.getSaldoCapital(), BigDecimal::add);
            cantidades.merge(credito.getEstado(), 1L, Long::sum);
        }
        return saldos.entrySet().stream()
                .map(entry -> new DashboardGraficosResponse.PorEstado(
                        entry.getKey(), entry.getValue(), cantidades.getOrDefault(entry.getKey(), 0L)))
                .sorted(Comparator.comparing(DashboardGraficosResponse.PorEstado::saldo).reversed())
                .toList();
    }

    private List<String> ultimosMeses(int cantidad) {
        List<String> meses = new ArrayList<>();
        YearMonth actual = YearMonth.now();
        for (int i = cantidad - 1; i >= 0; i--) {
            YearMonth mes = actual.minusMonths(i);
            meses.add(mes.getYear() + "-" + String.format("%02d", mes.getMonthValue()));
        }
        return meses;
    }

    private String mesDe(LocalDate fecha) {
        YearMonth mes = YearMonth.from(fecha);
        return mes.getYear() + "-" + String.format("%02d", mes.getMonthValue());
    }

    private String mesDe(LocalDateTime fecha) {
        return mesDe(fecha.toLocalDate());
    }

    private String mesDe(Instant fecha) {
        return mesDe(fecha.atZone(ZoneId.systemDefault()).toLocalDate());
    }

    public DashboardResumenResponse resumen() {
        MorosidadResponse mora = morosidad();
        int sociosActivos = (int) socioRepository.countByEstado("ACTIVO");
        long creditosVigentes = creditoRepository.countVigentes();
        int cajasAbiertas = cajaAperturaRepository.findByEstadoOrderByOpenedAtAsc("ABIERTA").size();
        BigDecimal disponibleCaja = disponibleCaja();
        BigDecimal disponibleBancos = cuentaBancariaRepository.sumSaldoContable();
        return new DashboardResumenResponse(
                sociosActivos,
                (int) creditosVigentes,
                mora.carteraColocada(),
                mora.saldoVencido(),
                mora.porcentajeMorosidad(),
                cajasAbiertas,
                disponibleCaja,
                disponibleBancos);
    }

    private long calcularDiasVencido(CuotaCredito cuota, LocalDate hoy) {
        if ("PENDIENTE".equals(cuota.getEstado()) && cuota.getFechaVencimiento().isBefore(hoy)) {
            return ChronoUnit.DAYS.between(cuota.getFechaVencimiento(), hoy);
        }
        return 0;
    }

    private BigDecimal disponibleCaja() {
        List<CajaApertura> abiertas = cajaAperturaRepository.findByEstadoOrderByOpenedAtAsc("ABIERTA");
        BigDecimal total = BigDecimal.ZERO;
        for (CajaApertura apertura : abiertas) {
            BigDecimal saldo = apertura.getSaldoInicial();
            for (CajaMovimiento movimiento : cajaMovimientoRepository.findByCajaAperturaId(apertura.getId())) {
                TipoMovimiento tipo = TipoMovimiento.from(movimiento.getTipo());
                if (tipo != null && tipo.isEgreso()) {
                    saldo = saldo.subtract(movimiento.getMonto());
                } else {
                    saldo = saldo.add(movimiento.getMonto());
                }
            }
            total = total.add(saldo);
        }
        return total;
    }
}

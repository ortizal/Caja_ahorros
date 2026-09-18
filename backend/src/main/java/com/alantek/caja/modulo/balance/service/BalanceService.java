package com.alantek.caja.modulo.balance.service;

import com.alantek.caja.modulo.ahorros.dto.CuentaAhorroResponse;
import com.alantek.caja.modulo.ahorros.repository.CuentaAhorroRepository;
import com.alantek.caja.modulo.ahorros.service.AhorroService;
import com.alantek.caja.modulo.aportaciones.dto.AportacionResponse;
import com.alantek.caja.modulo.aportaciones.repository.AportacionRepository;
import com.alantek.caja.modulo.aportaciones.service.AportacionService;
import com.alantek.caja.modulo.balance.dto.BalanceComprobacionResponse;
import com.alantek.caja.modulo.balance.dto.BalancePersonalResponse;
import com.alantek.caja.modulo.balance.dto.BalanceSocialResponse;
import com.alantek.caja.modulo.balance.dto.BalanceSituacionResponse;
import com.alantek.caja.modulo.balance.dto.BalanceSituacionResponse.LineaClasificada;
import com.alantek.caja.modulo.contabilidad.dto.BalanceLinea;
import com.alantek.caja.modulo.reportes.service.ReporteService;
import com.alantek.caja.modulo.reportes.service.ReporteService.TablaReporte;
import com.alantek.caja.modulo.contabilidad.entity.AsientoContable;
import com.alantek.caja.modulo.contabilidad.entity.AsientoDetalle;
import com.alantek.caja.modulo.contabilidad.entity.PlanCuenta;
import com.alantek.caja.modulo.contabilidad.repository.AsientoContableRepository;
import com.alantek.caja.modulo.contabilidad.repository.AsientoDetalleRepository;
import com.alantek.caja.modulo.contabilidad.repository.PlanCuentaRepository;
import com.alantek.caja.modulo.creditos.dto.CreditoResponse;
import com.alantek.caja.modulo.creditos.repository.CreditoRepository;
import com.alantek.caja.modulo.creditos.repository.CuotaCreditoRepository;
import com.alantek.caja.modulo.creditos.repository.PagoCuotaRepository;
import com.alantek.caja.modulo.creditos.service.CreditoService;
import com.alantek.caja.modulo.socios.entity.Socio;
import com.alantek.caja.modulo.socios.repository.SocioRepository;
import com.alantek.caja.shared.exception.BusinessException;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class BalanceService {

    private final SocioRepository socioRepository;
    private final CuentaAhorroRepository cuentaAhorroRepository;
    private final AportacionRepository aportacionRepository;
    private final CreditoRepository creditoRepository;
    private final CuotaCreditoRepository cuotaCreditoRepository;
    private final PagoCuotaRepository pagoCuotaRepository;
    private final PlanCuentaRepository planCuentaRepository;
    private final AsientoDetalleRepository asientoDetalleRepository;
    private final AsientoContableRepository asientoContableRepository;
    private final AhorroService ahorroService;
    private final AportacionService aportacionService;
    private final CreditoService creditoService;

    public BalanceService(SocioRepository socioRepository,
                          CuentaAhorroRepository cuentaAhorroRepository,
                          AportacionRepository aportacionRepository,
                          CreditoRepository creditoRepository,
                          CuotaCreditoRepository cuotaCreditoRepository,
                          PagoCuotaRepository pagoCuotaRepository,
                          PlanCuentaRepository planCuentaRepository,
                          AsientoDetalleRepository asientoDetalleRepository,
                          AsientoContableRepository asientoContableRepository,
                          AhorroService ahorroService,
                          AportacionService aportacionService,
                          CreditoService creditoService) {
        this.socioRepository = socioRepository;
        this.cuentaAhorroRepository = cuentaAhorroRepository;
        this.aportacionRepository = aportacionRepository;
        this.creditoRepository = creditoRepository;
        this.cuotaCreditoRepository = cuotaCreditoRepository;
        this.pagoCuotaRepository = pagoCuotaRepository;
        this.planCuentaRepository = planCuentaRepository;
        this.asientoDetalleRepository = asientoDetalleRepository;
        this.asientoContableRepository = asientoContableRepository;
        this.ahorroService = ahorroService;
        this.aportacionService = aportacionService;
        this.creditoService = creditoService;
    }

    @Transactional(readOnly = true)
    public BalanceSituacionResponse balanceSituacion() {
        List<LineaClasificada> activo = clasificarCuentas("ACTIVO");
        List<LineaClasificada> pasivo = clasificarCuentas("PASIVO");
        List<LineaClasificada> patrimonio = clasificarCuentas("PATRIMONIO");

        BigDecimal totalActivo = activo.stream()
                .map(LineaClasificada::saldo)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalPasivo = pasivo.stream()
                .map(LineaClasificada::saldo)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalPatrimonio = patrimonio.stream()
                .map(LineaClasificada::saldo)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new BalanceSituacionResponse(totalActivo, totalPasivo, totalPatrimonio,
                activo, pasivo, patrimonio);
    }

    private List<LineaClasificada> clasificarCuentas(String tipo) {
        List<PlanCuenta> cuentas = planCuentaRepository.findByTipoAndAceptaMovimientoTrueOrderByCodigo(tipo);
        List<LineaClasificada> resultado = new ArrayList<>();
        for (PlanCuenta cuenta : cuentas) {
            BigDecimal saldo = asientoDetalleRepository.saldoPorCuentaYAnio(cuenta.getId(), LocalDate.now().getYear());
            if (saldo.compareTo(BigDecimal.ZERO) != 0) {
                resultado.add(new LineaClasificada(cuenta.getCodigo(), cuenta.getNombre(), saldo));
            }
        }
        return resultado;
    }

    @Transactional(readOnly = true)
    public BalanceComprobacionResponse balanceComprobacion(Integer anio, Integer mes) {
        LocalDate desde = LocalDate.of(anio, mes, 1);
        LocalDate hasta = LocalDate.of(anio, mes, desde.lengthOfMonth());
        List<AsientoContable> asientos = asientoContableRepository
                .findByFechaBetweenOrderByFechaAsc(desde, hasta);

        Map<Long, BigDecimal[]> porCuenta = new LinkedHashMap<>();
        for (AsientoContable asiento : asientos) {
            for (AsientoDetalle detalle : asientoDetalleRepository.findByAsiento_Id(asiento.getId())) {
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

        BigDecimal totalDebe = lineas.stream().map(BalanceLinea::debe)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalHaber = lineas.stream().map(BalanceLinea::haber)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new BalanceComprobacionResponse(anio, mes, totalDebe, totalHaber, lineas);
    }

    @Transactional(readOnly = true)
    public BalanceSocialResponse balanceSocial() {
        long totalSocios = socioRepository.count();
        long sociosActivos = socioRepository.countByEstado("ACTIVO");
        long sociosMujeres = socioRepository.countBySexoAndEstado("F", "ACTIVO");
        long sociosHombres = socioRepository.countBySexoAndEstado("M", "ACTIVO");
        double porcentajeInclusionFemenina = sociosActivos > 0
                ? (double) sociosMujeres / sociosActivos : 0.0;

        int totalCuentasAhorro = (int) cuentaAhorroRepository.count();
        BigDecimal saldoTotalAhorros = cuentaAhorroRepository.sumSaldoByEstado("ACTIVA");
        int cuentasDecimo13 = (int) cuentaAhorroRepository.countByTipoAhorro("DECIMO13");
        int cuentasDecimo14 = (int) cuentaAhorroRepository.countByTipoAhorro("DECIMO14");

        int aportacionesPagadas = (int) aportacionRepository.countByEstado("PAGADA");
        int aportacionesPendientes = (int) aportacionRepository.countByEstado("PENDIENTE")
                + (int) aportacionRepository.countByEstado("PARCIAL");
        int totalAportaciones = aportacionesPagadas + aportacionesPendientes;
        double tasaCumplimientoAportaciones = totalAportaciones > 0
                ? (double) aportacionesPagadas / totalAportaciones : 0.0;

        int creditosVigentes = (int) creditoRepository.countVigentes();
        BigDecimal carteraColocada = creditoRepository.sumSaldoVigente();
        int creditosNoSocios = (int) creditoRepository.countBySocioIdIsNull();

        BigDecimal saldoVencido = cuotaCreditoRepository.sumMoraVencida();
        double porcentajeMorosidad = carteraColocada.compareTo(BigDecimal.ZERO) > 0
                ? saldoVencido.divide(carteraColocada, 4, RoundingMode.HALF_UP).doubleValue()
                : 0.0;

        BigDecimal ingresosIntereses = pagoCuotaRepository.sumMontoInteres();
        BigDecimal ingresosMora = pagoCuotaRepository.sumMontoMora();
        BigDecimal gastosOperativos = asientoDetalleRepository.saldoPorTipoCuenta("GASTO").abs();

        return new BalanceSocialResponse(
                (int) totalSocios, (int) sociosActivos,
                (int) sociosMujeres, (int) sociosHombres, porcentajeInclusionFemenina,
                totalCuentasAhorro, saldoTotalAhorros, cuentasDecimo13, cuentasDecimo14,
                aportacionesPagadas, aportacionesPendientes, tasaCumplimientoAportaciones,
                creditosVigentes, carteraColocada, creditosNoSocios, porcentajeMorosidad,
                ingresosIntereses, ingresosMora, gastosOperativos);
    }

    @Transactional(readOnly = true)
    public BalancePersonalResponse balancePersonal(Long socioId) {
        Socio socio = socioRepository.findById(socioId)
                .orElseThrow(() -> new BusinessException("Socio no encontrado: " + socioId));

        List<CuentaAhorroResponse> cuentas = ahorroService.listarCuentas(socioId, Pageable.unpaged()).content();
        BigDecimal saldoTotalAhorros = cuentas.stream()
                .map(CuentaAhorroResponse::saldo)
                .filter(s -> s != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<AportacionResponse> aportaciones = aportacionService.listarAportaciones(null, socioId, Pageable.unpaged()).content();
        BigDecimal totalAportado = aportaciones.stream()
                .map(AportacionResponse::montoPagado)
                .filter(a -> a != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        String periodoActual = LocalDate.now().getYear() + "-"
                + (LocalDate.now().getMonthValue() < 10 ? "0" : "") + LocalDate.now().getMonthValue();
        BigDecimal aportePendienteActual = aportaciones.stream()
                .filter(a -> periodoActual.equals(a.periodo()) && !"PAGADA".equals(a.estado()))
                .map(a -> a.montoEsperado().subtract(a.montoPagado() != null ? a.montoPagado() : BigDecimal.ZERO))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<CreditoResponse> creditos = creditoService.listarCreditos(socioId, Pageable.unpaged()).content();
        BigDecimal saldoTotalCreditos = creditos.stream()
                .filter(c -> "VIGENTE".equals(c.estado()) || "EN_MORA".equals(c.estado()))
                .map(CreditoResponse::saldoCapital)
                .filter(c -> c != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int creditosVigentes = (int) creditos.stream()
                .filter(c -> "VIGENTE".equals(c.estado()) || "EN_MORA".equals(c.estado()))
                .count();

        List<Long> creditosIds = creditos.stream().map(CreditoResponse::id).toList();
        int cuotasVencidas = 0;
        BigDecimal moraTotal = BigDecimal.ZERO;
        if (!creditosIds.isEmpty()) {
            cuotasVencidas = (int) cuotaCreditoRepository.countByCreditoIdInAndEstado(creditosIds, "VENCIDA");
            moraTotal = cuotaCreditoRepository.sumMoraVencidaByCreditoIds(creditosIds);
        }

        BigDecimal posicionNeta = saldoTotalAhorros.subtract(saldoTotalCreditos);

        return new BalancePersonalResponse(
                socio.getId(), socio.getCodigo(),
                socio.getNombres() + " " + socio.getApellidos(),
                socio.getIdentificacion(), socio.getEstado(), socio.getFechaIngreso(),
                totalAportado, aportePendienteActual, aportaciones,
                saldoTotalAhorros, cuentas,
                saldoTotalCreditos, creditosVigentes, cuotasVencidas, moraTotal, creditos,
                posicionNeta);
    }

    public TablaReporte toTablaSituacion(BalanceSituacionResponse resp) {
        List<String> encabezados = List.of("Tipo", "Código", "Cuenta", "Saldo");
        List<List<String>> filas = new ArrayList<>();
        for (LineaClasificada l : resp.activo()) {
            filas.add(List.of("ACTIVO", l.cuentaCodigo(), l.cuentaNombre(), l.saldo().toPlainString()));
        }
        for (LineaClasificada l : resp.pasivo()) {
            filas.add(List.of("PASIVO", l.cuentaCodigo(), l.cuentaNombre(), l.saldo().toPlainString()));
        }
        for (LineaClasificada l : resp.patrimonio()) {
            filas.add(List.of("PATRIMONIO", l.cuentaCodigo(), l.cuentaNombre(), l.saldo().toPlainString()));
        }
        return new TablaReporte("Balance de Situación", encabezados, filas);
    }

    public TablaReporte toTablaComprobacion(BalanceComprobacionResponse resp) {
        List<String> encabezados = List.of("Código", "Cuenta", "Debe", "Haber");
        List<List<String>> filas = resp.lineas().stream()
                .map(l -> List.of(l.cuentaCodigo(), l.cuentaNombre(),
                        l.debe().toPlainString(), l.haber().toPlainString()))
                .toList();
        return new TablaReporte("Balance de Comprobación " + resp.anio() + "/" + resp.mes(), encabezados, filas);
    }

    public TablaReporte toTablaSocial(BalanceSocialResponse resp) {
        List<String> encabezados = List.of("Indicador", "Valor");
        List<List<String>> filas = List.of(
                List.of("Total Socios", String.valueOf(resp.totalSocios())),
                List.of("Socios Activos", String.valueOf(resp.sociosActivos())),
                List.of("Socios Mujeres", String.valueOf(resp.sociosMujeres())),
                List.of("Socios Hombres", String.valueOf(resp.sociosHombres())),
                List.of("Inclusión Femenina", String.format("%.1f%%", resp.porcentajeInclusionFemenina() * 100)),
                List.of("Cuentas Ahorro", String.valueOf(resp.totalCuentasAhorro())),
                List.of("Saldo Ahorros", resp.saldoTotalAhorros().toPlainString()),
                List.of("Cuentas Déc. 13", String.valueOf(resp.cuentasDecimo13())),
                List.of("Cuentas Déc. 14", String.valueOf(resp.cuentasDecimo14())),
                List.of("Aportaciones Pagadas", String.valueOf(resp.aportacionesPagadas())),
                List.of("Aportaciones Pendientes", String.valueOf(resp.aportacionesPendientes())),
                List.of("Tasa Cumplimiento", String.format("%.1f%%", resp.tasaCumplimientoAportaciones() * 100)),
                List.of("Créditos Vigentes", String.valueOf(resp.creditosVigentes())),
                List.of("Cartera Colocada", resp.carteraColocada().toPlainString()),
                List.of("Créditos No Socios", String.valueOf(resp.creditosNoSocios())),
                List.of("Morosidad", String.format("%.2f%%", resp.porcentajeMorosidad() * 100)),
                List.of("Ingresos Intereses", resp.ingresosIntereses().toPlainString()),
                List.of("Ingresos Mora", resp.ingresosMora().toPlainString()),
                List.of("Gastos Operativos", resp.gastosOperativos().toPlainString()));
        return new TablaReporte("Balance Social Cooperativo", encabezados, filas);
    }

    public TablaReporte toTablaPersonal(BalancePersonalResponse resp) {
        List<String> encabezados = List.of("Indicador", "Valor");
        List<List<String>> filas = List.of(
                List.of("Socio", resp.codigo() + " - " + resp.nombreCompleto()),
                List.of("Identificación", resp.identificacion()),
                List.of("Estado", resp.estado()),
                List.of("Fecha Ingreso", resp.fechaIngreso().toString()),
                List.of("Total Aportado", resp.totalAportado().toPlainString()),
                List.of("Aporte Pendiente", resp.aportePendienteActual().toPlainString()),
                List.of("Saldo Ahorros", resp.saldoTotalAhorros().toPlainString()),
                List.of("Cuentas Ahorro", String.valueOf(resp.cuentasAhorro().size())),
                List.of("Saldo Créditos", resp.saldoTotalCreditos().toPlainString()),
                List.of("Créditos Vigentes", String.valueOf(resp.creditosVigentes())),
                List.of("Cuotas Vencidas", String.valueOf(resp.cuotasVencidas())),
                List.of("Mora Total", resp.moraTotal().toPlainString()),
                List.of("Posición Neta", resp.posicionNeta().toPlainString()));
        return new TablaReporte("Balance Personal - " + resp.codigo(), encabezados, filas);
    }
}
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
import com.alantek.caja.shared.PageResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BalanceServiceTest {

    @Mock private SocioRepository socioRepository;
    @Mock private CuentaAhorroRepository cuentaAhorroRepository;
    @Mock private AportacionRepository aportacionRepository;
    @Mock private CreditoRepository creditoRepository;
    @Mock private CuotaCreditoRepository cuotaCreditoRepository;
    @Mock private PagoCuotaRepository pagoCuotaRepository;
    @Mock private PlanCuentaRepository planCuentaRepository;
    @Mock private AsientoDetalleRepository asientoDetalleRepository;
    @Mock private AsientoContableRepository asientoContableRepository;
    @Mock private AhorroService ahorroService;
    @Mock private AportacionService aportacionService;
    @Mock private CreditoService creditoService;

    @InjectMocks
    private BalanceService balanceService;

    @Test
    void balanceSituacion_retornaTotalesPorTipo() {
        PlanCuenta activo = new PlanCuenta();
        activo.setId(1L);
        activo.setCodigo("1.1");
        activo.setNombre("Caja");
        when(planCuentaRepository.findByTipoAndAceptaMovimientoTrueOrderByCodigo("ACTIVO"))
                .thenReturn(List.of(activo));
        when(planCuentaRepository.findByTipoAndAceptaMovimientoTrueOrderByCodigo("PASIVO"))
                .thenReturn(Collections.emptyList());
        when(planCuentaRepository.findByTipoAndAceptaMovimientoTrueOrderByCodigo("PATRIMONIO"))
                .thenReturn(Collections.emptyList());
        when(asientoDetalleRepository.saldoPorCuentaYAnio(eq(1L), anyInt()))
                .thenReturn(new BigDecimal("5000.00"));

        BalanceSituacionResponse resp = balanceService.balanceSituacion();

        assertEquals(0, new BigDecimal("5000.00").compareTo(resp.totalActivo()));
        assertEquals(0, BigDecimal.ZERO.compareTo(resp.totalPasivo()));
        assertEquals(1, resp.activo().size());
        assertEquals("1.1", resp.activo().get(0).cuentaCodigo());
    }

    @Test
    void balanceComprobacion_agrupaPorCuenta() {
        LocalDate fecha = LocalDate.of(2026, 9, 15);
        AsientoContable asiento = new AsientoContable();
        asiento.setId(1L);
        asiento.setFecha(fecha);

        AsientoDetalle detalle = mock(AsientoDetalle.class);
        when(detalle.getCuentaId()).thenReturn(10L);
        when(detalle.getDebe()).thenReturn(new BigDecimal("1000"));
        when(detalle.getHaber()).thenReturn(BigDecimal.ZERO);

        when(asientoContableRepository.findByFechaBetweenOrderByFechaAsc(any(), any()))
                .thenReturn(List.of(asiento));
        when(asientoDetalleRepository.findByAsiento_Id(1L)).thenReturn(List.of(detalle));

        PlanCuenta cuenta = new PlanCuenta();
        cuenta.setId(10L);
        cuenta.setCodigo("1.1");
        cuenta.setNombre("Caja");
        when(planCuentaRepository.findById(10L)).thenReturn(Optional.of(cuenta));

        BalanceComprobacionResponse resp = balanceService.balanceComprobacion(2026, 9);

        assertEquals(2026, resp.anio());
        assertEquals(9, resp.mes());
        assertEquals(1, resp.lineas().size());
        assertEquals(0, new BigDecimal("1000").compareTo(resp.totalDebe()));
    }

    @Test
    void balanceSocial_calculaMetricasCooperativas() {
        when(socioRepository.count()).thenReturn(50L);
        when(socioRepository.countByEstado("ACTIVO")).thenReturn(40L);
        when(socioRepository.countBySexoAndEstado("F", "ACTIVO")).thenReturn(20L);
        when(socioRepository.countBySexoAndEstado("M", "ACTIVO")).thenReturn(20L);
        when(cuentaAhorroRepository.count()).thenReturn(35L);
        when(cuentaAhorroRepository.sumSaldoByEstado("ACTIVA")).thenReturn(new BigDecimal("25000.00"));
        when(cuentaAhorroRepository.countByTipoAhorro("DECIMO13")).thenReturn(5L);
        when(cuentaAhorroRepository.countByTipoAhorro("DECIMO14")).thenReturn(3L);
        when(aportacionRepository.countByEstado("PAGADA")).thenReturn(30L);
        when(aportacionRepository.countByEstado("PENDIENTE")).thenReturn(5L);
        when(aportacionRepository.countByEstado("PARCIAL")).thenReturn(5L);
        when(creditoRepository.countVigentes()).thenReturn(10L);
        when(creditoRepository.sumSaldoVigente()).thenReturn(new BigDecimal("50000.00"));
        when(creditoRepository.countBySocioIdIsNull()).thenReturn(2L);
        when(cuotaCreditoRepository.sumMoraVencida()).thenReturn(new BigDecimal("500.00"));
        when(pagoCuotaRepository.sumMontoInteres()).thenReturn(new BigDecimal("1500.00"));
        when(pagoCuotaRepository.sumMontoMora()).thenReturn(new BigDecimal("200.00"));
        when(asientoDetalleRepository.saldoPorTipoCuenta("GASTO")).thenReturn(new BigDecimal("800.00"));

        BalanceSocialResponse resp = balanceService.balanceSocial();

        assertEquals(50, resp.totalSocios());
        assertEquals(40, resp.sociosActivos());
        assertEquals(20, resp.sociosMujeres());
        assertEquals(20, resp.sociosHombres());
        assertEquals(0.5, resp.porcentajeInclusionFemenina(), 0.001);
        assertEquals(35, resp.totalCuentasAhorro());
        assertEquals(0, new BigDecimal("25000.00").compareTo(resp.saldoTotalAhorros()));
        assertEquals(30, resp.aportacionesPagadas());
        assertEquals(10, resp.aportacionesPendientes());
        assertEquals(0.75, resp.tasaCumplimientoAportaciones(), 0.001);
        assertEquals(0.01, resp.porcentajeMorosidad(), 0.001);
        assertEquals(0, new BigDecimal("800.00").compareTo(resp.gastosOperativos()));
    }

    @Test
    void balancePersonal_calculaPosicionNeta() {
        Socio socio = new Socio();
        socio.setId(1L);
        socio.setCodigo("SOC-001");
        socio.setNombres("Juan");
        socio.setApellidos("Perez");
        socio.setIdentificacion("1234567890");
        socio.setEstado("ACTIVO");
        socio.setFechaIngreso(LocalDate.of(2020, 1, 15));
        when(socioRepository.findById(1L)).thenReturn(Optional.of(socio));

        CuentaAhorroResponse cuenta = mock(CuentaAhorroResponse.class);
        when(cuenta.saldo()).thenReturn(new BigDecimal("5000.00"));
        when(ahorroService.listarCuentas(eq(1L), any(Pageable.class)))
                .thenReturn(new PageResponse<>(List.of(cuenta), 0, 10, 1, 1));

        AportacionResponse aportacion = new AportacionResponse(
                1L, 1L, "SOC-001", "Juan Perez", 1L,
                "2026-09", new BigDecimal("1000.00"),
                new BigDecimal("1000.00"), BigDecimal.ZERO, "PAGADA");
        when(aportacionService.listarAportaciones(isNull(), eq(1L), any(Pageable.class)))
                .thenReturn(new PageResponse<>(List.of(aportacion), 0, 10, 1, 1));

        CreditoResponse credito = mock(CreditoResponse.class);
        when(credito.id()).thenReturn(100L);
        when(credito.estado()).thenReturn("VIGENTE");
        when(credito.saldoCapital()).thenReturn(new BigDecimal("2000.00"));
        when(creditoService.listarCreditos(eq(1L), any(Pageable.class)))
                .thenReturn(new PageResponse<>(List.of(credito), 0, 10, 1, 1));

        when(cuotaCreditoRepository.countByCreditoIdInAndEstado(eq(List.of(100L)), eq("VENCIDA")))
                .thenReturn(0L);
        when(cuotaCreditoRepository.sumMoraVencidaByCreditoIds(eq(List.of(100L))))
                .thenReturn(BigDecimal.ZERO);

        BalancePersonalResponse resp = balanceService.balancePersonal(1L);

        assertEquals(1L, resp.socioId());
        assertEquals("SOC-001", resp.codigo());
        assertEquals("Juan Perez", resp.nombreCompleto());
        assertEquals(0, new BigDecimal("5000.00").compareTo(resp.saldoTotalAhorros()));
        assertEquals(0, new BigDecimal("2000.00").compareTo(resp.saldoTotalCreditos()));
        assertEquals(0, new BigDecimal("3000.00").compareTo(resp.posicionNeta()));
    }
}
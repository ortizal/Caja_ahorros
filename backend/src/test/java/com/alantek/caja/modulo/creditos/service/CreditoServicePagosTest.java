package com.alantek.caja.modulo.creditos.service;

import com.alantek.caja.modulo.caja.dto.CajaMovimientoRequest;
import com.alantek.caja.modulo.caja.dto.CajaMovimientoResponse;
import com.alantek.caja.modulo.caja.repository.ComprobanteRepository;
import com.alantek.caja.modulo.caja.service.CajaService;
import com.alantek.caja.modulo.creditos.dto.PagoCuotaRequest;
import com.alantek.caja.modulo.creditos.dto.PagoCuotaResponse;
import com.alantek.caja.modulo.creditos.entity.Credito;
import com.alantek.caja.modulo.creditos.entity.CuotaCredito;
import com.alantek.caja.modulo.creditos.entity.PagoCuota;
import com.alantek.caja.modulo.creditos.repository.CreditoEstadoHistorialRepository;
import com.alantek.caja.modulo.creditos.repository.CreditoRepository;
import com.alantek.caja.modulo.creditos.repository.CuotaCreditoRepository;
import com.alantek.caja.modulo.creditos.repository.PagoCuotaRepository;
import com.alantek.caja.modulo.creditos.repository.ProductoCreditoRepository;
import com.alantek.caja.modulo.creditos.repository.SolicitudCreditoRepository;
import com.alantek.caja.modulo.socios.repository.SocioRepository;
import com.alantek.caja.shared.audit.AuditService;
import com.alantek.caja.shared.exception.BusinessException;
import com.alantek.caja.shared.reports.JasperReportService;
import com.alantek.caja.shared.security.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CreditoServicePagosTest {

    @Mock private ProductoCreditoRepository productoRepository;
    @Mock private SolicitudCreditoRepository solicitudRepository;
    @Mock private CreditoRepository creditoRepository;
    @Mock private CuotaCreditoRepository cuotaRepository;
    @Mock private PagoCuotaRepository pagoRepository;
    @Mock private CreditoEstadoHistorialRepository historialRepository;
    @Mock private SocioRepository socioRepository;
    @Mock private ComprobanteRepository comprobanteRepository;
    @Mock private CajaService cajaService;
    @Mock private CurrentUserService currentUserService;
    @Mock private AuditService auditService;
    @Mock private JasperReportService jasperReportService;

    private CreditoService service;

    @BeforeEach
    void setUp() {
        service = new CreditoService(productoRepository, solicitudRepository, creditoRepository,
                cuotaRepository, pagoRepository, historialRepository, socioRepository,
                comprobanteRepository, cajaService, new AmortizacionService(),
                currentUserService, auditService, jasperReportService);
        when(currentUserService.requireUserId()).thenReturn(1L);
    }

    private Credito creditoVigente(BigDecimal saldo) {
        Credito credito = new Credito();
        credito.setId(10L);
        credito.setEstado("VIGENTE");
        credito.setSaldoCapital(saldo);
        credito.setAbonoCapitalTotal(BigDecimal.ZERO);
        return credito;
    }

    private CuotaCredito cuota(Long id, int numero, BigDecimal capital, BigDecimal interes,
                               BigDecimal cuotaTotal, BigDecimal saldo, String estado, LocalDate venc) {
        CuotaCredito c = new CuotaCredito();
        c.setId(id);
        c.setCreditoId(10L);
        c.setNumeroCuota(numero);
        c.setCapital(capital);
        c.setInteres(interes);
        c.setCuotaTotal(cuotaTotal);
        c.setSaldoCapital(saldo);
        c.setMora(BigDecimal.ZERO);
        c.setEstado(estado);
        c.setFechaVencimiento(venc);
        return c;
    }

    @Test
    void pagoNormalMarcaCuotaPagadaYReduceSaldo() {
        Credito credito = creditoVigente(new BigDecimal("1000"));
        CuotaCredito cuota = cuota(1L, 1, new BigDecimal("83.33"), new BigDecimal("15.00"),
                new BigDecimal("98.33"), new BigDecimal("916.67"), "PENDIENTE", LocalDate.now().minusMonths(1));
        when(creditoRepository.findById(10L)).thenReturn(Optional.of(credito));
        when(cuotaRepository.findById(1L)).thenReturn(Optional.of(cuota));
        when(cajaService.registrarMovimiento(any(CajaMovimientoRequest.class)))
                .thenReturn(new CajaMovimientoResponse(200L, 1L, 300L, "COMP-001", "COBRO_CREDITO",
                        new BigDecimal("98.33"), "credito", 10L, null));
        when(cuotaRepository.countByCreditoIdAndEstado(10L, "PENDIENTE")).thenReturn(2L);
        when(cuotaRepository.countByCreditoIdAndEstado(10L, "VENCIDA")).thenReturn(0L);
        when(pagoRepository.save(any(PagoCuota.class))).thenAnswer(inv -> {
            PagoCuota p = inv.getArgument(0);
            p.setId(500L);
            return p;
        });

        PagoCuotaResponse resp = service.pagarCuota(new PagoCuotaRequest(1L, null, null, null, null, "CUOTA", null));

        assertThat(resp.tipo()).isEqualTo("CUOTA");
        assertThat(cuota.getEstado()).isEqualTo("PAGADA");
        assertThat(credito.getSaldoCapital()).isEqualByComparingTo(new BigDecimal("916.67"));
        ArgumentCaptor<CajaMovimientoRequest> cap = ArgumentCaptor.forClass(CajaMovimientoRequest.class);
        verify(cajaService).registrarMovimiento(cap.capture());
        assertThat(cap.getValue().monto()).isEqualByComparingTo(new BigDecimal("98.33"));
        assertThat(cap.getValue().montoCapital()).isEqualByComparingTo(new BigDecimal("83.33"));
    }

    @Test
    void pagoAdelantadoConCuotaFutura() {
        Credito credito = creditoVigente(new BigDecimal("1000"));
        CuotaCredito cuota = cuota(3L, 3, new BigDecimal("85.12"), new BigDecimal("12.00"),
                new BigDecimal("97.12"), new BigDecimal("830.00"), "PENDIENTE", LocalDate.now().plusMonths(2));
        when(creditoRepository.findById(10L)).thenReturn(Optional.of(credito));
        when(cuotaRepository.findById(3L)).thenReturn(Optional.of(cuota));
        when(cajaService.registrarMovimiento(any(CajaMovimientoRequest.class)))
                .thenReturn(new CajaMovimientoResponse(201L, 1L, 301L, "COMP-002", "COBRO_CREDITO",
                        new BigDecimal("97.12"), "credito", 10L, null));
        when(cuotaRepository.countByCreditoIdAndEstado(10L, "PENDIENTE")).thenReturn(1L);
        when(cuotaRepository.countByCreditoIdAndEstado(10L, "VENCIDA")).thenReturn(0L);
        when(pagoRepository.save(any(PagoCuota.class))).thenAnswer(inv -> {
            PagoCuota p = inv.getArgument(0);
            p.setId(501L);
            return p;
        });

        PagoCuotaResponse resp = service.pagarCuota(new PagoCuotaRequest(3L, null, null, null, null, "ADELANTADO", null));

        assertThat(resp.tipo()).isEqualTo("ADELANTADO");
        assertThat(cuota.getEstado()).isEqualTo("PAGADA");
        ArgumentCaptor<CajaMovimientoRequest> cap = ArgumentCaptor.forClass(CajaMovimientoRequest.class);
        verify(cajaService).registrarMovimiento(cap.capture());
        assertThat(cap.getValue().montoMora()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void pagoAdelantadoRechazaCuotaVencida() {
        Credito credito = creditoVigente(new BigDecimal("1000"));
        CuotaCredito cuota = cuota(1L, 1, new BigDecimal("83.33"), new BigDecimal("15.00"),
                new BigDecimal("98.33"), new BigDecimal("916.67"), "PENDIENTE", LocalDate.now().minusDays(5));
        when(creditoRepository.findById(10L)).thenReturn(Optional.of(credito));
        when(cuotaRepository.findById(1L)).thenReturn(Optional.of(cuota));

        assertThatThrownBy(() -> service.pagarCuota(
                new PagoCuotaRequest(1L, null, null, null, null, "ADELANTADO", null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("vencida");
        assertThat(cuota.getEstado()).isEqualTo("PENDIENTE");
    }

    @Test
    void abonoCapitalReduceCapitalDeCuotasPendientes() {
        Credito credito = creditoVigente(new BigDecimal("1000"));
        CuotaCredito q1 = cuota(1L, 1, new BigDecimal("100.00"), new BigDecimal("15.00"),
                new BigDecimal("115.00"), new BigDecimal("900.00"), "PENDIENTE", LocalDate.now().plusMonths(1));
        CuotaCredito q2 = cuota(2L, 2, new BigDecimal("100.00"), new BigDecimal("13.50"),
                new BigDecimal("113.50"), new BigDecimal("800.00"), "PENDIENTE", LocalDate.now().plusMonths(2));
        CuotaCredito q3 = cuota(3L, 3, new BigDecimal("100.00"), new BigDecimal("12.00"),
                new BigDecimal("112.00"), new BigDecimal("700.00"), "PENDIENTE", LocalDate.now().plusMonths(3));

        when(creditoRepository.findById(10L)).thenReturn(Optional.of(credito));
        when(cuotaRepository.findById(1L)).thenReturn(Optional.of(q1));
        when(cuotaRepository.findByCreditoIdOrderByNumeroCuotaAsc(10L)).thenReturn(List.of(q1, q2, q3));
        when(cajaService.registrarMovimiento(any(CajaMovimientoRequest.class)))
                .thenReturn(new CajaMovimientoResponse(202L, 1L, 302L, "COMP-003", "COBRO_CREDITO",
                        new BigDecimal("120.00"), "credito", 10L, null));
        when(cuotaRepository.countByCreditoIdAndEstado(10L, "PENDIENTE")).thenReturn(3L);
        when(cuotaRepository.countByCreditoIdAndEstado(10L, "VENCIDA")).thenReturn(0L);
        when(pagoRepository.save(any(PagoCuota.class))).thenAnswer(inv -> {
            PagoCuota p = inv.getArgument(0);
            p.setId(502L);
            return p;
        });

        PagoCuotaResponse resp = service.pagarCuota(
                new PagoCuotaRequest(1L, null, null, null, new BigDecimal("120.00"), "ABONO", "Extra"));

        assertThat(resp.tipo()).isEqualTo("ABONO");
        assertThat(resp.montoAbonoCapital()).isEqualByComparingTo(new BigDecimal("120.00"));
        assertThat(credito.getSaldoCapital()).isEqualByComparingTo(new BigDecimal("880.00"));
        assertThat(credito.getAbonoCapitalTotal()).isEqualByComparingTo(new BigDecimal("120.00"));
        assertThat(q1.getCapital()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(q2.getCapital()).isEqualByComparingTo(new BigDecimal("80.00"));
        assertThat(q3.getCapital()).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(q1.getSaldoCapital()).isEqualByComparingTo(new BigDecimal("900.00"));
        assertThat(q2.getSaldoCapital()).isEqualByComparingTo(new BigDecimal("880.00"));
        assertThat(q3.getSaldoCapital()).isEqualByComparingTo(new BigDecimal("700.00"));
        assertThat(q1.getEstado()).isEqualTo("PENDIENTE");
    }

    @Test
    void abonoRechazaMontoSuperiorAlSaldo() {
        Credito credito = creditoVigente(new BigDecimal("100"));
        CuotaCredito cuota = cuota(1L, 1, new BigDecimal("100.00"), new BigDecimal("15.00"),
                new BigDecimal("115.00"), new BigDecimal("0.00"), "PENDIENTE", LocalDate.now().plusMonths(1));
        when(creditoRepository.findById(10L)).thenReturn(Optional.of(credito));
        when(cuotaRepository.findById(1L)).thenReturn(Optional.of(cuota));

        assertThatThrownBy(() -> service.pagarCuota(
                new PagoCuotaRequest(1L, null, null, null, new BigDecimal("150.00"), "ABONO", null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("saldo de capital");
    }
}

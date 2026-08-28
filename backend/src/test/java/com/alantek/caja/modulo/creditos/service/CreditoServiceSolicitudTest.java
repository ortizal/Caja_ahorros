package com.alantek.caja.modulo.creditos.service;

import com.alantek.caja.modulo.caja.repository.ComprobanteRepository;
import com.alantek.caja.modulo.caja.service.CajaService;
import com.alantek.caja.modulo.creditos.dto.SolicitudCreditoRequest;
import com.alantek.caja.modulo.creditos.dto.SolicitudCreditoResponse;
import com.alantek.caja.modulo.creditos.entity.ProductoCredito;
import com.alantek.caja.modulo.creditos.entity.SolicitudCredito;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CreditoServiceSolicitudTest {

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

    private ProductoCredito producto(boolean activo, boolean permiteNoSocio) {
        ProductoCredito p = new ProductoCredito();
        p.setId(7L);
        p.setNombre("CREDITO GENERAL");
        p.setActivo(activo);
        p.setPermiteNoSocio(permiteNoSocio);
        p.setMontoMin(BigDecimal.ZERO);
        p.setMontoMax(new BigDecimal("5000"));
        p.setTasaInteres(new BigDecimal("18"));
        p.setPlazoMaxMeses(24);
        return p;
    }

    @Test
    void creaSolicitudDeNoSocioConDatosDelCliente() {
        when(productoRepository.findById(7L)).thenReturn(Optional.of(producto(true, true)));
        when(solicitudRepository.save(any(SolicitudCredito.class))).thenAnswer(inv -> {
            SolicitudCredito s = inv.getArgument(0);
            s.setId(50L);
            return s;
        });

        SolicitudCreditoResponse resp = service.crearSolicitud(new SolicitudCreditoRequest(
                null, "JUAN PEREZ", "1712345678", "0987654321",
                7L, new BigDecimal("1000"), 12, "CONSUMO"));

        assertThat(resp.clienteNoSocioNombre()).isEqualTo("JUAN PEREZ");
        assertThat(resp.clienteNoSocioIdentificacion()).isEqualTo("1712345678");
        assertThat(resp.socioId()).isNull();
        verify(solicitudRepository).save(any(SolicitudCredito.class));
    }

    @Test
    void noSocioConProductoQueNoPermiteEsRechazado() {
        when(productoRepository.findById(7L)).thenReturn(Optional.of(producto(true, false)));

        assertThatThrownBy(() -> service.crearSolicitud(new SolicitudCreditoRequest(
                null, "JUAN PEREZ", "1712345678", null,
                7L, new BigDecimal("1000"), 12, "CONSUMO")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("no permite creditos a no socios");
    }

    @Test
    void noSocioSinNombreEsRechazado() {
        assertThatThrownBy(() -> service.crearSolicitud(new SolicitudCreditoRequest(
                null, "   ", "1712345678", null,
                7L, new BigDecimal("1000"), 12, "CONSUMO")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("nombre");
    }

    @Test
    void noSocioSinIdentificacionEsRechazado() {
        assertThatThrownBy(() -> service.crearSolicitud(new SolicitudCreditoRequest(
                null, "JUAN PEREZ", "  ", null,
                7L, new BigDecimal("1000"), 12, "CONSUMO")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("identificacion");
    }
}

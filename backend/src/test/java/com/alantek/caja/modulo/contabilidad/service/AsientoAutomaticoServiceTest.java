package com.alantek.caja.modulo.contabilidad.service;

import com.alantek.caja.modulo.contabilidad.entity.AsientoContable;
import com.alantek.caja.modulo.contabilidad.entity.AsientoDetalle;
import com.alantek.caja.modulo.contabilidad.entity.PeriodoContable;
import com.alantek.caja.modulo.caja.entity.Comprobante;
import com.alantek.caja.modulo.caja.repository.ComprobanteRepository;
import com.alantek.caja.modulo.contabilidad.repository.AsientoDetalleRepository;
import com.alantek.caja.modulo.contabilidad.repository.PeriodoContableRepository;
import com.alantek.caja.modulo.contabilidad.repository.PlanCuentaRepository;
import com.alantek.caja.shared.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AsientoAutomaticoServiceTest {

    private static final LocalDate FECHA = LocalDate.of(2030, 1, 15);

    @Autowired
    private AsientoAutomaticoService asientoAutomaticoService;

    @Autowired
    private AsientoDetalleRepository asientoDetalleRepository;

    @Autowired
    private PeriodoContableRepository periodoContableRepository;

    @Autowired
    private PlanCuentaRepository planCuentaRepository;

    @Autowired
    private ComprobanteRepository comprobanteRepository;

    @Test
    void cadaOperacionDeLaMatrizGeneraAsientoCuadrado() {
        List<String> operaciones = List.of(
                "APORTACION", "DEPOSITO_AHORRO", "RETIRO_AHORRO", "DESEMBOLSO_CREDITO",
                "PAGO_CAPITAL", "PAGO_INTERES", "PAGO_MORA", "GASTO_PAGADO", "COBRO_CUENTA");

        BigDecimal monto = new BigDecimal("125.75");

        for (String operacion : operaciones) {
            AsientoContable asiento = asientoAutomaticoService.generarAsientoSimple(
                    operacion, null, FECHA, monto, "Test " + operacion);

            BigDecimal totalDebe = asientoDetalleRepository.sumDebe(asiento.getId());
            BigDecimal totalHaber = asientoDetalleRepository.sumHaber(asiento.getId());

            assertThat(totalDebe).as("Total Debe de %s", operacion)
                    .isEqualByComparingTo(monto);
            assertThat(totalHaber).as("Total Haber de %s", operacion)
                    .isEqualByComparingTo(monto);
            assertThat(totalDebe).as("Cuadre de %s", operacion).isEqualByComparingTo(totalHaber);
        }
    }

    @Test
    void cobroDeCuotaGeneraUnSoloAsientoConTresComponentes() {
        Comprobante comprobante = new Comprobante();
        comprobante.setNumero("CMP-TEST-999");
        comprobante.setTipo("INGRESO");
        comprobante.setDescripcion("Test cobro de cuota");
        Long comprobanteId = comprobanteRepository.save(comprobante).getId();

        AsientoContable asiento = asientoAutomaticoService.generarAsiento("Cobro de cuota", comprobanteId, FECHA,
                List.of(
                        new AsientoAutomaticoService.ReglaAplicada("PAGO_CAPITAL", new BigDecimal("100.00")),
                        new AsientoAutomaticoService.ReglaAplicada("PAGO_INTERES", new BigDecimal("20.00")),
                        new AsientoAutomaticoService.ReglaAplicada("PAGO_MORA", new BigDecimal("5.00"))));

        List<AsientoDetalle> detalles = asientoDetalleRepository.findByAsiento_Id(asiento.getId());
        assertThat(detalles).hasSize(6);

        BigDecimal totalDebe = asientoDetalleRepository.sumDebe(asiento.getId());
        BigDecimal totalHaber = asientoDetalleRepository.sumHaber(asiento.getId());

        assertThat(totalDebe).isEqualByComparingTo("125.00");
        assertThat(totalHaber).isEqualByComparingTo("125.00");
        assertThat(asiento.getComprobanteId()).isEqualTo(comprobanteId);
    }

    @Test
    void componenteConMontoCeroSeOmiten() {
        AsientoContable asiento = asientoAutomaticoService.generarAsiento("Pago sin mora", null, FECHA,
                List.of(
                        new AsientoAutomaticoService.ReglaAplicada("PAGO_CAPITAL", new BigDecimal("100.00")),
                        new AsientoAutomaticoService.ReglaAplicada("PAGO_MORA", BigDecimal.ZERO)));

        List<AsientoDetalle> detalles = asientoDetalleRepository.findByAsiento_Id(asiento.getId());
        assertThat(detalles).hasSize(2);
        assertThat(asientoDetalleRepository.sumDebe(asiento.getId())).isEqualByComparingTo("100.00");
    }

    @Test
    void operacionSinReglaVigenteLanzaExcepcion() {
        assertThatThrownBy(() -> asientoAutomaticoService.generarAsientoSimple(
                "OPERACION_INVENTADA", null, FECHA, new BigDecimal("10.00"), "inventada"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("No existe regla contable vigente para la operación 'OPERACION_INVENTADA'");
    }

    @Test
    void sinReglasLanzaExcepcion() {
        assertThatThrownBy(() -> asientoAutomaticoService.generarAsiento(
                "sin reglas", null, FECHA, List.of()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Debe indicar al menos una regla contable");
    }

    @Test
    void periodoCerradoBloqueaRegistroDeMovimientos() {
        PeriodoContable periodo = new PeriodoContable();
        periodo.setAnio(2030);
        periodo.setMes(6);
        periodo.setEstado("CERRADO");
        periodoContableRepository.save(periodo);

        LocalDate fechaCerrada = LocalDate.of(2030, 6, 10);

        assertThatThrownBy(() -> asientoAutomaticoService.generarAsientoSimple(
                "APORTACION", null, fechaCerrada, new BigDecimal("50.00"), "bloqueado"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("está CERRADO; no se pueden registrar movimientos");
    }

    @Test
    void retiroDeAhorroInvierteDebeYHaber() {
        AsientoContable asiento = asientoAutomaticoService.generarAsientoSimple(
                "RETIRO_AHORRO", null, FECHA, new BigDecimal("30.00"), "Retiro");

        List<AsientoDetalle> detalles = asientoDetalleRepository.findByAsiento_Id(asiento.getId());
        assertThat(detalles).hasSize(2);

        AsientoDetalle ahorro = detalles.stream()
                .filter(d -> d.getCuentaId().equals(planCuentaRepository.findByCodigo("2.1.02").orElseThrow().getId()))
                .findFirst().orElseThrow();
        AsientoDetalle caja = detalles.stream()
                .filter(d -> d.getCuentaId().equals(planCuentaRepository.findByCodigo("1.1.01").orElseThrow().getId()))
                .findFirst().orElseThrow();

        assertThat(ahorro.getDebe()).isEqualByComparingTo("30.00");
        assertThat(ahorro.getHaber()).isEqualByComparingTo("0.00");
        assertThat(caja.getHaber()).isEqualByComparingTo("30.00");
        assertThat(caja.getDebe()).isEqualByComparingTo("0.00");
    }
}

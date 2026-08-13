package com.alantek.caja.modulo.creditos.service;

import com.alantek.caja.modulo.creditos.dto.SimulacionCreditoResponse;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AmortizacionServiceTest {

    private final AmortizacionService service = new AmortizacionService();

    @Test
    void francesCuotaConstanteYSaldoCierraEnCero() {
        SimulacionCreditoResponse result = service.simular(
                new BigDecimal("1000.00"), new BigDecimal("18.0000"), 12, "FRANCES",
                LocalDate.of(2026, 1, 1));

        assertThat(result.cuotaMensual())
                .isCloseTo(new BigDecimal("91.68"), by(0.01));
        assertThat(result.cuotas()).hasSize(12);
        assertThat(result.totalInteres())
                .isCloseTo(new BigDecimal("100.16"), by(0.20));

        BigDecimal sumaCapital = result.cuotas().stream()
                .map(SimulacionCreditoResponse.CuotaSimulada::capital)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(sumaCapital).isEqualByComparingTo(new BigDecimal("1000.00"));

        BigDecimal saldoFinal = result.cuotas().get(11).saldo();
        assertThat(saldoFinal).isEqualByComparingTo(BigDecimal.ZERO);

        BigDecimal primera = result.cuotas().get(0).cuota();
        BigDecimal ultima = result.cuotas().get(11).cuota();
        assertThat(primera.subtract(ultima).abs()).isLessThanOrEqualTo(new BigDecimal("0.05"));
    }

    @Test
    void alemanCapitalConstante() {
        SimulacionCreditoResponse result = service.simular(
                new BigDecimal("1000.00"), new BigDecimal("18.0000"), 12, "ALEMAN",
                LocalDate.of(2026, 1, 1));

        BigDecimal capitalPrimera = result.cuotas().get(0).capital();
        BigDecimal capitalSegunda = result.cuotas().get(1).capital();
        assertThat(capitalPrimera).isEqualByComparingTo(capitalSegunda);
        assertThat(result.cuotas().get(0).interes()).isEqualByComparingTo(new BigDecimal("15.00"));
        assertThat(result.cuotas().get(11).saldo()).isEqualByComparingTo(BigDecimal.ZERO);

        BigDecimal sumaCapital = result.cuotas().stream()
                .map(SimulacionCreditoResponse.CuotaSimulada::capital)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(sumaCapital).isEqualByComparingTo(new BigDecimal("1000.00"));
    }

    @Test
    void americanoPagaSoloInteresesYCapitalAlFinal() {
        SimulacionCreditoResponse result = service.simular(
                new BigDecimal("1000.00"), new BigDecimal("18.0000"), 12, "AMERICANO",
                LocalDate.of(2026, 1, 1));

        assertThat(result.cuotas().get(0).cuota()).isEqualByComparingTo(new BigDecimal("15.00"));
        assertThat(result.cuotas().get(10).capital()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.cuotas().get(11).capital()).isEqualByComparingTo(new BigDecimal("1000.00"));
        assertThat(result.cuotas().get(11).saldo()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void montoInvalidoLanzaError() {
        assertThatThrownBy(() -> service.simular(BigDecimal.ZERO, new BigDecimal("18.0"), 12, "FRANCES"))
                .hasMessageContaining("mayor a cero");
    }

    @Test
    void sistemaNoSoportadoLanzaError() {
        assertThatThrownBy(() -> service.simular(
                new BigDecimal("1000.00"), new BigDecimal("18.0"), 12, "ALEMANA"))
                .hasMessageContaining("no soportado");
    }

    @Test
    void fechasVencimientoSeGeneranMensualmente() {
        SimulacionCreditoResponse result = service.simular(
                new BigDecimal("600.00"), new BigDecimal("12.0000"), 3, "FRANCES",
                LocalDate.of(2026, 2, 15));

        assertThat(result.cuotas().get(0).fechaVencimiento()).isEqualTo(LocalDate.of(2026, 2, 15));
        assertThat(result.cuotas().get(1).fechaVencimiento()).isEqualTo(LocalDate.of(2026, 3, 15));
        assertThat(result.cuotas().get(2).fechaVencimiento()).isEqualTo(LocalDate.of(2026, 4, 15));
    }

    private static org.assertj.core.data.Offset<BigDecimal> by(double valor) {
        return org.assertj.core.data.Offset.offset(
                new BigDecimal(valor).setScale(2, RoundingMode.HALF_UP));
    }
}

package com.alantek.caja.modulo.creditos.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record SimulacionCreditoResponse(
        BigDecimal cuotaMensual,
        BigDecimal totalInteres,
        BigDecimal totalPagar,
        String sistemaAmortizacion,
        List<CuotaSimulada> cuotas) {

    public record CuotaSimulada(
            int numero,
            LocalDate fechaVencimiento,
            BigDecimal capital,
            BigDecimal interes,
            BigDecimal cuota,
            BigDecimal saldo) {
    }
}

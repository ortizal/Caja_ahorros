package com.alantek.caja.modulo.cartera.dto;

import java.math.BigDecimal;

public record MoraClienteResponse(
        Long socioId,
        String socioCodigo,
        String socioNombre,
        String socioIdentificacion,
        String socioTelefono,
        String socioEmail,
        int creditosEnMora,
        int cuotasVencidas,
        BigDecimal moraTotal,
        BigDecimal saldoCapitalTotal,
        long diasMoraMaximo) {
}

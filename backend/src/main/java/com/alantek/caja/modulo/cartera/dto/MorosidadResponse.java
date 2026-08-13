package com.alantek.caja.modulo.cartera.dto;

import java.math.BigDecimal;

public record MorosidadResponse(
        int cuotasVencidas,
        BigDecimal saldoVencido,
        BigDecimal carteraColocada,
        BigDecimal porcentajeMorosidad,
        int creditosEnMora) {
}

package com.alantek.caja.modulo.cartera.dto;

import java.math.BigDecimal;

public record DashboardResumenResponse(
        int sociosActivos,
        int creditosVigentes,
        BigDecimal carteraColocada,
        BigDecimal carteraVencida,
        BigDecimal porcentajeMorosidad,
        int cajasAbiertas,
        BigDecimal disponibleCaja,
        BigDecimal disponibleBancos) {
}

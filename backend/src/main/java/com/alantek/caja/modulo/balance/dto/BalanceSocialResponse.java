package com.alantek.caja.modulo.balance.dto;

import java.math.BigDecimal;

public record BalanceSocialResponse(
        int totalSocios,
        int sociosActivos,
        int sociosMujeres,
        int sociosHombres,
        double porcentajeInclusionFemenina,
        int totalCuentasAhorro,
        BigDecimal saldoTotalAhorros,
        int cuentasDecimo13,
        int cuentasDecimo14,
        int aportacionesPagadas,
        int aportacionesPendientes,
        double tasaCumplimientoAportaciones,
        int creditosVigentes,
        BigDecimal carteraColocada,
        int creditosNoSocios,
        double porcentajeMorosidad,
        BigDecimal ingresosIntereses,
        BigDecimal ingresosMora,
        BigDecimal gastosOperativos) {
}
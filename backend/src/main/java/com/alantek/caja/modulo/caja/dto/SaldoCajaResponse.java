package com.alantek.caja.modulo.caja.dto;

import java.math.BigDecimal;

public record SaldoCajaResponse(
        Long cajaAperturaId,
        BigDecimal saldoInicial,
        BigDecimal totalIngresos,
        BigDecimal totalEgresos,
        BigDecimal saldoActual) {
}

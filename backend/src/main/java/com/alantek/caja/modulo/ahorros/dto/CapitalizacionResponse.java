package com.alantek.caja.modulo.ahorros.dto;

import java.math.BigDecimal;

public record CapitalizacionResponse(
        int anio,
        int mes,
        int cuentasCapitalizadas,
        BigDecimal totalInteres) {
}

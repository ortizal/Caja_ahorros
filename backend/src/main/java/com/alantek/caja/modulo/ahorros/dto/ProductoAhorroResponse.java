package com.alantek.caja.modulo.ahorros.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ProductoAhorroResponse(
        Long id,
        String nombre,
        BigDecimal tasaInteres,
        String periodicidadCapitalizacion,
        BigDecimal saldoMinimo,
        Integer limiteRetirosMes,
        LocalDate vigenteDesde,
        LocalDate vigenteHasta,
        boolean activo) {
}

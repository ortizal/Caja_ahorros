package com.alantek.caja.modulo.creditos.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ProductoCreditoResponse(
        Long id,
        String nombre,
        BigDecimal tasaInteres,
        BigDecimal tasaMora,
        String sistemaAmortizacion,
        Integer plazoMaxMeses,
        BigDecimal montoMin,
        BigDecimal montoMax,
        boolean requiereGarante,
        LocalDate vigenteDesde,
        LocalDate vigenteHasta,
        boolean activo) {
}

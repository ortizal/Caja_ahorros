package com.alantek.caja.modulo.balance.dto;

import com.alantek.caja.modulo.contabilidad.dto.BalanceLinea;

import java.math.BigDecimal;
import java.util.List;

public record BalanceComprobacionResponse(
        Integer anio,
        Integer mes,
        BigDecimal totalDebe,
        BigDecimal totalHaber,
        List<BalanceLinea> lineas) {
}
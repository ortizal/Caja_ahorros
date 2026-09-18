package com.alantek.caja.modulo.balance.dto;

import java.math.BigDecimal;
import java.util.List;

public record BalanceSituacionResponse(
        BigDecimal totalActivo,
        BigDecimal totalPasivo,
        BigDecimal totalPatrimonio,
        List<LineaClasificada> activo,
        List<LineaClasificada> pasivo,
        List<LineaClasificada> patrimonio) {

    public record LineaClasificada(
            String cuentaCodigo,
            String cuentaNombre,
            BigDecimal saldo) {
    }
}
package com.alantek.caja.modulo.caja.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record CajaAperturaResponse(
        Long id,
        Long cajeroId,
        LocalDate fecha,
        BigDecimal saldoInicial,
        String estado,
        Instant openedAt,
        Instant closedAt) {
}

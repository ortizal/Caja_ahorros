package com.alantek.caja.modulo.caja.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record CajaMovimientoResponse(
        Long id,
        Long cajaAperturaId,
        Long comprobanteId,
        String comprobanteNumero,
        String tipo,
        BigDecimal monto,
        String referenciaTabla,
        Long referenciaId,
        Instant createdAt) {
}

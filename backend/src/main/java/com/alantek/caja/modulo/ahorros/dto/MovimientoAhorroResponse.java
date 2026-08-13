package com.alantek.caja.modulo.ahorros.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record MovimientoAhorroResponse(
        Long id,
        Long cuentaId,
        String tipo,
        BigDecimal monto,
        BigDecimal saldoResultante,
        Long comprobanteId,
        String comprobanteNumero,
        String estado,
        Instant createdAt) {
}

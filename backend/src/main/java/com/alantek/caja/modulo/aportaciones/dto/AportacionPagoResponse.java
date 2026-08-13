package com.alantek.caja.modulo.aportaciones.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record AportacionPagoResponse(
        Long id,
        Long aportacionId,
        BigDecimal monto,
        Long cajaMovimientoId,
        String comprobanteNumero,
        Instant pagadoAt) {
}

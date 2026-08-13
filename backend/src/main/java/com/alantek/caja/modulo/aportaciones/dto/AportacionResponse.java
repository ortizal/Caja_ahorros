package com.alantek.caja.modulo.aportaciones.dto;

import java.math.BigDecimal;

public record AportacionResponse(
        Long id,
        Long socioId,
        String socioCodigo,
        String socioNombre,
        Long configId,
        String periodo,
        BigDecimal montoEsperado,
        BigDecimal montoPagado,
        BigDecimal mora,
        String estado) {
}

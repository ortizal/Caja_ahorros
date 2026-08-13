package com.alantek.caja.modulo.creditos.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PagoCuotaResponse(
        Long id,
        Long cuotaId,
        Long creditoId,
        BigDecimal montoCapital,
        BigDecimal montoInteres,
        BigDecimal montoMora,
        Long comprobanteId,
        String comprobanteNumero,
        LocalDateTime pagadoAt) {
}

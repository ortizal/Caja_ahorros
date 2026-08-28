package com.alantek.caja.modulo.creditos.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PagoCuotaResponse(
        Long id,
        Long cuotaId,
        Integer cuotaNumero,
        Long creditoId,
        String tipo,
        BigDecimal montoCapital,
        BigDecimal montoInteres,
        BigDecimal montoMora,
        BigDecimal montoAbonoCapital,
        String descripcion,
        Long comprobanteId,
        String comprobanteNumero,
        LocalDateTime pagadoAt) {
}

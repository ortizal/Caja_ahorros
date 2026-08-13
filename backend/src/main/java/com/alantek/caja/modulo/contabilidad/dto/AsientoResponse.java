package com.alantek.caja.modulo.contabilidad.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record AsientoResponse(
        Long id,
        Long periodoId,
        Long comprobanteId,
        LocalDate fecha,
        String descripcion,
        String origen,
        String estado,
        Instant createdAt,
        Long createdBy,
        List<DetalleResponse> detalles) {

    public record DetalleResponse(
            Long cuentaId,
            String cuentaCodigo,
            String cuentaNombre,
            BigDecimal debe,
            BigDecimal haber) {
    }
}

package com.alantek.caja.modulo.tesoreria.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record GastoResponse(
        Long id,
        String concepto,
        String descripcion,
        BigDecimal monto,
        Long cuentaContableId,
        String cuentaContableCodigo,
        LocalDate fechaSolicitud,
        Long solicitadoPor,
        String estado,
        Long aprobadoPor,
        Instant fechaAprobacion,
        String motivoRechazo,
        Long comprobanteId,
        Long cajaMovimientoId) {
}

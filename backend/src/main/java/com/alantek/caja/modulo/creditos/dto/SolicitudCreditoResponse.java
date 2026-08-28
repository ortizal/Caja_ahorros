package com.alantek.caja.modulo.creditos.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SolicitudCreditoResponse(
        Long id,
        Long socioId,
        String socioCodigo,
        String socioNombre,
        String clienteNoSocioNombre,
        String clienteNoSocioIdentificacion,
        String clienteNoSocioTelefono,
        Long productoId,
        String nombreProducto,
        BigDecimal montoSolicitado,
        Integer plazoMeses,
        String destino,
        String estado,
        Long solicitadoPor,
        Long evaluadoPor,
        Long aprobadoPor,
        String motivoRechazo,
        LocalDateTime createdAt) {
}

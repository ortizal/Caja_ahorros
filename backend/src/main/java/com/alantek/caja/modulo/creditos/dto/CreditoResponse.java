package com.alantek.caja.modulo.creditos.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record CreditoResponse(
        Long id,
        Long solicitudId,
        Long socioId,
        String socioCodigo,
        String socioNombre,
        Long productoId,
        String nombreProducto,
        BigDecimal montoDesembolsado,
        BigDecimal tasaInteres,
        Integer plazoMeses,
        LocalDate fechaDesembolso,
        BigDecimal saldoCapital,
        String estado,
        int cuotasPendientes,
        LocalDateTime createdAt) {
}

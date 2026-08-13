package com.alantek.caja.modulo.cartera.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CarteraItemResponse(
        Long id,
        Long creditoId,
        Long socioId,
        String socioCodigo,
        String socioNombre,
        String nombreProducto,
        BigDecimal saldoCapital,
        Integer numeroCuota,
        LocalDate fechaVencimiento,
        BigDecimal cuotaTotal,
        BigDecimal mora,
        BigDecimal totalPagar,
        String estado,
        long diasVencido) {
}

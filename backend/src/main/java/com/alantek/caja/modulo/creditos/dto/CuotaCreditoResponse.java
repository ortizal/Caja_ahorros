package com.alantek.caja.modulo.creditos.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CuotaCreditoResponse(
        Long id,
        Long creditoId,
        Integer numeroCuota,
        LocalDate fechaVencimiento,
        BigDecimal capital,
        BigDecimal interes,
        BigDecimal cuotaTotal,
        BigDecimal saldoCapital,
        BigDecimal mora,
        String estado) {
}

package com.alantek.caja.modulo.cartera.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CuotaMoraDetalle(
        Long cuotaId,
        Integer numeroCuota,
        LocalDate fechaVencimiento,
        BigDecimal capital,
        BigDecimal interes,
        BigDecimal cuotaTotal,
        BigDecimal mora,
        BigDecimal totalPagar,
        long diasVencido,
        String estado) {
}

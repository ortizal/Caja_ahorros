package com.alantek.caja.modulo.contabilidad.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record MayorLinea(
        LocalDate fecha,
        Long asientoId,
        String descripcion,
        BigDecimal debe,
        BigDecimal haber,
        BigDecimal saldo) {
}

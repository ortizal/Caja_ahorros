package com.alantek.caja.modulo.contabilidad.dto;

import java.math.BigDecimal;

public record BalanceLinea(
        String cuentaCodigo,
        String cuentaNombre,
        BigDecimal debe,
        BigDecimal haber) {
}

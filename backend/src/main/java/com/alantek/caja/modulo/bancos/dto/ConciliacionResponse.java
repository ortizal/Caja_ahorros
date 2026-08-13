package com.alantek.caja.modulo.bancos.dto;

import java.math.BigDecimal;

public record ConciliacionResponse(
        Long id,
        Long cuentaBancariaId,
        String periodo,
        BigDecimal saldoContable,
        BigDecimal saldoBancario,
        BigDecimal diferencia) {
}

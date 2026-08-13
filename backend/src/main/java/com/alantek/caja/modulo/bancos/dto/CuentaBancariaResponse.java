package com.alantek.caja.modulo.bancos.dto;

import java.math.BigDecimal;

public record CuentaBancariaResponse(
        Long id,
        String banco,
        String numeroCuenta,
        String tipo,
        BigDecimal saldoContable) {
}

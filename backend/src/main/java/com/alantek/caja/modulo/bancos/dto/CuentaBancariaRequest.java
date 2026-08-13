package com.alantek.caja.modulo.bancos.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CuentaBancariaRequest(
        @NotBlank(message = "El banco es obligatorio") String banco,
        @NotBlank(message = "El número de cuenta es obligatorio") String numeroCuenta,
        String tipo,
        BigDecimal saldoContable) {
}

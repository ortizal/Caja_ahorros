package com.alantek.caja.modulo.caja.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CajaAperturaRequest(
        @NotNull(message = "El saldo inicial es obligatorio")
        @DecimalMin(value = "0.0", message = "El saldo inicial no puede ser negativo")
        BigDecimal saldoInicial,

        LocalDate fecha) {
}

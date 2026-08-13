package com.alantek.caja.modulo.caja.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CajaArqueoRequest(
        @NotNull(message = "El saldo físico es obligatorio") BigDecimal saldoFisico,
        String observacion) {
}

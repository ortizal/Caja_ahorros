package com.alantek.caja.modulo.bancos.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ConciliacionRequest(
        @NotBlank(message = "El período es obligatorio") String periodo,
        @NotNull(message = "El saldo bancario es obligatorio") BigDecimal saldoBancario) {
}

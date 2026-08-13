package com.alantek.caja.modulo.ahorros.dto;

import jakarta.validation.constraints.NotNull;

public record CuentaAhorroRequest(
        @NotNull(message = "El socio es obligatorio") Long socioId,
        @NotNull(message = "El producto es obligatorio") Long productoId) {
}

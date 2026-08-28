package com.alantek.caja.modulo.ahorros.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record CuentaAhorroRequest(
        @NotNull(message = "El socio es obligatorio") Long socioId,
        @NotNull(message = "El producto es obligatorio") Long productoId,
        @Pattern(regexp = "NORMAL|DECIMO13|DECIMO14", message = "Tipo de ahorro no valido")
        String tipoAhorro) {
}

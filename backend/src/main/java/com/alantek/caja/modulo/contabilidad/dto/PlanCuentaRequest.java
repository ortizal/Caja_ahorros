package com.alantek.caja.modulo.contabilidad.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PlanCuentaRequest(
        @NotBlank(message = "El código es obligatorio") String codigo,
        @NotBlank(message = "El nombre es obligatorio") String nombre,
        @NotBlank(message = "El tipo es obligatorio") String tipo,
        Long cuentaPadreId,
        @NotNull(message = "El nivel es obligatorio") Integer nivel,
        Boolean aceptaMovimiento) {
}

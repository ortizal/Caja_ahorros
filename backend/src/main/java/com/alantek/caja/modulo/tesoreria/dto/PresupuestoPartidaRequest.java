package com.alantek.caja.modulo.tesoreria.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record PresupuestoPartidaRequest(
        @NotNull(message = "El anio es obligatorio")
        @Min(value = 2000, message = "El anio debe ser mayor o igual a 2000") Integer anio,
        @NotBlank(message = "El concepto es obligatorio") String concepto,
        @NotNull(message = "La cuenta contable es obligatoria") Long cuentaContableId,
        @NotNull(message = "El monto presupuestado es obligatorio")
        @DecimalMin(value = "0.01", message = "El monto debe ser mayor a cero") BigDecimal montoPresupuestado) {
}

package com.alantek.caja.modulo.creditos.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record RefinanciarRequest(
        @NotNull(message = "El nuevo plazo es obligatorio")
        @Min(value = 1, message = "El plazo debe ser al menos 1 mes")
        @Max(value = 120, message = "El plazo no puede superar 120 meses") Integer plazoMeses,
        @NotNull(message = "La tasa de interes es obligatoria")
        @DecimalMin(value = "0.0", message = "La tasa no puede ser negativa") BigDecimal tasaInteres) {
}

package com.alantek.caja.modulo.creditos.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;

public record SimulacionCreditoRequest(
        @NotNull(message = "El monto es obligatorio")
        @DecimalMin(value = "0.01", message = "El monto debe ser mayor a cero") BigDecimal monto,
        @NotNull(message = "El plazo es obligatorio")
        @Min(value = 1, message = "El plazo debe ser al menos 1 mes")
        @Max(value = 120, message = "El plazo no puede superar 120 meses") Integer plazoMeses,
        @NotNull(message = "La tasa de interes es obligatoria")
        @DecimalMin(value = "0.0", message = "La tasa no puede ser negativa") BigDecimal tasaInteres,
        @NotBlank(message = "El sistema de amortizacion es obligatorio")
        @Pattern(regexp = "FRANCES|ALEMAN|AMERICANO", message = "Sistema de amortizacion no valido")
        String sistemaAmortizacion) {
}

package com.alantek.caja.modulo.creditos.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ProductoCreditoRequest(
        @NotBlank(message = "El nombre es obligatorio") String nombre,
        @NotNull(message = "La tasa de interes es obligatoria")
        @DecimalMin(value = "0.0", message = "La tasa de interes no puede ser negativa") BigDecimal tasaInteres,
        @DecimalMin(value = "0.0", message = "La tasa de mora no puede ser negativa") BigDecimal tasaMora,
        @Pattern(regexp = "FRANCES|ALEMAN|AMERICANO", message = "Sistema de amortizacion no valido")
        String sistemaAmortizacion,
        @NotNull(message = "El plazo maximo es obligatorio")
        @Min(value = 1, message = "El plazo maximo debe ser al menos 1") Integer plazoMaxMeses,
        @DecimalMin(value = "0.0", message = "El monto minimo no puede ser negativo") BigDecimal montoMin,
        @DecimalMin(value = "0.01", message = "El monto maximo debe ser mayor a cero") BigDecimal montoMax,
        Boolean requiereGarante,
        @NotNull(message = "La fecha de inicio de vigencia es obligatoria") LocalDate vigenteDesde,
        LocalDate vigenteHasta) {
}

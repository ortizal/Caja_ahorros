package com.alantek.caja.modulo.ahorros.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ProductoAhorroRequest(
        @NotBlank(message = "El nombre es obligatorio") String nombre,
        @NotNull(message = "La tasa de interes es obligatoria")
        @DecimalMin(value = "0.00", message = "La tasa debe ser mayor o igual a cero") BigDecimal tasaInteres,
        @NotBlank(message = "La periodicidad de capitalizacion es obligatoria")
        String periodicidadCapitalizacion,
        @DecimalMin(value = "0.00", message = "El saldo minimo no puede ser negativo")
        BigDecimal saldoMinimo,
        @Min(value = 1, message = "El limite de retiros debe ser mayor a cero") Integer limiteRetirosMes,
        @NotNull(message = "La fecha de vigencia es obligatoria") LocalDate vigenteDesde,
        LocalDate vigenteHasta) {
}

package com.alantek.caja.modulo.bancos.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record BancoMovimientoRequest(
        @NotBlank(message = "El tipo es obligatorio") String tipo,
        @NotNull(message = "El monto es obligatorio")
        @DecimalMin(value = "0.01", message = "El monto debe ser mayor a cero") BigDecimal monto,
        @NotNull(message = "La fecha es obligatoria") LocalDate fecha,
        Long comprobanteId) {
}

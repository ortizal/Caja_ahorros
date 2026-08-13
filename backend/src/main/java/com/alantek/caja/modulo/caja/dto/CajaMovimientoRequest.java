package com.alantek.caja.modulo.caja.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CajaMovimientoRequest(
        @NotBlank(message = "El tipo es obligatorio") String tipo,
        @NotNull(message = "El monto es obligatorio")
        @DecimalMin(value = "0.01", message = "El monto debe ser mayor a cero") BigDecimal monto,
        String referenciaTabla,
        Long referenciaId,
        String descripcion,
        BigDecimal montoCapital,
        BigDecimal montoInteres,
        BigDecimal montoMora) {
}

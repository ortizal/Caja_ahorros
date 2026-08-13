package com.alantek.caja.modulo.creditos.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record SolicitudCreditoRequest(
        @NotNull(message = "El socio es obligatorio") Long socioId,
        @NotNull(message = "El producto es obligatorio") Long productoId,
        @NotNull(message = "El monto solicitado es obligatorio")
        @DecimalMin(value = "0.01", message = "El monto debe ser mayor a cero") BigDecimal montoSolicitado,
        @NotNull(message = "El plazo es obligatorio")
        @Min(value = 1, message = "El plazo debe ser al menos 1 mes")
        @Max(value = 120, message = "El plazo no puede superar 120 meses") Integer plazoMeses,
        String destino) {
}

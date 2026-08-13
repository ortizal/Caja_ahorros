package com.alantek.caja.modulo.aportaciones.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AportacionConfigRequest(
        @NotBlank(message = "El tipo es obligatorio") String tipo,
        @NotBlank(message = "El modo de calculo es obligatorio") String modoCalculo,
        @NotNull(message = "El valor es obligatorio")
        @DecimalMin(value = "0.01", message = "El valor debe ser mayor a cero") BigDecimal valor,
        @NotBlank(message = "La periodicidad es obligatoria") String periodicidad,
        BigDecimal montoMinimo,
        BigDecimal montoMaximo,
        @NotNull(message = "La fecha de vigencia es obligatoria") LocalDate vigenteDesde,
        LocalDate vigenteHasta) {
}

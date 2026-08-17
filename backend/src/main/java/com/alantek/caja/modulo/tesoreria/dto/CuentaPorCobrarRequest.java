package com.alantek.caja.modulo.tesoreria.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CuentaPorCobrarRequest(
        Long socioId,
        @NotBlank(message = "El deudor es obligatorio") String deudor,
        @NotBlank(message = "El concepto es obligatorio") String concepto,
        @NotNull(message = "El monto es obligatorio")
        @DecimalMin(value = "0.01", message = "El monto debe ser mayor a cero") BigDecimal monto,
        @NotNull(message = "La cuenta contable es obligatoria") Long cuentaContableId,
        LocalDate fechaEmision,
        @NotNull(message = "La fecha de vencimiento es obligatoria") LocalDate fechaVencimiento) {
}

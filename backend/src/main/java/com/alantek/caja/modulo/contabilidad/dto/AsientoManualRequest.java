package com.alantek.caja.modulo.contabilidad.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record AsientoManualRequest(
        @NotNull(message = "La fecha es obligatoria") LocalDate fecha,
        @NotBlank(message = "La descripción es obligatoria") String descripcion,
        @NotEmpty(message = "Debe indicar al menos un detalle") List<@Valid DetalleRequest> detalles) {

    public record DetalleRequest(
            @NotNull(message = "La cuenta es obligatoria") Long cuentaId,
            BigDecimal debe,
            BigDecimal haber) {
    }
}

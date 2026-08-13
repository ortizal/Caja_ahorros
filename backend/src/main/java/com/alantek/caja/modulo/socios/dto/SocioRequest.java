package com.alantek.caja.modulo.socios.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record SocioRequest(
        String codigo,

        @NotBlank(message = "La identificación es obligatoria")
        @Size(max = 20, message = "La identificación no puede exceder 20 caracteres")
        String identificacion,

        @NotBlank(message = "Los nombres son obligatorios")
        String nombres,

        @NotBlank(message = "Los apellidos son obligatorios")
        String apellidos,

        String telefono,
        String email,
        String direccion,

        @NotNull(message = "La fecha de ingreso es obligatoria")
        LocalDate fechaIngreso,

        String estado,
        Long usuarioId,

        @Valid
        List<BeneficiarioRequest> beneficiarios) {

    public record BeneficiarioRequest(
            @NotBlank(message = "El nombre del beneficiario es obligatorio") String nombres,
            String parentesco,
            @DecimalMax(value = "100.00", message = "El porcentaje máximo es 100")
            BigDecimal porcentaje) {
    }
}

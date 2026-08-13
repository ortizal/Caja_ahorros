package com.alantek.caja.modulo.socios.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record SocioResponse(
        Long id,
        String codigo,
        String identificacion,
        String nombres,
        String apellidos,
        String telefono,
        String email,
        String direccion,
        LocalDate fechaIngreso,
        LocalDate fechaRetiro,
        String estado,
        Long usuarioId,
        List<BeneficiarioResponse> beneficiarios) {

    public record BeneficiarioResponse(
            Long id,
            String nombres,
            String parentesco,
            BigDecimal porcentaje) {
    }
}

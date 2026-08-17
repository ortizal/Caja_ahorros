package com.alantek.caja.modulo.portal.dto;

import java.time.LocalDate;

public record PortalSocioResponse(
        Long id,
        String codigo,
        String identificacion,
        String nombres,
        String apellidos,
        String estado,
        LocalDate fechaIngreso) {
}

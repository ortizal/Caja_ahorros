package com.alantek.caja.modulo.creditos.dto;

import jakarta.validation.constraints.NotNull;

public record AprobarSolicitudRequest(
        @NotNull(message = "La decision es obligatoria") Boolean aprobar,
        String motivoRechazo) {
}

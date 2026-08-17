package com.alantek.caja.modulo.tesoreria.dto;

import jakarta.validation.constraints.NotNull;

public record AprobacionGastoRequest(
        @NotNull(message = "El campo aprobar es obligatorio") Boolean aprobar,
        String motivoRechazo) {
}

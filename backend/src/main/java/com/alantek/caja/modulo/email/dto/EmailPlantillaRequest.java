package com.alantek.caja.modulo.email.dto;

import jakarta.validation.constraints.NotBlank;

public record EmailPlantillaRequest(
    @NotBlank String modulo,
    @NotBlank String nombre,
    @NotBlank String asunto,
    @NotBlank String cuerpoHtml,
    String variables,
    Boolean activo
) {}

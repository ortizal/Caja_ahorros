package com.alantek.caja.modulo.email.dto;

public record EmailPlantillaResponse(
    Long id,
    String modulo,
    String nombre,
    String asunto,
    String cuerpoHtml,
    String variables,
    Boolean activo
) {}

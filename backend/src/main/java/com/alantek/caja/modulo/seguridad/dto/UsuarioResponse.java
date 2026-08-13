package com.alantek.caja.modulo.seguridad.dto;

import java.time.Instant;
import java.util.List;

public record UsuarioResponse(
        Long id,
        String username,
        String nombreCompleto,
        String email,
        String estado,
        List<String> roles,
        Instant ultimoAcceso,
        Instant createdAt) {
}

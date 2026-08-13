package com.alantek.caja.modulo.seguridad.dto;

import java.util.Set;

public record LoginResponse(
        String token,
        Long usuarioId,
        String username,
        String nombreCompleto,
        Set<String> roles,
        Set<String> permisos) {
}

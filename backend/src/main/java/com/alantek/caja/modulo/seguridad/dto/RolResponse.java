package com.alantek.caja.modulo.seguridad.dto;

public record RolResponse(
        Long id,
        String nombre,
        String descripcion,
        java.util.List<PermisoResponse> permisos) {
}

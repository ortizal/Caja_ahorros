package com.alantek.caja.modulo.seguridad.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record UsuarioRequest(
        @NotBlank(message = "El username es obligatorio")
        @Size(max = 50, message = "El username no puede exceder 50 caracteres")
        String username,

        @Size(min = 6, message = "La contraseña debe tener al menos 6 caracteres")
        String password,

        @NotBlank(message = "El nombre completo es obligatorio")
        String nombreCompleto,

        @Email(message = "Email inválido")
        String email,

        Set<Long> rolIds) {
}

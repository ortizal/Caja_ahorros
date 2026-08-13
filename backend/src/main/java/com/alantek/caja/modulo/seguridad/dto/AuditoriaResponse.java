package com.alantek.caja.modulo.seguridad.dto;

import java.time.Instant;

public record AuditoriaResponse(
        Long id,
        Long usuarioId,
        String tablaAfectada,
        Long registroId,
        String accion,
        String valorAnterior,
        String valorNuevo,
        String ip,
        Instant createdAt) {
}

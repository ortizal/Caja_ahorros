package com.alantek.caja.modulo.notificaciones.dto;

import java.time.Instant;

public record NotificacionResponse(
        Long id,
        String tipo,
        String referenciaTabla,
        Long referenciaId,
        String mensaje,
        boolean leida,
        Instant createdAt) {
}

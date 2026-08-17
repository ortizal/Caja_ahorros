package com.alantek.caja.modulo.portal.dto;

import java.math.BigDecimal;

public record PortalResumenResponse(
        PortalSocioResponse socio,
        BigDecimal saldoAhorro,
        BigDecimal totalAportado,
        BigDecimal aportePendientePeriodo,
        BigDecimal saldoCreditoVigente,
        int cuotasVencidas,
        int cuotasPendientes,
        long notificacionesNoLeidas) {
}

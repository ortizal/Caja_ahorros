package com.alantek.caja.modulo.portal.dto;

import com.alantek.caja.modulo.aportaciones.dto.AportacionPagoResponse;
import com.alantek.caja.modulo.aportaciones.dto.AportacionResponse;

import java.util.List;

public record PortalAportacionResponse(
        AportacionResponse aportacion,
        List<AportacionPagoResponse> pagos) {
}

package com.alantek.caja.modulo.portal.dto;

import com.alantek.caja.modulo.ahorros.dto.CuentaAhorroResponse;
import com.alantek.caja.modulo.ahorros.dto.MovimientoAhorroResponse;

import java.util.List;

public record PortalAhorroResponse(
        CuentaAhorroResponse cuenta,
        List<MovimientoAhorroResponse> movimientos) {
}

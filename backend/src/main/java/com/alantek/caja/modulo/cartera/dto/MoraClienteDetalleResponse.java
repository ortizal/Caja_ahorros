package com.alantek.caja.modulo.cartera.dto;

import java.math.BigDecimal;
import java.util.List;

public record MoraClienteDetalleResponse(
        MoraClienteResponse socio,
        List<CreditoMoraDetalle> creditos) {
}

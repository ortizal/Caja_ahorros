package com.alantek.caja.modulo.portal.dto;

import com.alantek.caja.modulo.creditos.dto.CreditoResponse;
import com.alantek.caja.modulo.creditos.dto.CuotaCreditoResponse;
import com.alantek.caja.modulo.creditos.dto.PagoCuotaResponse;

import java.util.List;

public record PortalCreditoResponse(
        CreditoResponse credito,
        List<CuotaCreditoResponse> cuotas,
        List<PagoCuotaResponse> pagos) {
}

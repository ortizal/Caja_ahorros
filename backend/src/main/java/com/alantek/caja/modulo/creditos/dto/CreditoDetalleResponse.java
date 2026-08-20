package com.alantek.caja.modulo.creditos.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record CreditoDetalleResponse(
        CreditoResponse credito,
        SocioCredito socio,
        ProductoCreditoResponse producto,
        List<CuotaCreditoResponse> cuotas,
        List<PagoCuotaResponse> pagos,
        List<HistorialEstado> historial,
        BigDecimal moraTotal,
        int cuotasPagadas,
        int cuotasPendientes,
        int cuotasVencidas) {

    public record SocioCredito(
            Long id,
            String codigo,
            String identificacion,
            String nombres,
            String apellidos,
            String telefono,
            String email,
            String direccion,
            LocalDate fechaIngreso,
            String estado) {}

    public record HistorialEstado(
            Long id,
            String estadoAnterior,
            String estadoNuevo,
            String motivo,
            LocalDateTime changedAt) {}
}

package com.alantek.caja.modulo.cartera.dto;

import java.math.BigDecimal;
import java.util.List;

public record DashboardGraficosResponse(
        List<SerieMensual> colocacionPorMes,
        List<SerieMensual> cobranzaPorMes,
        List<FlujoMensual> flujoCajaPorMes,
        List<PorEstado> carteraPorEstado) {

    public record SerieMensual(String mes, BigDecimal monto) {
    }

    public record FlujoMensual(String mes, BigDecimal ingresos, BigDecimal egresos) {
    }

    public record PorEstado(String estado, BigDecimal saldo, long cantidad) {
    }
}

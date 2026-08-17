package com.alantek.caja.modulo.tesoreria.dto;

import java.math.BigDecimal;
import java.util.List;

public record PresupuestoResumenResponse(
        Integer anio,
        List<PresupuestoPartidaResponse> partidas,
        BigDecimal totalPresupuestado,
        BigDecimal totalEjecutado,
        BigDecimal porcentajeEjecucion) {
}

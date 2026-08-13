package com.alantek.caja.modulo.contabilidad.dto;

public record PlanCuentaResponse(
        Long id,
        String codigo,
        String nombre,
        String tipo,
        Long cuentaPadreId,
        Integer nivel,
        Boolean aceptaMovimiento) {
}

package com.alantek.caja.modulo.socios.dto;

import java.time.LocalDate;
import java.util.List;

public record EstadoCuentaResponse(
        Long socioId,
        String codigo,
        String nombreCompleto,
        String identificacion,
        String estado,
        LocalDate fechaIngreso,
        List<Item> aportaciones,
        List<Item> ahorros,
        List<Item> creditos) {

    public record Item(String concepto, String detalle) {
    }
}

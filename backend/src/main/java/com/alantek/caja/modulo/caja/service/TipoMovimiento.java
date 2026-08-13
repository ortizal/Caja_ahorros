package com.alantek.caja.modulo.caja.service;

import java.util.Arrays;

public enum TipoMovimiento {

    APORTACION(false),
    DEPOSITO(false),
    COBRO_CREDITO(false),
    RETIRO(true),
    DESEMBOLSO(true);

    private final boolean egreso;

    TipoMovimiento(boolean egreso) {
        this.egreso = egreso;
    }

    public boolean isEgreso() {
        return egreso;
    }

    public static TipoMovimiento from(String valor) {
        if (valor == null) {
            return null;
        }
        return Arrays.stream(values())
                .filter(t -> t.name().equals(valor.toUpperCase()))
                .findFirst()
                .orElse(null);
    }
}

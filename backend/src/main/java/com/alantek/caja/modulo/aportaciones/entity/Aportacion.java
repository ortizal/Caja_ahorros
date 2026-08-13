package com.alantek.caja.modulo.aportaciones.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "aportaciones")
public class Aportacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "socio_id", nullable = false)
    private Long socioId;

    @Column(name = "config_id", nullable = false)
    private Long configId;

    @Column(nullable = false, length = 7)
    private String periodo;

    @Column(name = "monto_esperado", nullable = false)
    private BigDecimal montoEsperado;

    @Column(name = "monto_pagado", nullable = false)
    private BigDecimal montoPagado = BigDecimal.ZERO;

    @Column(nullable = false)
    private BigDecimal mora = BigDecimal.ZERO;

    @Column(nullable = false, length = 20)
    private String estado = "PENDIENTE";

    @Column(name = "exonerado_por")
    private Long exoneradoPor;

    @Column(name = "motivo_exoneracion")
    private String motivoExoneracion;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getSocioId() {
        return socioId;
    }

    public void setSocioId(Long socioId) {
        this.socioId = socioId;
    }

    public Long getConfigId() {
        return configId;
    }

    public void setConfigId(Long configId) {
        this.configId = configId;
    }

    public String getPeriodo() {
        return periodo;
    }

    public void setPeriodo(String periodo) {
        this.periodo = periodo;
    }

    public BigDecimal getMontoEsperado() {
        return montoEsperado;
    }

    public void setMontoEsperado(BigDecimal montoEsperado) {
        this.montoEsperado = montoEsperado;
    }

    public BigDecimal getMontoPagado() {
        return montoPagado;
    }

    public void setMontoPagado(BigDecimal montoPagado) {
        this.montoPagado = montoPagado;
    }

    public BigDecimal getMora() {
        return mora;
    }

    public void setMora(BigDecimal mora) {
        this.mora = mora;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public Long getExoneradoPor() {
        return exoneradoPor;
    }

    public void setExoneradoPor(Long exoneradoPor) {
        this.exoneradoPor = exoneradoPor;
    }

    public String getMotivoExoneracion() {
        return motivoExoneracion;
    }

    public void setMotivoExoneracion(String motivoExoneracion) {
        this.motivoExoneracion = motivoExoneracion;
    }
}

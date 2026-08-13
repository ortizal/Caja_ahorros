package com.alantek.caja.modulo.aportaciones.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "aportacion_pagos")
public class AportacionPago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "aportacion_id", nullable = false)
    private Long aportacionId;

    @Column(nullable = false)
    private BigDecimal monto;

    @Column(name = "caja_movimiento_id")
    private Long cajaMovimientoId;

    @CreatedDate
    @Column(name = "pagado_at", updatable = false, insertable = false)
    private Instant pagadoAt;

    @CreatedBy
    @Column(name = "registrado_por", updatable = false, insertable = false)
    private Long registradoPor;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getAportacionId() {
        return aportacionId;
    }

    public void setAportacionId(Long aportacionId) {
        this.aportacionId = aportacionId;
    }

    public BigDecimal getMonto() {
        return monto;
    }

    public void setMonto(BigDecimal monto) {
        this.monto = monto;
    }

    public Long getCajaMovimientoId() {
        return cajaMovimientoId;
    }

    public void setCajaMovimientoId(Long cajaMovimientoId) {
        this.cajaMovimientoId = cajaMovimientoId;
    }

    public Instant getPagadoAt() {
        return pagadoAt;
    }

    public void setPagadoAt(Instant pagadoAt) {
        this.pagadoAt = pagadoAt;
    }

    public Long getRegistradoPor() {
        return registradoPor;
    }

    public void setRegistradoPor(Long registradoPor) {
        this.registradoPor = registradoPor;
    }
}

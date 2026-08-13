package com.alantek.caja.modulo.caja.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "caja_arqueo")
public class CajaArqueo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "caja_apertura_id", nullable = false)
    private Long cajaAperturaId;

    @Column(name = "saldo_sistema", nullable = false)
    private BigDecimal saldoSistema;

    @Column(name = "saldo_fisico", nullable = false)
    private BigDecimal saldoFisico;

    @Column(nullable = false)
    private BigDecimal diferencia;

    @Column(length = 255)
    private String observacion;

    @Column(name = "realizado_por")
    private Long realizadoPor;

    @Column(name = "realizado_at", nullable = false, updatable = false)
    private Instant realizadoAt = Instant.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getCajaAperturaId() { return cajaAperturaId; }
    public void setCajaAperturaId(Long cajaAperturaId) { this.cajaAperturaId = cajaAperturaId; }
    public BigDecimal getSaldoSistema() { return saldoSistema; }
    public void setSaldoSistema(BigDecimal saldoSistema) { this.saldoSistema = saldoSistema; }
    public BigDecimal getSaldoFisico() { return saldoFisico; }
    public void setSaldoFisico(BigDecimal saldoFisico) { this.saldoFisico = saldoFisico; }
    public BigDecimal getDiferencia() { return diferencia; }
    public void setDiferencia(BigDecimal diferencia) { this.diferencia = diferencia; }
    public String getObservacion() { return observacion; }
    public void setObservacion(String observacion) { this.observacion = observacion; }
    public Long getRealizadoPor() { return realizadoPor; }
    public void setRealizadoPor(Long realizadoPor) { this.realizadoPor = realizadoPor; }
    public Instant getRealizadoAt() { return realizadoAt; }
    public void setRealizadoAt(Instant realizadoAt) { this.realizadoAt = realizadoAt; }
}

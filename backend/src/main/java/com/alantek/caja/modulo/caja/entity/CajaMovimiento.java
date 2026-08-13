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
@Table(name = "caja_movimiento")
public class CajaMovimiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "caja_apertura_id", nullable = false)
    private Long cajaAperturaId;

    @Column(name = "comprobante_id")
    private Long comprobanteId;

    @Column(nullable = false, length = 20)
    private String tipo;

    @Column(nullable = false)
    private BigDecimal monto;

    @Column(name = "referencia_tabla", length = 60)
    private String referenciaTabla;

    @Column(name = "referencia_id")
    private Long referenciaId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "created_by")
    private Long createdBy;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getCajaAperturaId() { return cajaAperturaId; }
    public void setCajaAperturaId(Long cajaAperturaId) { this.cajaAperturaId = cajaAperturaId; }
    public Long getComprobanteId() { return comprobanteId; }
    public void setComprobanteId(Long comprobanteId) { this.comprobanteId = comprobanteId; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public BigDecimal getMonto() { return monto; }
    public void setMonto(BigDecimal monto) { this.monto = monto; }
    public String getReferenciaTabla() { return referenciaTabla; }
    public void setReferenciaTabla(String referenciaTabla) { this.referenciaTabla = referenciaTabla; }
    public Long getReferenciaId() { return referenciaId; }
    public void setReferenciaId(Long referenciaId) { this.referenciaId = referenciaId; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
}

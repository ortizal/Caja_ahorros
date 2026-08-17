package com.alantek.caja.modulo.tesoreria.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "presupuesto_partidas")
public class PresupuestoPartida {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer anio;

    @Column(nullable = false, length = 200)
    private String concepto;

    @Column(name = "cuenta_contable_id", nullable = false)
    private Long cuentaContableId;

    @Column(name = "monto_presupuestado", nullable = false)
    private BigDecimal montoPresupuestado;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Integer getAnio() { return anio; }
    public void setAnio(Integer anio) { this.anio = anio; }
    public String getConcepto() { return concepto; }
    public void setConcepto(String concepto) { this.concepto = concepto; }
    public Long getCuentaContableId() { return cuentaContableId; }
    public void setCuentaContableId(Long cuentaContableId) { this.cuentaContableId = cuentaContableId; }
    public BigDecimal getMontoPresupuestado() { return montoPresupuestado; }
    public void setMontoPresupuestado(BigDecimal montoPresupuestado) { this.montoPresupuestado = montoPresupuestado; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}

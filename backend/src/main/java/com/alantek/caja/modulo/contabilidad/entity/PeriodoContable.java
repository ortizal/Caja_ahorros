package com.alantek.caja.modulo.contabilidad.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

@Entity
@Table(name = "periodo_contable", uniqueConstraints = @UniqueConstraint(columnNames = {"anio", "mes"}))
public class PeriodoContable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer anio;

    @Column(nullable = false)
    private Integer mes;

    @Column(nullable = false, length = 20)
    private String estado = "ABIERTO";

    @Column(name = "cerrado_por")
    private Long cerradoPor;

    @Column(name = "cerrado_at")
    private Instant cerradoAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Integer getAnio() { return anio; }
    public void setAnio(Integer anio) { this.anio = anio; }
    public Integer getMes() { return mes; }
    public void setMes(Integer mes) { this.mes = mes; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public Long getCerradoPor() { return cerradoPor; }
    public void setCerradoPor(Long cerradoPor) { this.cerradoPor = cerradoPor; }
    public Instant getCerradoAt() { return cerradoAt; }
    public void setCerradoAt(Instant cerradoAt) { this.cerradoAt = cerradoAt; }
}

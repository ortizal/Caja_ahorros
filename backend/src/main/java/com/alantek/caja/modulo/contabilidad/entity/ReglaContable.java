package com.alantek.caja.modulo.contabilidad.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;

@Entity
@Table(name = "regla_contable")
public class ReglaContable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 60)
    private String operacion;

    @Column(name = "cuenta_debe_id", nullable = false)
    private Long cuentaDebeId;

    @Column(name = "cuenta_haber_id", nullable = false)
    private Long cuentaHaberId;

    @Column(name = "vigente_desde", nullable = false)
    private LocalDate vigenteDesde;

    @Column(name = "vigente_hasta")
    private LocalDate vigenteHasta;

    @Column(nullable = false)
    private Boolean activo = true;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getOperacion() { return operacion; }
    public void setOperacion(String operacion) { this.operacion = operacion; }
    public Long getCuentaDebeId() { return cuentaDebeId; }
    public void setCuentaDebeId(Long cuentaDebeId) { this.cuentaDebeId = cuentaDebeId; }
    public Long getCuentaHaberId() { return cuentaHaberId; }
    public void setCuentaHaberId(Long cuentaHaberId) { this.cuentaHaberId = cuentaHaberId; }
    public LocalDate getVigenteDesde() { return vigenteDesde; }
    public void setVigenteDesde(LocalDate vigenteDesde) { this.vigenteDesde = vigenteDesde; }
    public LocalDate getVigenteHasta() { return vigenteHasta; }
    public void setVigenteHasta(LocalDate vigenteHasta) { this.vigenteHasta = vigenteHasta; }
    public Boolean getActivo() { return activo; }
    public void setActivo(Boolean activo) { this.activo = activo; }
}

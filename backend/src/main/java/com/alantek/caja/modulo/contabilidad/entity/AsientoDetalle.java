package com.alantek.caja.modulo.contabilidad.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "asiento_detalle")
public class AsientoDetalle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "asiento_id", nullable = false)
    private AsientoContable asiento;

    @Column(name = "cuenta_id", nullable = false)
    private Long cuentaId;

    @Column(nullable = false)
    private BigDecimal debe = BigDecimal.ZERO;

    @Column(nullable = false)
    private BigDecimal haber = BigDecimal.ZERO;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public AsientoContable getAsiento() { return asiento; }
    public void setAsiento(AsientoContable asiento) { this.asiento = asiento; }
    public Long getAsientoId() { return asiento == null ? null : asiento.getId(); }
    public Long getCuentaId() { return cuentaId; }
    public void setCuentaId(Long cuentaId) { this.cuentaId = cuentaId; }
    public BigDecimal getDebe() { return debe; }
    public void setDebe(BigDecimal debe) { this.debe = debe; }
    public BigDecimal getHaber() { return haber; }
    public void setHaber(BigDecimal haber) { this.haber = haber; }
}

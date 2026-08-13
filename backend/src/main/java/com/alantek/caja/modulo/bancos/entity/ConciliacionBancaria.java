package com.alantek.caja.modulo.bancos.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "conciliacion_bancaria")
public class ConciliacionBancaria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cuenta_bancaria_id", nullable = false)
    private Long cuentaBancariaId;

    @Column(nullable = false, length = 7)
    private String periodo;

    @Column(name = "saldo_contable", nullable = false)
    private BigDecimal saldoContable;

    @Column(name = "saldo_bancario", nullable = false)
    private BigDecimal saldoBancario;

    @Column(nullable = false)
    private BigDecimal diferencia;

    @Column(name = "realizado_por")
    private Long realizadoPor;

    @Column(name = "realizado_at", nullable = false, updatable = false)
    private Instant realizadoAt = Instant.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getCuentaBancariaId() { return cuentaBancariaId; }
    public void setCuentaBancariaId(Long cuentaBancariaId) { this.cuentaBancariaId = cuentaBancariaId; }
    public String getPeriodo() { return periodo; }
    public void setPeriodo(String periodo) { this.periodo = periodo; }
    public BigDecimal getSaldoContable() { return saldoContable; }
    public void setSaldoContable(BigDecimal saldoContable) { this.saldoContable = saldoContable; }
    public BigDecimal getSaldoBancario() { return saldoBancario; }
    public void setSaldoBancario(BigDecimal saldoBancario) { this.saldoBancario = saldoBancario; }
    public BigDecimal getDiferencia() { return diferencia; }
    public void setDiferencia(BigDecimal diferencia) { this.diferencia = diferencia; }
    public Long getRealizadoPor() { return realizadoPor; }
    public void setRealizadoPor(Long realizadoPor) { this.realizadoPor = realizadoPor; }
    public Instant getRealizadoAt() { return realizadoAt; }
    public void setRealizadoAt(Instant realizadoAt) { this.realizadoAt = realizadoAt; }
}

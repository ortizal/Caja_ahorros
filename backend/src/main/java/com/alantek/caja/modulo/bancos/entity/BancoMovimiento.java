package com.alantek.caja.modulo.bancos.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "banco_movimiento")
public class BancoMovimiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cuenta_bancaria_id", nullable = false)
    private Long cuentaBancariaId;

    @Column(nullable = false, length = 20)
    private String tipo;

    @Column(nullable = false)
    private BigDecimal monto;

    @Column(name = "comprobante_id")
    private Long comprobanteId;

    @Column(nullable = false)
    private Boolean conciliado = false;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getCuentaBancariaId() { return cuentaBancariaId; }
    public void setCuentaBancariaId(Long cuentaBancariaId) { this.cuentaBancariaId = cuentaBancariaId; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public BigDecimal getMonto() { return monto; }
    public void setMonto(BigDecimal monto) { this.monto = monto; }
    public Long getComprobanteId() { return comprobanteId; }
    public void setComprobanteId(Long comprobanteId) { this.comprobanteId = comprobanteId; }
    public Boolean getConciliado() { return conciliado; }
    public void setConciliado(Boolean conciliado) { this.conciliado = conciliado; }
    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}

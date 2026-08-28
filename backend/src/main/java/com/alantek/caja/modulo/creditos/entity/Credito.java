package com.alantek.caja.modulo.creditos.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "credito")
public class Credito {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "solicitud_id")
    private Long solicitudId;

    @Column(name = "socio_id")
    private Long socioId;

    @Column(name = "cliente_no_socio_nombre", length = 150)
    private String clienteNoSocioNombre;

    @Column(name = "cliente_no_socio_identificacion", length = 20)
    private String clienteNoSocioIdentificacion;

    @Column(name = "cliente_no_socio_telefono", length = 30)
    private String clienteNoSocioTelefono;

    @Column(name = "producto_id", nullable = false)
    private Long productoId;

    @Column(name = "monto_desembolsado", nullable = false)
    private BigDecimal montoDesembolsado;

    @Column(name = "tasa_interes", nullable = false)
    private BigDecimal tasaInteres;

    @Column(name = "plazo_meses", nullable = false)
    private Integer plazoMeses;

    @Column(name = "fecha_desembolso")
    private LocalDate fechaDesembolso;

    @Column(name = "saldo_capital", nullable = false)
    private BigDecimal saldoCapital;

    @Column(name = "abono_capital_total", nullable = false)
    private BigDecimal abonoCapitalTotal = BigDecimal.ZERO;

    @Column(nullable = false, length = 20)
    private String estado = "APROBADA";

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "created_by")
    private Long createdBy;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getSolicitudId() {
        return solicitudId;
    }

    public void setSolicitudId(Long solicitudId) {
        this.solicitudId = solicitudId;
    }

    public Long getSocioId() {
        return socioId;
    }

    public void setSocioId(Long socioId) {
        this.socioId = socioId;
    }

    public String getClienteNoSocioNombre() {
        return clienteNoSocioNombre;
    }

    public void setClienteNoSocioNombre(String clienteNoSocioNombre) {
        this.clienteNoSocioNombre = clienteNoSocioNombre;
    }

    public String getClienteNoSocioIdentificacion() {
        return clienteNoSocioIdentificacion;
    }

    public void setClienteNoSocioIdentificacion(String clienteNoSocioIdentificacion) {
        this.clienteNoSocioIdentificacion = clienteNoSocioIdentificacion;
    }

    public String getClienteNoSocioTelefono() {
        return clienteNoSocioTelefono;
    }

    public void setClienteNoSocioTelefono(String clienteNoSocioTelefono) {
        this.clienteNoSocioTelefono = clienteNoSocioTelefono;
    }

    public Long getProductoId() {
        return productoId;
    }

    public void setProductoId(Long productoId) {
        this.productoId = productoId;
    }

    public BigDecimal getMontoDesembolsado() {
        return montoDesembolsado;
    }

    public void setMontoDesembolsado(BigDecimal montoDesembolsado) {
        this.montoDesembolsado = montoDesembolsado;
    }

    public BigDecimal getTasaInteres() {
        return tasaInteres;
    }

    public void setTasaInteres(BigDecimal tasaInteres) {
        this.tasaInteres = tasaInteres;
    }

    public Integer getPlazoMeses() {
        return plazoMeses;
    }

    public void setPlazoMeses(Integer plazoMeses) {
        this.plazoMeses = plazoMeses;
    }

    public LocalDate getFechaDesembolso() {
        return fechaDesembolso;
    }

    public void setFechaDesembolso(LocalDate fechaDesembolso) {
        this.fechaDesembolso = fechaDesembolso;
    }

    public BigDecimal getSaldoCapital() {
        return saldoCapital;
    }

    public void setSaldoCapital(BigDecimal saldoCapital) {
        this.saldoCapital = saldoCapital;
    }

    public BigDecimal getAbonoCapitalTotal() {
        return abonoCapitalTotal;
    }

    public void setAbonoCapitalTotal(BigDecimal abonoCapitalTotal) {
        this.abonoCapitalTotal = abonoCapitalTotal;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }
}

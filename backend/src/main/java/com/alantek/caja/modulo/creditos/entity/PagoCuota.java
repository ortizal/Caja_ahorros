package com.alantek.caja.modulo.creditos.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "pago_cuota")
public class PagoCuota {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cuota_id")
    private Long cuotaId;

    @Column(name = "credito_id", nullable = false)
    private Long creditoId;

    @Column(nullable = false, length = 20)
    private String tipo = "CUOTA";

    @Column(name = "monto_capital", nullable = false)
    private BigDecimal montoCapital;

    @Column(name = "monto_interes", nullable = false)
    private BigDecimal montoInteres;

    @Column(name = "monto_mora", nullable = false)
    private BigDecimal montoMora = BigDecimal.ZERO;

    @Column(name = "monto_abono_capital", nullable = false)
    private BigDecimal montoAbonoCapital = BigDecimal.ZERO;

    @Column(length = 255)
    private String descripcion;

    @Column(name = "comprobante_id")
    private Long comprobanteId;

    @Column(name = "pagado_at", nullable = false)
    private LocalDateTime pagadoAt = LocalDateTime.now();

    @Column(name = "registrado_por")
    private Long registradoPor;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCuotaId() {
        return cuotaId;
    }

    public void setCuotaId(Long cuotaId) {
        this.cuotaId = cuotaId;
    }

    public Long getCreditoId() {
        return creditoId;
    }

    public void setCreditoId(Long creditoId) {
        this.creditoId = creditoId;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public BigDecimal getMontoCapital() {
        return montoCapital;
    }

    public void setMontoCapital(BigDecimal montoCapital) {
        this.montoCapital = montoCapital;
    }

    public BigDecimal getMontoInteres() {
        return montoInteres;
    }

    public void setMontoInteres(BigDecimal montoInteres) {
        this.montoInteres = montoInteres;
    }

    public BigDecimal getMontoMora() {
        return montoMora;
    }

    public void setMontoMora(BigDecimal montoMora) {
        this.montoMora = montoMora;
    }

    public BigDecimal getMontoAbonoCapital() {
        return montoAbonoCapital;
    }

    public void setMontoAbonoCapital(BigDecimal montoAbonoCapital) {
        this.montoAbonoCapital = montoAbonoCapital;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public Long getComprobanteId() {
        return comprobanteId;
    }

    public void setComprobanteId(Long comprobanteId) {
        this.comprobanteId = comprobanteId;
    }

    public LocalDateTime getPagadoAt() {
        return pagadoAt;
    }

    public void setPagadoAt(LocalDateTime pagadoAt) {
        this.pagadoAt = pagadoAt;
    }

    public Long getRegistradoPor() {
        return registradoPor;
    }

    public void setRegistradoPor(Long registradoPor) {
        this.registradoPor = registradoPor;
    }
}

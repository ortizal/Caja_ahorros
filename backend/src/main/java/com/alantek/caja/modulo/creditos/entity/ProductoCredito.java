package com.alantek.caja.modulo.creditos.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "producto_credito")
public class ProductoCredito {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 80)
    private String nombre;

    @Column(name = "tasa_interes", nullable = false)
    private BigDecimal tasaInteres;

    @Column(name = "tasa_mora", nullable = false)
    private BigDecimal tasaMora = BigDecimal.ONE;

    @Column(name = "sistema_amortizacion", nullable = false, length = 20)
    private String sistemaAmortizacion = "FRANCES";

    @Column(name = "plazo_max_meses", nullable = false)
    private Integer plazoMaxMeses;

    @Column(name = "monto_min")
    private BigDecimal montoMin;

    @Column(name = "monto_max")
    private BigDecimal montoMax;

    @Column(name = "requiere_garante", nullable = false)
    private boolean requiereGarante = false;

    @Column(name = "vigente_desde", nullable = false)
    private LocalDate vigenteDesde;

    @Column(name = "vigente_hasta")
    private LocalDate vigenteHasta;

    @Column(nullable = false)
    private boolean activo = true;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public BigDecimal getTasaInteres() {
        return tasaInteres;
    }

    public void setTasaInteres(BigDecimal tasaInteres) {
        this.tasaInteres = tasaInteres;
    }

    public BigDecimal getTasaMora() {
        return tasaMora;
    }

    public void setTasaMora(BigDecimal tasaMora) {
        this.tasaMora = tasaMora;
    }

    public String getSistemaAmortizacion() {
        return sistemaAmortizacion;
    }

    public void setSistemaAmortizacion(String sistemaAmortizacion) {
        this.sistemaAmortizacion = sistemaAmortizacion;
    }

    public Integer getPlazoMaxMeses() {
        return plazoMaxMeses;
    }

    public void setPlazoMaxMeses(Integer plazoMaxMeses) {
        this.plazoMaxMeses = plazoMaxMeses;
    }

    public BigDecimal getMontoMin() {
        return montoMin;
    }

    public void setMontoMin(BigDecimal montoMin) {
        this.montoMin = montoMin;
    }

    public BigDecimal getMontoMax() {
        return montoMax;
    }

    public void setMontoMax(BigDecimal montoMax) {
        this.montoMax = montoMax;
    }

    public boolean isRequiereGarante() {
        return requiereGarante;
    }

    public void setRequiereGarante(boolean requiereGarante) {
        this.requiereGarante = requiereGarante;
    }

    public LocalDate getVigenteDesde() {
        return vigenteDesde;
    }

    public void setVigenteDesde(LocalDate vigenteDesde) {
        this.vigenteDesde = vigenteDesde;
    }

    public LocalDate getVigenteHasta() {
        return vigenteHasta;
    }

    public void setVigenteHasta(LocalDate vigenteHasta) {
        this.vigenteHasta = vigenteHasta;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }
}

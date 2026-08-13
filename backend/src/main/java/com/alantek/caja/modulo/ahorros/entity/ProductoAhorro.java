package com.alantek.caja.modulo.ahorros.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "producto_ahorro")
public class ProductoAhorro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 80)
    private String nombre;

    @Column(name = "tasa_interes", nullable = false)
    private BigDecimal tasaInteres;

    @Column(name = "periodicidad_capitalizacion", nullable = false, length = 20)
    private String periodicidadCapitalizacion;

    @Column(name = "saldo_minimo", nullable = false)
    private BigDecimal saldoMinimo = BigDecimal.ZERO;

    @Column(name = "limite_retiros_mes")
    private Integer limiteRetirosMes;

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

    public String getPeriodicidadCapitalizacion() {
        return periodicidadCapitalizacion;
    }

    public void setPeriodicidadCapitalizacion(String periodicidadCapitalizacion) {
        this.periodicidadCapitalizacion = periodicidadCapitalizacion;
    }

    public BigDecimal getSaldoMinimo() {
        return saldoMinimo;
    }

    public void setSaldoMinimo(BigDecimal saldoMinimo) {
        this.saldoMinimo = saldoMinimo;
    }

    public Integer getLimiteRetirosMes() {
        return limiteRetirosMes;
    }

    public void setLimiteRetirosMes(Integer limiteRetirosMes) {
        this.limiteRetirosMes = limiteRetirosMes;
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

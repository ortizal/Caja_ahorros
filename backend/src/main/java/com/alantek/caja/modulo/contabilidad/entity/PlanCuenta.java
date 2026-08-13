package com.alantek.caja.modulo.contabilidad.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "plan_cuentas")
public class PlanCuenta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 20)
    private String codigo;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(nullable = false, length = 20)
    private String tipo;

    @Column(name = "cuenta_padre_id")
    private Long cuentaPadreId;

    @Column(nullable = false)
    private Integer nivel;

    @Column(name = "acepta_movimiento", nullable = false)
    private Boolean aceptaMovimiento = true;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public Long getCuentaPadreId() { return cuentaPadreId; }
    public void setCuentaPadreId(Long cuentaPadreId) { this.cuentaPadreId = cuentaPadreId; }
    public Integer getNivel() { return nivel; }
    public void setNivel(Integer nivel) { this.nivel = nivel; }
    public Boolean getAceptaMovimiento() { return aceptaMovimiento; }
    public void setAceptaMovimiento(Boolean aceptaMovimiento) { this.aceptaMovimiento = aceptaMovimiento; }
}

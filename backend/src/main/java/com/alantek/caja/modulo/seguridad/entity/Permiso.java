package com.alantek.caja.modulo.seguridad.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "permisos", uniqueConstraints = @UniqueConstraint(columnNames = {"modulo", "accion"}))
public class Permiso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String modulo;

    @Column(nullable = false, length = 30)
    private String accion;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getModulo() { return modulo; }
    public void setModulo(String modulo) { this.modulo = modulo; }
    public String getAccion() { return accion; }
    public void setAccion(String accion) { this.accion = accion; }

    public String getAuthority() { return modulo + ":" + accion; }
}

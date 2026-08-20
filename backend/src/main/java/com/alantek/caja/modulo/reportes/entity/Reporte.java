package com.alantek.caja.modulo.reportes.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;

@Entity
@Table(name = "reportes")
public class Reporte {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String nombre;

    @Column(length = 255)
    private String descripcion;

    @Column(nullable = false, length = 200)
    private String titulo;

    @Column(nullable = false, length = 50)
    private String entidad;

    @Column(name = "formato_default", nullable = false, length = 10)
    private String formatoDefault = "pdf";

    @Column(nullable = false, length = 10)
    private String orientacion = "portrait";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> parametros;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String jrxml;

    @Column(nullable = false)
    private Boolean activo = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    public Reporte() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public String getEntidad() { return entidad; }
    public void setEntidad(String entidad) { this.entidad = entidad; }

    public String getFormatoDefault() { return formatoDefault; }
    public void setFormatoDefault(String formatoDefault) { this.formatoDefault = formatoDefault; }

    public String getOrientacion() { return orientacion; }
    public void setOrientacion(String orientacion) { this.orientacion = orientacion; }

    public Map<String, Object> getParametros() { return parametros; }
    public void setParametros(Map<String, Object> parametros) { this.parametros = parametros; }

    public String getJrxml() { return jrxml; }
    public void setJrxml(String jrxml) { this.jrxml = jrxml; }

    public Boolean getActivo() { return activo; }
    public void setActivo(Boolean activo) { this.activo = activo; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}

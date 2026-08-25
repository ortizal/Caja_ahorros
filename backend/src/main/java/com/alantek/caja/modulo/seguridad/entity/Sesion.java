package com.alantek.caja.modulo.seguridad.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "sesiones")
public class Sesion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id")
    private Long usuarioId;

    @Column(columnDefinition = "TEXT")
    private String token;

    @Column(length = 45)
    private String ip;

    @Column(name = "user_agent", length = 255)
    private String userAgent;

    @Column(name = "iniciada_at", nullable = false, updatable = false)
    private Instant iniciadaAt = Instant.now();

    @Column(name = "cerrada_at")
    private Instant cerradaAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Long usuarioId) { this.usuarioId = usuarioId; }
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public String getIp() { return ip; }
    public void setIp(String ip) { this.ip = ip; }
    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
    public Instant getIniciadaAt() { return iniciadaAt; }
    public void setIniciadaAt(Instant iniciadaAt) { this.iniciadaAt = iniciadaAt; }
    public Instant getCerradaAt() { return cerradaAt; }
    public void setCerradaAt(Instant cerradaAt) { this.cerradaAt = cerradaAt; }
}

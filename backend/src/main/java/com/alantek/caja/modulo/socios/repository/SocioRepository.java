package com.alantek.caja.modulo.socios.repository;

import com.alantek.caja.modulo.socios.entity.Socio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface SocioRepository extends JpaRepository<Socio, Long> {

    Optional<Socio> findByCodigo(String codigo);

    Optional<Socio> findByIdentificacion(String identificacion);

    List<Socio> findByUsuarioId(Long usuarioId);

    @Query("SELECT COALESCE(MAX(s.id), 0) FROM Socio s")
    Long maxId();

    long countByEstado(String estado);

    java.util.List<Socio> findByEstado(String estado);
}

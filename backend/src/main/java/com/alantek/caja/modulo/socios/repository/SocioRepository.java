package com.alantek.caja.modulo.socios.repository;

import com.alantek.caja.modulo.socios.entity.Socio;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SocioRepository extends JpaRepository<Socio, Long> {

    Optional<Socio> findByCodigo(String codigo);

    Optional<Socio> findByIdentificacion(String identificacion);

    List<Socio> findByEstado(String estado);

    Page<Socio> findByEstado(String estado, Pageable pageable);

    @Query("""
            SELECT s FROM Socio s
            WHERE (:estado IS NULL OR s.estado = :estado)
              AND (:q IS NULL
                   OR LOWER(s.nombres) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(s.apellidos) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(s.identificacion) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(s.codigo) LIKE LOWER(CONCAT('%', :q, '%')))
            """)
    Page<Socio> buscar(@Param("q") String q, @Param("estado") String estado, Pageable pageable);

    List<Socio> findByUsuarioId(Long usuarioId);

    @Query("SELECT COALESCE(MAX(s.id), 0) FROM Socio s")
    Long maxId();

    long countByEstado(String estado);
}

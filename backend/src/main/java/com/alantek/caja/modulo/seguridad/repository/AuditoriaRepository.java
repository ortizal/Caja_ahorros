package com.alantek.caja.modulo.seguridad.repository;

import com.alantek.caja.modulo.seguridad.entity.Auditoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface AuditoriaRepository extends JpaRepository<Auditoria, Long> {

    @Query("""
            SELECT a FROM Auditoria a
            WHERE (:tabla IS NULL OR a.tablaAfectada = :tabla)
              AND a.createdAt >= :desde
              AND a.createdAt <= :hasta
            ORDER BY a.createdAt DESC
            """)
    List<Auditoria> filtrar(@Param("tabla") String tabla,
                            @Param("desde") Instant desde,
                            @Param("hasta") Instant hasta);
}

package com.alantek.caja.modulo.notificaciones.repository;

import com.alantek.caja.modulo.notificaciones.entity.Notificacion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {

    List<Notificacion> findByUsuarioIdOrderByCreatedAtDesc(Long usuarioId);

    Page<Notificacion> findByUsuarioIdOrderByCreatedAtDesc(Long usuarioId, Pageable pageable);

    List<Notificacion> findByUsuarioIdAndLeidaFalseOrderByCreatedAtDesc(Long usuarioId);

    Optional<Notificacion> findByIdAndUsuarioId(Long id, Long usuarioId);

    long countByUsuarioIdAndLeidaFalse(Long usuarioId);

    boolean existsByUsuarioIdAndTipoAndReferenciaTablaAndReferenciaId(
            Long usuarioId, String tipo, String referenciaTabla, Long referenciaId);
}

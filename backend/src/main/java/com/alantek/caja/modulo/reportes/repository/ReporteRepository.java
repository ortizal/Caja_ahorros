package com.alantek.caja.modulo.reportes.repository;

import com.alantek.caja.modulo.reportes.entity.Reporte;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReporteRepository extends JpaRepository<Reporte, Long> {

    Optional<Reporte> findByNombre(String nombre);

    List<Reporte> findAllByActivoTrue();

    Page<Reporte> findByNombreContainingIgnoreCaseOrDescripcionContainingIgnoreCaseOrEntidadContainingIgnoreCase(
            String nombre, String descripcion, String entidad, Pageable pageable);

    boolean existsByNombre(String nombre);
}

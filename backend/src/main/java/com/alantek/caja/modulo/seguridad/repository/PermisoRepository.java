package com.alantek.caja.modulo.seguridad.repository;

import com.alantek.caja.modulo.seguridad.entity.Permiso;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PermisoRepository extends JpaRepository<Permiso, Long> {

    Optional<Permiso> findByModuloAndAccion(String modulo, String accion);
}

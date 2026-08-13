package com.alantek.caja.modulo.seguridad.repository;

import com.alantek.caja.modulo.seguridad.entity.Sesion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SesionRepository extends JpaRepository<Sesion, Long> {

    Optional<Sesion> findByToken(String token);
}

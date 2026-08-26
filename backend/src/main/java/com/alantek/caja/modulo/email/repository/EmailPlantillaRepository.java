package com.alantek.caja.modulo.email.repository;

import com.alantek.caja.modulo.email.entity.EmailPlantilla;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EmailPlantillaRepository extends JpaRepository<EmailPlantilla, Long> {
    List<EmailPlantilla> findByModuloOrderByNombreAsc(String modulo);
    List<EmailPlantilla> findAllByOrderByModuloAscNombreAsc();
    Optional<EmailPlantilla> findByModuloAndNombre(String modulo, String nombre);
}

package com.alantek.caja.modulo.email.repository;

import com.alantek.caja.modulo.email.entity.EmailConfiguracion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmailConfiguracionRepository extends JpaRepository<EmailConfiguracion, Long> {
    Optional<EmailConfiguracion> findFirstByActivoTrue();
}

package com.alantek.caja.modulo.caja.repository;

import com.alantek.caja.modulo.caja.entity.Comprobante;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface ComprobanteRepository extends JpaRepository<Comprobante, Long> {

    Optional<Comprobante> findByNumero(String numero);

    @Query("SELECT COALESCE(MAX(c.id), 0) FROM Comprobante c")
    Long maxId();
}

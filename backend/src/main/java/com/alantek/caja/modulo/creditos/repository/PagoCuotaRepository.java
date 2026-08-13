package com.alantek.caja.modulo.creditos.repository;

import com.alantek.caja.modulo.creditos.entity.PagoCuota;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PagoCuotaRepository extends JpaRepository<PagoCuota, Long> {

    List<PagoCuota> findByCreditoIdOrderByPagadoAtAsc(Long creditoId);

    List<PagoCuota> findByCuotaId(Long cuotaId);
}

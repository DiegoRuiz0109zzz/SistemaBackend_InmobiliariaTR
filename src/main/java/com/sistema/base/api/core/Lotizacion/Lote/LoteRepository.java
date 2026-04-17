package com.sistema.base.api.core.Lotizacion.Lote;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LoteRepository extends JpaRepository<Lote, Long> {
    List<Lote> findByEnabledTrue();
    List<Lote> findByManzanaIdAndEnabledTrue(Long manzanaId); // Para listar los lotes de una manzana
}

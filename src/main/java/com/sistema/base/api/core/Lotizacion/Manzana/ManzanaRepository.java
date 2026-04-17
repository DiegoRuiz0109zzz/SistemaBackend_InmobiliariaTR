package com.sistema.base.api.core.Lotizacion.Manzana;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ManzanaRepository extends JpaRepository<Manzana, Long> {
    List<Manzana> findByEnabledTrue();
    List<Manzana> findByEtapaIdAndEnabledTrue(Long etapaId); // Para el combo en cascada
}

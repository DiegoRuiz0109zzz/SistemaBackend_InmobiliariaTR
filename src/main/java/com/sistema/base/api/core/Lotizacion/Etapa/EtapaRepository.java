package com.sistema.base.api.core.Lotizacion.Etapa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EtapaRepository extends JpaRepository<Etapa, Long> {
    List<Etapa> findByEnabledTrue();
    List<Etapa> findByUrbanizacionIdAndEnabledTrue(Long urbanizacionId); // Para el combo en cascada
}

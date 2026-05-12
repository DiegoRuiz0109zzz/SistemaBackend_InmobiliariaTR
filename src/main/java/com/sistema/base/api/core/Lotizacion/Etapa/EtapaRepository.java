package com.sistema.base.api.core.Lotizacion.Etapa;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EtapaRepository extends JpaRepository<Etapa, Long> {
    List<Etapa> findByEnabledTrue();
    List<Etapa> findByUrbanizacionIdAndEnabledTrue(Long urbanizacionId); // Para el combo en cascada

    // ✅ PARA LA TABLA PAGINADA: Las 4 combinaciones posibles
    Page<Etapa> findByEnabledTrue(Pageable pageable);

    Page<Etapa> findByEnabledTrueAndNombreContainingIgnoreCase(String nombre, Pageable pageable);

    Page<Etapa> findByEnabledTrueAndUrbanizacionId(Long urbanizacionId, Pageable pageable);

    Page<Etapa> findByEnabledTrueAndUrbanizacionIdAndNombreContainingIgnoreCase(Long urbanizacionId, String nombre, Pageable pageable);
}

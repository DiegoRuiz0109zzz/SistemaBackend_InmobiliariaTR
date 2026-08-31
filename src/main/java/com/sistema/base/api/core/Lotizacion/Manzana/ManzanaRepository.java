package com.sistema.base.api.core.Lotizacion.Manzana;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ManzanaRepository extends JpaRepository<Manzana, Long> {
    List<Manzana> findByEnabledTrue();
    List<Manzana> findByEtapaIdAndEnabledTrueOrderByNombreAsc(Long etapaId);// Para el combo en cascada

    @Query("SELECT m FROM Manzana m WHERE m.enabled = true " +
            "AND (COALESCE(:urbanizacionId, NULL) IS NULL OR m.etapa.urbanizacion.id = :urbanizacionId) " +
            "AND (COALESCE(:etapaId, NULL) IS NULL OR m.etapa.id = :etapaId) " +
            "AND (COALESCE(:nombre, NULL) IS NULL OR LOWER(m.nombre) LIKE LOWER(CONCAT('%', :nombre, '%')))")
    Page<Manzana> findByFiltrosPaginado(
            @Param("urbanizacionId") Long urbanizacionId,
            @Param("etapaId") Long etapaId,
            @Param("nombre") String nombre,
            Pageable pageable);

    @Query("SELECT COALESCE(SUM(m.areaTotal), 0.0) FROM Manzana m WHERE m.enabled = true " +
           "AND (:urbanizacionId IS NULL OR m.etapa.urbanizacion.id = :urbanizacionId) " +
           "AND (:etapaId IS NULL OR m.etapa.id = :etapaId) " +
           "AND (:manzanaId IS NULL OR m.id = :manzanaId)")
    Double sumAreaTotalManzanas(@Param("urbanizacionId") Long urbanizacionId, @Param("etapaId") Long etapaId, @Param("manzanaId") Long manzanaId);
}

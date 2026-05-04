package com.sistema.base.api.core.Lotizacion.Lote;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

@Repository
public interface LoteRepository extends JpaRepository<Lote, Long> {
    List<Lote> findByEnabledTrue();
    List<Lote> findByManzanaIdAndEnabledTrue(Long manzanaId); // Para listar los lotes de una manzana

    // ==========================================
    // CONSULTAS PARA DASHBOARD
    // ==========================================
    @Query("SELECT COUNT(l) FROM Lote l WHERE l.enabled = true " +
           "AND (:urbanizacionId IS NULL OR l.manzana.etapa.urbanizacion.id = :urbanizacionId) " +
           "AND (:etapaId IS NULL OR l.manzana.etapa.id = :etapaId) " +
           "AND (:manzanaId IS NULL OR l.manzana.id = :manzanaId)")
    Long countTotalLotes(@Param("urbanizacionId") Long urbanizacionId, @Param("etapaId") Long etapaId, @Param("manzanaId") Long manzanaId);

    @Query("SELECT COUNT(l) FROM Lote l WHERE l.enabled = true AND l.estadoVenta = :estado " +
           "AND (:urbanizacionId IS NULL OR l.manzana.etapa.urbanizacion.id = :urbanizacionId) " +
           "AND (:etapaId IS NULL OR l.manzana.etapa.id = :etapaId) " +
           "AND (:manzanaId IS NULL OR l.manzana.id = :manzanaId)")
    Long countLotesByEstado(@Param("estado") EstadoLote estado, @Param("urbanizacionId") Long urbanizacionId, @Param("etapaId") Long etapaId, @Param("manzanaId") Long manzanaId);

    @Query("SELECT COALESCE(SUM(l.precioVenta), 0.0) FROM Lote l WHERE l.enabled = true " +
           "AND (:urbanizacionId IS NULL OR l.manzana.etapa.urbanizacion.id = :urbanizacionId) " +
           "AND (:etapaId IS NULL OR l.manzana.etapa.id = :etapaId) " +
           "AND (:manzanaId IS NULL OR l.manzana.id = :manzanaId)")
    Double sumValorTotalLotes(@Param("urbanizacionId") Long urbanizacionId, @Param("etapaId") Long etapaId, @Param("manzanaId") Long manzanaId);

    @Query("SELECT COALESCE(SUM(l.precioVenta), 0.0) FROM Lote l WHERE l.enabled = true AND l.estadoVenta = :estado " +
           "AND (:urbanizacionId IS NULL OR l.manzana.etapa.urbanizacion.id = :urbanizacionId) " +
           "AND (:etapaId IS NULL OR l.manzana.etapa.id = :etapaId) " +
           "AND (:manzanaId IS NULL OR l.manzana.id = :manzanaId)")
    Double sumValorLotesByEstado(@Param("estado") EstadoLote estado, @Param("urbanizacionId") Long urbanizacionId, @Param("etapaId") Long etapaId, @Param("manzanaId") Long manzanaId);
}

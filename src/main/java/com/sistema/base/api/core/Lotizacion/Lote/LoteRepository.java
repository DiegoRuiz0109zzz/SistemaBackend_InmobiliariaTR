package com.sistema.base.api.core.Lotizacion.Lote;

import com.sistema.base.api.core.Dashboard.dtos.MensualChartDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

@Repository
public interface LoteRepository extends JpaRepository<Lote, Long> {
    List<Lote> findByEnabledTrue();
    // Para combos en cascada (ordenado por número)
    List<Lote> findByManzanaIdAndEnabledTrueOrderByNumeroAsc(Long manzanaId);

    // ✅ PARA LA TABLA PAGINADA: Las 4 combinaciones posibles
    Page<Lote> findByEnabledTrue(Pageable pageable);

    Page<Lote> findByEnabledTrueAndNumeroContainingIgnoreCase(String numero, Pageable pageable);

    Page<Lote> findByEnabledTrueAndManzanaId(Long manzanaId, Pageable pageable);

    Page<Lote> findByEnabledTrueAndManzanaIdAndNumeroContainingIgnoreCase(Long manzanaId, String numero, Pageable pageable);
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

    @Query("SELECT new com.sistema.base.api.core.Dashboard.dtos.MensualChartDTO(" +
            "CAST(MONTH(c.fechaContrato) AS string), COUNT(c), SUM(c.precioTotal)) " +
            "FROM Contrato c WHERE YEAR(c.fechaContrato) = :anio AND " +
            "(:urbId IS NULL OR c.lote.manzana.etapa.urbanizacion.id = :urbId) AND " +
            "(:etapaId IS NULL OR c.lote.manzana.etapa.id = :etapaId) AND " +
            "(:manzId IS NULL OR c.lote.manzana.id = :manzId) " +
            "GROUP BY MONTH(c.fechaContrato)")
    List<MensualChartDTO> findVentasMensuales(Integer anio, Long urbId, Long etapaId, Long manzId);
}

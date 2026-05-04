package com.sistema.base.api.core.Financiamiento.Cuota;

import com.sistema.base.api.core.Dashboard.dtos.MensualChartDTO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

@Repository
public interface CuotaRepository extends JpaRepository<Cuota, Long> {
    List<Cuota> findByContratoIdAndEnabledTrueOrderByNumeroCuotaAsc(Long contratoId);

    // ==========================================
    // CONSULTAS PARA DASHBOARD
    // ==========================================
    @Query("SELECT COALESCE(SUM(c.montoPagado), 0.0) FROM Cuota c WHERE c.enabled = true " +
           "AND (:urbanizacionId IS NULL OR c.contrato.lote.manzana.etapa.urbanizacion.id = :urbanizacionId) " +
           "AND (:etapaId IS NULL OR c.contrato.lote.manzana.etapa.id = :etapaId) " +
           "AND (:manzanaId IS NULL OR c.contrato.lote.manzana.id = :manzanaId)")
    Double sumMontoPagado(@Param("urbanizacionId") Long urbanizacionId, @Param("etapaId") Long etapaId, @Param("manzanaId") Long manzanaId);

    @Query("SELECT COALESCE(SUM(c.montoTotal - c.montoPagado), 0.0) FROM Cuota c WHERE c.enabled = true AND c.montoTotal > c.montoPagado " +
           "AND (:urbanizacionId IS NULL OR c.contrato.lote.manzana.etapa.urbanizacion.id = :urbanizacionId) " +
           "AND (:etapaId IS NULL OR c.contrato.lote.manzana.etapa.id = :etapaId) " +
           "AND (:manzanaId IS NULL OR c.contrato.lote.manzana.id = :manzanaId)")
    Double sumMontoPorCobrar(@Param("urbanizacionId") Long urbanizacionId, @Param("etapaId") Long etapaId, @Param("manzanaId") Long manzanaId);

    @Query("SELECT new com.sistema.base.api.core.Dashboard.dtos.MensualChartDTO(CAST(MONTH(c.fechaVencimiento) AS string), 0L, COALESCE(SUM(c.montoTotal), 0.0)) " +
           "FROM Cuota c WHERE c.enabled = true AND YEAR(c.fechaVencimiento) = :anio " +
           "AND (:urbanizacionId IS NULL OR c.contrato.lote.manzana.etapa.urbanizacion.id = :urbanizacionId) " +
           "AND (:etapaId IS NULL OR c.contrato.lote.manzana.etapa.id = :etapaId) " +
           "AND (:manzanaId IS NULL OR c.contrato.lote.manzana.id = :manzanaId) " +
           "GROUP BY MONTH(c.fechaVencimiento) ORDER BY MONTH(c.fechaVencimiento)")
    List<MensualChartDTO> findProyeccionCobrosMensuales(@Param("anio") Integer anio, @Param("urbanizacionId") Long urbanizacionId, @Param("etapaId") Long etapaId, @Param("manzanaId") Long manzanaId);
}
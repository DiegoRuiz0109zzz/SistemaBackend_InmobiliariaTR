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

    @Query("SELECT new com.sistema.base.api.core.Dashboard.dtos.MensualChartDTO(" +
            "CAST(EXTRACT(MONTH FROM c.fechaVencimiento) AS string), COUNT(c), SUM(c.montoTotal)) " +
            "FROM Cuota c WHERE EXTRACT(YEAR FROM c.fechaVencimiento) = :anio AND " +
            "(:urbId IS NULL OR c.contrato.lote.manzana.etapa.urbanizacion.id = :urbId) AND " +
            "(:etapaId IS NULL OR c.contrato.lote.manzana.etapa.id = :etapaId) AND " +
            "(:manzId IS NULL OR c.contrato.lote.manzana.id = :manzId) " +
            "GROUP BY EXTRACT(MONTH FROM c.fechaVencimiento)")
    List<MensualChartDTO> findProyeccionCobrosMensuales(Integer anio, Long urbId, Long etapaId, Long manzId);
}
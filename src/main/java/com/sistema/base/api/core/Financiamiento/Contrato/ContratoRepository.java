package com.sistema.base.api.core.Financiamiento.Contrato;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

@Repository
public interface ContratoRepository extends JpaRepository<Contrato, Long> {
    List<Contrato> findByEnabledTrue();
    List<Contrato> findByClienteIdAndEnabledTrue(Long clienteId);

    // ==========================================
    // CONSULTAS PARA DASHBOARD
    // ==========================================
    @Query("SELECT new com.sistema.base.api.core.Dashboard.dtos.MensualChartDTO(CAST(MONTH(c.fechaContrato) AS string), COUNT(c), COALESCE(SUM(c.precioTotal), 0.0)) " +
           "FROM Contrato c WHERE c.enabled = true AND YEAR(c.fechaContrato) = :anio " +
           "AND (:urbanizacionId IS NULL OR c.lote.manzana.etapa.urbanizacion.id = :urbanizacionId) " +
           "AND (:etapaId IS NULL OR c.lote.manzana.etapa.id = :etapaId) " +
           "AND (:manzanaId IS NULL OR c.lote.manzana.id = :manzanaId) " +
           "GROUP BY MONTH(c.fechaContrato) ORDER BY MONTH(c.fechaContrato)")
    List<com.sistema.base.api.core.Dashboard.dtos.MensualChartDTO> findVentasMensuales(@Param("anio") Integer anio, @Param("urbanizacionId") Long urbanizacionId, @Param("etapaId") Long etapaId, @Param("manzanaId") Long manzanaId);
}

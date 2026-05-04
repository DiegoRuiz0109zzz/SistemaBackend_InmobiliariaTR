package com.sistema.base.api.core.Financiamiento.Contrato;

import com.sistema.base.api.core.Dashboard.dtos.MensualChartDTO;
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
    @Query("SELECT new com.sistema.base.api.core.Dashboard.dtos.MensualChartDTO(" +
            "CAST(EXTRACT(MONTH FROM c.fechaContrato) AS string), COUNT(c), SUM(c.precioTotal)) " +
            "FROM Contrato c WHERE EXTRACT(YEAR FROM c.fechaContrato) = :anio AND " +
            "(:urbId IS NULL OR c.lote.manzana.etapa.urbanizacion.id = :urbId) AND " +
            "(:etapaId IS NULL OR c.lote.manzana.etapa.id = :etapaId) AND " +
            "(:manzId IS NULL OR c.lote.manzana.id = :manzId) " +
            "GROUP BY EXTRACT(MONTH FROM c.fechaContrato)")
    List<MensualChartDTO> findVentasMensuales(Integer anio, Long urbId, Long etapaId, Long manzId);
}

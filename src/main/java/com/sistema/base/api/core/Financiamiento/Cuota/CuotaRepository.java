package com.sistema.base.api.core.Financiamiento.Cuota;

import com.sistema.base.api.core.Dashboard.dtos.MensualChartDTO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface CuotaRepository extends JpaRepository<Cuota, Long> {
    List<Cuota> findByContratoIdAndEnabledTrueOrderByNumeroCuotaAsc(Long contratoId);

    // ==========================================
    // CONSULTAS PARA DASHBOARD
    // ==========================================
   @Query("SELECT SUM(c.montoPagado) FROM Cuota c WHERE " +
           "(:urbId IS NULL OR c.contrato.lote.manzana.etapa.urbanizacion.id = :urbId) AND " +
           "(:etapaId IS NULL OR c.contrato.lote.manzana.etapa.id = :etapaId) AND " +
           "(:manzId IS NULL OR c.contrato.lote.manzana.id = :manzId)")
    Double sumMontoPagado(Long urbId, Long etapaId, Long manzId);

    @Query("SELECT SUM(c.montoTotal - COALESCE(c.montoPagado, 0)) FROM Cuota c WHERE " +
           "(:urbId IS NULL OR c.contrato.lote.manzana.etapa.urbanizacion.id = :urbId) AND " +
           "(:etapaId IS NULL OR c.contrato.lote.manzana.etapa.id = :etapaId) AND " +
           "(:manzId IS NULL OR c.contrato.lote.manzana.id = :manzId)")
    Double sumMontoPorCobrar(Long urbId, Long etapaId, Long manzId);

    @Query("SELECT new com.sistema.base.api.core.Dashboard.dtos.MensualChartDTO(" +
           "CAST(MONTH(c.fechaVencimiento) AS string), COUNT(c), SUM(c.montoTotal)) " +
           "FROM Cuota c WHERE YEAR(c.fechaVencimiento) = :anio AND " +
           "(:urbId IS NULL OR c.contrato.lote.manzana.etapa.urbanizacion.id = :urbId) AND " +
           "(:etapaId IS NULL OR c.contrato.lote.manzana.etapa.id = :etapaId) AND " +
           "(:manzId IS NULL OR c.contrato.lote.manzana.id = :manzId) " +
           "GROUP BY MONTH(c.fechaVencimiento), CAST(MONTH(c.fechaVencimiento) AS string)")
    List<MensualChartDTO> findProyeccionCobrosMensuales(Integer anio, Long urbId, Long etapaId, Long manzId);

    @Query("SELECT c FROM Cuota c WHERE c.enabled = true AND c.estado IN :estados AND c.fechaVencimiento < :fecha")
    List<Cuota> findByEstadoInAndFechaVencimientoBefore(@Param("estados") List<EstadoCuota> estados, @Param("fecha") LocalDate fecha);
}
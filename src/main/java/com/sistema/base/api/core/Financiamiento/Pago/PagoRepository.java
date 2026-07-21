package com.sistema.base.api.core.Financiamiento.Pago;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.util.List;

@Repository
public interface PagoRepository extends JpaRepository<Pago, Long> , JpaSpecificationExecutor<Pago>{
    List<Pago> findByCuotaIdAndEnabledTrue(Long cuotaId);
    List<Pago> findByCuotaId(Long cuotaId);
    List<Pago> findByNumeroComprobante(String numeroComprobante);
    List<Pago> findByEstado(EstadoPago estado);

    @Query("SELECT EXTRACT(MONTH FROM p.fechaPago), SUM(p.montoAbonado) " +
            "FROM Pago p " +
            "WHERE EXTRACT(YEAR FROM p.fechaPago) = :anio " +
            "AND p.estado = 'PROCESADO' AND p.enabled = true " +
            "AND (:urbId IS NULL OR p.cuota.contrato.lote.manzana.etapa.urbanizacion.id = :urbId) " +
            "AND (:etapaId IS NULL OR p.cuota.contrato.lote.manzana.etapa.id = :etapaId) " +
            "AND (:manzId IS NULL OR p.cuota.contrato.lote.manzana.id = :manzId) " +
            "GROUP BY EXTRACT(MONTH FROM p.fechaPago)")
    List<Object[]> findRecaudacionMensual(Integer anio, Long urbId, Long etapaId, Long manzId);}

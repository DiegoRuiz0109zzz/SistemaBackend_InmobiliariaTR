package com.sistema.base.api.core.Financiamiento.Pago;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.util.List;

@Repository
public interface PagoRepository extends JpaRepository<Pago, Long> , JpaSpecificationExecutor<Pago>{
    List<Pago> findByCuotaIdAndEnabledTrue(Long cuotaId);
    List<Pago> findByCuotaId(Long cuotaId);
    List<Pago> findByNumeroComprobante(String numeroComprobante);
    List<Pago> findByEstado(EstadoPago estado);
}

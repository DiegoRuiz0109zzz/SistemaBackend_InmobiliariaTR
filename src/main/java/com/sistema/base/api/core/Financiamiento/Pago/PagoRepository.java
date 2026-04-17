package com.sistema.base.api.core.Financiamiento.Pago;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PagoRepository extends JpaRepository<Pago, Long> {
    List<Pago> findByCuotaIdAndEnabledTrue(Long cuotaId);
}

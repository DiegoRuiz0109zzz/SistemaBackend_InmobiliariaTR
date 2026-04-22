package com.sistema.base.api.core.Financiamiento.Cotizacion;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CotizacionRepository extends JpaRepository<Cotizacion, Long> {
    List<Cotizacion> findByEnabledTrueOrderByFechaCotizacionDesc();

    // Búsqueda mágica por DNI del interesado
    List<Cotizacion> findByInteresadoNumeroDocumentoAndEnabledTrueOrderByFechaCotizacionDesc(String numeroDocumento);
}
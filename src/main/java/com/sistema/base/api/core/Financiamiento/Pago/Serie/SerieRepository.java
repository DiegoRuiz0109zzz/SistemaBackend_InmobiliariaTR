package com.sistema.base.api.core.Financiamiento.Pago.Serie;

import com.sistema.base.api.core.Financiamiento.Pago.TipoComprobante;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SerieRepository extends JpaRepository<Serie, Long> {

    // El Lock bloquea la fila a nivel de Base de Datos para evitar que dos hilos/usuarios la alteren en paralelo
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Serie s WHERE s.tipoComprobante = :tipo AND s.activo = true")
    Optional<Serie> findActiveSerieForUpdate(@Param("tipo") TipoComprobante tipo);
}

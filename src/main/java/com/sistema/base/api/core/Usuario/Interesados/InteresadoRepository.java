package com.sistema.base.api.core.Usuario.Interesados;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InteresadoRepository extends JpaRepository<Interesado, Long> {
    Boolean existsByTelefono(String telefono);
    Boolean existsByNumeroDocumento(String numeroDocumento);
}

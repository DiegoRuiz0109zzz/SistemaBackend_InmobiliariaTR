package com.sistema.base.api.core.Ubigeo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Data // Si usas Lombok, si no, genera los getters y setters manualmente
@Entity
@Table(name = "ubigeos")
public class Ubigeo {

    @Id
    @Column(name = "id_ubigeo", length = 6, nullable = false)
    private String idUbigeo;

    @Column(name = "departamento", length = 100)
    private String departamento;

    @Column(name = "provincia", length = 100)
    private String provincia;

    @Column(name = "distrito", length = 100)
    private String distrito;

}

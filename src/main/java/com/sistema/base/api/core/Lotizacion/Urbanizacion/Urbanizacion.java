package com.sistema.base.api.core.Lotizacion.Urbanizacion;

import com.sistema.base.api.core.Lotizacion.Etapa.Etapa;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;

import jakarta.persistence.*;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "urbanizaciones")
public class Urbanizacion {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    private String ubicacion;

    @Builder.Default
    private boolean enabled = true;
}

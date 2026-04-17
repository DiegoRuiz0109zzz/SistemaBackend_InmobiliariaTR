package com.sistema.base.api.core.Lotizacion.Etapa;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sistema.base.api.core.Lotizacion.Urbanizacion.Urbanizacion;
import jakarta.persistence.*;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "etapas")
public class Etapa {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "urbanizacion_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Urbanizacion urbanizacion;

    @Builder.Default
    private boolean enabled = true;
}

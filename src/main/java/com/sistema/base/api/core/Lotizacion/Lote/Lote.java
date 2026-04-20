package com.sistema.base.api.core.Lotizacion.Lote;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sistema.base.api.core.Lotizacion.Manzana.Manzana;
import jakarta.persistence.*;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "lotes")
public class Lote {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String numero;

    // Nuevas variables para el cálculo
    @Column(nullable = false)
    private Double area;

    @Column(nullable = false)
    private Double precioMetroCuadrado;

    @Column(nullable = false)
    private Double precioCosto;

    @Column(nullable = false)
    private Double precioVenta;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private EstadoLote estadoVenta = EstadoLote.DISPONIBLE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manzana_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Manzana manzana;

    @Builder.Default
    private boolean enabled = true;
}
package com.sistema.base.api.core.Financiamiento.Contrato.ContratoMedida;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sistema.base.api.core.Financiamiento.Contrato.Contrato;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "contrato_medidas")
public class ContratoMedidas {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Relación 1:1 con el Contrato
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contrato_id", nullable = false)
    @JsonIgnoreProperties({"medidas", "hibernateLazyInitializer", "handler"})
    private Contrato contrato;

    // Medidas Lineales (ML)
    @Column(name = "ml_frente")
    private Double mlFrente;

    @Column(name = "ml_derecha")
    private Double mlDerecha;

    @Column(name = "ml_izquierda")
    private Double mlIzquierda;

    @Column(name = "ml_fondo")
    private Double mlFondo;

    // Colindancias
    @Column(name = "colindancia_frente", length = 255)
    private String colindanciaFrente;

    @Column(name = "colindancia_derecha", length = 255)
    private String colindanciaDerecha;

    @Column(name = "colindancia_izquierda", length = 255)
    private String colindanciaIzquierda;

    @Column(name = "colindancia_fondo", length = 255)
    private String colindanciaFondo;

    @Column(name = "perimetro")
    private Double perimetro;
}

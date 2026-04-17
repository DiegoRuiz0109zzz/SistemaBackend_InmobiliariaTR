package com.sistema.base.api.core.Financiamiento.Cuota;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sistema.base.api.core.Financiamiento.Contrato.Contrato;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "cuotas")
public class Cuota {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contrato_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Contrato contrato;

    @Column(nullable = false)
    private Integer numeroCuota; // Ej: 1, 2, 3...

    @Column(nullable = false)
    private Double montoTotal; // Lo que debe pagar (Ej: 400.0)

    @Builder.Default
    @Column(nullable = false)
    private Double montoPagado = 0.0; // Lo que va pagando (Empieza en 0)

    @Column(nullable = false)
    private LocalDate fechaVencimiento; // Fecha límite de pago

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private EstadoCuota estado = EstadoCuota.PENDIENTE;

    @Builder.Default
    private boolean enabled = true;
}

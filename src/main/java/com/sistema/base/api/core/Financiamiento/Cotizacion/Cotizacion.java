package com.sistema.base.api.core.Financiamiento.Cotizacion;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sistema.base.api.core.Financiamiento.Contrato.TipoInicial;
import com.sistema.base.api.core.Lotizacion.Lote.Lote;
import com.sistema.base.api.core.Usuario.Interesados.Interesado;
import com.sistema.base.api.core.Vendedores.Vendedor;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "cotizaciones")
public class Cotizacion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lote_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Lote lote;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "interesado_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Interesado interesado;

    // NUEVO: EL CO-COMPRADOR (También es un Interesado en esta etapa)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "co_comprador_id", nullable = true)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Interesado coComprador;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendedor_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Vendedor vendedor;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_inicial", length = 20)
    private TipoInicial tipoInicial;

    @Column(nullable = false)
    private Double precioTotal;

    @Column(nullable = false)
    private Double montoInicialAcordado;

    @Column(nullable = false)
    private Integer cantidadCuotas;

    @Column(name = "detalle_tramos", length = 1000)
    private String detalleTramos;

    private Double montoCuotaCotizacion;

    @Column(nullable = false)
    private Double saldoFinanciar;

    @Column(name = "cuotas_flexibles")
    private Boolean cuotasFlexibles;

    @Column(nullable = false)
    private LocalDate fechaCotizacion;

    @Column(nullable = false)
    private LocalDate fechaValidez;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private EstadoCotizacion estado = EstadoCotizacion.VIGENTE;

    @Builder.Default
    private boolean enabled = true;

    @PrePersist
    protected void onCreate() {
        if (this.cuotasFlexibles == null) {
            this.cuotasFlexibles = false;
        }
    }
}
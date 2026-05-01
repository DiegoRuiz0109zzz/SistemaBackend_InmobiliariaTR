package com.sistema.base.api.core.Financiamiento.Contrato;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sistema.base.api.core.Financiamiento.Cotizacion.Cotizacion;
import com.sistema.base.api.core.Lotizacion.Lote.Lote;
import com.sistema.base.api.core.Usuario.Clientes.Cliente;
import com.sistema.base.api.core.Vendedores.Vendedor;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "contratos")
public class Contrato {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Relaciones
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lote_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Lote lote;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "co_comprador_id", nullable = true)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Cliente coComprador;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendedor_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Vendedor vendedor;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cotizacion_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Cotizacion cotizacionOrigen;

    // DATOS DEL CONTRATO
    @Column(nullable = false)
    private Double precioTotal;

    // GESTION DE INICIAL
    @Column(nullable = false)
    private Double montoInicial;

    @Column(nullable = false)
    private Double montoAbonadoIncial;

    @Column(nullable = false)
    private Double saldoFinanciar;

    @Column(nullable = false)
    private Integer cantidadCuotas;

    @Column(length = 1000)
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_inicial", length = 20)
    private TipoInicial tipoInicial;

    @Column(length = 1000)
    private String observacion;

    @Column(name = "cuotas_flexibles")
    private Boolean cuotasFlexibles;

    // Fechas
    @Column(name="fecha_inicio_cronograma")
    private LocalDate fechaInicioCronograma;

    @Column(name = "fecha_contrato")
    private LocalDateTime fechaContrato;

    @Column(name = "fecha_registro")
    private LocalDateTime fechaRegistro;

    // --- ESTADO Y AUDITORÍA ---
    @Enumerated(EnumType.STRING)
    @Column(name = "estado_contrato", length = 30)
    private EstadoContrato estadoContrato;

    @Builder.Default
    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    @PrePersist
    protected void onCreate() {
        this.fechaRegistro = LocalDateTime.now();
        if (this.estadoContrato == null) {
            this.estadoContrato = EstadoContrato.ACTIVO;
        }
        if (this.cuotasFlexibles == null) {
            this.cuotasFlexibles = false;
        }
    }
}
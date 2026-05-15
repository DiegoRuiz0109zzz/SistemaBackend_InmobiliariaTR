package com.sistema.base.api.core.Financiamiento.Contrato;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sistema.base.api.core.Financiamiento.Contrato.ContratoHistorial.ContratoHistorial;
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
import java.util.ArrayList;
import java.util.List;

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

    @Column(nullable = false)
    private Double precioTotal;

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

    @Column(name = "cuotas_flexibles")
    private Boolean cuotasFlexibles;

    @Column(name="fecha_inicio_cronograma")
    private LocalDate fechaInicioCronograma;

    // FECHA DE CONTRATO DINÁMICA (Se actualiza al generar el documento)
    @Column(name = "fecha_contrato")
    private LocalDate fechaContrato;

    // FECHA DE CREACIÓN DEL REGISTRO
    @Column(name = "fecha_registro", updatable = false)
    private LocalDate fechaRegistro;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_contrato", length = 30)
    private EstadoContrato estadoContrato;

    // ✅ NUEVO: Columna para gestión documental (Alerta de Falta de Documento)
    @Column(name = "url_documento_firmado", length = 500)
    private String urlDocumentoFirmado;

    @Builder.Default
    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    // RELACIÓN AL HISTORIAL DE DOCUMENTOS
    @OneToMany(mappedBy = "contrato", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnoreProperties({"contrato", "hibernateLazyInitializer", "handler"})
    @Builder.Default
    private List<ContratoHistorial> historiales = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        this.fechaRegistro = LocalDate.now();
        this.fechaContrato = LocalDate.now();

        // El contrato nace por defecto como SEPARADO
        if (this.estadoContrato == null) {
            this.estadoContrato = EstadoContrato.SEPARADO;
        }
        if (this.cuotasFlexibles == null) {
            this.cuotasFlexibles = false;
        }
    }
}
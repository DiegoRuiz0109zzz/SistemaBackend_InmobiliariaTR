package com.sistema.base.api.core.Financiamiento.Pago;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sistema.base.api.core.Financiamiento.Cuota.Cuota;
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
@Table(name = "pagos")
public class Pago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cuota_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Cuota cuota;

    @Column(nullable = false)
    private Double montoAbonado;

    @Column(length = 500)
    private String fotoVoucherUrl;

    @Column(length = 50)
    private String metodoPago;

    @Column(length = 100)
    private String numeroOperacion;

    @Column(name = "dias_retraso")
    @Builder.Default
    private Integer diasRetraso = 0;

    @Column(name = "pago_a_destiempo")
    @Builder.Default
    private boolean pagoADestiempo = false;

    // ✅ NUEVO: Campos para el Comprobante (Nota de Venta)
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_comprobante", length = 30)
    private TipoComprobante tipoComprobante;

    @Column(name = "numero_comprobante", length = 20)
    private String numeroComprobante; // Ej: "NV01-000015"

    // ✅ NUEVO: Campo para la descripción/observación del pago
    @Column(length = 1000)
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoPago estado;

    @Column(name = "fecha_pago", updatable = false)
    private LocalDate fechaPago;

    @Column(name = "fecha_registro", updatable = false)
    private LocalDate fechaRegistro;

    @Column(name = "enlace_pdf_sunat", length = 500)
    private String enlacePdfSunat;

    @Column(name = "enlace_xml_sunat", length = 500)
    private String enlaceXmlSunat;

    @Column(name = "external_id_sunat", length = 150)
    private String externalIdSunat;

    @Column(name = "estado_sunat", length = 50)
    private String estadoSunat;

    @Builder.Default
    @Column(nullable = false)
    private boolean enabled = true;

    @PrePersist
    protected void onCreate() {
        fechaPago = LocalDate.now();
        fechaRegistro = LocalDate.now();
    }
}
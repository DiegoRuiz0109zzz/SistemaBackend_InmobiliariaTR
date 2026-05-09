package com.sistema.base.api.core.Financiamiento.Contrato.ContratoHistorial;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sistema.base.api.core.Financiamiento.Contrato.Contrato;
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
@Table(name = "contrato_historial")
public class ContratoHistorial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contrato_id", nullable = false)
    @JsonIgnoreProperties({"historiales", "hibernateLazyInitializer", "handler"})
    private Contrato contrato;

    // ✅ NUEVO: La descripción automática del sistema (Ej: "Cambio de Titular...")
    @Column(length = 1000)
    private String descripcion;

    @Column(name = "tipo_registro", length = 50)
    private String tipoRegistro;

    @Column(name = "fecha_registro", nullable = false, updatable = false)
    private LocalDate fechaRegistro;

    @Column(name = "ruta_documento_pdf")
    private String rutaDocumentoPdf;

    // Mantenemos observación para notas manuales opcionales
    @Column(length = 1000)
    private String observacion;

    @PrePersist
    protected void onCreate() {
        this.fechaRegistro = LocalDate.now();
    }
}
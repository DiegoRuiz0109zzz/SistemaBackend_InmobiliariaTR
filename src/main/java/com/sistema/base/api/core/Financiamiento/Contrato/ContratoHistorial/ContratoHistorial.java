package com.sistema.base.api.core.Financiamiento.Contrato.ContratoHistorial;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sistema.base.api.core.Financiamiento.Contrato.Contrato;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
    @JsonIgnoreProperties({"historiales", "hibernateLazyInitializer", "handler"}) // Evita bucle infinito JSON
    private Contrato contrato;

    @Column(nullable = false, length = 50)
    private String estado;

    @Column(name = "fecha_registro", nullable = false, updatable = false)
    private LocalDateTime fechaRegistro;

    @Column(name = "ruta_documento_pdf", length = 500)
    private String rutaDocumentoPdf;

    @Column(length = 500)
    private String observacion;

    @PrePersist
    protected void onCreate() {
        this.fechaRegistro = LocalDateTime.now(); // SE REGISTRA AUTOMÁTICAMENTE
    }
}
package com.sistema.base.api.core.Financiamiento.Contrato.ContratoHistorial;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sistema.base.api.core.Financiamiento.Contrato.Contrato; // Ajusta el import a tu ruta real
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
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Contrato contrato;

    @Column(nullable = false, length = 50)
    private String estado; // SEPARADO, ACTIVO, ANULADO, etc.

    @Column(name = "fecha_registro", nullable = false)
    private LocalDateTime fechaRegistro;

    @Column(name = "ruta_documento_pdf", length = 500)
    private String rutaDocumentoPdf;

    @Column(length = 500)
    private String observacion;
}

package com.sistema.base.api.core.Financiamiento.Pago.Serie;

import com.sistema.base.api.core.Financiamiento.Pago.TipoComprobante;
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
@Table(name = "series_comprobantes")
public class Serie {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_comprobante", nullable = false, length = 30)
    private TipoComprobante tipoComprobante;

    @Column(nullable = false, length = 10)
    private String serie; // Ej: "NV01"

    @Column(name = "ultimo_correlativo", nullable = false)
    private Integer ultimoCorrelativo; // Ej: 15

    @Builder.Default
    private boolean activo = true;
}

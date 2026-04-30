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
    private Integer numeroCuota; // 0 (para inicial), 1, 2, 3... o 99 para especiales

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_cuota", length = 20)
    private TipoCuota tipoCuota; // INICIAL, MENSUAL, ESPECIAL

    @Column(nullable = false)
    private Double montoTotal;

    @Builder.Default
    @Column(nullable = false)
    private Double montoPagado = 0.0;

    @Column(nullable = false)
    private LocalDate fechaVencimiento;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private EstadoCuota estado; // PENDIENTE, PAGADO_PARCIAL, PAGADO_TOTAL, VENCIDO

    // ✅ CAMBIO CLAVE: Agregado para que tu Repositorio no falle
    @Builder.Default
    @Column(nullable = false)
    private boolean enabled = true;

    @PrePersist
    protected void onCreate() {
        if (this.montoPagado == null) {
            this.montoPagado = 0.0;
        }
        if (this.estado == null) {
            this.estado = EstadoCuota.PENDIENTE;
        }
        if (this.tipoCuota == null) {
            this.tipoCuota = TipoCuota.MENSUAL;
        }
    }
}
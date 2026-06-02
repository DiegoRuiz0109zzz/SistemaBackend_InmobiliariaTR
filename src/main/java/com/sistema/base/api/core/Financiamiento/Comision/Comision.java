package com.sistema.base.api.core.Financiamiento.Comision;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sistema.base.api.core.Financiamiento.Contrato.Contrato;
import com.sistema.base.api.core.Vendedores.Vendedor;
import com.sistema.base.api.entity.User;
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
@Table(name = "comisiones")
public class Comision {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contrato_id", nullable = false)
    @JsonIgnoreProperties({"historiales", "medidas", "hibernateLazyInitializer", "handler"})
    private Contrato contrato;

    @Enumerated(EnumType.STRING)
    @Column(name = "rol_beneficiario", nullable = false, length = 20)
    private RolComision rolBeneficiario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendedor_id", nullable = true)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Vendedor vendedor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "jefe_ventas_id", nullable = true)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private User jefeVentas;

    @Column(name = "precio_oficina_lote")
    private Double precioOficinaLote;

    @Column(name = "precio_venta_contrato")
    private Double precioVentaContrato;

    @Column(name = "diferencia_precio")
    private Double diferenciaPrecio;

    // --- DESGLOSE ECONÓMICO INDIVIDUAL ---
    @Column(name = "monto_base", nullable = false)
    private Double montoBase; // Solo será 400 para quien haga la venta directa

    // ✅ NUEVA COLUMNA: Bono Global (Exclusivo para el Jefe de Ventas)
    @Column(name = "monto_bono_global", nullable = false)
    private Double montoBonoGlobal; // Será 100 para el jefe, 0 para el vendedor

    @Column(name = "porcentaje_bono_diferencia")
    private Double porcentajeBonoDiferencia;

    @Column(name = "monto_bono_diferencia", nullable = false)
    private Double montoBonoDiferencia;

    @Column(name = "total_comision", nullable = false)
    private Double totalComision;

    // --- ESTADO DE CAJA INDEPENDIENTE ---
    @Enumerated(EnumType.STRING)
    @Column(name = "estado_pago", nullable = false, length = 20)
    private EstadoComision estadoPago;

    @Column(name = "observacion_pago", length = 1000)
    private String observacionPago;

    @Column(name = "fecha_pago")
    private LocalDateTime fechaPago;

    @Column(name = "fecha_generacion", nullable = false, updatable = false)
    private LocalDate fechaGeneracion;

    @PrePersist
    protected void onCreate() {
        this.fechaGeneracion = LocalDate.now();
        if (this.estadoPago == null) {
            this.estadoPago = EstadoComision.PENDIENTE;
        }
    }
}
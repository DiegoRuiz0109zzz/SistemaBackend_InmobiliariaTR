package com.sistema.base.api.core.Financiamiento.Contrato;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sistema.base.api.core.Lotizacion.Lote.Lote;
import com.sistema.base.api.core.Usuario.Clientes.Cliente;
import com.sistema.base.api.core.Vendedores.Vendedor;
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
    @JoinColumn(name = "vendedor_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Vendedor vendedor;

    // Matemática del Financiamiento
    @Column(nullable = false)
    private Double precioTotal;

    @Column(nullable = false)
    private Double montoInicial;

    @Column(nullable = false)
    private Double saldoFinanciar;

    @Column(nullable = false)
    private Integer cantidadCuotas;

    // Fechas
    @Column(name = "fecha_contrato", updatable = false)
    private LocalDateTime fechaContrato;

    @Builder.Default
    private boolean enabled = true;

    @PrePersist
    protected void onCreate() {
        fechaContrato = LocalDateTime.now();
    }
}

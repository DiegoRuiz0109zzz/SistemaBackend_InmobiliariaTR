package com.sistema.base.api.core.Financiamiento.Pago;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sistema.base.api.core.Financiamiento.Cuota.Cuota;
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
    private Double montoAbonado; // Ej: 200.0

    @Column(length = 500)
    private String fotoVoucherUrl; // Ruta donde se guarda la foto

    @Column(name = "fecha_pago", updatable = false)
    private LocalDateTime fechaPago;

    @Builder.Default
    private boolean enabled = true;

    @PrePersist
    protected void onCreate() {
        fechaPago = LocalDateTime.now(); // Graba la hora exacta del registro
    }
}

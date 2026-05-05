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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private EstadoPago estado = EstadoPago.PROCESADO;

    @Column(name = "fecha_pago", updatable = false)
    private LocalDateTime fechaPago;

    @Builder.Default
    private boolean enabled = true;

    @PrePersist
    protected void onCreate() {
        fechaPago = LocalDateTime.now();
    }
}
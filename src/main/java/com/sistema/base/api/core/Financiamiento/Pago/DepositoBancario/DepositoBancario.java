package com.sistema.base.api.core.Financiamiento.Pago.DepositoBancario;

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
@Table(name = "depositos_bancarios")
public class DepositoBancario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "numero_recibo_caja", length = 20, nullable = false)
    private String numeroReciboCaja; // Aquí guardaremos "RI01-000005"

    @Column(length = 50)
    private String banco; // Ej: "BCP", "BBVA"

    @Column(length = 100)
    private String numeroOperacion;

    @Column(nullable = false)
    private Double monto;

    @Column(length = 500)
    private String fotoVoucherUrl;

    @Column(name = "fecha_deposito")
    private LocalDate fechaDeposito;
}

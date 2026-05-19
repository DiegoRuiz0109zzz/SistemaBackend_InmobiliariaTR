package com.sistema.base.api.core.Vendedores;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sistema.base.api.entity.User;
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
@Table(name = "vendedores")
public class Vendedor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 15)
    private String numeroDocumento;

    @Column(nullable = false)
    private String nombres;

    private String apellidos;
    private String telefono;
    private String email;

    @Column(nullable = false, length = 20)
    private String tipoDocumento;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "jefe_ventas_id")
    // ✅ AGREGA ESTA LÍNEA AQUÍ
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private User jefeVentas;

    @Builder.Default
    private boolean enabled = true;
}

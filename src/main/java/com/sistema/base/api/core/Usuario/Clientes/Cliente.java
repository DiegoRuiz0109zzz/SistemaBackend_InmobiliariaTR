package com.sistema.base.api.core.Usuario.Clientes;

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
@Table(name = "clientes")
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String numeroDocumento; // DNI, CE o RUC

    @Column(nullable = false)
    private String nombres;
    @Column(nullable = false)
    private String apellidos;
    @Column(nullable = false)
    private LocalDate fechaIngreso;

    private String departamento;
    private String provincia;
    private String distrito;
    private String ubigeo;

    private String direccion;
    private String telefono;
    private String email;

    @Column(nullable = false, length = 20)
    private String tipoDocumento;

    @PrePersist
    protected void onCreate() {
        fechaIngreso = LocalDate.now();
    }

    @Builder.Default
    private boolean enabled = true;
}
package com.sistema.base.api.core.Usuario.Interesados;

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
@Table(name = "interesados")
public class Interesado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombres;

    private String apellidos;
    private String telefono;
    private String email;

    private String departamento;
    private String provincia;
    private String distrito;
    private String ubigeo;

    @Column(nullable = false, length = 20)
    private String tipoDocumento;

    @Column(unique = true, nullable = false)
    private String numeroDocumento;

    @Column(nullable = false)
    private LocalDate fechaIngreso;

    @PrePersist
    protected void onCreate() {
        fechaIngreso = LocalDate.now();
    }

    // Podrías añadir campos específicos como:
    // private String proyectoDeInteres;
    // private String estadoContacto; // ej: "Pendiente", "Contactado"

    @Builder.Default
    private boolean enabled = true;
}
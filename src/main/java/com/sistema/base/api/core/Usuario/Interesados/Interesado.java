package com.sistema.base.api.core.Usuario.Interesados;

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

    // Podrías añadir campos específicos como:
    // private String proyectoDeInteres;
    // private String estadoContacto; // ej: "Pendiente", "Contactado"

    @Builder.Default
    private boolean enabled = true;
}
package com.sistema.base.api.core.Empresa;

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
@Table(name = "empresa")
public class Empresa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 11)
    private String ruc;

    @Column(nullable = false)
    private String razonSocial;

    private String nombreComercial;

    private String departamento;
    private String provincia;
    private String distrito;

    private String direccion;
    private String telefono;
    private String email;
    private String web;

    @Builder.Default
    private boolean enabled = true;
}

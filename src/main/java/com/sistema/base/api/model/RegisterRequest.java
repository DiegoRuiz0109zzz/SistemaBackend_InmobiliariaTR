package com.sistema.base.api.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para la solicitud de registro de usuarios.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RegisterRequest {
    private String username;
    private String password;
    private String nombres;
    private String apellidos;
    private String email;
    private String telefono;
    private String docIdentidad;
    private String role;
}
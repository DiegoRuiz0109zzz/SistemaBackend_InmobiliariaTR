package com.sistema.base.api.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRequest {
	private int id;
	private String username;
	private String password;
	private String nombres;
	private String apellidos;
	private String email;
	private String telefono;
	private String docIdentidad;
}

package com.sistema.base.api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.sistema.base.api.config.JwtService;
import com.sistema.base.api.entity.Profile;
import com.sistema.base.api.entity.User;
import com.sistema.base.api.model.AuthenticationRequest;
import com.sistema.base.api.model.AuthenticationResponse;
import com.sistema.base.api.model.RegisterRequest;
import com.sistema.base.api.repository.ProfileRepository;
import com.sistema.base.api.repository.UserRepository;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;
	private final AuthenticationManager authenticationManager;
	private final ProfileRepository profileRepository;

	public AuthenticationResponse register(RegisterRequest request) {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

		// 1. Validar que el usuario esté logueado
		if (authentication == null || !authentication.isAuthenticated() || authentication.getName().equals("anonymousUser")) {
			throw new IllegalStateException("Debes iniciar sesión para registrar usuarios.");
		}

		// 2. Obtener los datos del creador y el rol que se desea registrar
		User creador = userRepository.findByUsername(authentication.getName())
				.orElseThrow(() -> new RuntimeException("Usuario creador no encontrado."));

		String rolCreador = creador.getProfile().getName();
		String rolSolicitado = request.getRole(); // <-- Ahora es dinámico según lo que envíe el frontend

		// 3. Validar permisos exactos mediante el switch
		List<String> rolesPermitidos = getAllowedProfiles(rolCreador);

		if (!rolesPermitidos.contains(rolSolicitado)) {
			throw new IllegalStateException("Operación denegada: Un " + rolCreador + " no tiene permisos para crear un " + rolSolicitado);
		}

		// 4. Obtener el perfil real de la base de datos
		Profile perfilDestino = profileRepository.findByName(rolSolicitado)
				.orElseThrow(() -> new RuntimeException("El perfil seleccionado (" + rolSolicitado + ") no existe en el sistema."));

		// 5. Guardar el nuevo usuario
		var user = User.builder()
				.username(request.getUsername())
				.password(passwordEncoder.encode(request.getPassword()))
				.nombres(request.getNombres())
				.apellidos(request.getApellidos())
				.email(request.getEmail())
				.telefono(request.getTelefono())
				.docIdentidad(request.getDocIdentidad())
				.profile(perfilDestino)
				.build();

		userRepository.save(user);
		var jwtToken = jwtService.generateToken(user);

		return AuthenticationResponse.builder()
				.token(jwtToken)
				.build();
	}

	// LISTA DE PERMISOS ESTRICTOS POR ROL
	private List<String> getAllowedProfiles(String currentProfileName) {
		switch (currentProfileName) {
			case "SUPER_ADMINISTRADOR":
				// El Super Admin puede crear todos los perfiles de la inmobiliaria
				return Arrays.asList("GERENTE_GENERAL", "JEFE_ADMINISTRACION", "JEFE_VENTAS", "CONTADORA", "ABOGADA", "ADMINISTRADORA", "ASISTENTE_ADMINISTRATIVO");

			case "GERENTE_GENERAL":
				// El Gerente puede crear cualquier rol operativo o administrativo
				return Arrays.asList("JEFE_ADMINISTRACION", "JEFE_VENTAS", "CONTADORA", "ABOGADA", "ADMINISTRADORA", "ASISTENTE_ADMINISTRATIVO");

			case "JEFE_ADMINISTRACION":
				// El Jefe de Administración solo puede crear sus perfiles subordinados
				return Arrays.asList("ADMINISTRADORA", "ASISTENTE_ADMINISTRATIVO", "CONTADORA");

			default:
				// Cualquier otro rol (como vendedores o asistentes) devuelve una lista vacía y no podrá registrar a nadie
				return Collections.emptyList();
		}
	}

	public AuthenticationResponse authenticate(AuthenticationRequest request) {
		authenticationManager.authenticate(
				new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
		);
		var user = userRepository.findByUsername(request.getUsername()).orElseThrow();
		var jwtToken = jwtService.generateToken(user);
		return AuthenticationResponse.builder()
				.token(jwtToken)
				.id(user.getId())
				.role(user.getProfile().getName())
				.permissions(user.getAuthorities().stream()
						.map(org.springframework.security.core.GrantedAuthority::getAuthority)
						.collect(java.util.stream.Collectors.toSet()))
				.profileImage(user.getProfileImage())
				.build();
	}

	public String recoverPassword(String username, String docIdentidad, String telefono) {
		User user = userRepository.findByUsername(username)
				.orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado."));

		if (!user.getDocIdentidad().equals(docIdentidad)) {
			throw new IllegalArgumentException("El documento de identidad no coincide.");
		}

		if (!user.getTelefono().equals(telefono)) {
			throw new IllegalArgumentException("El número de teléfono no coincide.");
		}

		user.setPassword(passwordEncoder.encode(docIdentidad));
		userRepository.save(user);

		return "Contraseña restablecida correctamente a su número de documento.";
	}
}
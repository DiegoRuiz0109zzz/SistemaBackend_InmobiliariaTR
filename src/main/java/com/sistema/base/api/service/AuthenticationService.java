package com.sistema.base.api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
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

@Service
@RequiredArgsConstructor
public class AuthenticationService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;
	private final AuthenticationManager authenticationManager;
	private final ProfileRepository profileRepository;

	public AuthenticationResponse register(RegisterRequest request) {
		org.springframework.security.core.Authentication authentication = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
		String roleName = "USUARIO";

		if (authentication != null && authentication.isAuthenticated() && !(authentication instanceof org.springframework.security.authentication.AnonymousAuthenticationToken)) {
			// Usuario autenticado creando otro usuario (Jerarquía)
			if (request.getRole() != null && !request.getRole().isEmpty()) {
				User currentUser = (User) authentication.getPrincipal(); // Asume que el principal es User
				String currentRole = currentUser.getProfile().getName();
				java.util.List<String> allowed = getAllowedProfiles(currentRole);
				
				if (!allowed.contains(request.getRole())) {
					throw new IllegalStateException("No tiene permisos para crear un usuario con el rol: " + request.getRole());
				}
				roleName = request.getRole();
			}
		} else {
			// Registro público: Siempre USUARIO
			roleName = "USUARIO";
		}
		
		String finalRole = roleName;
		Profile userProfile = profileRepository.findByName(finalRole)
				.orElseThrow(() -> new IllegalStateException("El perfil '" + finalRole + "' no se encontró en la base de datos."));

		var user = User.builder().username(request.getUsername())
				.password(passwordEncoder.encode(request.getPassword())).nombres(request.getNombres())
				.apellidos(request.getApellidos()).email(request.getEmail()).telefono(request.getTelefono())
				.docIdentidad(request.getDocIdentidad()).profile(userProfile).build();
		userRepository.save(user);
		var jwtToken = jwtService.generateToken(user);
		return AuthenticationResponse.builder()
				.token(jwtToken)
				.permissions(user.getAuthorities().stream().map(org.springframework.security.core.GrantedAuthority::getAuthority).collect(java.util.stream.Collectors.toSet()))
				.build();
	}
	
	private java.util.List<String> getAllowedProfiles(String currentProfileName) {
		switch (currentProfileName) {
            case "SUPER_ADMINISTRADOR":
            	return java.util.Arrays.asList("ADMINISTRADOR", "MODERADOR", "EDITOR", "USUARIO");
            case "ADMINISTRADOR":
                return java.util.Arrays.asList("ADMINISTRADOR", "MODERADOR", "EDITOR", "USUARIO");
            case "MODERADOR":
            	return java.util.Arrays.asList("MODERADOR", "EDITOR", "USUARIO");
            case "EDITOR":
            	return java.util.Arrays.asList("EDITOR", "USUARIO");
            default:
            	return java.util.Arrays.asList("USUARIO");
        }
	}

	public AuthenticationResponse authenticate(AuthenticationRequest request) {
		authenticationManager
				.authenticate(new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));
		var user = userRepository.findByUsername(request.getUsername()).orElseThrow();
		var jwtToken = jwtService.generateToken(user);
		return AuthenticationResponse.builder()
				.token(jwtToken)
				.id(user.getId())
				.role(user.getProfile().getName())
				.permissions(user.getAuthorities().stream().map(org.springframework.security.core.GrantedAuthority::getAuthority).collect(java.util.stream.Collectors.toSet()))
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
		
		// Reset password to DNI
		user.setPassword(passwordEncoder.encode(docIdentidad));
		userRepository.save(user);
		
		return "Contraseña restablecida exitosamente. Tu nueva contraseña es tu número de documento.";
	}
}

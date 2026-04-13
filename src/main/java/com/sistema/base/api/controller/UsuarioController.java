package com.sistema.base.api.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.sistema.base.api.entity.User;
import com.sistema.base.api.model.AuthenticationResponse;
import com.sistema.base.api.model.ChangePasswordRequest;
import com.sistema.base.api.model.ChangeProfileRequest;
import com.sistema.base.api.model.MessageResponse;
import com.sistema.base.api.model.RegisterRequest;
import com.sistema.base.api.model.UserRequest;
import com.sistema.base.api.repository.UserRepository;
import com.sistema.base.api.service.AuthenticationService;
import com.sistema.base.api.service.FileStorageService;
import com.sistema.base.api.service.UsuarioService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/usuario")
@RequiredArgsConstructor
public class UsuarioController {

	private final AuthenticationService service;

	@Autowired
	PasswordEncoder encoder;

	@Autowired
	private UsuarioService usuarioService;

	@Autowired
	UserRepository userRepository;

	@Autowired
	private FileStorageService fileStorageService;

	@PostMapping("/")
	@PreAuthorize("hasAuthority('GESTION_USUARIOS')")
	public ResponseEntity<AuthenticationResponse> guardar(@RequestBody RegisterRequest request) {
		return ResponseEntity.ok(service.register(request));
	}

	@GetMapping("/{Id}")
	@PreAuthorize("isAuthenticated()")
	public User listarUsuarioPorId(@PathVariable("Id") Integer Id) {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		User user = (User) authentication.getPrincipal();
		
		boolean hasManageUsers = user.getAuthorities().stream()
				.anyMatch(a -> a.getAuthority().equals("GESTION_USUARIOS"));
				
		if (!user.getId().equals(Id) && !hasManageUsers) {
			throw new org.springframework.security.access.AccessDeniedException("No tienes permisos para ver este perfil");
		}
		
		return usuarioService.obtenerUsuario(Id);
	}

	@PutMapping("/")
	@PreAuthorize("isAuthenticated()")
	public User actualizarUser(@RequestBody UserRequest userRequest) {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		User user = (User) authentication.getPrincipal();

		boolean hasManageUsers = user.getAuthorities().stream()
				.anyMatch(a -> a.getAuthority().equals("GESTION_USUARIOS"));

		if ((userRequest.getId() != user.getId()) && !hasManageUsers) {
			throw new org.springframework.security.access.AccessDeniedException("No tienes permisos para editar este usuario");
		}
		
		return usuarioService.actualizarUsuario(userRequest);
	}

	@DeleteMapping("/{Id}")
	@PreAuthorize("hasAuthority('GESTION_USUARIOS')")
	public void eliminarUser(@PathVariable("Id") Integer Id) {
		usuarioService.eliminarUsuario(Id);
	}

	@PostMapping("/changepassword")
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<?> changePassword(@Validated @RequestBody ChangePasswordRequest changePasswordRequest) {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		User userPrincipal = (User) authentication.getPrincipal();

		if (!userPrincipal.getId().equals(changePasswordRequest.getId())) {
			return ResponseEntity.status(403)
					.body(new MessageResponse("Error: No tiene permiso para cambiar la contraseña de otro usuario."));
		}

		User user = null;
		try {
			user = userRepository.findById(changePasswordRequest.getId())
					.orElseThrow(() -> new RuntimeException("User not found"));
		} catch (RuntimeException e) {
			e.printStackTrace();
		}
		if (!encoder.matches(changePasswordRequest.getOldPassword(), user.getPassword())) {
			return ResponseEntity.badRequest().body(new MessageResponse("Error: password anterior es incorrecto"));
		}
		user.setPassword(encoder.encode(changePasswordRequest.getNewPassword()));
		userRepository.save(user);
		return ResponseEntity.ok(new MessageResponse("Password fue cambiado, successfully"));
	}

	@PostMapping("/passgenerico")
	@PreAuthorize("hasAuthority('GESTION_USUARIOS')")
	public ResponseEntity<?> changePasswordGenerico(@Validated @RequestBody UserRequest user) {
		user.setPassword(encoder.encode(user.getDocIdentidad()));
		usuarioService.actualizarUsuario(user);
		return ResponseEntity.ok(new MessageResponse("Password fue cambiado, successfully"));
	}

	@GetMapping("/total")
	@PreAuthorize("hasAuthority('GESTION_USUARIOS')")
	public Integer contarTotalUsuarios() {
		return usuarioService.contarTotalUsuarios();
	}

	@GetMapping("/")
	@PreAuthorize("hasAuthority('GESTION_USUARIOS')")
	public ResponseEntity<Page<User>> listarUsuarios(@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size, @RequestParam(required = false) String search) {

		Page<User> usuarios;

		if (search != null && !search.trim().isEmpty()) {
			usuarios = usuarioService.buscarUsuarios(search, page, size);
		} else {
			usuarios = usuarioService.obtenerUsuariosPaginados(page, size);
		}

		return ResponseEntity.ok(usuarios);
	}

	@PostMapping("/asignar-perfil")
	@PreAuthorize("hasAuthority('GESTION_USUARIOS')")
	public ResponseEntity<User> asignarPerfil(@RequestBody ChangeProfileRequest request) {
		try {
			User userUpdated = usuarioService.changeUserProfile(request);
			return ResponseEntity.ok(userUpdated);
		} catch (SecurityException e) {
			return ResponseEntity.status(403).body(null);
		} catch (RuntimeException e) {
			return ResponseEntity.badRequest().body(null);
		}
	}

	@PostMapping("/upload-profile-image")
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<?> uploadProfileImage(@RequestParam("file") MultipartFile file) {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		User userPrincipal = (User) authentication.getPrincipal();

		try {
			// Obtener el usuario actual de la base de datos
			User user = userRepository.findById(userPrincipal.getId())
					.orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

			// Eliminar imagen anterior si existe
			if (user.getProfileImage() != null && !user.getProfileImage().isEmpty()) {
				fileStorageService.deleteFile(user.getProfileImage());
			}

			// Guardar nueva imagen
			String imagePath = fileStorageService.storeFile(file, String.valueOf(user.getId()));

			// Actualizar usuario con la nueva ruta de imagen
			user.setProfileImage(imagePath);
			userRepository.save(user);

			return ResponseEntity.ok(new java.util.HashMap<String, String>() {{
				put("message", "Imagen subida exitosamente");
				put("imageUrl", imagePath);
			}});
		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().body(new MessageResponse("Error: " + e.getMessage()));
		} catch (Exception e) {
			return ResponseEntity.status(500).body(new MessageResponse("Error al subir la imagen: " + e.getMessage()));
		}
	}

	@DeleteMapping("/delete-profile-image")
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<?> deleteProfileImage() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		User userPrincipal = (User) authentication.getPrincipal();

		try {
			User user = userRepository.findById(userPrincipal.getId())
					.orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

			if (user.getProfileImage() != null && !user.getProfileImage().isEmpty()) {
				fileStorageService.deleteFile(user.getProfileImage());
				user.setProfileImage(null);
				userRepository.save(user);
			}

			return ResponseEntity.ok(new MessageResponse("Imagen eliminada exitosamente"));
		} catch (Exception e) {
			return ResponseEntity.status(500).body(new MessageResponse("Error al eliminar la imagen: " + e.getMessage()));
		}
	}

}
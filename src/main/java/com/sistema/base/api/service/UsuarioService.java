package com.sistema.base.api.service;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sistema.base.api.entity.Profile;
import com.sistema.base.api.entity.User;
import com.sistema.base.api.model.ChangeProfileRequest;
import com.sistema.base.api.model.UserRequest;
import com.sistema.base.api.repository.ProfileRepository;
import com.sistema.base.api.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UsuarioService {

	private final UserRepository usuarioRepository;
    private final ProfileRepository profileRepository;

	public User agregarUsuario(User user) {
		return usuarioRepository.save(user);
	}

	public User actualizarUsuario(UserRequest userRequest) {
		Optional<User> existingUserOpt = usuarioRepository.findById(userRequest.getId());

		if (existingUserOpt.isPresent()) {
			User existingUser = existingUserOpt.get();
			existingUser.setUsername(userRequest.getUsername());
			existingUser.setEmail(userRequest.getEmail());
			existingUser.setPassword(userRequest.getPassword());
			existingUser.setNombres(userRequest.getNombres());
			existingUser.setApellidos(userRequest.getApellidos());
			existingUser.setDocIdentidad(userRequest.getDocIdentidad());
			existingUser.setTelefono(userRequest.getTelefono());

			return usuarioRepository.save(existingUser);
		} else {
			throw new RuntimeException("Usuario no encontrado con ID: " + userRequest.getId());
		}

	}

	public Set<User> obtenerUsuarios() {
		return new LinkedHashSet<>(usuarioRepository.findAll());
	}

	public User obtenerUsuario(Integer id) {
		return usuarioRepository.findById(id).get();
	}

	public User obtenerUsuarioDNI(String docIdentidad) {
		return usuarioRepository.findByDocIdentidad(docIdentidad).get();
	}

	public void eliminarUsuario(Integer id) {
		User usr = new User();
		usr.setId(id);
		usuarioRepository.delete(usr);
	}

	public Integer contarTotalUsuarios() {
		return usuarioRepository.contarTotalUsuarios();
	}

	public Page<User> obtenerUsuariosPaginados(int page, int size) {
		Pageable pageable = PageRequest.of(page, size, Sort.by("id").ascending());
		
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = (User) authentication.getPrincipal();
        String currentProfileName = currentUser.getProfile().getName();
        
        if ("SUPER_ADMINISTRADOR".equals(currentProfileName)) {
        	return usuarioRepository.findAll(pageable);
        }
        
        List<String> allowed = getAllowedProfiles(currentProfileName);
		return usuarioRepository.findByProfileNameIn(allowed, pageable);
	}

	public Page<User> buscarUsuarios(String search, int page, int size) {
		Pageable pageable = PageRequest.of(page, size);
		
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = (User) authentication.getPrincipal();
        String currentProfileName = currentUser.getProfile().getName();
        
        if ("SUPER_ADMINISTRADOR".equals(currentProfileName)) {
        	return usuarioRepository.buscarPorDniNombreOApellido(search, pageable);
        }
        
        List<String> allowed = getAllowedProfiles(currentProfileName);
		return usuarioRepository.buscarPorDniNombreOApellidoYPerfiles(search, allowed, pageable);
	}

	private List<String> getAllowedProfiles(String currentProfileName) {
		switch (currentProfileName) {
            case "ADMINISTRADOR":
                return Arrays.asList("ADMINISTRADOR", "MODERADOR", "EDITOR", "USUARIO");
            case "MODERADOR":
            	return Arrays.asList("MODERADOR", "EDITOR", "USUARIO");
            case "EDITOR":
            	return Arrays.asList("EDITOR", "USUARIO");
            default:
            	return Arrays.asList("USUARIO");
        }
	}
	
	@Transactional
    public User changeUserProfile(ChangeProfileRequest request) {
        // 1. Obtener el usuario que está intentando hacer el cambio (el autenticado)
		
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = (User) authentication.getPrincipal();
        String currentProfileName = currentUser.getProfile().getName();

        // 2. Definir los perfiles que el usuario autenticado tiene permitido asignar
        
        List<String> allowedProfilesToAssign;
        
        switch (currentProfileName) {
            case "SUPER_ADMINISTRADOR":
                allowedProfilesToAssign = Arrays.asList("ADMINISTRADOR", "MODERADOR", "EDITOR", "USUARIO");
                break;
            case "ADMINISTRADOR":
                allowedProfilesToAssign = Arrays.asList("ADMINISTRADOR", "MODERADOR", "EDITOR", "USUARIO");
                break;
            case "MODERADOR":
                allowedProfilesToAssign = Arrays.asList("MODERADOR", "EDITOR", "USUARIO");
                break;
            default:
                throw new SecurityException("Perfil no autorizado para asignar roles.");
        }
        
        // 3. Validar si el perfil solicitado está en la lista de permitidos
        
        if (!allowedProfilesToAssign.contains(request.getNewProfileName())) {
            throw new SecurityException("No tiene permiso para asignar el perfil: " + request.getNewProfileName());
        }

        // 4. Buscar el usuario a modificar y el nuevo perfil
        
        User userToUpdate = usuarioRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("Usuario con ID " + request.getUserId() + " no encontrado."));

            Profile newProfile = profileRepository.findByName(request.getNewProfileName())
                .orElseThrow(() -> new RuntimeException("Perfil " + request.getNewProfileName() + " no encontrado."));

        // 5. Asignar el nuevo perfil y guardar
		userToUpdate.setProfile(newProfile);
        return usuarioRepository.save(userToUpdate);
    }
}

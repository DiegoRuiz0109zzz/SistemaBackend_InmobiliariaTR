package com.sistema.base.api.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sistema.base.api.entity.Permission;
import com.sistema.base.api.entity.Profile;
import com.sistema.base.api.repository.PermissionRepository;
import com.sistema.base.api.repository.ProfileRepository;
import com.sistema.base.api.repository.UserRepository;

@Service
public class ProfileService {

    private final ProfileRepository profileRepository;
    private final PermissionRepository permissionRepository;
    private final UserRepository userRepository;

    public ProfileService(ProfileRepository profileRepository, PermissionRepository permissionRepository, UserRepository userRepository) {
        this.profileRepository = profileRepository;
        this.permissionRepository = permissionRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public void addPermissionToProfile(Long profileId, Long permissionId) {
        Profile profile = profileRepository.findById(profileId)
                .orElseThrow(() -> new RuntimeException("Perfil no encontrado"));

        Permission permission = permissionRepository.findById(permissionId)
                .orElseThrow(() -> new RuntimeException("Permiso no encontrado"));

        if (!profile.getPermissions().contains(permission)) {
            profile.getPermissions().add(permission);
            profileRepository.save(profile);
        } else {
            throw new IllegalArgumentException("El permiso ya está asignado a este perfil.");
        }
    }

    @Transactional
    public void removePermissionFromProfile(Long profileId, Long permissionId) {
        Profile profile = profileRepository.findById(profileId)
                .orElseThrow(() -> new RuntimeException("Perfil no encontrado"));

        Permission permission = permissionRepository.findById(permissionId)
                .orElseThrow(() -> new RuntimeException("Permiso no encontrado"));

        if (profile.getPermissions().contains(permission)) {
            profile.getPermissions().remove(permission);
            profileRepository.save(profile);
        } else {
            throw new IllegalArgumentException("El permiso no está asignado a este perfil.");
        }
    }
    @Transactional(readOnly = true)
    public java.util.List<Profile> getAllProfiles() {
        return profileRepository.findAll();
    }

    @Transactional
    public Profile createProfile(String name) {
    	if (profileRepository.findByName(name).isPresent()) {
    		throw new IllegalArgumentException("Ya existe un perfil con ese nombre.");
    	}
    	Profile profile = new Profile();
    	profile.setName(name);
    	profile.setHierarchyLevel(1); // Default low level
    	return profileRepository.save(profile);
    }
    
    @Transactional
    public Profile updateProfileName(Long id, String newName) {
    	Profile profile = profileRepository.findById(id)
    			.orElseThrow(() -> new RuntimeException("Perfil no encontrado"));
    	if (!profile.getName().equals(newName) && profileRepository.findByName(newName).isPresent()) {
    		throw new IllegalArgumentException("Ya existe un perfil con ese nombre.");
    	}
    	profile.setName(newName);
    	return profileRepository.save(profile);
    }
    
    @Transactional
    public void deleteProfile(Long id) {
    	Profile profile = profileRepository.findById(id)
    			.orElseThrow(() -> new RuntimeException("Perfil no encontrado"));
    	if ("SUPER_ADMINISTRADOR".equals(profile.getName())) {
    		throw new IllegalArgumentException("No se puede eliminar el perfil SUPER_ADMINISTRADOR.");
    	}
    	// Check if any users are assigned to this profile
    	if (userRepository.existsByProfileId(id)) {
    		throw new IllegalArgumentException("No se puede eliminar: hay usuarios activos con este rol.");
    	}
    	profileRepository.delete(profile);
    }
    
    @Transactional
    public void updateHierarchy(java.util.List<Long> orderedIds) {
    	// orderedIds[0] is the highest rank
    	int size = orderedIds.size();
    	for (int i = 0; i < size; i++) {
    		Long id = orderedIds.get(i);
    		Profile profile = profileRepository.findById(id).orElse(null);
    		if (profile != null) {
    			// Higher index in list = Lower Level?
    			// Discord: Top of list = Highest Role.
    			// Our logic: hierarchyLevel 100 > hierarchyLevel 1.
    			// So Index 0 should have Level = Size. Index 1 = Size - 1.
    			profile.setHierarchyLevel(size - i);
    			profileRepository.save(profile);
    		}
    	}
    }

    @Transactional
    public java.util.List<Profile> getProfilesForRole(String roleName) {
        java.util.List<Profile> allProfiles = profileRepository.findAll();
        
        Profile currentProfile = allProfiles.stream()
                .filter(p -> p.getName().equals(roleName))
                .findFirst()
                .orElse(null);

        if (currentProfile == null) {
            return java.util.Collections.emptyList();
        }
        
        // Auto-bootstrap hierarchy if needed (Fix for empty levels)
        if ("SUPER_ADMINISTRADOR".equals(currentProfile.getName()) && 
           (currentProfile.getHierarchyLevel() == null || currentProfile.getHierarchyLevel() == 0)) {
            bootstrapHierarchy();
            // Refetch to get updated values
            allProfiles = profileRepository.findAll();
            currentProfile = allProfiles.stream()
                    .filter(p -> p.getName().equals(roleName))
                    .findFirst()
                    .orElse(currentProfile);
        }

        // Super Admin sees everything
        if ("SUPER_ADMINISTRADOR".equals(currentProfile.getName())) {
            return allProfiles;
        }

        int currentLevel = currentProfile.getHierarchyLevel() != null ? currentProfile.getHierarchyLevel() : 0;

        return allProfiles.stream()
                .filter(p -> {
                    int pLevel = p.getHierarchyLevel() != null ? p.getHierarchyLevel() : 0;
                    return pLevel < currentLevel;
                })
                .collect(java.util.stream.Collectors.toList());
    }

    private void bootstrapHierarchy() {
        updateLevel("SUPER_ADMINISTRADOR", 100);
        updateLevel("ADMINISTRADOR", 50);
        updateLevel("MODERADOR", 30);
        updateLevel("EDITOR", 20);
        updateLevel("USUARIO", 10);
    }

    private void updateLevel(String name, int level) {
        profileRepository.findByName(name).ifPresent(p -> {
            if (p.getHierarchyLevel() == null || p.getHierarchyLevel() == 0) {
                p.setHierarchyLevel(level);
                profileRepository.save(p);
            }
        });
    }
}
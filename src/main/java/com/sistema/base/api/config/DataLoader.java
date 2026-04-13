package com.sistema.base.api.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.sistema.base.api.entity.Permission;
import com.sistema.base.api.entity.Profile;
import com.sistema.base.api.entity.Theme;
import com.sistema.base.api.repository.PermissionRepository;
import com.sistema.base.api.repository.ProfileRepository;
import com.sistema.base.api.repository.ThemeRepository;

@Configuration
public class DataLoader {

    private final PermissionRepository permissionRepository;
    private final ProfileRepository profileRepository;
    private final ThemeRepository themeRepository;

    public DataLoader(PermissionRepository permissionRepository, 
                      ProfileRepository profileRepository,
                      ThemeRepository themeRepository) {
        this.permissionRepository = permissionRepository;
        this.profileRepository = profileRepository;
        this.themeRepository = themeRepository;
    }

    @Bean
    CommandLineRunner initDatabase() {
        return args -> {
            // 1. Crear permisos Simplificados (Por Módulo)
            
            // Base
            Permission accesoBasico = createPermissionIfNotFound("ACCESO_BASICO");

            // Módulos Completos
            Permission gestionUsuarios = createPermissionIfNotFound("GESTION_USUARIOS");
            Permission gestionRoles = createPermissionIfNotFound("GESTION_ROLES");
            Permission gestionJerarquia = createPermissionIfNotFound("GESTION_JERARQUIA");

            // 2. Crear perfiles y asignar permisos

            // SUPER_ADMINISTRADOR (Todo + Nivel 100)
            if (profileRepository.findByName("SUPER_ADMINISTRADOR").isEmpty()) {
                Profile superAdmin = new Profile();
                superAdmin.setName("SUPER_ADMINISTRADOR");
                superAdmin.setHierarchyLevel(100);
                superAdmin.getPermissions().add(accesoBasico);
                superAdmin.getPermissions().add(gestionUsuarios);
                superAdmin.getPermissions().add(gestionRoles);
                superAdmin.getPermissions().add(gestionJerarquia);
                profileRepository.save(superAdmin);
            }

            // ADMINISTRADOR (Todo)
            if (profileRepository.findByName("ADMINISTRADOR").isEmpty()) {
                Profile admin = new Profile();
                admin.setName("ADMINISTRADOR");
                admin.setHierarchyLevel(50);
                admin.getPermissions().add(accesoBasico);
                admin.getPermissions().add(gestionUsuarios);
                admin.getPermissions().add(gestionRoles);
                admin.getPermissions().add(gestionJerarquia);
                profileRepository.save(admin);
            }

            // MODERADOR (Solo Usuarios)
            if (profileRepository.findByName("MODERADOR").isEmpty()) {
                Profile mod = new Profile();
                mod.setName("MODERADOR");
                mod.setHierarchyLevel(30);
                mod.getPermissions().add(accesoBasico);
                mod.getPermissions().add(gestionUsuarios);
                profileRepository.save(mod);
            }

            // EDITOR (Básico)
            if (profileRepository.findByName("EDITOR").isEmpty()) {
                Profile editor = new Profile();
                editor.setName("EDITOR");
                editor.setHierarchyLevel(20);
                editor.getPermissions().add(accesoBasico);
                profileRepository.save(editor);
            }

            // USUARIO (Básico)
            if (profileRepository.findByName("USUARIO").isEmpty()) {
                Profile usuario = new Profile();
                usuario.setName("USUARIO");
                usuario.setHierarchyLevel(10);
                usuario.getPermissions().add(accesoBasico);
                profileRepository.save(usuario);
            }

            // 3. Crear Temas Iniciales
            createThemeIfNotFound("base", "Clásico", "Tema azul profesional", "#357ABD", "#f4f7fa", "#357ABD", "#ffffff", "#ffffff", "#212529", "#6c757d", false, true);
            createThemeIfNotFound("aqua", "Aqua", "Tema verde-agua moderno", "#00897b", "#e0f2f1", "#00897b", "#ffffff", "#ffffff", "#212529", "#607d8b", false, true);
            createThemeIfNotFound("dark", "Oscuro", "Modo oscuro nativo", "#5c6bc0", "#1a1a2e", "#16213e", "#e8e8e8", "#1f1f38", "#e8e8e8", "#a0a0a0", true, true);

        };
    }

    private void createThemeIfNotFound(String key, String name, String desc, String primary, String bg, String topbar, String topbarText, String card, String textP, String textS, boolean isDark, boolean isSystem) {
        if (themeRepository.findByThemeKey(key).isEmpty()) {
            Theme theme = Theme.builder()
                    .themeKey(key)
                    .name(name)
                    .description(desc)
                    .primaryColor(primary)
                    .backgroundColor(bg)
                    .topbarColor(topbar)
                    .topbarTextColor(topbarText)
                    .cardBackground(card)
                    .textPrimary(textP)
                    .textSecondary(textS)
                    .isDark(isDark)
                    .isSystem(isSystem)
                    .build();
            themeRepository.save(theme);
        }
    }

    private Permission createPermissionIfNotFound(String name) {
        return permissionRepository.findByName(name)
                .orElseGet(() -> permissionRepository.save(new Permission(null, name)));
    }
}
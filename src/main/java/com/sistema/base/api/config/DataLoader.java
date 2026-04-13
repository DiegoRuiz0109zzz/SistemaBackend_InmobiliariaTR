package com.sistema.base.api.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.sistema.base.api.entity.Permission;
import com.sistema.base.api.entity.Profile;
import com.sistema.base.api.entity.Theme;
import com.sistema.base.api.entity.User;
import com.sistema.base.api.repository.PermissionRepository;
import com.sistema.base.api.repository.ProfileRepository;
import com.sistema.base.api.repository.ThemeRepository;
import com.sistema.base.api.repository.UserRepository;

@Configuration
public class DataLoader {

    private final PermissionRepository permissionRepository;
    private final ProfileRepository profileRepository;
    private final ThemeRepository themeRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataLoader(PermissionRepository permissionRepository,
                      ProfileRepository profileRepository,
                      ThemeRepository themeRepository,
                      UserRepository userRepository,
                      PasswordEncoder passwordEncoder) {
        this.permissionRepository = permissionRepository;
        this.profileRepository = profileRepository;
        this.themeRepository = themeRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Bean
    CommandLineRunner initDatabase() {
        return args -> {
            // 1. Crear Permisos de Gestión
            Permission accesoBasico = createPermissionIfNotFound("ACCESO_BASICO");
            Permission gestionUsuarios = createPermissionIfNotFound("GESTION_USUARIOS");
            Permission gestionRoles = createPermissionIfNotFound("GESTION_ROLES");
            Permission gestionJerarquia = createPermissionIfNotFound("GESTION_JERARQUIA");

            // 2. Crear Perfiles Corporativos (Terranort)

            // SUPER_ADMINISTRADOR - Máxima jerarquía técnica
            Profile superAdmin = profileRepository.findByName("SUPER_ADMINISTRADOR")
                    .orElseGet(() -> {
                        Profile p = new Profile();
                        p.setName("SUPER_ADMINISTRADOR");
                        p.setHierarchyLevel(100);
                        p.getPermissions().add(accesoBasico);
                        p.getPermissions().add(gestionUsuarios);
                        p.getPermissions().add(gestionRoles);
                        p.getPermissions().add(gestionJerarquia);
                        return profileRepository.save(p);
                    });

            // GERENTE GENERAL - Acceso total al negocio
            if (profileRepository.findByName("GERENTE_GENERAL").isEmpty()) {
                Profile p = new Profile();
                p.setName("GERENTE_GENERAL");
                p.setHierarchyLevel(95);
                p.getPermissions().add(accesoBasico);
                p.getPermissions().add(gestionUsuarios);
                p.getPermissions().add(gestionRoles);
                profileRepository.save(p);
            }

            // Roles con permisos básicos iniciales
            createProfileIfNotFound("JEFE_ADMINISTRACION", 90, accesoBasico);
            createProfileIfNotFound("JEFE_VENTAS", 85, accesoBasico);
            createProfileIfNotFound("CONTADORA", 80, accesoBasico);
            createProfileIfNotFound("ABOGADA", 80, accesoBasico);
            createProfileIfNotFound("ADMINISTRADORA", 75, accesoBasico);
            createProfileIfNotFound("ASISTENTE_ADMINISTRATIVO", 70, accesoBasico);

            // 3. Crear Usuario Inicial (admin / admin)
            if (userRepository.findByUsername("admin").isEmpty()) {
                User adminUser = User.builder()
                        .username("admin")
                        .password(passwordEncoder.encode("admin"))
                        .nombres("Administrador")
                        .apellidos("Terranort")
                        .email("admin@terranort.pe")
                        .telefono("999999999")
                        .docIdentidad("00000000") // Valor ficticio para cumplir el nullable=false
                        .profile(superAdmin)
                        .build();

                // Nota: Si aún no agregas el campo 'enabled' en User.java,
                // no llames a .enabled(true) aquí para evitar errores de compilación.

                userRepository.save(adminUser);
                System.out.println(">>> CONFIGURACIÓN INICIAL: Usuario 'admin' creado con contraseña 'admin'");
            }

            // 4. Temas (Manteniendo tus configuraciones)
            createThemeIfNotFound("base", "Clásico", "Tema azul profesional", "#357ABD", "#f4f7fa", "#357ABD", "#ffffff", "#ffffff", "#212529", "#6c757d", false, true);
            createThemeIfNotFound("dark", "Oscuro", "Modo oscuro nativo", "#5c6bc0", "#1a1a2e", "#16213e", "#e8e8e8", "#1f1f38", "#e8e8e8", "#a0a0a0", true, true);
        };
    }

    private void createProfileIfNotFound(String name, int level, Permission permission) {
        if (profileRepository.findByName(name).isEmpty()) {
            Profile profile = new Profile();
            profile.setName(name);
            profile.setHierarchyLevel(level);
            profile.getPermissions().add(permission);
            profileRepository.save(profile);
        }
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
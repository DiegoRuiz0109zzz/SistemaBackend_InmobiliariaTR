package com.sistema.base.api.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.sistema.base.api.entity.User;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Integer> {
	
    Optional<User> findByUsername(String username);
    
    Boolean existsByUsername(String username);

	Boolean existsByEmail(String email);
	
	Optional<User> findById(Integer id);
	
	Optional<User> findByDocIdentidad(String docIdentidad);
	
	@Query("SELECT COUNT(u.id) FROM User u")
	Integer contarTotalUsuarios();
	
	@Query("SELECT u FROM User u WHERE LOWER(u.docIdentidad) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(u.nombres) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(u.apellidos) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<User> buscarPorDniNombreOApellido(String search, Pageable pageable);

	Page<User> findByProfileNameIn(java.util.List<String> profiles, Pageable pageable);

	@Query("SELECT u FROM User u WHERE (u.profile.name IN :profiles) AND (LOWER(u.docIdentidad) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(u.nombres) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(u.apellidos) LIKE LOWER(CONCAT('%', :search, '%')))")
	Page<User> buscarPorDniNombreOApellidoYPerfiles(String search, java.util.List<String> profiles, Pageable pageable);

	@Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END FROM User u WHERE u.profile.id = :profileId")
	Boolean existsByProfileId(Long profileId);
}

package com.sistema.base.api.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sistema.base.api.entity.Profile;

import java.util.Optional;

public interface ProfileRepository extends JpaRepository<Profile, Long> {
	
    Optional<Profile> findByName(String name);
}

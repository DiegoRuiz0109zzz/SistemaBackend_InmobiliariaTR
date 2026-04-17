package com.sistema.base.api.core.Ubigeo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface UbigeoRepository extends JpaRepository<Ubigeo, String> {

    // 1. Obtener lista de departamentos (Únicos y ordenados)
    @Query("SELECT DISTINCT u.departamento FROM Ubigeo u WHERE u.departamento IS NOT NULL AND u.departamento != '' ORDER BY u.departamento ASC")
    List<String> findDistinctDepartamentos();

    // 2. Obtener provincias según el departamento seleccionado
    @Query("SELECT DISTINCT u.provincia FROM Ubigeo u WHERE u.departamento = :departamento AND u.provincia IS NOT NULL AND u.provincia != '' ORDER BY u.provincia ASC")
    List<String> findDistinctProvinciasByDepartamento(String departamento);

    // 3. Obtener los distritos completos (con su id_ubigeo) según Dep y Prov
    @Query("SELECT u FROM Ubigeo u WHERE u.departamento = :departamento AND u.provincia = :provincia AND u.distrito IS NOT NULL AND u.distrito != '' ORDER BY u.distrito ASC")
    List<Ubigeo> findDistritos(String departamento, String provincia);
}

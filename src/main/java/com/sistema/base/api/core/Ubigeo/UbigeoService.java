package com.sistema.base.api.core.Ubigeo;

import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class UbigeoService {

    private final UbigeoRepository ubigeoRepository;

    public UbigeoService(UbigeoRepository ubigeoRepository) {
        this.ubigeoRepository = ubigeoRepository;
    }

    public List<String> getDepartamentos() {
        return ubigeoRepository.findDistinctDepartamentos();
    }

    public List<String> getProvincias(String departamento) {
        return ubigeoRepository.findDistinctProvinciasByDepartamento(departamento);
    }

    public List<Ubigeo> getDistritos(String departamento, String provincia) {
        return ubigeoRepository.findDistritos(departamento, provincia);
    }

    public Ubigeo getUbigeoPorId(String idUbigeo) {
        return ubigeoRepository.findById(idUbigeo)
                .orElseThrow(() -> new RuntimeException("Código de Ubigeo no encontrado: " + idUbigeo));
    }
}
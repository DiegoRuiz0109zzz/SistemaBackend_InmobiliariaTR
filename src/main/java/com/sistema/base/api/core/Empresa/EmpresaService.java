package com.sistema.base.api.core.Empresa;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EmpresaService {

    private final EmpresaRepository empresaRepository;

    @Transactional(readOnly = true)
    public List<Empresa> listarTodas() {
        return empresaRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Empresa obtenerPorId(Long id) {
        return empresaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Empresa no encontrada con ID: " + id));
    }

    @Transactional
    public Empresa guardar(Empresa empresa) {
        if (empresaRepository.existsByRuc(empresa.getRuc())) {
            throw new RuntimeException("El RUC ya se encuentra registrado.");
        }
        return empresaRepository.save(empresa);
    }

    @Transactional
    public Empresa actualizar(Long id, Empresa empresaRequest) {
        Empresa empresa = obtenerPorId(id);

        empresa.setRazonSocial(empresaRequest.getRazonSocial());
        empresa.setDireccion(empresaRequest.getDireccion());
        empresa.setTelefono(empresaRequest.getTelefono());
        empresa.setEmail(empresaRequest.getEmail());
        empresa.setWeb(empresaRequest.getWeb());
        empresa.setEnabled(empresaRequest.isEnabled());

        return empresaRepository.save(empresa);
    }

    @Transactional
    public void eliminar(Long id) {
        empresaRepository.deleteById(id);
    }
}

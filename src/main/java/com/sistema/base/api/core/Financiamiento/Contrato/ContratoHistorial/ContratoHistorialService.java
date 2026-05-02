package com.sistema.base.api.core.Financiamiento.Contrato.ContratoHistorial;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ContratoHistorialService {

    private final ContratoHistorialRepository contratoHistorialRepository;

    @Transactional(readOnly = true)
    public List<ContratoHistorial> listarPorContrato(Long contratoId) {
        return contratoHistorialRepository.findByContratoIdOrderByFechaRegistroDesc(contratoId);
    }

    @Transactional(readOnly = true)
    public ContratoHistorial obtenerPorId(Long id) {
        return contratoHistorialRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Registro histórico no encontrado con ID: " + id));
    }

    @Transactional
    public ContratoHistorial guardar(ContratoHistorial contratoHistorial) {
        return contratoHistorialRepository.save(contratoHistorial);
    }

    @Transactional
    public void eliminar(Long id) {
        contratoHistorialRepository.deleteById(id);
    }
}
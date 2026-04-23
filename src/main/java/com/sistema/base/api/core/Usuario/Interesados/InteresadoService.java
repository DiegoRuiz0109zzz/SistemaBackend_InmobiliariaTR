package com.sistema.base.api.core.Usuario.Interesados;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InteresadoService {

    private final InteresadoRepository interesadoRepository;

    @Transactional(readOnly = true)
    public List<Interesado> listarTodos() {
        return interesadoRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Interesado obtenerPorId(Long id) {
        return interesadoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Interesado no encontrado con ID: " + id));
    }

    @Transactional
    public Interesado guardar(Interesado interesado) {
        if (interesado.getTelefono() != null && interesadoRepository.existsByTelefono(interesado.getTelefono())) {
            throw new RuntimeException("Este número de teléfono ya está registrado como interesado.");
        }
        if (interesado.getNumeroDocumento() != null && !interesado.getNumeroDocumento().isEmpty()
                && interesadoRepository.existsByNumeroDocumento(interesado.getNumeroDocumento())) {
            throw new RuntimeException("El número de documento ya está registrado en otro interesado.");
        }
        return interesadoRepository.save(interesado);
    }

    @Transactional
    public Interesado actualizar(Long id, Interesado interesadoRequest) {
        Interesado interesado = obtenerPorId(id);

        interesado.setTipoDocumento(interesadoRequest.getTipoDocumento());
        interesado.setNumeroDocumento(interesadoRequest.getNumeroDocumento());
        interesado.setFechaIngreso(interesadoRequest.getFechaIngreso());
        interesado.setNombres(interesadoRequest.getNombres());
        interesado.setApellidos(interesadoRequest.getApellidos());
        interesado.setTelefono(interesadoRequest.getTelefono());
        interesado.setEmail(interesadoRequest.getEmail());
        interesado.setEnabled(interesadoRequest.isEnabled());

        return interesadoRepository.save(interesado);
    }

    @Transactional
    public void eliminar(Long id) {
        Interesado interesado = obtenerPorId(id);
        interesado.setEnabled(false);
        interesadoRepository.save(interesado);
    }
}
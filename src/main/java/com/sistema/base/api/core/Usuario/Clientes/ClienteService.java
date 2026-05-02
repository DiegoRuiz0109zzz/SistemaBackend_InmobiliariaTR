package com.sistema.base.api.core.Usuario.Clientes;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ClienteService {

    private final ClienteRepository clienteRepository;

    @Transactional(readOnly = true)
    public List<Cliente> listarTodos() {
        return clienteRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Cliente obtenerPorId(Long id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado con ID: " + id));
    }

    @Transactional
    public Cliente guardar(Cliente cliente) {
        if (clienteRepository.existsByNumeroDocumento(cliente.getNumeroDocumento())) {
            throw new RuntimeException("El número de documento ya se encuentra registrado.");
        }
        return clienteRepository.save(cliente);
    }

    @Transactional
    public Cliente actualizar(Long id, Cliente clienteRequest) {
        Cliente cliente = obtenerPorId(id);

        cliente.setTipoDocumento(clienteRequest.getTipoDocumento());
        cliente.setNombres(clienteRequest.getNombres());
        cliente.setApellidos(clienteRequest.getApellidos());
        cliente.setDireccion(clienteRequest.getDireccion());
        cliente.setTelefono(clienteRequest.getTelefono());
        cliente.setEmail(clienteRequest.getEmail());
        cliente.setDepartamento(clienteRequest.getDepartamento());
        cliente.setProvincia(clienteRequest.getProvincia());
        cliente.setDistrito(clienteRequest.getDistrito());
        cliente.setUbigeo(clienteRequest.getUbigeo());
        cliente.setEstadoCivil(clienteRequest.getEstadoCivil());
        cliente.setEnabled(clienteRequest.isEnabled());

        return clienteRepository.save(cliente);
    }

    @Transactional
    public void eliminar(Long id) {
        Cliente cliente = obtenerPorId(id);
        // En lugar de clienteRepository.delete(cliente); hacemos esto:
        cliente.setEnabled(false); // Cambia el estado a 0
        clienteRepository.save(cliente);
    }
}

package com.example.BarberiaLaClasica.service;

import com.example.BarberiaLaClasica.model.Cliente;
import com.example.BarberiaLaClasica.repository.ClienteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class ClienteService {

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    public List<Cliente> listarTodos() {
        return clienteRepository.findAll();
    }

    // MODIFICADO: Recibe la variable search y decide qué consulta realizar
    public Page<Cliente> listarTodosPaginado(Pageable pageable, String search) {
        if (search != null && !search.trim().isEmpty()) {
            String query = search.trim();
            return clienteRepository.findByDniContainingOrNombresContainingIgnoreCaseOrApellidosContainingIgnoreCase(
                    query, query, query, pageable);
        }
        return clienteRepository.findAll(pageable);
    }

    public List<Cliente> listarActivos() {
        return clienteRepository.findByEstado(1);
    }

    public Cliente buscarPorId(Long id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));
    }

    public Cliente crearDesdeAdmin(Cliente c, String passwordPlana) {
        if (clienteRepository.existsByDni(c.getDni())) {
            throw new RuntimeException("Ya existe un cliente con ese DNI.");
        }
        if (c.getCorreo() != null && clienteRepository.existsByCorreo(c.getCorreo())) {
            throw new RuntimeException("Ya existe una cuenta con ese correo.");
        }
        
        c.setPassword(passwordEncoder.encode((CharSequence) passwordPlana));
        c.setEstado(1);
        return clienteRepository.save(c);
    }

    public Cliente actualizarDesdeAdmin(Long id, Cliente datosActualizados) {
        Cliente clienteExistente = buscarPorId(id);

        if (datosActualizados.getCorreo() != null && !datosActualizados.getCorreo().equalsIgnoreCase(clienteExistente.getCorreo())) {
            if (clienteRepository.existsByCorreo(datosActualizados.getCorreo())) {
                throw new RuntimeException("El correo ya está registrado por otro cliente.");
            }
        }

        clienteExistente.setNombres(datosActualizados.getNombres());
        clienteExistente.setApellidos(datosActualizados.getApellidos());
        clienteExistente.setTelefono(datosActualizados.getTelefono());
        clienteExistente.setCorreo(datosActualizados.getCorreo());

        return clienteRepository.save(clienteExistente);
    }

    public void registrarOnline(Cliente cliente, String passwordPlana) {
        if (clienteRepository.existsByDni(cliente.getDni())) {
            throw new RuntimeException("Ya existe una cuenta con ese DNI.");
        }
        if (clienteRepository.existsByCorreo(cliente.getCorreo())) {
            throw new RuntimeException("Ya existe una cuenta con ese correo.");
        }
        cliente.setPassword(passwordEncoder.encode(passwordPlana));
        cliente.setEstado(1);
        clienteRepository.save(cliente);
    }

    public void cambiarEstado(Long id) {
        Cliente c = buscarPorId(id);
        c.setEstado(c.getEstado() == 1 ? 0 : 1);
        clienteRepository.save(c);
    }

    public long contarActivos() {
        return clienteRepository.findByEstado(1).size();
    }
}
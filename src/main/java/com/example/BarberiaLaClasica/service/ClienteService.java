package com.example.BarberiaLaClasica.service;

import com.example.BarberiaLaClasica.model.Cliente;
import com.example.BarberiaLaClasica.repository.ClienteRepository;
import org.springframework.beans.factory.annotation.Autowired;
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

    public List<Cliente> listarActivos() {
        return clienteRepository.findByEstado(1);
    }

    public Cliente buscarPorId(Long id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));
    }

    // Crear cuenta desde sistema (secretario)
    public Cliente crearDesdeAdmin(Cliente cliente, String passwordPlana) {
        if (clienteRepository.existsByDni(cliente.getDni())) {
            throw new RuntimeException("Ya existe un cliente con ese DNI.");
        }
        if (cliente.getCorreo() != null && clienteRepository.existsByCorreo(cliente.getCorreo())) {
            throw new RuntimeException("Ya existe una cuenta con ese correo.");
        }
        cliente.setPassword(passwordEncoder.encode(passwordPlana));
        cliente.setEstado(1);
        clienteRepository.save(cliente);
        return cliente; // ← esta línea falta
    }

    // Registro online del cliente
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
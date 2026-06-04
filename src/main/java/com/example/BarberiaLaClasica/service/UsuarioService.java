package com.example.BarberiaLaClasica.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import com.example.BarberiaLaClasica.model.Usuario;
import com.example.BarberiaLaClasica.repository.UsuarioRepository;
import java.util.List;
import java.util.Optional;

@Service
public class UsuarioService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    public List<Usuario> listarActivos() {
        return usuarioRepository.findByEstado(1);
    }

    public Usuario guardar(Usuario usuario) {
        // Validar límite de 5 usuarios activos
        long totalActivos = usuarioRepository.countByEstado(1);
        if (totalActivos >= 5) {
            throw new IllegalStateException("Límite máximo de 5 usuarios alcanzado.");
        }

        // Validar email duplicado (solo en creación)
        if (usuario.getId() == null && usuarioRepository.existsByEmail(usuario.getEmail())) {
            throw new IllegalArgumentException("El email ya está registrado.");
        }

        String passCifrada = passwordEncoder.encode(usuario.getPassword());
        usuario.setPassword(passCifrada);
        usuario.setEstado(1);
        return usuarioRepository.save(usuario);
    }

    public Usuario editarUsuario(Usuario usuario) {
        Usuario existente = usuarioRepository.findById(usuario.getId())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        existente.setNombre(usuario.getNombre());
        existente.setEmail(usuario.getEmail());
        existente.setPerfil(usuario.getPerfil());

        // Solo actualiza contraseña si viene una nueva
        if (usuario.getPassword() != null && !usuario.getPassword().isBlank()) {
            existente.setPassword(passwordEncoder.encode(usuario.getPassword()));
        }

        return usuarioRepository.save(existente);
    }

    // Contar usuarios activos para el frontend
    public long contarActivos() {
        return usuarioRepository.countByEstado(1);
    }

    public boolean desactivarUsuario(Long idAEliminar, Long idUsuarioLogueado) {
        if (idAEliminar.equals(idUsuarioLogueado)) {
            return false;
        }
        Optional<Usuario> u = usuarioRepository.findById(idAEliminar);
        if (u.isPresent()) {
            Usuario usuario = u.get();
            usuario.setEstado(0);
            usuarioRepository.save(usuario);
            return true;
        }
        return false;
    }

    public void eliminarLogico(Long id) {
        usuarioRepository.findById(id).ifPresent(usuario -> {
            usuario.setEstado(0);
            usuarioRepository.save(usuario);
        });
    }

    public void cambiarEstado(Long id) {
        usuarioRepository.findById(id).ifPresent(u -> {
            u.setEstado(u.getEstado() == 1 ? 0 : 1);
            usuarioRepository.save(u);
        });
    }

    public List<Usuario> listarTodos() {
        return usuarioRepository.findAll();
    }
}
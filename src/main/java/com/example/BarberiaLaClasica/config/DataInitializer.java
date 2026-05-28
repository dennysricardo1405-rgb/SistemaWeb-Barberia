package com.example.BarberiaLaClasica.config;

import jakarta.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import com.example.BarberiaLaClasica.model.Perfil;
import com.example.BarberiaLaClasica.model.Usuario;
import com.example.BarberiaLaClasica.repository.PerfilRepository;
import com.example.BarberiaLaClasica.repository.UsuarioRepository;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PerfilRepository perfilRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        // Solo crea el admin si no existe
        if (!usuarioRepository.existsByEmail("admin@gmail.com")) {

            // Busca el perfil Administrador que ya existe en BD
            Perfil adminPerfil = perfilRepository.findById(1L)
                    .orElseThrow(() -> new RuntimeException("Perfil Administrador no encontrado"));

            Usuario admin = new Usuario();
            admin.setNombre("Dennys Lozano");
            admin.setEmail("admin@gmail.com");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setEstado(1);
            admin.setPerfil(adminPerfil);
            usuarioRepository.save(admin);

            System.out.println("✅ Usuario admin creado con éxito.");
        } else {
            System.out.println("ℹ️ Admin ya existe, no se creó nada.");
        }
        // Al final del método run() agrégalo siempre, fuera del if
        Usuario u = usuarioRepository.findByEmail("admin@gmail.com").orElse(null);
        if (u != null) {
            System.out.println("✅ Usuario encontrado: " + u.getEmail());
            System.out.println("✅ Perfil: " + u.getPerfil().getNombrePerfil());
            System.out.println("✅ Permisos: " + u.getPerfil().getPermisos().size());
            System.out.println("✅ Password hash: " + u.getPassword());
        } else {
            System.out.println("❌ Usuario NO encontrado");
        }
    }
    
}
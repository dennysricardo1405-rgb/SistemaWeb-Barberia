package com.example.BarberiaLaClasica.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.example.BarberiaLaClasica.model.Perfil;
import com.example.BarberiaLaClasica.model.Producto;
import com.example.BarberiaLaClasica.model.Usuario;
import com.example.BarberiaLaClasica.repository.BarberoRepository;
import com.example.BarberiaLaClasica.repository.CategoriaRepository;
import com.example.BarberiaLaClasica.repository.ClienteRepository;
import com.example.BarberiaLaClasica.repository.PerfilRepository;
import com.example.BarberiaLaClasica.repository.ProductoRepository;
import com.example.BarberiaLaClasica.repository.ServicioRepository;
import com.example.BarberiaLaClasica.service.BarberoService;
import com.example.BarberiaLaClasica.service.ConfiguracionSitioService;
import com.example.BarberiaLaClasica.service.PerfilService;
import com.example.BarberiaLaClasica.service.PromocionService;
import com.example.BarberiaLaClasica.service.SliderImageService;
import com.example.BarberiaLaClasica.service.UsuarioService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Controller
public class NavigationController {

    @Autowired private UsuarioService usuarioService;
    @Autowired private PerfilRepository perfilRepository;
    @Autowired private PerfilService perfilService;
    @Autowired private BarberoRepository barberoRepository;
    @Autowired private ClienteRepository clienteRepository;
    @Autowired private ProductoRepository productoRepository;
    @Autowired private CategoriaRepository categoriaRepository;
    @Autowired private BarberoService barberoService;
    @Autowired private ServicioRepository servicioRepository;
    @Autowired private SliderImageService sliderImageService;
    @Autowired private PromocionService promocionService;
    @Autowired private ConfiguracionSitioService configuracionSitioService;

    @GetMapping("/")
    public String index(Model model) {
        List<Producto> productosWeb = productoRepository.findByActivoTrue();
        model.addAttribute("productosBarberia", productosWeb);
        model.addAttribute("servicios", servicioRepository.findByEstado(1));
        model.addAttribute("barberos", barberoService.listarTodos());
        model.addAttribute("subcategoriasBarberia",
                categoriaRepository.findByPadreNombreAndActivoTrue("Productos de Barbería"));
        model.addAttribute("sliderImagenes", sliderImageService.listarActivas());
        model.addAttribute("promociones", promocionService.listarActivas());
        model.addAttribute("config", configuracionSitioService.obtenerMapa());
        return "index";
    }

    // ── Dashboard Admin ──────────────────────────────────────
    @GetMapping("/admin/dashboard")
    public String dashboard(Model model, Authentication authentication) {
        model.addAttribute("usuarioNombre", authentication.getName());
        long totalBarberos = barberoRepository.count();
        long totalClientes = clienteRepository.count();
        model.addAttribute("totalBarberos", totalBarberos);
        model.addAttribute("totalClientes", totalClientes);
        model.addAttribute("citasHoy", 0);
        model.addAttribute("ingresosMes", "0.00");
        return "admin-dashboard";
    }

    // ── Usuarios ─────────────────────────────────────────────
    @GetMapping("/admin/usuarios")
    public String gestionUsuarios(Model model, Authentication authentication) {
        model.addAttribute("usuarios", usuarioService.listarTodos());
        model.addAttribute("perfiles", perfilService.listarTodo());
        model.addAttribute("usuarioLogueado", authentication.getName());
        return "usuarios-lista";
    }

    @GetMapping("/admin/usuarios/estado/{id}")
    public String cambiarEstadoUsuario(@PathVariable("id") Long id) {
        usuarioService.cambiarEstado(id);
        return "redirect:/admin/usuarios";
    }

    // ── Contador de usuarios activos (para el badge y barra) ──
    @GetMapping("/admin/usuarios/count")
    @ResponseBody
    public Map<String, Long> contarUsuarios() {
        return Map.of("total", usuarioService.contarActivos());
    }

    @PostMapping("/admin/usuarios/guardar")
    public String guardarUsuario(@ModelAttribute Usuario usuario,
            @RequestParam("perfilId") Long perfilId,
            Model model, Authentication authentication) {
        try {
            Perfil p = perfilRepository.findById(perfilId)
                    .orElseThrow(() -> new RuntimeException("Perfil no encontrado"));
            usuario.setPerfil(p);
            usuarioService.guardar(usuario);
            return "redirect:/admin/usuarios";
        } catch (IllegalStateException e) {
            model.addAttribute("errorLimite", "⚠️ No puedes agregar más usuarios. El máximo permitido es 5.");
            model.addAttribute("usuarios", usuarioService.listarTodos());
            model.addAttribute("perfiles", perfilService.listarTodo());
            model.addAttribute("usuarioLogueado", authentication.getName());
            return "usuarios-lista";
        }
    }

    @PostMapping("/admin/usuarios/editar")
    public String editarUsuario(@ModelAttribute Usuario usuario,
            @RequestParam("perfilId") Long perfilId) {
        Perfil p = perfilRepository.findById(perfilId)
                .orElseThrow(() -> new RuntimeException("Perfil no encontrado"));
        usuario.setPerfil(p);
        usuarioService.guardar(usuario);
        return "redirect:/admin/usuarios?editado";
    }

    @GetMapping("/admin/usuarios/eliminar/{id}")
    public String eliminarUsuario(@PathVariable("id") Long id) {
        usuarioService.eliminarLogico(id);
        return "redirect:/admin/usuarios?eliminado";
    }

    // ── Perfiles/Roles ───────────────────────────────────────
    @GetMapping("/admin/perfiles")
    public String gestionPerfiles(Model model) {
        model.addAttribute("perfiles", perfilService.listarTodo());
        model.addAttribute("todosLosPermisos", perfilService.listarPermisos());
        return "perfiles-lista";
    }

    @PostMapping("/admin/perfiles/guardar-permisos")
    public String guardarPermisos(@RequestParam("perfilId") Long perfilId,
            @RequestParam(value = "permisoIds", required = false) List<Long> permisoIds) {
        if (permisoIds == null)
            permisoIds = new ArrayList<>();
        perfilService.guardarPerfilConPermisos(perfilId, permisoIds);
        return "redirect:/admin/perfiles?success";
    }
}
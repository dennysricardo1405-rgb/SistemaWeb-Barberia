package com.example.BarberiaLaClasica.controller;

import com.example.BarberiaLaClasica.model.Categoria;
import com.example.BarberiaLaClasica.service.CategoriaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin/categorias")
public class CategoriaController {

    @Autowired
    private CategoriaService categoriaService;

    // Listar todo en la pantalla de gestión
    @GetMapping
    public String listarCategorias(Model model) {
        model.addAttribute("categorias", categoriaService.listarTodas());
        
        // Listamos las principales activas por si el admin quiere crear una subcategoría dentro de ellas
        model.addAttribute("categoriasPrincipales", categoriaService.listarPrincipalesActivas());
        
        // Objeto vacío para el formulario de creación/modal
        model.addAttribute("nuevaCategoria", new Categoria()); 
        
        return "categorias/lista"; 
    }

    // Guardar tanto Categorías como Subcategorías
    // Modifica este método en tu CategoriaController.java

@PostMapping("/guardar")
public String guardarCategoria(@ModelAttribute("nuevaCategoria") Categoria categoria) {
    // --- AQUÍ ESTÁ EL ARREGLO CONTRA EL ERROR 500 ---
    // Si el objeto padre viene instanciado pero su ID está vacío o es nulo, lo limpiamos a null
    if (categoria.getPadre() != null && (categoria.getPadre().getId() == null || categoria.getPadre().getId() == 0)) {
        categoria.setPadre(null);
    }
    
    categoriaService.guardar(categoria);
    return "redirect:/admin/categorias?exito";
}

    // Switch de estado activo/inactivo
    @GetMapping("/estado/{id}")
    public String cambiarEstado(@PathVariable("id") Long id) {
        categoriaService.cambiarEstado(id);
        return "redirect:/admin/categorias?estadoCambiado";
    }
}
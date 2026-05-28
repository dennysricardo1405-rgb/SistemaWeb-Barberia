package com.example.BarberiaLaClasica.service;

import com.example.BarberiaLaClasica.model.Categoria;
import com.example.BarberiaLaClasica.repository.CategoriaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class CategoriaService {

    @Autowired
    private CategoriaRepository categoriaRepository;

    public List<Categoria> listarTodas() {
        return categoriaRepository.findAll();
    }

    public List<Categoria> listarPrincipalesActivas() {
        return categoriaRepository.findByPadreIsNullAndActivoTrue();
    }

    public List<Categoria> listarSubcategoriasPorPadre(Long padreId) {
        return categoriaRepository.findByPadreIdAndActivoTrue(padreId);
    }

    public List<Categoria> listarTodasActivas() {
        return categoriaRepository.findByActivoTrue();
    }

    @Transactional
    public Categoria guardar(Categoria categoria) {
        return categoriaRepository.save(categoria);
    }

    public Categoria buscarPorId(Long id) {
        return categoriaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Categoría o Subcategoría no encontrada con ID: " + id));
    }

    @Transactional
    public void cambiarEstado(Long id) {
        Categoria categoria = buscarPorId(id);
        categoria.setActivo(!categoria.isActivo());
        categoriaRepository.save(categoria);
    }
}
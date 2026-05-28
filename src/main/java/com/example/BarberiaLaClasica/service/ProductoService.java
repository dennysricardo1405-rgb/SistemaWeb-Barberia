package com.example.BarberiaLaClasica.service;

import com.example.BarberiaLaClasica.model.CompraProveedor;
import com.example.BarberiaLaClasica.model.Producto;
import com.example.BarberiaLaClasica.repository.CompraProveedorRepository;
import com.example.BarberiaLaClasica.repository.ProductoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class ProductoService {

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private CompraProveedorRepository compraProveedorRepository;

    public List<Producto> listarTodos() {
        return productoRepository.findAll();
    }

    public List<Producto> listarActivos() {
        return productoRepository.findByActivoTrue();
    }

    public Producto buscarPorId(Long id) {
        return productoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado con ID: " + id));
    }

    @Transactional
    public Producto guardar(Producto producto) {
        return productoRepository.save(producto);
    }

    @Transactional
    public void cambiarEstado(Long id) {
        Producto producto = buscarPorId(id);
        producto.setActivo(!producto.isActivo());
        productoRepository.save(producto);
    }

    // Alerta que usarás en el Dashboard (Punto 4 de tus requerimientos)
    public List<Producto> verificarAlertasStock() {
        return productoRepository.findByStockLessThanEqualAndActivoTrue(3);
    }

    // Lógica de Stock por Compra (Escenarios 1 y 2)
    @Transactional
    public void registrarCompra(CompraProveedor compra) {
        // 1. Buscamos el producto real
        Producto producto = productoRepository.findById(compra.getProducto().getId())
                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));

        // 2. Calculamos las unidades a incrementar
        int stockAIncrementar = 0;
        if ("PAQUETE".equals(compra.getTipoCompra().name())) {
            stockAIncrementar = compra.getCantidadPaquetes() * compra.getUnidadesPorPaquete();
        } else {
            stockAIncrementar = compra.getCantidadPaquetes();
        }

        // 3. Seteamos los totales de la compra
        compra.setTotalUnidades(stockAIncrementar);
        compra.setTotalInvertido(compra.getCantidadPaquetes() * compra.getPrecioCompraPaquete());

        // 4. Actualizamos el catálogo
        producto.setStock(producto.getStock() + stockAIncrementar);
        producto.setPrecioVenta(compra.getPrecioVentaUnidad());

        // 5. Guardamos todo con normalidad
        productoRepository.save(producto);
        compraProveedorRepository.save(compra); // MySQL no chillará porque el ID nunca será null
    }
    
}
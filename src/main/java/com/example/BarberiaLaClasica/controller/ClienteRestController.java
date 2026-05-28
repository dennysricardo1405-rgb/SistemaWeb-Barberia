package com.example.BarberiaLaClasica.controller;

import com.example.BarberiaLaClasica.model.Cliente;
import com.example.BarberiaLaClasica.repository.ClienteRepository;
import com.example.BarberiaLaClasica.service.ClienteService;
import com.example.BarberiaLaClasica.service.DniService;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/clientes")
public class ClienteRestController {

    @Autowired
    private DniService dniService;
    @Autowired
    private ClienteRepository clienteRepository;
    @GetMapping("/consulta-dni/{dni}")
    public ResponseEntity<String> consultarDni(@PathVariable String dni) {
        String resultado = dniService.consultarDni(dni);
        return ResponseEntity.ok(resultado);
    }

    @Autowired
    private ClienteService clienteService;

    @PostMapping("/guardar-rapido")
    public ResponseEntity<?> guardarRapido(@RequestBody Map<String, String> datos) {
        try {
            Cliente c = new Cliente();
            c.setDni(datos.get("dni"));
            c.setNombres(datos.get("nombres"));
            c.setApellidos(datos.get("apellidos"));
            c.setTelefono(datos.get("telefono"));
            c.setCorreo(datos.get("correo"));

            // Password predeterminada: B + dni
            String passwordPlana = "B" + datos.get("dni");
            Cliente guardado = clienteService.crearDesdeAdmin(c, passwordPlana);

            return ResponseEntity.ok(Map.of(
                    "id", guardado.getId(),
                    "dni", guardado.getDni(),
                    "nombres", guardado.getNombres(),
                    "apellidos", guardado.getApellidos()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/buscar-dni")
    public ResponseEntity<List<Map<String, Object>>> buscarPorDni(@RequestParam String q) {
        List<Cliente> clientes = clienteRepository
                .findByDniContainingOrNombresContainingIgnoreCaseOrApellidosContainingIgnoreCase(q, q, q);

        List<Map<String, Object>> resultado = clientes.stream()
                .filter(c -> c.getEstado() == 1)
                .limit(5)
                .map(c -> Map.<String, Object>of(
                        "id", c.getId(),
                        "dni", c.getDni(),
                        "nombres", c.getNombres(),
                        "apellidos", c.getApellidos()))
                .toList();

        return ResponseEntity.ok(resultado);
    }
}
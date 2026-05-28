package com.example.BarberiaLaClasica.service;

import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class DniService {

    private final String API_URL = "https://miapi.cloud/v1/dni/";
    private final String TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VyX2lkIjo2MzQsImV4cCI6MTc2NTMyMzM1Nn0.-WHevNsbZ_rm4NeIFOSHRG-5Jsk6Y8jw75m_IQVyqeM"; 

    public String consultarDni(String dni) {
        RestTemplate restTemplate = new RestTemplate();
        
        // 1. Configurar las cabeceras con el Bearer Token
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(TOKEN);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> entity = new HttpEntity<>(headers);

        // 2. Hacer la petición GET
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                API_URL + dni, 
                HttpMethod.GET, 
                entity, 
                String.class
            );
            return response.getBody(); // Esto devuelve el JSON que viste en la imagen
        } catch (Exception e) {
            return "{\"success\": false, \"message\": \"Error al conectar con la API\"}";
        }
    }
}
// ═══════════════════════════════════════════════════════════════════
// ARCHIVO 2: Promocion.java  →  model/Promocion.java
// ═══════════════════════════════════════════════════════════════════
package com.example.BarberiaLaClasica.model;
 
import jakarta.persistence.*;
import java.time.LocalDate;
 
@Entity
@Table(name = "promociones")
public class Promocion {
 
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
 
    @Column(name = "titulo", nullable = false, length = 100)
    private String titulo;
 
    @Column(name = "descripcion", length = 255)
    private String descripcion;
 
    @Column(name = "imagen_url", length = 500)
    private String imagenUrl;
 
    @Column(name = "badge", length = 30)
    private String badge;          // "NUEVO", "20% OFF", "OFERTA", etc.
 
    @Column(name = "activo", nullable = false)
    private Boolean activo = true;
 
    @Column(name = "orden", nullable = false)
    private Integer orden = 0;
 
    @Column(name = "fecha_inicio")
    private LocalDate fechaInicio;
 
    @Column(name = "fecha_fin")
    private LocalDate fechaFin;
 
    public Promocion() {}
 
    // Getters / Setters
    public Long getId()             { return id; }
    public void setId(Long id)      { this.id = id; }
 
    public String getTitulo()       { return titulo; }
    public void setTitulo(String t) { this.titulo = t; }
 
    public String getDescripcion()          { return descripcion; }
    public void setDescripcion(String d)    { this.descripcion = d; }
 
    public String getImagenUrl()            { return imagenUrl; }
    public void setImagenUrl(String u)      { this.imagenUrl = u; }
 
    public String getBadge()                { return badge; }
    public void setBadge(String b)          { this.badge = b; }
 
    public Boolean getActivo()              { return activo; }
    public void setActivo(Boolean a)        { this.activo = a; }
 
    public Integer getOrden()               { return orden; }
    public void setOrden(Integer o)         { this.orden = o; }
 
    public LocalDate getFechaInicio()       { return fechaInicio; }
    public void setFechaInicio(LocalDate f) { this.fechaInicio = f; }
 
    public LocalDate getFechaFin()          { return fechaFin; }
    public void setFechaFin(LocalDate f)    { this.fechaFin = f; }
}
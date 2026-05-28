package com.example.BarberiaLaClasica.repository;

import com.example.BarberiaLaClasica.model.Cita;
import com.example.BarberiaLaClasica.model.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CitaRepository extends JpaRepository<Cita, Long> {

    // Historial de citas de un cliente ordenado por fecha descendente
    List<Cita> findByClienteOrderByFechaDescHoraInicioDesc(Cliente cliente);

    // Verificar si un horario ya está ocupado para un barbero
    @Query("""
                SELECT COUNT(c) > 0 FROM Cita c
                WHERE c.barbero.id = :barberoId
                AND c.fecha = :fecha
                AND c.estado NOT IN (0)
                AND (
                    (c.horaInicio <= :horaInicio AND c.horaFin > :horaInicio)
                    OR (c.horaInicio < :horaFin AND c.horaFin >= :horaFin)
                    OR (c.horaInicio >= :horaInicio AND c.horaFin <= :horaFin)
                )
            """)
    boolean existeConflictoHorario(
            @Param("barberoId") Long barberoId,
            @Param("fecha") LocalDate fecha,
            @Param("horaInicio") LocalTime horaInicio,
            @Param("horaFin") LocalTime horaFin);

    // Horas ya ocupadas para un barbero en una fecha
    @Query("""
                SELECT c.horaInicio, c.estado FROM Cita c
                WHERE c.barbero.id = :barberoId
                AND c.fecha = :fecha
                AND c.estado NOT IN (0)
            """)
    List<Object[]> findHorasConEstadoPorBarberoYFecha(
            @Param("barberoId") Long barberoId,
            @Param("fecha") LocalDate fecha);

    // Para el secretario: citas pendientes de confirmar
    List<Cita> findByEstadoOrderByFechaAscHoraInicioAsc(int estado);

    // Para el secretario: todas las citas de hoy
    List<Cita> findByFechaOrderByHoraInicioAsc(LocalDate fecha);
    Optional<Cita> findByBarberoIdAndFechaAndEstado(Long barberoId, LocalDate fecha, int estado);
}
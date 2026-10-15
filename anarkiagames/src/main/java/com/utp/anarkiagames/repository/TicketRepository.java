package com.utp.anarkiagames.repository;

import com.utp.anarkiagames.model.Inscripcion;
import com.utp.anarkiagames.model.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TicketRepository extends JpaRepository<Ticket, Long> {
    List<Ticket> findByInscripcionOrderByIdAsc(Inscripcion inscripcion);
    Optional<Ticket> findByCodigo(String codigo);

    /** Devuelve 0 si el ticket ya estaba usado: dos lecturas simultáneas del mismo QR no entran dos veces. */
    @Transactional
    @Modifying(clearAutomatically = true)
    @Query("update Ticket t set t.fechaUso = :ahora where t.id = :id and t.fechaUso is null")
    int marcarUsado(@Param("id") Long id, @Param("ahora") LocalDateTime ahora);
}

package com.utp.anarkiagames.repository;

import com.utp.anarkiagames.model.TipoParticipacion;
import com.utp.anarkiagames.model.TipoTicket;
import com.utp.anarkiagames.model.Torneo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TipoTicketRepository extends JpaRepository<TipoTicket, Long> {
    List<TipoTicket> findByTorneo(Torneo torneo);
    Optional<TipoTicket> findByTorneoAndTipo(Torneo torneo, TipoParticipacion tipo);
}
package com.utp.anarkiagames.repository;

import com.utp.anarkiagames.model.Inscripcion;
import com.utp.anarkiagames.model.TipoTicket;
import com.utp.anarkiagames.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface InscripcionRepository extends JpaRepository<Inscripcion, Long> {
    List<Inscripcion> findByUsuario(User usuario);
    List<Inscripcion> findByTipoTicket(TipoTicket tipoTicket);
    Optional<Inscripcion> findByUsuarioAndTipoTicket(User usuario, TipoTicket tipoTicket);
    long countByTipoTicket(TipoTicket tipoTicket);
}
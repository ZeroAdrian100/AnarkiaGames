package com.utp.anarkiagames.repository;

import com.utp.anarkiagames.model.Inscripcion;
import com.utp.anarkiagames.model.TipoParticipacion;
import com.utp.anarkiagames.model.TipoTicket;
import com.utp.anarkiagames.model.Torneo;
import com.utp.anarkiagames.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface InscripcionRepository extends JpaRepository<Inscripcion, Long> {
    List<Inscripcion> findByUsuario(User usuario);
    List<Inscripcion> findByTipoTicket(TipoTicket tipoTicket);
    boolean existsByUsuarioAndTipoTicket(User usuario, TipoTicket tipoTicket);

    @Query("select count(i) > 0 from Inscripcion i " +
            "where i.usuario = :usuario and i.tipoTicket.torneo = :torneo and i.tipoTicket.tipo <> :tipo")
    boolean existsConOtroTipoEnTorneo(@Param("usuario") User usuario,
                                      @Param("torneo") Torneo torneo,
                                      @Param("tipo") TipoParticipacion tipo);

    /** Tickets vendidos de un tipo: suma de cantidades, no cantidad de inscripciones. */
    @Query("select coalesce(sum(i.cantidad), 0L) from Inscripcion i where i.tipoTicket = :tipoTicket")
    long sumCantidadByTipoTicket(@Param("tipoTicket") TipoTicket tipoTicket);
}

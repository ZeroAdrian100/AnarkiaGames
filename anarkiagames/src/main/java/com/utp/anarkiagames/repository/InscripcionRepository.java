package com.utp.anarkiagames.repository;

import com.utp.anarkiagames.model.EstadoInscripcion;
import com.utp.anarkiagames.model.Inscripcion;
import com.utp.anarkiagames.model.TipoParticipacion;
import com.utp.anarkiagames.model.TipoTicket;
import com.utp.anarkiagames.model.Torneo;
import com.utp.anarkiagames.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface InscripcionRepository extends JpaRepository<Inscripcion, Long> {
    List<Inscripcion> findByUsuario(User usuario);
    Page<Inscripcion> findByTipoTicketTorneo(Torneo torneo, Pageable pageable);
    List<Inscripcion> findByTipoTicketTorneoAndEstado(Torneo torneo, EstadoInscripcion estado);
    boolean existsByTipoTicket(TipoTicket tipoTicket);
    boolean existsByUsuarioAndTipoTicketAndEstado(User usuario, TipoTicket tipoTicket, EstadoInscripcion estado);

    /** El usuario ya tiene una compra pagada de otro tipo de participación en el torneo. */
    @Query("select count(i) > 0 from Inscripcion i " +
            "where i.usuario = :usuario and i.tipoTicket.torneo = :torneo and i.tipoTicket.tipo <> :tipo " +
            "and i.estado = com.utp.anarkiagames.model.EstadoInscripcion.PAGADA")
    boolean existsConOtroTipoEnTorneo(@Param("usuario") User usuario,
                                      @Param("torneo") Torneo torneo,
                                      @Param("tipo") TipoParticipacion tipo);

    /** Tickets vendidos de un tipo: suma de cantidades de las compras pagadas. */
    @Query("select coalesce(sum(i.cantidad), 0L) from Inscripcion i " +
            "where i.tipoTicket = :tipoTicket and i.estado = com.utp.anarkiagames.model.EstadoInscripcion.PAGADA")
    long sumCantidadByTipoTicket(@Param("tipoTicket") TipoTicket tipoTicket);

    /** Ingresos de un tipo de ticket en céntimos: suma de montos de las compras pagadas. */
    @Query("select coalesce(sum(i.monto), 0L) from Inscripcion i " +
            "where i.tipoTicket = :tipoTicket and i.estado = com.utp.anarkiagames.model.EstadoInscripcion.PAGADA")
    long sumMontoByTipoTicket(@Param("tipoTicket") TipoTicket tipoTicket);

    /** Cambia el estado solo si sigue siendo {@code actual}. Devuelve 0 si otro proceso lo cambió antes. */
    @Transactional
    @Modifying(clearAutomatically = true)
    @Query("update Inscripcion i set i.estado = :nuevo where i.id = :id and i.estado = :actual")
    int cambiarEstado(@Param("id") Long id,
                      @Param("actual") EstadoInscripcion actual,
                      @Param("nuevo") EstadoInscripcion nuevo);

    @Transactional
    @Modifying(clearAutomatically = true)
    @Query("update Inscripcion i set i.referenciaReembolso = :referencia where i.id = :id")
    void guardarReferenciaReembolso(@Param("id") Long id, @Param("referencia") String referencia);
}

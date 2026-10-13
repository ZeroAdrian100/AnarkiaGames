package com.utp.anarkiagames.repository;

import com.utp.anarkiagames.model.Torneo;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface TorneoRepository extends JpaRepository<Torneo, Long> {

    /** Bloquea la fila del torneo hasta el fin de la transacción para serializar las compras. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from Torneo t where t.id = :id")
    Optional<Torneo> findByIdForUpdate(@Param("id") Long id);
}

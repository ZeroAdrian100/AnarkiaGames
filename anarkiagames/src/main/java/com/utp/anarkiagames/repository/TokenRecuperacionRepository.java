package com.utp.anarkiagames.repository;

import com.utp.anarkiagames.model.TokenRecuperacion;
import com.utp.anarkiagames.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

public interface TokenRecuperacionRepository extends JpaRepository<TokenRecuperacion, Long> {
    Optional<TokenRecuperacion> findByTokenHash(String tokenHash);

    @Transactional
    @Modifying
    @Query("delete from TokenRecuperacion t where t.usuario = :usuario and t.usadoEn is null")
    void eliminarPendientes(@Param("usuario") User usuario);

    /** Devuelve 0 si el token ya se usó: dos pedidos simultáneos con el mismo enlace no cambian la contraseña dos veces. */
    @Transactional
    @Modifying(clearAutomatically = true)
    @Query("update TokenRecuperacion t set t.usadoEn = :ahora where t.id = :id and t.usadoEn is null")
    int marcarUsado(@Param("id") Long id, @Param("ahora") LocalDateTime ahora);
}

package com.utp.anarkiagames.repository;

import com.utp.anarkiagames.model.Torneo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TorneoRepository extends JpaRepository<Torneo, Long> {
}

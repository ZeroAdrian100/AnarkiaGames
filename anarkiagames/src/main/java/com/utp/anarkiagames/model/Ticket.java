package com.utp.anarkiagames.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/** Entrada individual de una compra: un espectador que compra 3 tickets recibe 3 códigos. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "tickets")
public class Ticket {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "inscripcion_id", nullable = false)
    private Inscripcion inscripcion;

    /** Valor del QR. Aleatorio y difícil de adivinar: quien lo tiene puede entrar al evento. */
    @Column(nullable = false, unique = true, length = 64)
    private String codigo;

    @Column(name = "fecha_uso")
    private LocalDateTime fechaUso;

    /** Sirve para entrar: no se usó, la compra no fue reembolsada y el torneo no fue cancelado. */
    public boolean esValido() {
        return fechaUso == null
                && inscripcion.getEstado() == EstadoInscripcion.PAGADA
                && inscripcion.getTipoTicket().getTorneo().getEstado() == EstadoTorneo.ACTIVO;
    }
}

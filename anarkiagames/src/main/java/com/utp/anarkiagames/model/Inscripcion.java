package com.utp.anarkiagames.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "inscripciones")
public class Inscripcion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "usuario_id", nullable = false)
    private User usuario;

    @ManyToOne
    @JoinColumn(name = "tipo_ticket_id", nullable = false)
    private TipoTicket tipoTicket;

    /** Tickets comprados en esta inscripción: siempre 1 para COMPETIDOR. */
    @ColumnDefault("1")
    @Column(nullable = false)
    private int cantidad;

    /** Monto total cobrado, en céntimos. */
    @ColumnDefault("0")
    @Column(nullable = false)
    private long monto;

    @Column(name = "fecha_compra", nullable = false)
    private LocalDateTime fechaCompra;

    /** Referencia del cargo en la pasarela de pago (Culqi o mock). */
    @Column(name = "culqi_charge_id")
    private String referenciaPago;
}

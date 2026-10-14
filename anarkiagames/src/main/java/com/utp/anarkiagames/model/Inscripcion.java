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

    /** Solo las inscripciones PAGADA cuentan para el stock y las reglas de compra. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private EstadoInscripcion estado = EstadoInscripcion.PAGADA;

    /** Referencia del reembolso en la pasarela, si la compra fue reembolsada. */
    @Column(name = "referencia_reembolso")
    private String referenciaReembolso;
}

package com.utp.anarkiagames.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Data
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "inscripciones",
        uniqueConstraints = @UniqueConstraint(columnNames = {"usuario_id", "tipo_ticket_id"}))
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

    @Column(name = "fecha_compra", nullable = false)
    private LocalDateTime fechaCompra;
}
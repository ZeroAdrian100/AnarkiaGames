package com.utp.anarkiagames.model;

import jakarta.persistence.*;
import lombok.*;

@Data
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "tipo_tickets", uniqueConstraints = @UniqueConstraint(columnNames = {"torneo_id", "tipo"}))
public class TipoTicket {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "torneo_id", nullable = false)
    private Torneo torneo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoParticipacion tipo;

    @Column(name = "stock_maximo", nullable = false)
    private int stockMaximo;

    @Column(nullable = false)
    private long precio;

    private String descripcion;

    @Column(name = "imagen_url")
    private String imagenUrl;
}

-- Tickets individuales con código para el check-in.

CREATE TABLE tickets (
    id             BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    inscripcion_id BIGINT       NOT NULL CONSTRAINT fk_tickets_inscripcion REFERENCES inscripciones (id),
    codigo         VARCHAR(64)  NOT NULL CONSTRAINT uk_tickets_codigo UNIQUE,
    fecha_uso      DATETIME2(6)
);

CREATE INDEX ix_tickets_inscripcion ON tickets (inscripcion_id);
GO

-- Las compras pagadas anteriores reciben un ticket por cada unidad comprada.
-- NEWID() se evalúa por fila y genera 122 bits aleatorios.
INSERT INTO tickets (inscripcion_id, codigo)
SELECT i.id, LOWER(REPLACE(CONVERT(VARCHAR(36), NEWID()), '-', ''))
FROM inscripciones i
JOIN (SELECT TOP 100 ROW_NUMBER() OVER (ORDER BY (SELECT NULL)) AS n FROM sys.all_objects) numeros
    ON numeros.n <= i.cantidad
WHERE i.estado = 'PAGADA';

-- Gamer tag (obligatorio para competidores) y equipo (opcional) en cada inscripción.
-- Las inscripciones anteriores quedan sin estos datos.

ALTER TABLE inscripciones ADD gamer_tag VARCHAR(50) NULL;
ALTER TABLE inscripciones ADD equipo VARCHAR(100) NULL;

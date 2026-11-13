-- Datos del torneo que muestra el frontend: descripción, imagen, lugar y duración.
-- Todos son opcionales; los torneos existentes quedan sin ellos.

ALTER TABLE torneos ADD descripcion VARCHAR(1000) NULL;
ALTER TABLE torneos ADD imagen_url VARCHAR(500) NULL;
ALTER TABLE torneos ADD lugar VARCHAR(255) NULL;
ALTER TABLE torneos ADD duracion_horas INT NULL;

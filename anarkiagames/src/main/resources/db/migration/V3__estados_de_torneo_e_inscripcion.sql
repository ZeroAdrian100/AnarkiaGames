-- Estados para cancelar torneos y reembolsar compras.
-- Las filas existentes quedan como ACTIVO y PAGADA.

ALTER TABLE torneos ADD estado VARCHAR(255) NOT NULL
    CONSTRAINT df_torneos_estado DEFAULT 'ACTIVO'
    CONSTRAINT ck_torneos_estado CHECK (estado IN ('ACTIVO', 'CANCELADO'));

ALTER TABLE inscripciones ADD estado VARCHAR(255) NOT NULL
    CONSTRAINT df_inscripciones_estado DEFAULT 'PAGADA'
    CONSTRAINT ck_inscripciones_estado CHECK (estado IN ('PAGADA', 'REEMBOLSADA'));

ALTER TABLE inscripciones ADD referencia_reembolso VARCHAR(255);

-- Esquema tal como lo generaba Hibernate antes de usar Flyway.
-- En una BD que ya existía, Flyway no ejecuta este script (baseline en la versión 1).

CREATE TABLE users (
    id            BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    nombre        VARCHAR(255) NOT NULL,
    email         VARCHAR(255) NOT NULL CONSTRAINT uk_users_email UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role          VARCHAR(255) NOT NULL CONSTRAINT ck_users_role CHECK (role IN ('ADMIN', 'CLIENTE')),
    created_at    DATETIME2(6)
);

CREATE TABLE torneos (
    id           BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    nombre       VARCHAR(255) NOT NULL,
    juego        VARCHAR(255) NOT NULL,
    fecha_inicio DATETIME2(6) NOT NULL,
    created_at   DATETIME2(6)
);

CREATE TABLE tipo_tickets (
    id           BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    torneo_id    BIGINT       NOT NULL CONSTRAINT fk_tipo_tickets_torneo REFERENCES torneos (id),
    tipo         VARCHAR(255) NOT NULL CONSTRAINT ck_tipo_tickets_tipo CHECK (tipo IN ('COMPETIDOR', 'ESPECTADOR')),
    stock_maximo INT          NOT NULL,
    precio       BIGINT       NOT NULL,
    descripcion  VARCHAR(255),
    imagen_url   VARCHAR(255),
    CONSTRAINT uk_tipo_tickets_torneo_tipo UNIQUE (torneo_id, tipo)
);

CREATE TABLE inscripciones (
    id              BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    usuario_id      BIGINT       NOT NULL CONSTRAINT fk_inscripciones_usuario REFERENCES users (id),
    tipo_ticket_id  BIGINT       NOT NULL CONSTRAINT fk_inscripciones_tipo_ticket REFERENCES tipo_tickets (id),
    fecha_compra    DATETIME2(6) NOT NULL,
    culqi_charge_id VARCHAR(255),
    CONSTRAINT uk_inscripciones_usuario_tipo_ticket UNIQUE (usuario_id, tipo_ticket_id)
);

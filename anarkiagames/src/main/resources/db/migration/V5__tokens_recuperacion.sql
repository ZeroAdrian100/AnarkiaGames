-- Tokens para restablecer la contraseña. Solo se guarda el hash SHA-256 del token.

CREATE TABLE tokens_recuperacion (
    id         BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    usuario_id BIGINT       NOT NULL CONSTRAINT fk_tokens_recuperacion_usuario REFERENCES users (id),
    token_hash VARCHAR(64)  NOT NULL CONSTRAINT uk_tokens_recuperacion_hash UNIQUE,
    expira_en  DATETIME2(6) NOT NULL,
    usado_en   DATETIME2(6)
);

CREATE INDEX ix_tokens_recuperacion_usuario ON tokens_recuperacion (usuario_id);

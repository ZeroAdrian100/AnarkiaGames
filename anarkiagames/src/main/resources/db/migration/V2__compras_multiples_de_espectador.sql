-- Un espectador puede comprar varias veces y varios tickets por compra.
-- El script es idempotente: sirve tanto para una BD nueva como para una que Hibernate
-- ya modificó con ddl-auto=update (con cantidad y monto creados, pero sin borrar la restricción).

-- 1. Borra la restricción única (usuario_id, tipo_ticket_id). En las BD creadas por Hibernate
--    el nombre es aleatorio, así que se buscan todas las UNIQUE de la tabla (solo existía esa).
DECLARE @restriccion SYSNAME;
DECLARE @sql NVARCHAR(400);
SELECT TOP 1 @restriccion = name
FROM sys.key_constraints
WHERE parent_object_id = OBJECT_ID('inscripciones') AND type = 'UQ';

WHILE @restriccion IS NOT NULL
BEGIN
    SET @sql = N'ALTER TABLE inscripciones DROP CONSTRAINT ' + QUOTENAME(@restriccion);
    EXEC sp_executesql @sql;
    SET @restriccion = NULL;
    SELECT TOP 1 @restriccion = name
    FROM sys.key_constraints
    WHERE parent_object_id = OBJECT_ID('inscripciones') AND type = 'UQ';
END;

-- 2. Columnas nuevas.
IF COL_LENGTH('inscripciones', 'cantidad') IS NULL
    ALTER TABLE inscripciones ADD cantidad INT NOT NULL CONSTRAINT df_inscripciones_cantidad DEFAULT 1;

IF COL_LENGTH('inscripciones', 'monto') IS NULL
    ALTER TABLE inscripciones ADD monto BIGINT NOT NULL CONSTRAINT df_inscripciones_monto DEFAULT 0;
GO

-- 3. Las compras anteriores no guardaban el monto: se estima con el precio actual del ticket.
UPDATE i
SET i.monto = t.precio * i.cantidad
FROM inscripciones i
JOIN tipo_tickets t ON t.id = i.tipo_ticket_id
WHERE i.monto = 0;

-- DEF-09: reemplaza a docs/sprint6-columnas.sql, que antes había que correr a mano.
-- En una base nueva no hace nada (V1 ya trae las columnas). En una base creada antes
-- del Sprint 6 agrega las columnas que Hibernate no pudo crear en la MariaDB de XAMPP.
-- "ADD COLUMN IF NOT EXISTS" es sintaxis de MariaDB (no de MySQL 8).

ALTER TABLE naves
    ADD COLUMN IF NOT EXISTS revocado_por        VARCHAR(120) NULL,
    ADD COLUMN IF NOT EXISTS solicitud_origen_id BIGINT       NULL,
    ADD COLUMN IF NOT EXISTS tramite_origen      VARCHAR(30)  NULL;

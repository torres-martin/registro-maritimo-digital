-- DEF-09: la MariaDB de XAMPP no ejecuta "ALTER TABLE IF EXISTS",
-- así que Hibernate no crea solas estas columnas.
-- Ejecutar una sola vez en phpMyAdmin (base amp_registro_db) antes de arrancar.
ALTER TABLE naves
  ADD COLUMN revocado_por VARCHAR(120) NULL,
  ADD COLUMN solicitud_origen_id BIGINT NULL,
  ADD COLUMN tramite_origen VARCHAR(30) NULL;

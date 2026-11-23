-- DEF-09 (Sprint 7): esquema completo del sistema al cierre del Sprint 6.
-- Lo ejecuta Flyway automáticamente al arrancar la aplicación; no correr a mano.
-- "IF NOT EXISTS" permite aplicarlo también sobre bases que Hibernate ya había creado.
-- Escrito para MariaDB 10.4 (la de XAMPP).

CREATE TABLE IF NOT EXISTS usuarios (
    id                BIGINT       NOT NULL AUTO_INCREMENT,
    correo            VARCHAR(120) NOT NULL,
    nombre            VARCHAR(120) NOT NULL,
    password_hash     VARCHAR(255) NOT NULL,
    rol               ENUM('ARMADOR','ASEGURADORA','FUNCIONARIO') NOT NULL,
    intentos_fallidos INT          NOT NULL DEFAULT 0,
    bloqueado_hasta   DATETIME(6)  NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_usuarios_correo UNIQUE (correo)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS naves (
    id                     BIGINT        NOT NULL AUTO_INCREMENT,
    tramite                VARCHAR(30)   NOT NULL,
    documento_base         VARCHAR(255)  NULL,
    tipo_transmitente      VARCHAR(255)  NULL,
    nombre_transmitente    VARCHAR(255)  NULL,
    domicilio_transmitente VARCHAR(255)  NULL,
    tipo_adquirente        VARCHAR(255)  NULL,
    propietario            VARCHAR(255)  NOT NULL,
    domicilio_propietario  VARCHAR(255)  NULL,
    nombre_nave            VARCHAR(255)  NOT NULL,
    imo                    VARCHAR(20)   NULL,
    distintivo_llamada     VARCHAR(255)  NULL,
    patente_navegacion     VARCHAR(255)  NULL,
    arqueo_bruto           DOUBLE        NULL,
    arqueo_neto            DOUBLE        NULL,
    eslora                 DOUBLE        NULL,
    manga                  DOUBLE        NULL,
    puntal                 DOUBLE        NULL,
    precio_venta           DOUBLE        NULL,
    anio_construccion      INT           NULL,
    tipo_nave              VARCHAR(255)  NULL,
    fecha_celebracion      VARCHAR(255)  NULL,
    tipo_legalizacion      VARCHAR(255)  NULL,
    solicitante            VARCHAR(255)  NULL,
    abogado                VARCHAR(255)  NULL,
    adicionales            TEXT          NULL,
    estado                 VARCHAR(255)  NULL,
    observacion            VARCHAR(1000) NULL,
    liquidacion            VARCHAR(40)   NULL,
    armador_correo         VARCHAR(120)  NULL,
    ruta_documento_pdf     VARCHAR(255)  NULL,
    fecha_creacion         DATE          NULL,
    certificado_folio      VARCHAR(30)   NULL,
    codigo_verificacion    VARCHAR(20)   NULL,
    fecha_emision          DATE          NULL,
    fecha_vencimiento      DATE          NULL,
    fecha_decision         DATE          NULL,
    revocado               BIT(1)        NULL,
    motivo_revocacion      VARCHAR(500)  NULL,
    fecha_revocacion       DATE          NULL,
    revocado_por           VARCHAR(120)  NULL,
    solicitud_origen_id    BIGINT        NULL,
    tramite_origen         VARCHAR(30)   NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_naves_tramite             UNIQUE (tramite),
    CONSTRAINT uk_naves_certificado_folio   UNIQUE (certificado_folio),
    CONSTRAINT uk_naves_codigo_verificacion UNIQUE (codigo_verificacion)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS borradores_solicitud (
    id                  VARCHAR(36)  NOT NULL,
    version             BIGINT       NULL,
    armador_correo      VARCHAR(120) NOT NULL,
    datos_json          TEXT         NOT NULL,
    nombre_nave         VARCHAR(100) NOT NULL,
    paso                INT          NOT NULL,
    origen_solicitud_id BIGINT       NULL,
    actualizado         DATETIME(6)  NOT NULL,
    nombre_documento    VARCHAR(255) NULL,
    tamano_documento    BIGINT       NULL,
    pdf                 LONGBLOB     NULL,
    PRIMARY KEY (id),
    INDEX idx_borrador_armador_actualizado (armador_correo, actualizado)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS bitacora_auditoria (
    id          BIGINT        NOT NULL AUTO_INCREMENT,
    fecha_hora  DATETIME(6)   NOT NULL,
    usuario     VARCHAR(120)  NOT NULL,
    accion      VARCHAR(40)   NOT NULL,
    nave_id     BIGINT        NULL,
    tramite     VARCHAR(30)   NULL,
    nombre_nave VARCHAR(100)  NULL,
    detalle     VARCHAR(1000) NULL,
    PRIMARY KEY (id),
    INDEX idx_bitacora_nave  (nave_id),
    INDEX idx_bitacora_fecha (fecha_hora)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

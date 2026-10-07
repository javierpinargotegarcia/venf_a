-- =====================================================================
-- Esquema de la base de datos "inventario" (MySQL 8.x)
-- Se ejecuta automaticamente al migrar desde los archivos JSON
-- o al arrancar la aplicacion en modo MySQL.
-- Los IDs los genera la aplicacion (sin AUTO_INCREMENT) para que
-- la migracion conserve exactamente los mismos identificadores.
-- =====================================================================

CREATE TABLE IF NOT EXISTS categoria (
    id          BIGINT       NOT NULL PRIMARY KEY,
    nombre      VARCHAR(100) NOT NULL,
    descripcion VARCHAR(255) NULL
);

CREATE TABLE IF NOT EXISTS proveedor (
    id          BIGINT       NOT NULL PRIMARY KEY,
    nombre      VARCHAR(150) NOT NULL,
    contacto    VARCHAR(100) NULL,
    telefono    VARCHAR(30)  NULL,
    email       VARCHAR(100) NULL,
    direccion   VARCHAR(200) NULL
);

CREATE TABLE IF NOT EXISTS usuario (
    id              BIGINT       NOT NULL PRIMARY KEY,
    usuario         VARCHAR(50)  NOT NULL UNIQUE,
    password_hash   VARCHAR(120) NOT NULL,
    nombre_completo VARCHAR(150) NOT NULL,
    rol             VARCHAR(20)  NOT NULL,
    activo          BOOLEAN      NOT NULL DEFAULT TRUE,
    fecha_registro  DATETIME     NULL
);

CREATE TABLE IF NOT EXISTS producto (
    id             BIGINT        NOT NULL PRIMARY KEY,
    codigo_barras  VARCHAR(50)   NOT NULL UNIQUE,
    nombre         VARCHAR(150)  NOT NULL,
    descripcion    VARCHAR(255)  NULL,
    categoria_id   BIGINT        NULL,
    proveedor_id   BIGINT        NULL,
    precio_compra  DECIMAL(12,2) NOT NULL DEFAULT 0,
    precio_venta   DECIMAL(12,2) NOT NULL DEFAULT 0,
    stock          INT           NOT NULL DEFAULT 0,
    stock_minimo   INT           NOT NULL DEFAULT 0,
    unidad         VARCHAR(20)   NOT NULL DEFAULT 'UND',
    activo         BOOLEAN       NOT NULL DEFAULT TRUE,
    fecha_registro DATETIME      NULL,
    CONSTRAINT fk_producto_categoria FOREIGN KEY (categoria_id) REFERENCES categoria (id),
    CONSTRAINT fk_producto_proveedor FOREIGN KEY (proveedor_id) REFERENCES proveedor (id)
);

CREATE TABLE IF NOT EXISTS movimiento (
    id               BIGINT      NOT NULL PRIMARY KEY,
    producto_id      BIGINT      NOT NULL,
    producto_nombre  VARCHAR(150) NOT NULL,
    tipo             VARCHAR(20) NOT NULL,
    cantidad         INT         NOT NULL,
    stock_anterior   INT         NOT NULL,
    stock_resultante INT         NOT NULL,
    motivo           VARCHAR(255) NULL,
    usuario_id       BIGINT      NULL,
    usuario_nombre   VARCHAR(150) NULL,
    fecha            DATETIME    NOT NULL,
    CONSTRAINT fk_movimiento_producto FOREIGN KEY (producto_id) REFERENCES producto (id)
);

-- Nota: los indices se crean junto con las tablas (MySQL no admite
-- "CREATE INDEX IF NOT EXISTS").

-- ============================================================================
-- Migración COMPLETA de ALTER TABLE para PRODUCCIÓN (caja_ahorros)
-- Sincroniza el esquema de la BD de producción con el nuevo modelado:
--   * Préstamos a socios y NO socios (socio_id nullable + cliente no socio)
--   * Tipos de ahorro (NORMAL / DECIMO13 / DECIMO14)
--   * Pagos adelantados y abonos a capital
--   * Corrección de tipo de dato de sesiones.token
--
-- 100% idempotente: usa IF NOT EXISTS / ADD COLUMN IF NOT EXISTS,
-- por lo que puede ejecutarse varias veces sin error.
--
-- Recomendado: ejecutar dentro de una transacción con rollback manual en caso
-- de error, o respaldar la BD antes de aplicar.
-- ============================================================================

BEGIN;

-- 1. CRÉDITOS A NO SOCIOS ------------------------------------------------
--    Permite socio_id NULL y agrega datos del cliente no socio.

-- 1a. Tabla solicitud_credito
ALTER TABLE solicitud_credito ALTER COLUMN socio_id DROP NOT NULL;
ALTER TABLE solicitud_credito ADD COLUMN IF NOT EXISTS cliente_no_socio_nombre VARCHAR(150);
ALTER TABLE solicitud_credito ADD COLUMN IF NOT EXISTS cliente_no_socio_identificacion VARCHAR(20);
ALTER TABLE solicitud_credito ADD COLUMN IF NOT EXISTS cliente_no_socio_telefono VARCHAR(30);

-- 1b. Tabla credito
ALTER TABLE credito ALTER COLUMN socio_id DROP NOT NULL;
ALTER TABLE credito ADD COLUMN IF NOT EXISTS cliente_no_socio_nombre VARCHAR(150);
ALTER TABLE credito ADD COLUMN IF NOT EXISTS cliente_no_socio_identificacion VARCHAR(20);
ALTER TABLE credito ADD COLUMN IF NOT EXISTS cliente_no_socio_telefono VARCHAR(30);

-- 1c. Producto de crédito: indica si el producto admite no socios
ALTER TABLE producto_credito ADD COLUMN IF NOT EXISTS permite_no_socio BOOLEAN NOT NULL DEFAULT false;

-- 2. TIPOS DE AHORRO ------------------------------------------------------
--    solo NORMAL al momento de migrar (los créditos de décimo se crean nuevos)
ALTER TABLE cuenta_ahorro ADD COLUMN IF NOT EXISTS tipo_ahorro VARCHAR(20) NOT NULL DEFAULT 'NORMAL';

-- 3. PAGOS: CUOTA / ADELANTADO / ABONO -----------------------------------
ALTER TABLE pago_cuota ALTER COLUMN cuota_id DROP NOT NULL;
ALTER TABLE pago_cuota ADD COLUMN IF NOT EXISTS tipo VARCHAR(20) NOT NULL DEFAULT 'CUOTA';
ALTER TABLE pago_cuota ADD COLUMN IF NOT EXISTS monto_abono_capital NUMERIC(14,2) NOT NULL DEFAULT 0;
ALTER TABLE pago_cuota ADD COLUMN IF NOT EXISTS descripcion VARCHAR(255);

-- 3b. Total abonado a capital por crédito
ALTER TABLE credito ADD COLUMN IF NOT EXISTS abono_capital_total NUMERIC(14,2) NOT NULL DEFAULT 0;

-- 4. CORRECCIÓN sesiones.token -------------------------------------------
--    En producción la columna pudo crearse como VARCHAR con longitud corta.
--    Se normaliza a TEXT para que el token JWT (token + sesión) no se trunque.
ALTER TABLE sesiones ALTER COLUMN token TYPE TEXT USING token::TEXT;

-- 5. ÍNDICES de apoyo (opcionales pero recomendados) ----------------------
CREATE INDEX IF NOT EXISTS idx_producto_credito_permite_no_socio ON producto_credito(permite_no_socio);
CREATE INDEX IF NOT EXISTS idx_credito_cliente_no_socio ON credito(cliente_no_socio_identificacion);
CREATE INDEX IF NOT EXISTS idx_solicitud_cliente_no_socio ON solicitud_credito(cliente_no_socio_identificacion);

COMMIT;

-- ============================================================================
-- NOTAS:
--  * Si ya ejecutó la migración anterior (migration_feature_prestamos_ahorros.sql),
--    este script es seguro de volver a correr (todo es IF NOT EXISTS).
--  * La columna tipo_ahorro queda en 'NORMAL' para las cuentas existentes.
--  * Los créditos/solicitudes existentes quedan con socio_id intacto; solo se
--    habilita el NULL para los futuros créditos a no socios.
-- ============================================================================

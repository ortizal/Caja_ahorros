-- ============================================================================
-- Migración: Campo Sexo en Socios + Índices para Balance Social
-- 100% idempotente: puede ejecutarse varias veces sin error.
-- ============================================================================

BEGIN;

-- 1. Campo sexo en socios (M=Masculino, F=Femenino) -------------------------
ALTER TABLE socios ADD COLUMN IF NOT EXISTS sexo VARCHAR(1);

-- 2. Índices para balance social y comprobación --------------------------------
CREATE INDEX IF NOT EXISTS idx_socios_sexo ON socios(sexo);
CREATE INDEX IF NOT EXISTS idx_socios_estado ON socios(estado);
CREATE INDEX IF NOT EXISTS idx_cuenta_ahorro_socio ON cuenta_ahorro(socio_id);
CREATE INDEX IF NOT EXISTS idx_cuenta_ahorro_tipo ON cuenta_ahorro(tipo_ahorro);
CREATE INDEX IF NOT EXISTS idx_aportaciones_socio ON aportaciones(socio_id);
CREATE INDEX IF NOT EXISTS idx_aportaciones_estado ON aportaciones(estado);
CREATE INDEX IF NOT EXISTS idx_credito_socio ON credito(socio_id);
CREATE INDEX IF NOT EXISTS idx_credito_estado ON credito(estado);

COMMIT;
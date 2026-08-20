-- ============================================================================
-- ALANTEK - Sistema Integral de Caja de Ahorros
-- Esquema PostgreSQL - Fases 1 y 2 (Fundación + Motor contable)
-- Fuente de verdad: PLAN_DESARROLLO_CAJA_AHORROS.md secciones 2.1, 2.2, 2.9-2.12
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 2.1 Administración y Seguridad
-- ----------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS usuarios (
  id BIGSERIAL PRIMARY KEY,
  username VARCHAR(50) UNIQUE NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  nombre_completo VARCHAR(150) NOT NULL,
  email VARCHAR(150),
  estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
  intentos_fallidos INT NOT NULL DEFAULT 0,
  ultimo_acceso TIMESTAMPTZ,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  created_by BIGINT,
  updated_at TIMESTAMPTZ,
  updated_by BIGINT
);

CREATE TABLE IF NOT EXISTS roles (
  id BIGSERIAL PRIMARY KEY,
  nombre VARCHAR(50) UNIQUE NOT NULL,
  descripcion VARCHAR(200)
);

CREATE TABLE IF NOT EXISTS usuario_roles (
  usuario_id BIGINT NOT NULL REFERENCES usuarios(id),
  rol_id BIGINT NOT NULL REFERENCES roles(id),
  PRIMARY KEY (usuario_id, rol_id)
);

CREATE TABLE IF NOT EXISTS permisos (
  id BIGSERIAL PRIMARY KEY,
  modulo VARCHAR(50) NOT NULL,
  accion VARCHAR(30) NOT NULL,
  UNIQUE (modulo, accion)
);

CREATE TABLE IF NOT EXISTS rol_permisos (
  rol_id BIGINT NOT NULL REFERENCES roles(id),
  permiso_id BIGINT NOT NULL REFERENCES permisos(id),
  PRIMARY KEY (rol_id, permiso_id)
);

CREATE TABLE IF NOT EXISTS sesiones (
  id BIGSERIAL PRIMARY KEY,
  usuario_id BIGINT REFERENCES usuarios(id),
  token TEXT,
  ip VARCHAR(45),
  user_agent VARCHAR(255),
  iniciada_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  cerrada_at TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS auditoria (
  id BIGSERIAL PRIMARY KEY,
  usuario_id BIGINT REFERENCES usuarios(id),
  tabla_afectada VARCHAR(60) NOT NULL,
  registro_id BIGINT NOT NULL,
  accion VARCHAR(20) NOT NULL,
  valor_anterior JSONB,
  valor_nuevo JSONB,
  ip VARCHAR(45),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS parametros_generales (
  clave VARCHAR(80) PRIMARY KEY,
  valor VARCHAR(255) NOT NULL,
  descripcion VARCHAR(255),
  updated_at TIMESTAMPTZ,
  updated_by BIGINT
);

-- ----------------------------------------------------------------------------
-- 2.2 Socios
-- ----------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS socios (
  id BIGSERIAL PRIMARY KEY,
  codigo VARCHAR(20) UNIQUE NOT NULL,
  usuario_id BIGINT REFERENCES usuarios(id),
  identificacion VARCHAR(20) UNIQUE NOT NULL,
  nombres VARCHAR(100) NOT NULL,
  apellidos VARCHAR(100) NOT NULL,
  telefono VARCHAR(30),
  email VARCHAR(150),
  direccion VARCHAR(255),
  fecha_ingreso DATE NOT NULL,
  fecha_retiro DATE,
  estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  created_by BIGINT,
  updated_at TIMESTAMPTZ,
  updated_by BIGINT
);

CREATE TABLE IF NOT EXISTS socio_beneficiarios (
  id BIGSERIAL PRIMARY KEY,
  socio_id BIGINT NOT NULL REFERENCES socios(id),
  nombres VARCHAR(150) NOT NULL,
  parentesco VARCHAR(50),
  porcentaje NUMERIC(5,2) NOT NULL DEFAULT 100
);

CREATE TABLE IF NOT EXISTS socio_documentos (
  id BIGSERIAL PRIMARY KEY,
  socio_id BIGINT NOT NULL REFERENCES socios(id),
  tipo_documento VARCHAR(50),
  url_archivo VARCHAR(500),
  uploaded_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ----------------------------------------------------------------------------
-- 2.9 Caja
-- ----------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS caja_apertura (
  id BIGSERIAL PRIMARY KEY,
  cajero_id BIGINT NOT NULL REFERENCES usuarios(id),
  fecha DATE NOT NULL,
  saldo_inicial NUMERIC(14,2) NOT NULL,
  estado VARCHAR(20) NOT NULL DEFAULT 'ABIERTA',
  opened_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  closed_at TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS comprobantes (
  id BIGSERIAL PRIMARY KEY,
  numero VARCHAR(20) UNIQUE NOT NULL,
  tipo VARCHAR(20) NOT NULL,
  descripcion VARCHAR(255),
  estado VARCHAR(20) NOT NULL DEFAULT 'EJECUTADO',
  motivo_anulacion VARCHAR(255),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  created_by BIGINT
);

CREATE TABLE IF NOT EXISTS caja_movimiento (
  id BIGSERIAL PRIMARY KEY,
  caja_apertura_id BIGINT NOT NULL REFERENCES caja_apertura(id),
  comprobante_id BIGINT REFERENCES comprobantes(id),
  tipo VARCHAR(20) NOT NULL,
  monto NUMERIC(14,2) NOT NULL,
  referencia_tabla VARCHAR(60),
  referencia_id BIGINT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  created_by BIGINT
);

CREATE TABLE IF NOT EXISTS caja_arqueo (
  id BIGSERIAL PRIMARY KEY,
  caja_apertura_id BIGINT NOT NULL REFERENCES caja_apertura(id),
  saldo_sistema NUMERIC(14,2) NOT NULL,
  saldo_fisico NUMERIC(14,2) NOT NULL,
  diferencia NUMERIC(14,2) NOT NULL,
  observacion VARCHAR(255),
  realizado_por BIGINT,
  realizado_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ----------------------------------------------------------------------------
-- 2.10 Bancos
-- ----------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS cuenta_bancaria (
  id BIGSERIAL PRIMARY KEY,
  banco VARCHAR(80) NOT NULL,
  numero_cuenta VARCHAR(30) NOT NULL,
  tipo VARCHAR(20),
  saldo_contable NUMERIC(14,2) NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS banco_movimiento (
  id BIGSERIAL PRIMARY KEY,
  cuenta_bancaria_id BIGINT NOT NULL REFERENCES cuenta_bancaria(id),
  tipo VARCHAR(20) NOT NULL,
  monto NUMERIC(14,2) NOT NULL,
  comprobante_id BIGINT REFERENCES comprobantes(id),
  conciliado BOOLEAN NOT NULL DEFAULT false,
  fecha DATE NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS conciliacion_bancaria (
  id BIGSERIAL PRIMARY KEY,
  cuenta_bancaria_id BIGINT NOT NULL REFERENCES cuenta_bancaria(id),
  periodo VARCHAR(7) NOT NULL,
  saldo_contable NUMERIC(14,2) NOT NULL,
  saldo_bancario NUMERIC(14,2) NOT NULL,
  diferencia NUMERIC(14,2) NOT NULL,
  realizado_por BIGINT,
  realizado_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ----------------------------------------------------------------------------
-- 2.11 Contabilidad
-- ----------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS plan_cuentas (
  id BIGSERIAL PRIMARY KEY,
  codigo VARCHAR(20) UNIQUE NOT NULL,
  nombre VARCHAR(150) NOT NULL,
  tipo VARCHAR(20) NOT NULL,
  cuenta_padre_id BIGINT REFERENCES plan_cuentas(id),
  nivel INT NOT NULL,
  acepta_movimiento BOOLEAN NOT NULL DEFAULT true
);

CREATE TABLE IF NOT EXISTS periodo_contable (
  id BIGSERIAL PRIMARY KEY,
  anio INT NOT NULL,
  mes INT NOT NULL,
  estado VARCHAR(20) NOT NULL DEFAULT 'ABIERTO',
  cerrado_por BIGINT,
  cerrado_at TIMESTAMPTZ,
  UNIQUE (anio, mes)
);

CREATE TABLE IF NOT EXISTS asiento_contable (
  id BIGSERIAL PRIMARY KEY,
  periodo_id BIGINT NOT NULL REFERENCES periodo_contable(id),
  comprobante_id BIGINT REFERENCES comprobantes(id),
  fecha DATE NOT NULL,
  descripcion VARCHAR(255),
  origen VARCHAR(40) NOT NULL DEFAULT 'AUTOMATICO',
  estado VARCHAR(20) NOT NULL DEFAULT 'EJECUTADO',
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  created_by BIGINT
);

CREATE TABLE IF NOT EXISTS asiento_detalle (
  id BIGSERIAL PRIMARY KEY,
  asiento_id BIGINT NOT NULL REFERENCES asiento_contable(id),
  cuenta_id BIGINT NOT NULL REFERENCES plan_cuentas(id),
  debe NUMERIC(14,2) NOT NULL DEFAULT 0,
  haber NUMERIC(14,2) NOT NULL DEFAULT 0
);

-- ----------------------------------------------------------------------------
-- 2.12 Motor de reglas contables (Debe/Haber automático)
-- ----------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS regla_contable (
  id BIGSERIAL PRIMARY KEY,
  operacion VARCHAR(60) UNIQUE NOT NULL,
  cuenta_debe_id BIGINT NOT NULL REFERENCES plan_cuentas(id),
  cuenta_haber_id BIGINT NOT NULL REFERENCES plan_cuentas(id),
  vigente_desde DATE NOT NULL,
  vigente_hasta DATE,
  activo BOOLEAN NOT NULL DEFAULT true
);

-- ----------------------------------------------------------------------------
-- 2.3 Aportaciones
-- ----------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS aportacion_config (
  id BIGSERIAL PRIMARY KEY,
  tipo VARCHAR(20) NOT NULL,
  modo_calculo VARCHAR(10) NOT NULL,
  valor NUMERIC(14,4) NOT NULL,
  periodicidad VARCHAR(20) NOT NULL,
  monto_minimo NUMERIC(14,2),
  monto_maximo NUMERIC(14,2),
  vigente_desde DATE NOT NULL,
  vigente_hasta DATE,
  created_by BIGINT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS aportaciones (
  id BIGSERIAL PRIMARY KEY,
  socio_id BIGINT NOT NULL REFERENCES socios(id),
  config_id BIGINT NOT NULL REFERENCES aportacion_config(id),
  periodo VARCHAR(7) NOT NULL,
  monto_esperado NUMERIC(14,2) NOT NULL,
  monto_pagado NUMERIC(14,2) NOT NULL DEFAULT 0,
  mora NUMERIC(14,2) NOT NULL DEFAULT 0,
  estado VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
  exonerado_por BIGINT,
  motivo_exoneracion VARCHAR(255),
  UNIQUE (socio_id, periodo)
);

CREATE TABLE IF NOT EXISTS aportacion_pagos (
  id BIGSERIAL PRIMARY KEY,
  aportacion_id BIGINT NOT NULL REFERENCES aportaciones(id),
  monto NUMERIC(14,2) NOT NULL,
  caja_movimiento_id BIGINT,
  pagado_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  registrado_por BIGINT
);

-- ----------------------------------------------------------------------------
-- 2.4 Ahorros
-- ----------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS producto_ahorro (
  id BIGSERIAL PRIMARY KEY,
  nombre VARCHAR(80) NOT NULL,
  tasa_interes NUMERIC(7,4) NOT NULL,
  periodicidad_capitalizacion VARCHAR(20) NOT NULL,
  saldo_minimo NUMERIC(14,2) NOT NULL DEFAULT 0,
  limite_retiros_mes INT,
  vigente_desde DATE NOT NULL,
  vigente_hasta DATE,
  activo BOOLEAN NOT NULL DEFAULT true
);

CREATE TABLE IF NOT EXISTS cuenta_ahorro (
  id BIGSERIAL PRIMARY KEY,
  socio_id BIGINT NOT NULL REFERENCES socios(id),
  producto_id BIGINT NOT NULL REFERENCES producto_ahorro(id),
  numero_cuenta VARCHAR(20) UNIQUE NOT NULL,
  saldo NUMERIC(14,2) NOT NULL DEFAULT 0,
  estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVA',
  fecha_apertura DATE NOT NULL,
  fecha_cierre DATE
);

CREATE TABLE IF NOT EXISTS movimiento_ahorro (
  id BIGSERIAL PRIMARY KEY,
  cuenta_id BIGINT NOT NULL REFERENCES cuenta_ahorro(id),
  tipo VARCHAR(20) NOT NULL,
  monto NUMERIC(14,2) NOT NULL,
  saldo_resultante NUMERIC(14,2) NOT NULL,
  comprobante_id BIGINT REFERENCES comprobantes(id),
  periodo VARCHAR(7),
  estado VARCHAR(20) NOT NULL DEFAULT 'EJECUTADO',
  motivo_anulacion VARCHAR(255),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  created_by BIGINT
);

-- Índices de uso frecuente
CREATE INDEX IF NOT EXISTS idx_socios_estado ON socios(estado);
CREATE INDEX IF NOT EXISTS idx_caja_apertura_cajero_fecha ON caja_apertura(cajero_id, fecha);
CREATE INDEX IF NOT EXISTS idx_caja_movimiento_apertura ON caja_movimiento(caja_apertura_id);
CREATE INDEX IF NOT EXISTS idx_asiento_periodo ON asiento_contable(periodo_id);
CREATE INDEX IF NOT EXISTS idx_asiento_detalle_asiento ON asiento_detalle(asiento_id);
CREATE INDEX IF NOT EXISTS idx_regla_vigencia ON regla_contable(operacion, vigente_desde, vigente_hasta);
CREATE INDEX IF NOT EXISTS idx_aportacion_socio_periodo ON aportaciones(socio_id, periodo);
CREATE INDEX IF NOT EXISTS idx_aporte_pago ON aportacion_pagos(aportacion_id);
CREATE INDEX IF NOT EXISTS idx_cuenta_ahorro_socio ON cuenta_ahorro(socio_id);
CREATE INDEX IF NOT EXISTS idx_movimiento_ahorro_cuenta ON movimiento_ahorro(cuenta_id);

-- ----------------------------------------------------------------------------
-- 2.5 Creditos
-- ----------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS producto_credito (
  id BIGSERIAL PRIMARY KEY,
  nombre VARCHAR(80) NOT NULL,
  tasa_interes NUMERIC(7,4) NOT NULL,
  tasa_mora NUMERIC(7,4) NOT NULL DEFAULT 1.0000,
  sistema_amortizacion VARCHAR(20) NOT NULL DEFAULT 'FRANCES',
  plazo_max_meses INT NOT NULL,
  monto_min NUMERIC(14,2),
  monto_max NUMERIC(14,2),
  requiere_garante BOOLEAN NOT NULL DEFAULT false,
  vigente_desde DATE NOT NULL,
  vigente_hasta DATE,
  activo BOOLEAN NOT NULL DEFAULT true
);

CREATE TABLE IF NOT EXISTS solicitud_credito (
  id BIGSERIAL PRIMARY KEY,
  socio_id BIGINT NOT NULL REFERENCES socios(id),
  producto_id BIGINT NOT NULL REFERENCES producto_credito(id),
  monto_solicitado NUMERIC(14,2) NOT NULL,
  plazo_meses INT NOT NULL,
  destino VARCHAR(255),
  estado VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
  solicitado_por BIGINT,
  evaluado_por BIGINT,
  aprobado_por BIGINT,
  motivo_rechazo VARCHAR(255),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS credito (
  id BIGSERIAL PRIMARY KEY,
  solicitud_id BIGINT REFERENCES solicitud_credito(id),
  socio_id BIGINT NOT NULL REFERENCES socios(id),
  producto_id BIGINT NOT NULL REFERENCES producto_credito(id),
  monto_desembolsado NUMERIC(14,2) NOT NULL,
  tasa_interes NUMERIC(7,4) NOT NULL,
  plazo_meses INT NOT NULL,
  fecha_desembolso DATE,
  saldo_capital NUMERIC(14,2) NOT NULL,
  estado VARCHAR(20) NOT NULL DEFAULT 'VIGENTE',
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  created_by BIGINT
);

CREATE TABLE IF NOT EXISTS tabla_amortizacion (
  id BIGSERIAL PRIMARY KEY,
  credito_id BIGINT NOT NULL REFERENCES credito(id),
  numero_cuota INT NOT NULL,
  fecha_vencimiento DATE NOT NULL,
  capital NUMERIC(14,2) NOT NULL,
  interes NUMERIC(14,2) NOT NULL,
  cuota_total NUMERIC(14,2) NOT NULL,
  saldo_capital NUMERIC(14,2) NOT NULL,
  mora NUMERIC(14,2) NOT NULL DEFAULT 0,
  estado VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
  UNIQUE (credito_id, numero_cuota)
);

CREATE TABLE IF NOT EXISTS pago_cuota (
  id BIGSERIAL PRIMARY KEY,
  cuota_id BIGINT NOT NULL REFERENCES tabla_amortizacion(id),
  credito_id BIGINT NOT NULL REFERENCES credito(id),
  monto_capital NUMERIC(14,2) NOT NULL,
  monto_interes NUMERIC(14,2) NOT NULL,
  monto_mora NUMERIC(14,2) NOT NULL DEFAULT 0,
  comprobante_id BIGINT REFERENCES comprobantes(id),
  pagado_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  registrado_por BIGINT
);

CREATE TABLE IF NOT EXISTS credito_estado_historial (
  id BIGSERIAL PRIMARY KEY,
  credito_id BIGINT NOT NULL REFERENCES credito(id),
  estado_anterior VARCHAR(20),
  estado_nuevo VARCHAR(20),
  motivo VARCHAR(255),
  changed_by BIGINT,
  changed_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_producto_credito_nombre ON producto_credito(nombre);
CREATE INDEX IF NOT EXISTS idx_solicitud_socio ON solicitud_credito(socio_id);
CREATE INDEX IF NOT EXISTS idx_solicitud_estado ON solicitud_credito(estado);
CREATE INDEX IF NOT EXISTS idx_credito_socio ON credito(socio_id);
CREATE INDEX IF NOT EXISTS idx_credito_estado ON credito(estado);
CREATE INDEX IF NOT EXISTS idx_amortizacion_credito ON tabla_amortizacion(credito_id);
CREATE INDEX IF NOT EXISTS idx_amortizacion_estado ON tabla_amortizacion(estado);
CREATE INDEX IF NOT EXISTS idx_pago_cuota_cuota ON pago_cuota(cuota_id);

-- ----------------------------------------------------------------------------
-- 2.8 Cartera (vista de consulta)
-- ----------------------------------------------------------------------------

CREATE OR REPLACE VIEW v_cartera AS
SELECT
  c.id AS credito_id,
  c.socio_id,
  c.saldo_capital,
  ta.id AS cuota_id,
  ta.numero_cuota,
  ta.fecha_vencimiento,
  ta.cuota_total,
  ta.mora,
  ta.estado,
  CASE
    WHEN ta.estado = 'PENDIENTE' AND ta.fecha_vencimiento < CURRENT_DATE
      THEN CURRENT_DATE - ta.fecha_vencimiento
    ELSE 0
  END AS dias_vencido
FROM credito c
JOIN tabla_amortizacion ta ON ta.credito_id = c.id
WHERE ta.estado IN ('PENDIENTE', 'VENCIDA');

-- ----------------------------------------------------------------------------
-- 2.6 Tesoreria y presupuesto (Fase 5)
-- ----------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS gastos (
  id BIGSERIAL PRIMARY KEY,
  concepto VARCHAR(200) NOT NULL,
  descripcion VARCHAR(500),
  monto NUMERIC(14,2) NOT NULL,
  cuenta_contable_id BIGINT NOT NULL REFERENCES plan_cuentas(id),
  fecha_solicitud DATE NOT NULL DEFAULT CURRENT_DATE,
  solicitado_por BIGINT NOT NULL REFERENCES usuarios(id),
  estado VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE', -- PENDIENTE, APROBADO, RECHAZADO, PAGADO, ANULADO
  aprobado_por BIGINT REFERENCES usuarios(id),
  fecha_aprobacion TIMESTAMP,
  motivo_rechazo VARCHAR(300),
  comprobante_id BIGINT REFERENCES comprobantes(id),
  caja_movimiento_id BIGINT REFERENCES caja_movimiento(id),
  created_at TIMESTAMP NOT NULL DEFAULT now(),
  updated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS cuentas_por_pagar (
  id BIGSERIAL PRIMARY KEY,
  proveedor VARCHAR(200) NOT NULL,
  concepto VARCHAR(200) NOT NULL,
  monto NUMERIC(14,2) NOT NULL,
  cuenta_contable_id BIGINT NOT NULL REFERENCES plan_cuentas(id),
  fecha_emision DATE NOT NULL DEFAULT CURRENT_DATE,
  fecha_vencimiento DATE NOT NULL,
  estado VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE', -- PENDIENTE, PAGADA
  comprobante_id BIGINT REFERENCES comprobantes(id),
  caja_movimiento_id BIGINT REFERENCES caja_movimiento(id),
  created_at TIMESTAMP NOT NULL DEFAULT now(),
  updated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS cuentas_por_cobrar (
  id BIGSERIAL PRIMARY KEY,
  socio_id BIGINT REFERENCES socios(id),
  deudor VARCHAR(200) NOT NULL,
  concepto VARCHAR(200) NOT NULL,
  monto NUMERIC(14,2) NOT NULL,
  cuenta_contable_id BIGINT NOT NULL REFERENCES plan_cuentas(id),
  fecha_emision DATE NOT NULL DEFAULT CURRENT_DATE,
  fecha_vencimiento DATE NOT NULL,
  estado VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE', -- PENDIENTE, COBRADA
  comprobante_id BIGINT REFERENCES comprobantes(id),
  caja_movimiento_id BIGINT REFERENCES caja_movimiento(id),
  created_at TIMESTAMP NOT NULL DEFAULT now(),
  updated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS presupuesto_partidas (
  id BIGSERIAL PRIMARY KEY,
  anio INT NOT NULL,
  concepto VARCHAR(200) NOT NULL,
  cuenta_contable_id BIGINT NOT NULL REFERENCES plan_cuentas(id),
  monto_presupuestado NUMERIC(14,2) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT now(),
  UNIQUE(anio, concepto)
);

CREATE INDEX IF NOT EXISTS idx_gasto_estado ON gastos(estado);
CREATE INDEX IF NOT EXISTS idx_cxp_estado ON cuentas_por_pagar(estado);
CREATE INDEX IF NOT EXISTS idx_cxc_estado ON cuentas_por_cobrar(estado);
CREATE INDEX IF NOT EXISTS idx_presupuesto_anio ON presupuesto_partidas(anio);

-- 2.7 Notificaciones y alertas (Fase 6)
-- ----------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS notificaciones (
  id BIGSERIAL PRIMARY KEY,
  usuario_id BIGINT NOT NULL REFERENCES usuarios(id),
  tipo VARCHAR(40) NOT NULL, -- CUOTA_PROXIMA, CUOTA_VENCIDA, MORA, APORTACION_PENDIENTE, CIERRE_PENDIENTE
  referencia_tabla VARCHAR(60),
  referencia_id BIGINT,
  mensaje VARCHAR(255) NOT NULL,
  leida BOOLEAN NOT NULL DEFAULT false,
  created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_notificacion_usuario ON notificaciones(usuario_id, leida);

-- ----------------------------------------------------------------------------
-- Reportes (plantillas JasperReports)
-- ----------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS reportes (
  id BIGSERIAL PRIMARY KEY,
  nombre VARCHAR(100) UNIQUE NOT NULL,
  descripcion VARCHAR(255),
  titulo VARCHAR(200) NOT NULL,
  entidad VARCHAR(50) NOT NULL,
  formato_default VARCHAR(10) NOT NULL DEFAULT 'pdf',
  orientacion VARCHAR(10) NOT NULL DEFAULT 'portrait',
  parametros JSONB,
  jrxml TEXT NOT NULL,
  activo BOOLEAN NOT NULL DEFAULT true,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ
);

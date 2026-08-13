# PLAN DE DESARROLLO — Sistema Integral de Caja de Ahorros / Cooperativa de Ahorro y Crédito

**Proyecto:** ALANTEK
**Stack:** Angular · Spring Boot · PostgreSQL · Spring Security + JWT · JasperReports · Docker + Nginx
**Documento vivo:** actualizar conforme avanza cada fase.

---

## Índice

1. Alcance y principios de diseño
2. Modelo de datos PostgreSQL (por módulo)
3. Roles y permisos
4. Estados y flujos de aprobación
5. Fórmulas de interés, mora y aportación
6. Motor de reglas contables (Debe/Haber automático)
7. Endpoints REST por módulo
8. Estructura de proyecto (Angular + Spring Boot)
9. Fases de desarrollo (sprints y entregables)
10. Plan de pruebas
11. Prompts paso a paso para el agente de programación

---

## 1. Alcance y principios de diseño

- Socio ≠ Usuario del sistema. `socios` y `usuarios` son entidades separadas, vinculadas opcionalmente por `usuario_id` en `socios` cuando exista portal/app.
- Ninguna transacción financiera se elimina físicamente. Toda anulación es una operación inversa registrada, nunca un `DELETE`.
- Las tasas, porcentajes y cuentas contables **nunca se codifican**; viven en tablas paramétricas con vigencia (`vigente_desde`, `vigente_hasta`).
- Toda entidad financiera lleva trazabilidad estándar: `created_at`, `created_by`, `updated_at`, `updated_by`, `estado`, `motivo_anulacion`, `approved_by`, `approved_at`.
- Flujo de aprobación estándar para operaciones sensibles: `CREADO → PENDIENTE → APROBADO → EJECUTADO` (o `RECHAZADO`). Quien crea no debería, por defecto, poder aprobar.
- Cada operación financiera relevante dispara automáticamente su asiento contable vía el motor de reglas (sección 6) — no hay doble digitación.

---

## 2. Modelo de datos PostgreSQL

Convenciones: `snake_case`, PK `id BIGSERIAL`, FKs `<entidad>_id`, timestamps `TIMESTAMPTZ`, montos `NUMERIC(14,2)`, tasas `NUMERIC(7,4)`.

### 2.1 Administración y Seguridad

```sql
CREATE TABLE usuarios (
  id BIGSERIAL PRIMARY KEY,
  username VARCHAR(50) UNIQUE NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  nombre_completo VARCHAR(150) NOT NULL,
  email VARCHAR(150),
  estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO', -- ACTIVO, BLOQUEADO, INACTIVO
  intentos_fallidos INT NOT NULL DEFAULT 0,
  ultimo_acceso TIMESTAMPTZ,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  created_by BIGINT,
  updated_at TIMESTAMPTZ,
  updated_by BIGINT
);

CREATE TABLE roles (
  id BIGSERIAL PRIMARY KEY,
  nombre VARCHAR(50) UNIQUE NOT NULL, -- ADMIN, GERENTE, CONTADOR, TESORERO, CREDITO, AUDITOR
  descripcion VARCHAR(200)
);

CREATE TABLE usuario_roles (
  usuario_id BIGINT REFERENCES usuarios(id),
  rol_id BIGINT REFERENCES roles(id),
  PRIMARY KEY (usuario_id, rol_id)
);

CREATE TABLE permisos (
  id BIGSERIAL PRIMARY KEY,
  modulo VARCHAR(50) NOT NULL,       -- SOCIOS, CREDITOS, CAJA, CONTABILIDAD...
  accion VARCHAR(30) NOT NULL        -- VER, CREAR, EDITAR, APROBAR, ANULAR, IMPRIMIR, EXPORTAR
);

CREATE TABLE rol_permisos (
  rol_id BIGINT REFERENCES roles(id),
  permiso_id BIGINT REFERENCES permisos(id),
  PRIMARY KEY (rol_id, permiso_id)
);

CREATE TABLE sesiones (
  id BIGSERIAL PRIMARY KEY,
  usuario_id BIGINT REFERENCES usuarios(id),
  token VARCHAR(255),
  ip VARCHAR(45),
  user_agent VARCHAR(255),
  iniciada_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  cerrada_at TIMESTAMPTZ
);

CREATE TABLE auditoria (
  id BIGSERIAL PRIMARY KEY,
  usuario_id BIGINT REFERENCES usuarios(id),
  tabla_afectada VARCHAR(60) NOT NULL,
  registro_id BIGINT NOT NULL,
  accion VARCHAR(20) NOT NULL,       -- CREAR, EDITAR, ANULAR, APROBAR, RECHAZAR
  valor_anterior JSONB,
  valor_nuevo JSONB,
  ip VARCHAR(45),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE parametros_generales (
  clave VARCHAR(80) PRIMARY KEY,
  valor VARCHAR(255) NOT NULL,
  descripcion VARCHAR(255),
  updated_at TIMESTAMPTZ,
  updated_by BIGINT
);
```

### 2.2 Socios

```sql
CREATE TABLE socios (
  id BIGSERIAL PRIMARY KEY,
  codigo VARCHAR(20) UNIQUE NOT NULL,
  usuario_id BIGINT REFERENCES usuarios(id),      -- nulo si no tiene acceso al sistema
  identificacion VARCHAR(20) UNIQUE NOT NULL,
  nombres VARCHAR(100) NOT NULL,
  apellidos VARCHAR(100) NOT NULL,
  telefono VARCHAR(30),
  email VARCHAR(150),
  direccion VARCHAR(255),
  fecha_ingreso DATE NOT NULL,
  fecha_retiro DATE,
  estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO', -- ACTIVO, SUSPENDIDO, RETIRADO, FALLECIDO
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  created_by BIGINT,
  updated_at TIMESTAMPTZ,
  updated_by BIGINT
);

CREATE TABLE socio_beneficiarios (
  id BIGSERIAL PRIMARY KEY,
  socio_id BIGINT REFERENCES socios(id),
  nombres VARCHAR(150) NOT NULL,
  parentesco VARCHAR(50),
  porcentaje NUMERIC(5,2) NOT NULL DEFAULT 100
);

CREATE TABLE socio_documentos (
  id BIGSERIAL PRIMARY KEY,
  socio_id BIGINT REFERENCES socios(id),
  tipo_documento VARCHAR(50),
  url_archivo VARCHAR(500),
  uploaded_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
```

### 2.3 Aportaciones

```sql
CREATE TABLE aportacion_config (
  id BIGSERIAL PRIMARY KEY,
  tipo VARCHAR(20) NOT NULL,          -- OBLIGATORIA, EXTRAORDINARIA
  modo_calculo VARCHAR(10) NOT NULL,  -- FIJO, PORCENTAJE
  valor NUMERIC(14,4) NOT NULL,
  periodicidad VARCHAR(20) NOT NULL,  -- MENSUAL, QUINCENAL, ANUAL
  monto_minimo NUMERIC(14,2),
  monto_maximo NUMERIC(14,2),
  vigente_desde DATE NOT NULL,
  vigente_hasta DATE,
  created_by BIGINT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE aportaciones (
  id BIGSERIAL PRIMARY KEY,
  socio_id BIGINT REFERENCES socios(id),
  config_id BIGINT REFERENCES aportacion_config(id),
  periodo VARCHAR(7) NOT NULL,        -- 'YYYY-MM'
  monto_esperado NUMERIC(14,2) NOT NULL,
  monto_pagado NUMERIC(14,2) NOT NULL DEFAULT 0,
  mora NUMERIC(14,2) NOT NULL DEFAULT 0,
  estado VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE', -- PENDIENTE, PARCIAL, PAGADA, EXONERADA
  exonerado_por BIGINT,
  motivo_exoneracion VARCHAR(255)
);

CREATE TABLE aportacion_pagos (
  id BIGSERIAL PRIMARY KEY,
  aportacion_id BIGINT REFERENCES aportaciones(id),
  monto NUMERIC(14,2) NOT NULL,
  caja_movimiento_id BIGINT,          -- FK a caja_movimientos (2.9)
  pagado_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  registrado_por BIGINT
);
```

### 2.4 Ahorros

```sql
CREATE TABLE producto_ahorro (
  id BIGSERIAL PRIMARY KEY,
  nombre VARCHAR(80) NOT NULL,       -- A LA VISTA, PROGRAMADO, A PLAZO
  tasa_interes NUMERIC(7,4) NOT NULL,
  periodicidad_capitalizacion VARCHAR(20) NOT NULL, -- DIARIA, MENSUAL, ANUAL
  saldo_minimo NUMERIC(14,2) NOT NULL DEFAULT 0,
  limite_retiros_mes INT,
  vigente_desde DATE NOT NULL,
  vigente_hasta DATE,
  activo BOOLEAN NOT NULL DEFAULT true
);

CREATE TABLE cuenta_ahorro (
  id BIGSERIAL PRIMARY KEY,
  socio_id BIGINT REFERENCES socios(id),
  producto_id BIGINT REFERENCES producto_ahorro(id),
  numero_cuenta VARCHAR(20) UNIQUE NOT NULL,
  saldo NUMERIC(14,2) NOT NULL DEFAULT 0,
  estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVA', -- ACTIVA, INACTIVA, CERRADA
  fecha_apertura DATE NOT NULL,
  fecha_cierre DATE
);

CREATE TABLE movimiento_ahorro (
  id BIGSERIAL PRIMARY KEY,
  cuenta_id BIGINT REFERENCES cuenta_ahorro(id),
  tipo VARCHAR(20) NOT NULL,          -- DEPOSITO, RETIRO, INTERES, AJUSTE
  monto NUMERIC(14,2) NOT NULL,
  saldo_resultante NUMERIC(14,2) NOT NULL,
  comprobante_id BIGINT,              -- FK a comprobantes (2.9)
  estado VARCHAR(20) NOT NULL DEFAULT 'EJECUTADO', -- EJECUTADO, ANULADO
  motivo_anulacion VARCHAR(255),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  created_by BIGINT
);
```

### 2.5 Créditos

```sql
CREATE TABLE producto_credito (
  id BIGSERIAL PRIMARY KEY,
  nombre VARCHAR(80) NOT NULL,        -- ORDINARIO, EMERGENTE, EDUCATIVO, CONSUMO
  tasa_interes NUMERIC(7,4) NOT NULL,
  sistema_amortizacion VARCHAR(20) NOT NULL, -- FRANCES, ALEMAN, AMERICANO
  plazo_max_meses INT NOT NULL,
  monto_min NUMERIC(14,2),
  monto_max NUMERIC(14,2),
  requiere_garante BOOLEAN NOT NULL DEFAULT false,
  vigente_desde DATE NOT NULL,
  vigente_hasta DATE
);

CREATE TABLE solicitud_credito (
  id BIGSERIAL PRIMARY KEY,
  socio_id BIGINT REFERENCES socios(id),
  producto_id BIGINT REFERENCES producto_credito(id),
  monto_solicitado NUMERIC(14,2) NOT NULL,
  plazo_meses INT NOT NULL,
  destino VARCHAR(255),
  estado VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE', -- PENDIENTE, EVALUACION, APROBADA, RECHAZADA
  evaluado_por BIGINT,
  aprobado_por BIGINT,
  motivo_rechazo VARCHAR(255),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE credito (
  id BIGSERIAL PRIMARY KEY,
  solicitud_id BIGINT REFERENCES solicitud_credito(id),
  socio_id BIGINT REFERENCES socios(id),
  producto_id BIGINT REFERENCES producto_credito(id),
  monto_desembolsado NUMERIC(14,2) NOT NULL,
  tasa_interes NUMERIC(7,4) NOT NULL,   -- copiada al desembolso (histórico)
  plazo_meses INT NOT NULL,
  fecha_desembolso DATE NOT NULL,
  saldo_capital NUMERIC(14,2) NOT NULL,
  estado VARCHAR(20) NOT NULL DEFAULT 'VIGENTE', -- VIGENTE, VENCIDO, MORA, REFINANCIADO, CANCELADO, CASTIGADO
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE tabla_amortizacion (
  id BIGSERIAL PRIMARY KEY,
  credito_id BIGINT REFERENCES credito(id),
  numero_cuota INT NOT NULL,
  fecha_vencimiento DATE NOT NULL,
  capital NUMERIC(14,2) NOT NULL,
  interes NUMERIC(14,2) NOT NULL,
  cuota_total NUMERIC(14,2) NOT NULL,
  saldo_capital NUMERIC(14,2) NOT NULL,
  estado VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE' -- PENDIENTE, PAGADA, VENCIDA, REFINANCIADA
);

CREATE TABLE pago_cuota (
  id BIGSERIAL PRIMARY KEY,
  cuota_id BIGINT REFERENCES tabla_amortizacion(id),
  monto_capital NUMERIC(14,2) NOT NULL,
  monto_interes NUMERIC(14,2) NOT NULL,
  monto_mora NUMERIC(14,2) NOT NULL DEFAULT 0,
  comprobante_id BIGINT,
  pagado_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  registrado_por BIGINT
);

CREATE TABLE garantia (
  id BIGSERIAL PRIMARY KEY,
  credito_id BIGINT REFERENCES credito(id),
  tipo VARCHAR(50),                   -- HIPOTECARIA, PRENDARIA, QUIROGRAFARIA
  descripcion VARCHAR(255),
  valor_estimado NUMERIC(14,2)
);

CREATE TABLE garante (
  id BIGSERIAL PRIMARY KEY,
  credito_id BIGINT REFERENCES credito(id),
  socio_id BIGINT REFERENCES socios(id),  -- opcional si el garante también es socio
  identificacion VARCHAR(20),
  nombres VARCHAR(150)
);

CREATE TABLE credito_estado_historial (
  id BIGSERIAL PRIMARY KEY,
  credito_id BIGINT REFERENCES credito(id),
  estado_anterior VARCHAR(20),
  estado_nuevo VARCHAR(20),
  motivo VARCHAR(255),
  changed_by BIGINT,
  changed_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
```

### 2.6 Motor de tasas y porcentajes (paramétrico)

```sql
CREATE TABLE parametro_tasa (
  id BIGSERIAL PRIMARY KEY,
  tipo VARCHAR(40) NOT NULL,    -- TASA_ACTIVA, TASA_PASIVA, MORA, COMISION, FONDO_RESERVA
  producto_id BIGINT,           -- opcional: referencia a producto_credito o producto_ahorro
  valor NUMERIC(7,4) NOT NULL,
  vigente_desde DATE NOT NULL,
  vigente_hasta DATE,
  autorizado_por BIGINT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
```

### 2.7 Simulador de créditos

No requiere persistencia obligatoria (puede ser un cálculo en memoria), pero se recomienda guardar simulaciones para trazabilidad comercial:

```sql
CREATE TABLE simulacion_credito (
  id BIGSERIAL PRIMARY KEY,
  socio_id BIGINT REFERENCES socios(id),
  producto_id BIGINT REFERENCES producto_credito(id),
  monto NUMERIC(14,2) NOT NULL,
  plazo_meses INT NOT NULL,
  tasa_usada NUMERIC(7,4) NOT NULL,
  cuota_aproximada NUMERIC(14,2) NOT NULL,
  interes_total NUMERIC(14,2) NOT NULL,
  total_a_pagar NUMERIC(14,2) NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  created_by BIGINT
);
```

### 2.8 Cartera y cobranza

Cartera se deriva por consulta sobre `tabla_amortizacion` + `credito`; no requiere tablas nuevas, pero sí una vista:

```sql
CREATE VIEW v_cartera AS
SELECT
  c.id AS credito_id,
  c.socio_id,
  c.saldo_capital,
  ta.numero_cuota,
  ta.fecha_vencimiento,
  ta.cuota_total,
  ta.estado,
  CASE
    WHEN ta.estado = 'PENDIENTE' AND ta.fecha_vencimiento < CURRENT_DATE
      THEN CURRENT_DATE - ta.fecha_vencimiento
    ELSE 0
  END AS dias_vencido
FROM credito c
JOIN tabla_amortizacion ta ON ta.credito_id = c.id
WHERE ta.estado IN ('PENDIENTE', 'VENCIDA');
```

### 2.9 Caja

```sql
CREATE TABLE caja_apertura (
  id BIGSERIAL PRIMARY KEY,
  cajero_id BIGINT REFERENCES usuarios(id),
  fecha DATE NOT NULL,
  saldo_inicial NUMERIC(14,2) NOT NULL,
  estado VARCHAR(20) NOT NULL DEFAULT 'ABIERTA', -- ABIERTA, CERRADA
  opened_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  closed_at TIMESTAMPTZ
);

CREATE TABLE comprobantes (
  id BIGSERIAL PRIMARY KEY,
  numero VARCHAR(20) UNIQUE NOT NULL,  -- numeración única, secuencial
  tipo VARCHAR(20) NOT NULL,           -- INGRESO, EGRESO, DIARIO
  descripcion VARCHAR(255),
  estado VARCHAR(20) NOT NULL DEFAULT 'EJECUTADO', -- EJECUTADO, ANULADO
  motivo_anulacion VARCHAR(255),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  created_by BIGINT
);

CREATE TABLE caja_movimiento (
  id BIGSERIAL PRIMARY KEY,
  caja_apertura_id BIGINT REFERENCES caja_apertura(id),
  comprobante_id BIGINT REFERENCES comprobantes(id),
  tipo VARCHAR(20) NOT NULL,           -- DEPOSITO, RETIRO, COBRO_CREDITO, APORTACION, DESEMBOLSO, INGRESO_VARIO, EGRESO, TRANSFERENCIA
  monto NUMERIC(14,2) NOT NULL,
  referencia_tabla VARCHAR(60),        -- ej. 'pago_cuota', 'aportacion_pagos'
  referencia_id BIGINT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  created_by BIGINT
);

CREATE TABLE caja_arqueo (
  id BIGSERIAL PRIMARY KEY,
  caja_apertura_id BIGINT REFERENCES caja_apertura(id),
  saldo_sistema NUMERIC(14,2) NOT NULL,
  saldo_fisico NUMERIC(14,2) NOT NULL,
  diferencia NUMERIC(14,2) NOT NULL,
  observacion VARCHAR(255),
  realizado_por BIGINT,
  realizado_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
```

### 2.10 Bancos

```sql
CREATE TABLE cuenta_bancaria (
  id BIGSERIAL PRIMARY KEY,
  banco VARCHAR(80) NOT NULL,
  numero_cuenta VARCHAR(30) NOT NULL,
  tipo VARCHAR(20),
  saldo_contable NUMERIC(14,2) NOT NULL DEFAULT 0
);

CREATE TABLE banco_movimiento (
  id BIGSERIAL PRIMARY KEY,
  cuenta_bancaria_id BIGINT REFERENCES cuenta_bancaria(id),
  tipo VARCHAR(20) NOT NULL,   -- DEPOSITO, RETIRO, TRANSFERENCIA, NOTA_DEBITO, NOTA_CREDITO
  monto NUMERIC(14,2) NOT NULL,
  comprobante_id BIGINT REFERENCES comprobantes(id),
  conciliado BOOLEAN NOT NULL DEFAULT false,
  fecha DATE NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE conciliacion_bancaria (
  id BIGSERIAL PRIMARY KEY,
  cuenta_bancaria_id BIGINT REFERENCES cuenta_bancaria(id),
  periodo VARCHAR(7) NOT NULL,
  saldo_contable NUMERIC(14,2) NOT NULL,
  saldo_bancario NUMERIC(14,2) NOT NULL,
  diferencia NUMERIC(14,2) NOT NULL,
  realizado_por BIGINT,
  realizado_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
```

### 2.11 Contabilidad

```sql
CREATE TABLE plan_cuentas (
  id BIGSERIAL PRIMARY KEY,
  codigo VARCHAR(20) UNIQUE NOT NULL,
  nombre VARCHAR(150) NOT NULL,
  tipo VARCHAR(20) NOT NULL,       -- ACTIVO, PASIVO, PATRIMONIO, INGRESO, GASTO
  cuenta_padre_id BIGINT REFERENCES plan_cuentas(id),
  nivel INT NOT NULL,
  acepta_movimiento BOOLEAN NOT NULL DEFAULT true
);

CREATE TABLE periodo_contable (
  id BIGSERIAL PRIMARY KEY,
  anio INT NOT NULL,
  mes INT NOT NULL,
  estado VARCHAR(20) NOT NULL DEFAULT 'ABIERTO', -- ABIERTO, CERRADO
  cerrado_por BIGINT,
  cerrado_at TIMESTAMPTZ,
  UNIQUE (anio, mes)
);

CREATE TABLE asiento_contable (
  id BIGSERIAL PRIMARY KEY,
  periodo_id BIGINT REFERENCES periodo_contable(id),
  comprobante_id BIGINT REFERENCES comprobantes(id),
  fecha DATE NOT NULL,
  descripcion VARCHAR(255),
  origen VARCHAR(40),          -- AUTOMATICO, MANUAL
  estado VARCHAR(20) NOT NULL DEFAULT 'EJECUTADO', -- EJECUTADO, ANULADO
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  created_by BIGINT
);

CREATE TABLE asiento_detalle (
  id BIGSERIAL PRIMARY KEY,
  asiento_id BIGINT REFERENCES asiento_contable(id),
  cuenta_id BIGINT REFERENCES plan_cuentas(id),
  debe NUMERIC(14,2) NOT NULL DEFAULT 0,
  haber NUMERIC(14,2) NOT NULL DEFAULT 0
);
-- Regla de integridad de aplicación: SUM(debe) = SUM(haber) por asiento_id (validar en servicio, con trigger opcional).
```

### 2.12 Motor de reglas contables

```sql
CREATE TABLE regla_contable (
  id BIGSERIAL PRIMARY KEY,
  operacion VARCHAR(60) UNIQUE NOT NULL,  -- 'APORTACION', 'DEPOSITO_AHORRO', 'RETIRO_AHORRO', 'DESEMBOLSO_CREDITO', 'PAGO_CAPITAL', 'PAGO_INTERES', 'PAGO_MORA'
  cuenta_debe_id BIGINT REFERENCES plan_cuentas(id),
  cuenta_haber_id BIGINT REFERENCES plan_cuentas(id),
  vigente_desde DATE NOT NULL,
  vigente_hasta DATE,
  activo BOOLEAN NOT NULL DEFAULT true
);
```

### 2.13 Tesorería

```sql
CREATE TABLE cuenta_por_cobrar (
  id BIGSERIAL PRIMARY KEY,
  origen_tabla VARCHAR(60),   -- 'credito', 'aportaciones'
  origen_id BIGINT,
  monto NUMERIC(14,2) NOT NULL,
  saldo NUMERIC(14,2) NOT NULL,
  fecha_vencimiento DATE,
  estado VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE'
);

CREATE TABLE cuenta_por_pagar (
  id BIGSERIAL PRIMARY KEY,
  proveedor_id BIGINT,
  documento VARCHAR(50),
  monto NUMERIC(14,2) NOT NULL,
  saldo NUMERIC(14,2) NOT NULL,
  fecha_vencimiento DATE,
  estado VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE'
);
```

### 2.14 Presupuesto

```sql
CREATE TABLE presupuesto (
  id BIGSERIAL PRIMARY KEY,
  anio INT NOT NULL,
  estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO'
);

CREATE TABLE presupuesto_partida (
  id BIGSERIAL PRIMARY KEY,
  presupuesto_id BIGINT REFERENCES presupuesto(id),
  cuenta_id BIGINT REFERENCES plan_cuentas(id),
  monto_inicial NUMERIC(14,2) NOT NULL,
  monto_reformado NUMERIC(14,2) NOT NULL DEFAULT 0,
  comprometido NUMERIC(14,2) NOT NULL DEFAULT 0,
  devengado NUMERIC(14,2) NOT NULL DEFAULT 0,
  pagado NUMERIC(14,2) NOT NULL DEFAULT 0
);
```

### 2.15 Gastos

```sql
CREATE TABLE proveedor (
  id BIGSERIAL PRIMARY KEY,
  ruc VARCHAR(20),
  razon_social VARCHAR(150) NOT NULL,
  telefono VARCHAR(30),
  email VARCHAR(150)
);

CREATE TABLE gasto (
  id BIGSERIAL PRIMARY KEY,
  proveedor_id BIGINT REFERENCES proveedor(id),
  categoria VARCHAR(60),
  centro_costo VARCHAR(60),
  monto NUMERIC(14,2) NOT NULL,
  estado VARCHAR(20) NOT NULL DEFAULT 'SOLICITADO', -- SOLICITADO, APROBADO, PAGADO, RECHAZADO
  aprobado_por BIGINT,
  comprobante_id BIGINT REFERENCES comprobantes(id),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  created_by BIGINT
);
```

### 2.16 Notificaciones y alertas

```sql
CREATE TABLE notificacion (
  id BIGSERIAL PRIMARY KEY,
  usuario_id BIGINT REFERENCES usuarios(id),
  tipo VARCHAR(40) NOT NULL,   -- CUOTA_PROXIMA, CUOTA_VENCIDA, MORA, APORTACION_PENDIENTE, SALDO_BAJO, CIERRE_PENDIENTE
  referencia_tabla VARCHAR(60),
  referencia_id BIGINT,
  mensaje VARCHAR(255) NOT NULL,
  leida BOOLEAN NOT NULL DEFAULT false,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
```

---

## 3. Roles y permisos

| Rol | Descripción | Permisos característicos |
|---|---|---|
| ADMIN | Administración total del sistema | Todos los módulos, incluida configuración de roles y parámetros |
| GERENTE | Visión gerencial y aprobaciones de alto nivel | Ver todo, aprobar créditos/gastos mayores, dashboard |
| CONTADOR | Gestión contable | Plan de cuentas, asientos, cierres, estados financieros |
| TESORERO/CAJERO | Operación diaria de caja | Apertura/cierre de caja, cobros, depósitos, arqueo |
| CREDITO/COBRANZA | Gestión de cartera | Solicitudes, evaluación, cobranza, gestión de mora |
| AUDITOR | Solo consulta y auditoría | Ver todo, sin crear/editar/aprobar |

Regla de aplicación: quien crea una solicitud de crédito o un gasto no debería tener el permiso `APROBAR` sobre esa misma operación salvo excepción explícita autorizada por ADMIN.

---

## 4. Estados y flujos de aprobación

**Flujo genérico (créditos, gastos, anulaciones, ajustes contables):**

```
CREADO → PENDIENTE → APROBADO → EJECUTADO
                 └──→ RECHAZADO
```

**Estados de crédito:** `PENDIENTE → EVALUACION → APROBADA/RECHAZADA` (solicitud) → al desembolsar: `VIGENTE → VENCIDO/MORA → REFINANCIADO/CANCELADO/CASTIGADO`.

**Estados de cuota:** `PENDIENTE → PAGADA` o `PENDIENTE → VENCIDA → (pago) → PAGADA` o `→ REFINANCIADA`.

**Estados de caja:** `ABIERTA → CERRADA` (una apertura por cajero por día; no se puede operar sin caja abierta).

**Estados de período contable:** `ABIERTO → CERRADO`, reapertura solo con autorización de ADMIN + justificación registrada en `auditoria`.

---

## 5. Fórmulas de interés, mora y aportación

**Interés de ahorro (simple, sobre saldo diario):**

```
interes = saldo × tasa × dias / base_anual        (base_anual = 360 o 365 según parametro_generales)
```

**Cuota de crédito — Sistema Francés (cuota fija):**

```
i = tasa_mensual = tasa_anual / 12
cuota = monto × [ i × (1+i)^n ] / [ (1+i)^n − 1 ]
```

Para cada período `k`:
```
interes_k = saldo_capital_(k-1) × i
capital_k = cuota − interes_k
saldo_capital_k = saldo_capital_(k-1) − capital_k
```

**Mora sobre cuota vencida:**

```
mora = capital_vencido × tasa_mora × dias_vencido / base_anual
```

**Aportación esperada por período:**

```
si modo_calculo = FIJO       → monto_esperado = valor
si modo_calculo = PORCENTAJE → monto_esperado = ingreso_base_socio × valor / 100  (o remuneración base parametrizada)
```

Todas las tasas usadas en el cálculo se leen de `parametro_tasa` / `producto_ahorro` / `producto_credito` vigentes a la **fecha de la operación**, nunca la tasa vigente al momento de la consulta — esto preserva el histórico.

---

## 6. Motor de reglas contables (Debe/Haber automático)

Matriz base a precargar en `regla_contable`:

| Operación | Cuenta Debe | Cuenta Haber |
|---|---|---|
| APORTACION | Caja | Aportes de socios |
| DEPOSITO_AHORRO | Caja | Ahorros de socios |
| RETIRO_AHORRO | Ahorros de socios | Caja |
| DESEMBOLSO_CREDITO | Cartera de créditos | Caja / Bancos |
| PAGO_CAPITAL | Caja | Cartera de créditos |
| PAGO_INTERES | Caja | Ingreso por intereses |
| PAGO_MORA | Caja | Ingreso por mora |
| GASTO_PAGADO | Cuenta de gasto correspondiente | Caja / Bancos |

**Flujo de integración (ejemplo — cobro de cuota):**

1. El cajero registra el pago de una cuota (`pago_cuota`).
2. El servicio de aplicación crea el `caja_movimiento` correspondiente.
3. El servicio busca en `regla_contable` las operaciones `PAGO_CAPITAL`, `PAGO_INTERES` y, si aplica, `PAGO_MORA`, vigentes a la fecha.
4. Se genera un único `asiento_contable` con un `asiento_detalle` por cada componente (capital, interés, mora), garantizando `SUM(debe) = SUM(haber)`.
5. El usuario nunca captura cuentas contables manualmente en operaciones de rutina; solo en asientos de tipo `MANUAL` (ajustes).

Este componente (`AsientoAutomaticoService` o similar) debe implementarse **antes** de terminar los módulos de ahorros y créditos, porque ambos dependen de él.

---

## 7. Endpoints REST por módulo

Prefijo base sugerido: `/api/v1`

```
# Seguridad
POST   /auth/login
POST   /auth/refresh
POST   /auth/logout
GET    /usuarios
POST   /usuarios
PUT    /usuarios/{id}
GET    /roles
POST   /roles/{id}/permisos
GET    /auditoria?tabla=&desde=&hasta=

# Socios
GET    /socios
POST   /socios
GET    /socios/{id}
PUT    /socios/{id}
GET    /socios/{id}/estado-cuenta

# Aportaciones
GET    /aportaciones/config
POST   /aportaciones/config
GET    /socios/{id}/aportaciones
POST   /aportaciones/{id}/pagos

# Ahorros
GET    /productos-ahorro
POST   /cuentas-ahorro
GET    /cuentas-ahorro/{id}/movimientos
POST   /cuentas-ahorro/{id}/depositos
POST   /cuentas-ahorro/{id}/retiros

# Créditos
POST   /solicitudes-credito
PUT    /solicitudes-credito/{id}/evaluar
PUT    /solicitudes-credito/{id}/aprobar
POST   /creditos/{id}/desembolsar
GET    /creditos/{id}/amortizacion
POST   /creditos/{id}/pagos
POST   /creditos/{id}/refinanciar
POST   /simulador-credito

# Cartera
GET    /cartera?estado=&socio_id=
GET    /cartera/morosidad

# Caja
POST   /caja/apertura
POST   /caja/cierre
GET    /caja/{id}/movimientos
POST   /caja/arqueo

# Bancos
GET    /cuentas-bancarias
POST   /cuentas-bancarias/{id}/movimientos
POST   /conciliacion-bancaria

# Contabilidad
GET    /plan-cuentas
POST   /asientos-contables            (manual)
GET    /libro-diario?desde=&hasta=
GET    /libro-mayor?cuenta_id=
GET    /balance-comprobacion?periodo=
GET    /estados-financieros?periodo=
POST   /periodos-contables/{id}/cerrar
POST   /periodos-contables/{id}/reabrir

# Tesorería / presupuesto / gastos
GET    /cuentas-por-pagar
GET    /cuentas-por-cobrar
GET    /presupuesto/{anio}
POST   /gastos
PUT    /gastos/{id}/aprobar

# Reportes / dashboard
GET    /reportes/socios
GET    /reportes/cartera
GET    /reportes/caja
GET    /dashboard/resumen

# Notificaciones
GET    /notificaciones?usuario_id=
PUT    /notificaciones/{id}/leida
```

Todos los endpoints de escritura deben validar el permiso correspondiente (`modulo` + `accion`) vía Spring Security antes de ejecutar la operación, y registrar en `auditoria` cualquier creación, edición, aprobación o anulación.

---

## 8. Estructura de proyecto

### 8.1 Backend (Spring Boot — Maven, arquitectura por capas)

```
src/main/java/com/alantek/caja/
├── config/            # SecurityConfig, JwtConfig, CorsConfig
├── security/          # JwtFilter, UserDetailsServiceImpl
├── modulo/
│   ├── seguridad/      (usuarios, roles, permisos, auditoria)
│   ├── socios/
│   ├── aportaciones/
│   ├── ahorros/
│   ├── creditos/
│   ├── cartera/
│   ├── caja/
│   ├── bancos/
│   ├── contabilidad/
│   │   ├── planCuentas/
│   │   ├── asientos/
│   │   └── reglaContable/     # motor de reglas Debe/Haber
│   ├── tesoreria/
│   ├── presupuesto/
│   ├── gastos/
│   ├── reportes/
│   └── notificaciones/
│       (cada módulo: controller / service / repository / dto / entity)
├── shared/             # excepciones, utilidades, AuditListener
└── CajaAhorrosApplication.java
```

### 8.2 Frontend (Angular — feature modules + lazy loading)

```
src/app/
├── core/               # interceptors (JWT, error), guards, auth.service
├── shared/             # componentes UI reutilizables, pipes, validadores
├── layout/             # shell, sidebar por rol
├── features/
│   ├── socios/
│   ├── aportaciones/
│   ├── ahorros/
│   ├── creditos/
│   │   ├── solicitudes/
│   │   ├── simulador/
│   │   └── amortizacion/
│   ├── cartera/
│   ├── caja/
│   ├── bancos/
│   ├── contabilidad/
│   ├── tesoreria/
│   ├── presupuesto/
│   ├── gastos/
│   ├── reportes/
│   └── dashboard/
└── app-routing.module.ts   # rutas protegidas por rol/permiso
```

---

## 9. Fases de desarrollo (sprints y entregables)

| Fase | Semanas | Entregable verificable |
|---|---|---|
| **1. Fundacional** | 1–6 | Login, roles/permisos, CRUD de socios, auditoría funcionando de extremo a extremo |
| **2. Motor contable** | 7–13 | Plan de cuentas, períodos, caja/bancos, `regla_contable` y generación automática de asientos (probado con al menos 2 operaciones reales) |
| **3. Aportes y ahorros** | 14–19 | Cálculo automático de aportación esperada, productos de ahorro, depósitos/retiros con asiento automático |
| **4. Créditos y cobranza** | 20–27 | Solicitud → aprobación → desembolso → tabla de amortización → cobro de cuota con asiento automático, vista de cartera y morosidad |
| **5. Tesorería y presupuesto** | 28–32 | Cuentas por pagar/cobrar, gastos con flujo de aprobación, presupuesto vs. ejecución |
| **6. Reportería y cierre** | 33–38 | Dashboard gerencial, reportes exportables, notificaciones/alertas, portal del socio (lectura), pruebas integrales y estabilización |

Cada fase termina con: (a) demo funcional con datos reales o de prueba, (b) manual de usuario breve del módulo, (c) checklist de pruebas de la sección 10 aprobado.

**Estado de avance:** Fases 1–4 implementadas y probadas (backend 45/45 tests, frontend 54/54 unit tests, e2e 23/23). Fase 4 (Créditos y cobranza) incluye módulo completo con simulador, mora y refinanciamiento.

---

## 10. Plan de pruebas

**Por cada fase, verificar:**

- [ ] Reglas de negocio: cálculo de interés, mora y cuota coincide con fórmulas de la sección 5 (comparar contra cálculo manual en hoja de cálculo de control).
- [ ] Cuadre contable: todo `asiento_contable` generado automáticamente cumple `SUM(debe) = SUM(haber)`.
- [ ] Inmutabilidad: intentar eliminar una transacción financiera debe estar bloqueado a nivel de API (solo anulación permitida).
- [ ] Auditoría: cada creación/edición/aprobación/anulación relevante deja registro en `auditoria` con usuario y timestamp.
- [ ] Permisos: un usuario sin permiso `APROBAR` en un módulo no puede ejecutar la acción aunque conozca el endpoint.
- [ ] Separación de funciones: quien crea una solicitud de crédito/gasto no puede aprobarla (salvo excepción autorizada).
- [ ] Cierre de período: no se pueden crear ni editar movimientos en un período `CERRADO` sin reapertura autorizada.
- [ ] Histórico de tasas: cambiar una tasa vigente hoy no debe alterar cuotas/intereses ya calculados en el pasado.
- [ ] Conciliación bancaria: los movimientos marcados `conciliado = true` no deben ser editables.

---

## 11. Prompts paso a paso para el agente de programación

Usar en orden, uno por sesión/tarea, adaptando `{módulo}` según la fase en curso. Pensados para pegar directamente en Claude Code u otro agente con acceso al repositorio.

### 11.1 Setup inicial del backend

```
Crea un proyecto Spring Boot 3 con Maven llamado "caja-ahorros-backend".
Dependencias: Spring Web, Spring Data JPA, PostgreSQL Driver, Spring Security,
JJWT (io.jsonwebtoken), Validation, Lombok.
Configura application.yml con datasource PostgreSQL (host, puerto, db, user,
password como variables de entorno) y perfil "dev".
Crea la estructura de paquetes: config, security, modulo (vacío por ahora), shared.
Configura CORS para permitir el origen del frontend Angular en desarrollo.
No implementes lógica de negocio todavía, solo el esqueleto compilable.
```

### 11.2 Setup inicial del frontend

```
Crea un proyecto Angular (standalone components, routing habilitado) llamado
"caja-ahorros-frontend". Configura:
- Interceptor HTTP para adjuntar el JWT en cada request.
- Interceptor de manejo de errores (401 → redirigir a login).
- AuthService con login/logout/refresh contra /api/v1/auth.
- AuthGuard basado en roles/permisos obtenidos del JWT.
- Layout base con sidebar que se arma dinámicamente según permisos del usuario.
No implementes features de negocio todavía, solo el shell de la aplicación.
```

### 11.3 Módulo de Seguridad (Fase 1)

```
Sobre el backend ya creado, implementa el módulo "seguridad" con las tablas
usuarios, roles, permisos, usuario_roles, rol_permisos, sesiones y auditoria
(usa el DDL de la sección 2.1 del PLAN_DESARROLLO_CAJA_AHORROS.md como referencia).
Implementa:
- Entidades JPA y repositorios.
- AuthController con /login (JWT), /refresh, /logout.
- Filtro JWT que puebla el SecurityContext con roles y permisos del usuario.
- Un AuditListener/aspecto que registre en "auditoria" cada creación, edición,
  aprobación o anulación en cualquier entidad marcada como auditable.
- Endpoints CRUD de usuarios y roles, protegidos por permiso "SEGURIDAD:CREAR/EDITAR/VER".
Escribe pruebas unitarias del flujo de login y de la generación de auditoría.
```

### 11.4 Módulo de Socios (Fase 1)

```
Implementa el módulo "socios" (tablas socios, socio_beneficiarios,
socio_documentos de la sección 2.2). Incluye:
- CRUD completo de socios con validación de identificación única.
- Endpoint GET /socios/{id}/estado-cuenta que por ahora retorne solo datos
  del socio (se completará en fases posteriores con aportes/ahorros/créditos).
- Protege cada endpoint con el permiso correspondiente del módulo SOCIOS.
- En el frontend, crea el feature "socios" con listado, formulario de
  creación/edición y vista de detalle con pestañas (Datos, Beneficiarios,
  Documentos, Estado de cuenta).
```

### 11.5 Motor contable (Fase 2)

```
Implementa el módulo "contabilidad" con plan_cuentas, periodo_contable,
asiento_contable, asiento_detalle y regla_contable (sección 2.11 y 2.12).
Requisitos clave:
- Servicio AsientoAutomaticoService con un método
  generarAsiento(operacion: String, comprobanteId: Long, montos: Map<String,BigDecimal>)
  que busque la regla_contable vigente para "operacion" y cree un
  asiento_contable con sus asiento_detalle, validando SUM(debe) = SUM(haber).
- Si no existe una regla_contable vigente para la operación, debe lanzar una
  excepción de negocio clara (no crear el asiento a medias).
- Endpoint para cerrar y reabrir periodo_contable, bloqueando escritura de
  asientos en periodos CERRADOS salvo reapertura autorizada (rol ADMIN).
- Precarga (seed) de la matriz Debe/Haber de la sección 6 de este plan.
Escribe pruebas que verifiquen el cuadre contable para cada operación de la matriz.
```

### 11.6 Módulo de Caja y Bancos (Fase 2)

```
Implementa "caja" y "bancos" (tablas de las secciones 2.9 y 2.10).
Requisitos:
- No se puede registrar ningún caja_movimiento sin una caja_apertura en
  estado ABIERTA para el cajero y la fecha actual.
- Cada caja_movimiento relevante (depósito, retiro, cobro, desembolso)
  debe invocar AsientoAutomaticoService con la operación correspondiente.
- Numeración de comprobantes: secuencial, sin huecos, no reutilizable tras
  una anulación (la anulación genera un comprobante de reverso, no borra el original).
- Endpoint de arqueo que compare saldo_sistema vs saldo_fisico y registre
  la diferencia.
En el frontend, crea la pantalla de apertura/cierre de caja y el punto de
cobro (aportaciones, cuotas, depósitos) como un formulario único de cajero.
```

### 11.7 Módulo de Ahorros (Fase 3)

```
Implementa "ahorros" (producto_ahorro, cuenta_ahorro, movimiento_ahorro,
sección 2.4), aplicando la fórmula de interés de la sección 5.
Requisitos:
- Job/endpoint de capitalización de intereses según periodicidad del
  producto, que genere movimiento_ahorro tipo INTERES y dispare el asiento
  contable correspondiente.
- Depósitos y retiros deben validar saldo_minimo y limite_retiros_mes del producto.
- Todo movimiento_ahorro debe quedar ligado a un comprobante y a su asiento contable.
```

### 11.8 Módulo de Créditos (Fase 4)

```
Implementa "creditos" completo (secciones 2.5 y 5): solicitud → evaluación
→ aprobación → desembolso → tabla_amortizacion (sistema francés) → pago_cuota
→ mora → refinanciamiento.
Requisitos:
- Servicio de generación de tabla_amortizacion que implemente exactamente
  las fórmulas de la sección 5 (sistema francés), como método puro y testeable.
- Endpoint de simulación (/simulador-credito) que reutilice ese mismo cálculo
  sin persistir un crédito real.
- El desembolso debe: crear el registro credito, generar tabla_amortizacion,
  y disparar el asiento DESEMBOLSO_CREDITO.
- El pago de cuota debe: separar capital/interés/mora, actualizar
  tabla_amortizacion.estado, y disparar los asientos PAGO_CAPITAL, PAGO_INTERES
  y PAGO_MORA como corresponda.
- Job diario que marque cuotas vencidas y calcule mora acumulada.
Escribe pruebas que comparen la tabla de amortización generada contra un
cálculo de referencia (Excel o cálculo manual) para al menos 2 escenarios.
```

### 11.9 Cartera, dashboard y reportes (Fase 4–6)

```
Implementa la vista v_cartera (sección 2.8) y los endpoints /cartera y
/cartera/morosidad. Sobre el frontend, crea:
- Un dashboard con tarjetas de indicadores (socios activos, cartera colocada,
  cartera vencida, % morosidad, disponible en caja/bancos) usando los
  endpoints /dashboard/resumen.
- Reportes exportables a PDF/Excel/CSV (usar JasperReports en backend o
  generación server-side) para socios, cartera y caja.
No inventes datos: todos los indicadores deben calcularse desde las tablas
reales, nunca hardcodeados.
```

### 11.10 Pruebas integrales finales (Fase 6)

```
Ejecuta el checklist de la sección 10 de PLAN_DESARROLLO_CAJA_AHORROS.md
contra el sistema completo. Para cada ítem que falle, crea un issue con
pasos de reproducción y corrígelo antes de cerrar la fase. Genera un reporte
final de cobertura de pruebas por módulo.
```

---

**Notas finales**

- Este documento es la fuente de verdad técnica del proyecto; actualízalo cuando el alcance real acordado con la institución difiera de lo aquí descrito.
- Los nombres de tablas/columnas son una propuesta de partida: ajustar únicamente si el levantamiento de requerimientos de la institución exige campos adicionales (p. ej. integraciones con SEPS/organismo de control local).

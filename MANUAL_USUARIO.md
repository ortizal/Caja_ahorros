# Manual de usuario breve — Caja de Ahorros ALANTEK

## 1. Puesta en marcha

1. Iniciar el backend (Spring Boot) en `http://localhost:8080`:
   - `backend\mvnw.cmd spring-boot:run` o ejecutar el JAR compilado (`backend\target\caja-ahorros-backend.jar`).
2. Iniciar el frontend (Angular) en `http://localhost:4200`:
   - `cd frontend && npm start` (dev server con proxy a `:8080`).
3. Abrir `http://localhost:4200` e iniciar sesión con un usuario del listado inferior.

Al arrancar por primera vez, el backend crea la base de datos PostgreSQL `caja_ahorros`, el plan de cuentas, las reglas contables y los datos demo (socios, productos, usuarios).

## 2. Usuarios demo (creados por el seeder)

| Usuario   | Contraseña   | Rol / alcance                                      |
|-----------|--------------|----------------------------------------------------|
| `admin`   | `admin123`   | Administración: socios, seguridad, auditoría, todo |
| `gerente` | `gerente123` | Visión gerencial, aprobación de gastos             |
| `contador`| `contador123`| Contabilidad: períodos, plan de cuentas, asientos  |
| `cajero`  | `cajero123`  | Operación diaria: caja, cobros, aportaciones       |
| `credito` | `credito123` | Créditos: solicitudes, desembolso, cobranza        |
| `socio`   | `socio123`   | Portal del socio (solo lectura) — `SOC-DEMO-01`    |

## 3. Módulos

- **Seguridad:** usuarios, roles y asignación de permisos por módulo+acción; toda operación se registra en **auditoría**.
- **Socios:** alta/edición de socios, estados (ACTIVO/SUSPENDIDO/EGRESADO) y exportación del listado (Excel/PDF).
- **Aportaciones:** configuración del aporte esperado, generación por período y registro de aportes.
- **Ahorros:** productos de ahorro, apertura de cuentas, depósitos y retiros con generación automática de asientos.
- **Créditos:** solicitud → aprobación → desembolso → tabla de amortización (francés, alemán o americano) → cobro de cuota con cálculo de mora; simulador; refinanciamiento. La tab **Cartera** muestra la colocación y morosidad y permite exportar a CSV/Excel/PDF.
- **Caja:** apertura diaria, registro de ingresos/egresos (aportaciones, cuotas, depósitos), arqueo y cierre; exportación del movimiento a Excel/PDF.
- **Bancos:** cuentas bancarias y movimientos (depósitos/retiros).
- **Contabilidad:** plan de cuentas, períodos contables, asientos automáticos derivados de las reglas contables y estado de resultado.
- **Tesorería:** gastos con flujo de aprobación, cuentas por pagar/cobrar y presupuesto vs. ejecución.
- **Notificaciones:** campana en el shell con alertas de cuotas próximas/vencidas, aportaciones pendientes y cierres de caja pendientes (generadas por un job diario).
- **Portal del socio:** acceso de solo lectura (`/portal`) con resumen de ahorro, aportaciones, créditos y notificaciones.
- **Dashboard:** KPIs gerenciales (socios activos, cartera colocada, cartera vencida, % morosidad, disponible en caja/bancos) calculados desde las tablas reales.

## 4. Reportes exportables

Los reportes se generan en el servidor y se descargan con el botón correspondiente:

| Módulo   | CSV | Excel (`.xlsx`) | PDF | Permiso requerido |
|----------|-----|-----------------|-----|-------------------|
| Socios   | Sí  | Sí              | Sí  | `SOCIOS:VER`      |
| Cartera  | Sí* | Sí              | Sí  | `CREDITOS:VER`    |
| Caja     | Sí  | Sí              | Sí  | `CAJA:VER`        |

\* El CSV de cartera se conserva en el endpoint histórico `GET /reportes/cartera`.
Los CSV usan separador `;` y codificación UTF-8 (con BOM) para apertura correcta en Excel.

Botones: tab **Cartera** (créditos), listado de **socios** y detalle de **caja**.

## 5. Notas

- No hay datos inventados: indicadores y reportes se calculan desde las tablas reales.
- El portal del socio es de solo lectura; los módulos administrativos no son accesibles con rol `SOCIO`.
- El checklist de pruebas (sección 10 del plan) está cubierto por pruebas automatizadas (backend 77/77, frontend 87/87, e2e 38/38).

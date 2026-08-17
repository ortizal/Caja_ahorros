# CajaAhorrosFrontend

Frontend Angular (22.x) de la Caja de Ahorros ALANTEK. Consume la API REST
del backend Spring Boot en `http://localhost:8080/api/v1`.

## Desarrollo

```bash
npm start          # sirve en http://localhost:4200 con proxy a :8080
```

## Comandos

```bash
ng build           # build de producción (salida en dist/)
ng test            # unit tests (Vitest)
ng e2e             # e2e (Playwright; requiere backend + npm start activos)
```

## Módulos

`login`, `dashboard`, `seguridad`, `socios`, `aportaciones`, `ahorros`,
`creditos` (incluye tab Cartera con exportación CSV/Excel/PDF), `caja`,
`bancos`, `contabilidad`, `tesoreria`, `notificaciones` y `portal`
(portal del socio, solo lectura).

Reportes exportables: botones en la tab **Cartera** (créditos), en el
listado de **socios** y en el detalle de **caja** (formatos CSV/Excel/PDF).

Ver `MANUAL_USUARIO.md` (raíz del repositorio) para usuarios demo y guía de uso.

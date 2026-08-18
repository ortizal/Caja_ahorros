import { expect, test } from '@playwright/test';
import { asegurarCajaAbierta, interceptLargePage, loginAs, operarConCaja } from './helpers';

const TS = Date.now().toString().slice(-8);
const GASTO_FLUJO = `Gasto flujo ${TS}`;
const GASTO_PENDIENTE = `Gasto pendiente ${TS}`;
const PROVEEDOR = `Proveedor ${TS}`;
const DEUDOR = `Deudor ${TS}`;
const PARTIDA = `Presupuesto E2E ${TS}`;
const CUENTA_GASTO = '5.1.01 — Gastos administrativos';
const CUENTA_CXC = '1.4.01 — Cuentas por cobrar';

test.describe.configure({ mode: 'serial' });

test.describe('Módulo de tesorería', () => {
  test('flujo completo: gasto con aprobación y pago, CxP, CxC y presupuesto', async ({ page }) => {
    await interceptLargePage(page, '**/api/v1/gastos*');
    await interceptLargePage(page, '**/api/v1/cuentas-por-pagar*');
    await interceptLargePage(page, '**/api/v1/cuentas-por-cobrar*');
    await loginAs(page, 'admin', 'admin123');
    await page.goto('/tesoreria');

    // Gastos: crear dos desde el formulario ruteado, aprobar y pagar el primero
    await page.getByTestId('btn-nuevo-gasto').click();
    await page.waitForURL(/\/tesoreria\/gastos\/nuevo/);
    await page.getByTestId('input-gasto-concepto').fill(GASTO_FLUJO);
    await page.getByTestId('input-gasto-descripcion').fill('Compra de materiales');
    await page.getByTestId('input-gasto-monto').fill('100');
    await page.getByTestId('input-gasto-cuenta').selectOption({ label: CUENTA_GASTO });
    await page.getByTestId('btn-crear-gasto').click();
    await expect(page.getByTestId('toast-item').filter({ hasText: 'Gasto solicitado correctamente.' })).toBeVisible();
    await expect(page).toHaveURL(/\/tesoreria$/);

    let fila = page.getByRole('row').filter({ hasText: GASTO_FLUJO });
    await expect(fila).toContainText('PENDIENTE');

    await page.getByTestId('btn-nuevo-gasto').click();
    await page.waitForURL(/\/tesoreria\/gastos\/nuevo/);
    await page.getByTestId('input-gasto-concepto').fill(GASTO_PENDIENTE);
    await page.getByTestId('input-gasto-monto').fill('50');
    await page.getByTestId('input-gasto-cuenta').selectOption({ label: CUENTA_GASTO });
    await page.getByTestId('btn-crear-gasto').click();
    await expect(page).toHaveURL(/\/tesoreria$/);

    fila = page.getByRole('row').filter({ hasText: GASTO_PENDIENTE });
    await expect(fila).toContainText('PENDIENTE');

    fila = page.getByRole('row').filter({ hasText: GASTO_FLUJO });
    await fila.getByTestId('btn-acciones-menu').click();
    await fila.getByTestId('btn-aprobar-gasto').click();
    await expect(fila).toContainText('APROBADO');

    await fila.getByTestId('btn-acciones-menu').click();
    await operarConCaja(page, 'btn-pagar-gasto', 'tesoreria-error', 'admin', 'admin123', fila.getByTestId('btn-acciones-menu'));
    fila = page.getByRole('row').filter({ hasText: GASTO_FLUJO });
    await expect(fila).toContainText('PAGADO');

    // Cuentas por pagar: registrar y pagar
    await page.getByTestId('tesoreria-tabs').getByText('Cuentas por pagar').click();
    await page.getByTestId('btn-nueva-cxp').click();
    await page.waitForURL(/\/tesoreria\/cuentas-pagar\/nuevo/);
    await page.getByTestId('input-cxp-proveedor').fill(PROVEEDOR);
    await page.getByTestId('input-cxp-concepto').fill('Servicios de limpieza');
    await page.getByTestId('input-cxp-monto').fill('200');
    await page.getByTestId('input-cxp-cuenta').selectOption({ label: CUENTA_GASTO });
    await page.getByTestId('input-cxp-vencimiento').fill('2026-12-31');
    await page.getByTestId('btn-crear-cxp').click();
    await expect(page.getByTestId('toast-item').filter({ hasText: 'Cuenta por pagar registrada.' })).toBeVisible();
    await expect(page).toHaveURL(/\/tesoreria$/);

    await page.getByTestId('tesoreria-tabs').getByText('Cuentas por pagar').click();
    let filaCxp = page.getByRole('row').filter({ hasText: PROVEEDOR });
    await expect(filaCxp).toContainText('PENDIENTE');

    await filaCxp.getByTestId('btn-acciones-menu').click();
    await filaCxp.getByTestId('btn-pagar-cxp').click();
    await expect(filaCxp).toContainText('PAGADA');

    // Cuentas por cobrar: registrar y cobrar
    await page.getByTestId('tesoreria-tabs').getByText('Cuentas por cobrar').click();
    await page.getByTestId('btn-nueva-cxc').click();
    await page.waitForURL(/\/tesoreria\/cuentas-cobrar\/nuevo/);
    await page.getByTestId('input-cxc-deudor').fill(DEUDOR);
    await page.getByTestId('input-cxc-concepto').fill('Venta de bienes');
    await page.getByTestId('input-cxc-monto').fill('300');
    await page.getByTestId('input-cxc-cuenta').selectOption({ label: CUENTA_CXC });
    await page.getByTestId('input-cxc-vencimiento').fill('2026-12-31');
    await page.getByTestId('btn-crear-cxc').click();
    await expect(page.getByTestId('toast-item').filter({ hasText: 'Cuenta por cobrar registrada.' })).toBeVisible();
    await expect(page).toHaveURL(/\/tesoreria$/);

    await page.getByTestId('tesoreria-tabs').getByText('Cuentas por cobrar').click();
    let filaCxc = page.getByRole('row').filter({ hasText: DEUDOR });
    await expect(filaCxc).toContainText('PENDIENTE');

    await filaCxc.getByTestId('btn-acciones-menu').click();
    await filaCxc.getByTestId('btn-cobrar-cxc').click();
    await expect(filaCxc).toContainText('COBRADA');

    // Presupuesto: crear partida y ver ejecución reflejando el gasto pagado
    await page.getByTestId('tesoreria-tabs').getByText('Presupuesto').click();
    await page.getByTestId('input-partida-concepto').fill(PARTIDA);
    await page.getByTestId('input-partida-cuenta').selectOption({ label: CUENTA_GASTO });
    await page.getByTestId('input-partida-monto').fill('1000');
    await page.getByTestId('btn-crear-partida').click();

    await expect(page.getByTestId('partidas-table')).toContainText(PARTIDA);
    await expect(page.getByTestId('total-ejecutado')).not.toHaveText('0.00');
  });

  test('gerente aprueba gastos pero no crea ni paga', async ({ page }) => {
    await interceptLargePage(page, '**/api/v1/gastos*');
    await loginAs(page, 'gerente', 'gerente123');
    await page.goto('/tesoreria');

    await expect(page.getByTestId('btn-nuevo-gasto')).toHaveCount(0);

    const fila = page.getByRole('row').filter({ hasText: GASTO_PENDIENTE });
    await expect(fila).toContainText('PENDIENTE');
    await fila.getByTestId('btn-acciones-menu').click();
    await fila.getByTestId('btn-aprobar-gasto').click();
    await expect(fila).toContainText('APROBADO');

    await fila.getByTestId('btn-acciones-menu').click();
    await expect(fila.getByTestId('btn-pagar-gasto')).toHaveCount(0);
  });

  test('cajero paga gastos aprobados pero no aprueba', async ({ page }) => {
    await interceptLargePage(page, '**/api/v1/gastos*');
    await loginAs(page, 'cajero', 'cajero123');
    await page.goto('/tesoreria');

    await expect(page.getByTestId('btn-nuevo-gasto')).toHaveCount(1);

    await asegurarCajaAbierta(page, 'cajero', 'cajero123');
    const fila = page.getByRole('row').filter({ hasText: GASTO_PENDIENTE });
    await expect(fila).toContainText('APROBADO');
    await fila.getByTestId('btn-acciones-menu').click();
    await expect(fila.getByTestId('btn-aprobar-gasto')).toHaveCount(0);
    await fila.getByTestId('btn-pagar-gasto').click();
    await expect(fila).toContainText('PAGADO');
  });

  test('usuario sin permiso no accede al módulo', async ({ page }) => {
    await loginAs(page, 'credito', 'credito123');
    await page.goto('/tesoreria');

    await expect(page).toHaveURL(/\/dashboard/);
    await expect(page.getByRole('link', { name: 'Tesorería' })).toHaveCount(0);
  });
});

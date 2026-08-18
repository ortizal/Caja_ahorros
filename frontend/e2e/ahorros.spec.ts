import { expect, test, type Page } from '@playwright/test';
import { interceptLargePage, loginAs, operarConCaja } from './helpers';

const NOMBRE = `E2E ${Date.now().toString().slice(-5)}`;
const MES = (new Date().getDate() % 12) + 1;
const ANIO = 2031;

test.describe.configure({ mode: 'serial' });

test.describe('Modulo de ahorros', () => {
  test('crear producto, aperturar cuenta, operar y capitalizar intereses', async ({ page }) => {
    await interceptLargePage(page, '**/api/v1/productos-ahorro*');
    await interceptLargePage(page, '**/api/v1/cuentas-ahorro*');
    await loginAs(page, 'admin', 'admin123');
    await page.goto('/ahorros');

    // Producto de ahorro sembrado por el seed
    await expect(page.getByTestId('productos-table')).toContainText('A LA VISTA');

    // Crear un producto nuevo a traves del form ruteado
    await page.getByTestId('btn-nuevo-producto').click();
    await page.waitForURL(/\/ahorros\/productos\/nuevo/);
    await page.getByTestId('input-nombre-producto').fill(NOMBRE);
    await page.getByTestId('input-tasa-producto').fill('3');
    await page.getByTestId('input-saldo-minimo-producto').fill('50');
    await page.getByTestId('btn-crear-producto').click();
    await expect(page.getByTestId('toast-item').filter({ hasText: 'Producto de ahorro creado' })).toBeVisible();
    await page.waitForURL(/\/ahorros$/);
    await expect(page.getByTestId('productos-table')).toContainText(NOMBRE);

    // Aperturar cuenta a traves del form ruteado con el primer socio activo
    await page.getByRole('button', { name: 'Cuentas' }).click();
    await page.getByTestId('btn-nueva-cuenta').click();
    await page.waitForURL(/\/ahorros\/cuentas\/nueva/);
    await page.getByTestId('input-socio-apertura').selectOption({ index: 1 });
    await page.getByTestId('input-producto-apertura').selectOption({ label: NOMBRE });
    await page.getByTestId('btn-aperturar').click();
    await expect(page.getByTestId('toast-item').filter({ hasText: 'Cuenta de ahorro aperturada' })).toBeVisible();
    await page.waitForURL(/\/ahorros$/);
    await page.getByRole('button', { name: 'Cuentas' }).click();
    await expect(page.getByTestId('cuentas-table')).toContainText(NOMBRE);

    // Deposito y retiro sobre la cuenta del producto nuevo
    const filaCuenta = page.getByTestId('cuentas-table').locator('tr').filter({ hasText: NOMBRE });
    await filaCuenta.getByTestId('btn-acciones-menu').click();
    await filaCuenta.getByTestId('btn-operar').click();
    await expect(page.getByTestId('operaciones-panel')).toBeVisible();

    await page.getByTestId('input-deposito').fill('100');
    await operarConCaja(page, 'btn-depositar', 'ahorros-error');
    await expect(page.getByTestId('movimientos-table')).toContainText('DEPOSITO');
    await expect(page.getByTestId('operaciones-panel')).toContainText('100.00');

    await page.getByTestId('input-retiro').fill('30');
    await operarConCaja(page, 'btn-retirar', 'ahorros-error');
    await expect(page.getByTestId('movimientos-table')).toContainText('RETIRO');
    await expect(page.getByTestId('operaciones-panel')).toContainText('70.00');

    // Capitalizar intereses del periodo
    await page.getByRole('button', { name: 'Intereses' }).click();
    await page.getByTestId('input-anio-capitalizar').fill(String(ANIO));
    await page.getByTestId('input-mes-capitalizar').fill(String(MES));
    await page.getByTestId('btn-capitalizar').click();
    await expect(
      page.getByTestId('capitalizacion-resultado').or(page.getByTestId('ahorros-error'))
    ).toContainText(/Cuentas capitalizadas|ya fue realizada/);
  });

  test('auditor solo consulta ahorros', async ({ page }) => {
    await loginAs(page, 'auditor', 'auditor123');
    await page.goto('/ahorros');

    await expect(page.getByTestId('productos-table')).toBeVisible();
    await expect(page.getByTestId('btn-nuevo-producto')).toHaveCount(0);
    await expect(page.getByTestId('btn-nueva-cuenta')).toHaveCount(0);
  });
});

import { expect, test } from '@playwright/test';
import { interceptLargePage, loginAs } from './helpers';

const NUMERO_CUENTA = `9${Date.now().toString().slice(-8)}`;

test.describe.configure({ mode: 'serial' });

test.describe('Módulo de bancos', () => {
  test('crear cuenta, registrar movimiento y conciliar', async ({ page }) => {
    await interceptLargePage(page, '**/api/v1/cuentas-bancarias*');
    await loginAs(page, 'admin', 'admin123');
    await page.goto('/bancos');

    // Crear cuenta bancaria desde el formulario ruteado
    await page.getByTestId('btn-nueva-cuenta').click();
    await expect(page).toHaveURL(/\/bancos\/nuevo/);
    await page.getByTestId('input-banco').fill('Banco E2E');
    await page.getByTestId('input-numero-cuenta').fill(NUMERO_CUENTA);
    await page.getByTestId('input-tipo-cuenta').selectOption('CORRIENTE');
    await page.getByTestId('btn-crear-cuenta').click();

    await expect(page).toHaveURL(/\/bancos$/);
    await expect(page.getByTestId('toast-item').filter({ hasText: NUMERO_CUENTA })).toContainText('creada correctamente.');

    const fila = page.getByRole('row').filter({ hasText: NUMERO_CUENTA });
    await expect(fila).toHaveCount(1);

    // Ver detalle
    await page.getByRole('row').filter({ hasText: NUMERO_CUENTA }).getByTestId('btn-acciones-menu').click();
    await page.getByRole('row').filter({ hasText: NUMERO_CUENTA }).getByRole('link', { name: 'Ver' }).click();
    await expect(page).toHaveURL(/\/bancos\/\d+$/);

    // Registrar depósito de 500
    await expect(page.getByTestId('cuenta-saldo-contable')).toHaveText('0.00');
    await page.getByTestId('input-tipo-banco').selectOption('DEPOSITO');
    await page.getByTestId('input-monto-banco').fill('500');
    await page.getByTestId('btn-registrar-banco').click();

    await expect(page.getByTestId('cuenta-saldo-contable')).toHaveText('500.00');
    await expect(page.getByTestId('movimientos-banco-table')).toContainText('DEPOSITO');

    // Conciliar sin diferencia
    await page.getByTestId('input-saldo-bancario').fill('500');
    await page.getByTestId('btn-conciliar').click();
    await expect(page.getByTestId('conciliacion-resultado')).toContainText('0.00');
  });

  test('auditor no puede crear cuenta bancaria', async ({ page }) => {
    await loginAs(page, 'auditor', 'auditor123');
    await page.goto('/bancos');

    await expect(page.getByTestId('btn-nueva-cuenta')).toHaveCount(0);
  });
});

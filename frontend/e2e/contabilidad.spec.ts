import { expect, test, type Page } from '@playwright/test';
import { interceptLargePage, loginAs } from './helpers';

const CODIGO = `E2E${Date.now().toString().slice(-5)}`;

async function cuentaAceptaMovimiento(page: Page): Promise<{ codigo: string; nombre: string }> {
  const login = await page.request.post('http://localhost:8080/api/v1/auth/login', {
    data: { username: 'admin', password: 'admin123' }
  });
  const { token } = (await login.json()) as { token: string };
  const res = await page.request.get('http://localhost:8080/api/v1/plan-cuentas?size=100', {
    headers: { Authorization: `Bearer ${token}` }
  });
  const { content: cuentas } = (await res.json()) as { content: { codigo: string; nombre: string; aceptaMovimiento: boolean }[] };
  const cuenta = cuentas.find((c) => c.aceptaMovimiento)!;
  return { codigo: cuenta.codigo, nombre: cuenta.nombre };
}

test.describe.configure({ mode: 'serial' });

test.describe('Módulo de contabilidad', () => {
  test('crear cuenta, registrar asiento manual y consultar balance', async ({ page }) => {
    await interceptLargePage(page, '**/api/v1/plan-cuentas*');
    await interceptLargePage(page, '**/api/v1/libro-diario*');
    await interceptLargePage(page, '**/api/v1/balance-comprobacion*');
    await loginAs(page, 'admin', 'admin123');
    await page.goto('/contabilidad');

    // Periodos
    await expect(page.getByTestId('periodos-table')).toContainText('ABIERTO');

    // Plan de cuentas: crear cuenta (form ruteado)
    await page.getByRole('button', { name: 'Plan de cuentas' }).click();
    await page.getByTestId('btn-nueva-cuenta').click();
    await expect(page).toHaveURL(/\/contabilidad\/cuentas\/nuevo/);
    await expect(page.getByTestId('cuenta-form')).toBeVisible();
    await page.getByTestId('input-codigo-cuenta').fill(CODIGO);
    await page.getByTestId('input-nombre-cuenta').fill('Cuenta E2E');
    await page.getByTestId('btn-crear-cuenta').click();
    await expect(page.getByTestId('toast-item').filter({ hasText: 'Cuenta contable creada' })).toBeVisible();
    await expect(page).toHaveURL(/\/contabilidad$/);
    await page.getByRole('button', { name: 'Plan de cuentas' }).click();
    await expect(page.getByTestId('plan-table')).toContainText(CODIGO);

    // Asiento manual cuadradado (form ruteado)
    await page.getByRole('button', { name: 'Asiento manual' }).click();
    await page.getByTestId('btn-nuevo-asiento').click();
    await expect(page).toHaveURL(/\/contabilidad\/asientos\/nuevo/);
    await expect(page.getByTestId('asiento-form')).toBeVisible();
    const cuentaA = await cuentaAceptaMovimiento(page);
    await page.getByTestId('input-asiento-fecha').fill('2026-08-12');
    await page.getByTestId('input-asiento-descripcion').fill(`Asiento E2E ${CODIGO}`);

    await page.getByTestId('input-asiento-cuenta-0').selectOption({ label: `${cuentaA.codigo} — ${cuentaA.nombre}` });
    await page.getByTestId('input-asiento-debe-0').fill('150');
    await page.getByTestId('input-asiento-cuenta-1').selectOption({ label: `${cuentaA.codigo} — ${cuentaA.nombre}` });
    await page.getByTestId('input-asiento-haber-1').fill('150');
    await page.getByTestId('btn-registrar-asiento').click();

    await expect(page.getByTestId('toast-item').filter({ hasText: 'Asiento registrado' })).toBeVisible();
    await expect(page).toHaveURL(/\/contabilidad$/);

    // Libro diario lo muestra
    await page.getByRole('button', { name: 'Libro diario' }).click();
    await page.getByTestId('btn-consultar-diario').click();
    await expect(page.getByTestId('asiento').filter({ hasText: `Asiento E2E ${CODIGO}` })).toBeVisible();

    // Balance cuadradado
    await page.getByRole('button', { name: 'Balance' }).click();
    await page.getByTestId('btn-consultar-balance').click();
    const debe = await page.getByTestId('balance-total-debe').textContent();
    const haber = await page.getByTestId('balance-total-haber').textContent();
    expect(debe).toBe(haber);
  });

  test('auditor solo consulta contabilidad', async ({ page }) => {
    await loginAs(page, 'auditor', 'auditor123');
    await page.goto('/contabilidad');

    await expect(page).toHaveURL(/\/contabilidad/);
    await expect(page.getByTestId('periodos-table')).toBeVisible();
    await expect(page.getByTestId('btn-nueva-cuenta')).toHaveCount(0);
    await expect(page.getByTestId('btn-nuevo-asiento')).toHaveCount(0);
  });
});

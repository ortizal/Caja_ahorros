import { expect, test, type Locator, type Page } from '@playwright/test';
import { asegurarCajaAbierta, loginAs } from './helpers';

const NOMBRE = `E2E CR ${Date.now().toString().slice(-5)}`;

interface SocioApi {
  id: number;
  codigo: string;
  nombres: string;
  apellidos: string;
  estado: string;
}

async function socioDisponible(page: Page): Promise<SocioApi> {
  const login = await page.request.post('http://localhost:8080/api/v1/auth/login', {
    data: { username: 'admin', password: 'admin123' }
  });
  const { token } = (await login.json()) as { token: string };
  const headers = { Authorization: `Bearer ${token}` };

  const res = await page.request.post('http://localhost:8080/api/v1/socios', {
    headers,
    data: {
      identificacion: `E2E${Date.now()}`,
      nombres: 'PRUEBA',
      apellidos: `CRED${Date.now().toString().slice(-4)}`,
      fechaIngreso: '2026-08-12'
    }
  });
  if (!res.ok()) {
    throw new Error(`No se pudo crear el socio de prueba: ${await res.text()}`);
  }
  return (await res.json()) as SocioApi;
}

async function clicarConCaja(page: Page, boton: Locator, abrirMenu?: Locator): Promise<void> {
  await boton.click();
  const error = page.getByTestId('creditos-error');
  const visible = await error.waitFor({ state: 'visible', timeout: 1500 }).then(() => true).catch(() => false);
  if (visible && /caja/i.test((await error.textContent()) ?? '')) {
    await asegurarCajaAbierta(page);
    if (abrirMenu) {
      await abrirMenu.click();
    }
    await boton.click();
  }
}

test.describe.configure({ mode: 'serial' });

test.describe('Modulo de creditos', () => {
  test('crear producto, solicitar, evaluar, aprobar, desembolsar, cobrar cuota, mora y refinanciar', async ({ page }) => {
    const socio = await socioDisponible(page);

    // El cajero (TESORERO) registra el producto y la solicitud
    await loginAs(page, 'cajero', 'cajero123');
    await page.goto('/creditos');

    // Producto sembrado por el seed
    await expect(page.getByTestId('productos-table')).toContainText('CREDITO PERSONAL');

    // Crear un producto nuevo desde el formulario ruteado
    await page.getByTestId('btn-nuevo-producto').click();
    await page.waitForURL(/\/creditos\/productos\/nuevo/);
    await page.getByTestId('input-nombre-producto').fill(NOMBRE);
    await page.getByTestId('input-tasa-producto').fill('20');
    await page.getByTestId('input-tasa-mora-producto').fill('1.5');
    await page.getByTestId('input-plazo-producto').fill('48');
    await page.getByTestId('input-monto-min-producto').fill('100');
    await page.getByTestId('input-monto-max-producto').fill('10000');
    await page.getByTestId('btn-crear-producto').click();
    await expect(page.getByTestId('toast-item').filter({ hasText: 'Producto de credito creado' })).toBeVisible();
    await expect(page).toHaveURL(/\/creditos$/);
    await expect(page.getByTestId('productos-table')).toContainText(NOMBRE);

    // Simulador
    await page.getByRole('button', { name: 'Simulador' }).click();
    await page.getByTestId('input-monto-simulador').fill('1000');
    await page.getByTestId('input-plazo-simulador').fill('12');
    await page.getByTestId('input-tasa-simulador').fill('18');
    await page.getByTestId('btn-simular').click();
    await expect(page.getByTestId('simulacion-resultado')).toContainText('Cuota mensual');

    // Solicitar credito para un socio libre desde el formulario ruteado
    await page.getByRole('button', { name: 'Solicitudes' }).click();
    await page.getByTestId('btn-nueva-solicitud').click();
    await page.waitForURL(/\/creditos\/solicitudes\/nuevo/);
    await page.getByTestId('input-socio-solicitud').selectOption({ label: `${socio.codigo} - ${socio.nombres} ${socio.apellidos}` });
    await page.getByTestId('input-producto-solicitud').selectOption({ label: NOMBRE });
    await page.getByTestId('input-monto-solicitud').fill('1000');
    await page.getByTestId('input-plazo-solicitud').fill('12');
    await page.getByTestId('input-destino-solicitud').fill('COMPRA');
    await page.getByTestId('btn-crear-solicitud').click();
    await expect(page.getByTestId('toast-item').filter({ hasText: 'Solicitud de credito registrada' })).toBeVisible();
    await expect(page).toHaveURL(/\/creditos$/);

    await page.getByRole('button', { name: 'Solicitudes' }).click();
    const filaSolicitud = page.getByTestId('solicitudes-table').locator('tr').filter({ hasText: NOMBRE });
    await expect(filaSolicitud).toContainText('PENDIENTE');

    // El analista/admin evalua y aprueba (no puede evaluar su propia solicitud)
    await page.getByRole('button', { name: 'Salir' }).click();
    await loginAs(page, 'admin', 'admin123');
    await page.goto('/creditos');
    await page.getByRole('button', { name: 'Solicitudes' }).click();
    await expect(filaSolicitud).toContainText('PENDIENTE');

    // Evaluar y aprobar
    await filaSolicitud.getByTestId('btn-acciones-menu').click();
    await filaSolicitud.getByTestId('btn-evaluar').click();
    await expect(filaSolicitud).toContainText('EVALUACION');
    await filaSolicitud.getByTestId('btn-acciones-menu').click();
    await filaSolicitud.getByTestId('btn-aprobar').click();
    await expect(filaSolicitud).toContainText('APROBADA');

    // Desembolsar el credito del producto nuevo
    await page.getByRole('button', { name: 'Creditos' }).click();
    const filaCredito = page.getByTestId('creditos-table').locator('tr').filter({ hasText: NOMBRE });
    await expect(filaCredito).toContainText('APROBADA');
    await filaCredito.getByTestId('btn-acciones-menu').click();
    await clicarConCaja(page, filaCredito.getByTestId('btn-desembolsar'), filaCredito.getByTestId('btn-acciones-menu'));
    await expect(filaCredito).toContainText('VIGENTE');

    // Detalle con tabla de amortizacion de 12 cuotas
    await filaCredito.getByTestId('btn-acciones-menu').click();
    await filaCredito.getByTestId('btn-detalle').click();
    await expect(page.getByTestId('detalle-panel')).toBeVisible();
    const cuotas = page.getByTestId('detalle-panel').getByTestId('cuotas-table').locator('tbody tr');
    await expect(cuotas).toHaveCount(12);

    // Cobrar la primera cuota
    const pagarBtn = cuotas.first().getByTestId('btn-pagar-cuota');
    await clicarConCaja(page, pagarBtn);
    await expect(cuotas.first()).toContainText('PAGADA');
    await expect(page.getByTestId('detalle-panel').getByTestId('pagos-table').locator('tbody tr')).toHaveCount(1);

    // Refinanciar: el plan vigente crece y la cuota pagada se conserva
    await page.getByTestId('input-plazo-refinanciar').fill('24');
    await page.getByTestId('btn-refinanciar').click();
    const cuotasRefinanciadas = page.getByTestId('detalle-panel').getByTestId('cuotas-table');
    await expect.poll(async () => cuotasRefinanciadas.locator('tbody tr').count()).toBeGreaterThan(20);
    await expect(cuotasRefinanciadas).toContainText('PAGADA');

    // Procesar vencidas (idempotente)
    await page.getByTestId('btn-procesar-mora').click();
    await expect(page.getByTestId('mora-resultado')).toBeVisible();
  });

  test('auditor solo consulta creditos', async ({ page }) => {
    await loginAs(page, 'auditor', 'auditor123');
    await page.goto('/creditos');

    await expect(page.getByTestId('productos-table')).toBeVisible();
    await expect(page.getByTestId('btn-nuevo-producto')).toHaveCount(0);
    await expect(page.getByTestId('btn-nueva-solicitud')).toHaveCount(0);
  });

  test('cartera muestra cuotas, filtra y exporta CSV', async ({ page }) => {
    await loginAs(page, 'admin', 'admin123');
    await page.goto('/creditos');

    await page.getByRole('button', { name: 'Cartera' }).click();
    await expect(page.getByTestId('cartera-panel')).toBeVisible();
    await expect(page.getByTestId('cartera-resumen')).toContainText('Cartera colocada');
    await expect(page.getByTestId('cartera-table')).toBeVisible();

    // Filtro por estado
    await page.getByTestId('cartera-filtro-vencida').click();
    await expect(page.getByTestId('cartera-table')).toBeVisible();
    await page.getByTestId('cartera-filtro-todos').click();
    await expect(page.getByTestId('cartera-table')).toBeVisible();

    // Exportar CSV
    const [download] = await Promise.all([
      page.waitForEvent('download'),
      page.getByTestId('btn-exportar-cartera').click()
    ]);
    expect(download.suggestedFilename()).toBe('cartera.csv');

    // Exportar Excel
    const [xlsx] = await Promise.all([
      page.waitForEvent('download'),
      page.getByTestId('btn-exportar-cartera-xlsx').click()
    ]);
    expect(xlsx.suggestedFilename()).toBe('cartera.xlsx');

    // Exportar PDF
    const [pdf] = await Promise.all([
      page.waitForEvent('download'),
      page.getByTestId('btn-exportar-cartera-pdf').click()
    ]);
    expect(pdf.suggestedFilename()).toBe('cartera.pdf');
  });
});

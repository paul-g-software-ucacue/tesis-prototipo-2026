// F14-H01 en un navegador, con la API simulada: el Responsable designado ve su firma pendiente y
// descarga el PDF vigente; sin pendientes, lo dice; un 403 en la descarga muestra el aviso; y la
// pantalla es accesible con axe-core (WCAG 2.1 AA) con pendientes y sin ellos.
import AxeBuilder from '@axe-core/playwright';
import { expect, test, type Page } from '@playwright/test';
import { ingresar, simularApi } from './simulacion';

const DESIGNADO = {
  estado: 200,
  cuerpo: {
    usuario: 'docente.demo',
    nombre: 'Docente (demo)',
    roles: ['DOCENTE', 'RESPONSABLE_CONSOLIDACION'],
    permisos: ['DISTRIBUTIVO:FIRMA', 'CONSOLIDACION:LECTURA', 'MI_DISTRIBUTIVO:LECTURA'],
  },
};

const DOCUMENTO = '3f2504e0-4f89-41d3-9a0c-0305e82c3301';
const PENDIENTE = {
  documento: DOCUMENTO,
  periodo: '2026-I',
  version: 1,
  orden: 1,
  rotulo: 'Elaborado por',
  generadoEn: '2026-11-14T08:43:00Z',
};

async function sinFallas(pagina: Page) {
  const resultado = await new AxeBuilder({ page: pagina })
    .withTags(['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa'])
    .analyze();
  const fallas = resultado.violations.map(
    (falla) =>
      `${falla.id} (${falla.impact}): ${falla.nodes.map((nodo) => nodo.target.join(' ')).join(', ')}`,
  );
  expect(fallas, fallas.join('\n')).toEqual([]);
}

async function abrir(pagina: Page, pendientes: object[] = [PENDIENTE]) {
  await simularApi(pagina, { sesion: DESIGNADO });
  await pagina.route('**/api/firma/pendientes', (ruta) => ruta.fulfill({ json: pendientes }));
  await pagina.goto('/iniciar-sesion');
  await ingresar(pagina, 'docente.demo');
  await pagina.getByRole('link', { name: 'Pendientes de firma' }).click();
  await expect(
    pagina.getByRole('heading', { level: 1, name: 'Pendientes de firma' }),
  ).toBeVisible();
}

test('el designado ve su firma pendiente y descarga el PDF vigente', async ({ page }) => {
  await page.route(`**/api/firma/documentos/${DOCUMENTO}/archivo`, (ruta) =>
    ruta.fulfill({
      status: 200,
      contentType: 'application/pdf',
      headers: {
        'Content-Disposition': 'attachment; filename="distributivo-2026-I-v1.pdf"',
        ETag: `"${'c'.repeat(64)}"`,
      },
      body: '%PDF-1.7 consolidado',
    }),
  );
  await abrir(page);

  const tabla = page.getByRole('table', { name: 'Firmas pendientes' });
  const fila = tabla.getByRole('row').filter({ hasText: '2026-I' });
  await expect(fila).toContainText('Elaborado por');
  await sinFallas(page);

  const descarga = page.waitForEvent('download');
  await fila.getByRole('button', { name: 'Descargar el PDF de 2026-I, versión 1' }).click();
  expect((await descarga).suggestedFilename()).toBe('distributivo-2026-I-v1.pdf');
});

test('sin pendientes, lo dice y es accesible', async ({ page }) => {
  await abrir(page, []);

  await expect(page.getByText('No tienes firmas pendientes.')).toBeVisible();
  await sinFallas(page);
});

test('si la descarga responde 403, muestra el aviso común', async ({ page }) => {
  await page.route(`**/api/firma/documentos/${DOCUMENTO}/archivo`, (ruta) =>
    ruta.fulfill({
      status: 403,
      json: {
        titulo: 'Acceso no autorizado',
        descripcion: 'No tienes permiso para esta operación.',
        codigo: 'SEGURIDAD_ACCESO_NO_AUTORIZADO',
      },
    }),
  );
  await abrir(page);

  await page.getByRole('button', { name: 'Descargar el PDF de 2026-I, versión 1' }).click();

  await expect(page.getByText('Acceso no autorizado')).toBeVisible();
  await sinFallas(page);
});

// F19-H01 en un navegador, con la API simulada: la Secretaría general académica genera el CSV para
// SIGA, descarga la plantilla, carga un layout válido y uno con problemas (US1, US3); el Rector no
// ve la opción (FR-019); y la pantalla no tiene fallas de accesibilidad con axe-core en sus tres
// estados (WCAG 2.1 AA, SC-010).
import AxeBuilder from '@axe-core/playwright';
import { expect, test, type Page } from '@playwright/test';
import { ingresar, simularApi } from './simulacion';

const SECRETARIA = {
  estado: 200,
  cuerpo: {
    usuario: 'secretaria.demo',
    nombre: 'Secretaría (demo)',
    roles: ['SECRETARIA_GENERAL'],
    permisos: ['DISTRIBUTIVO:LECTURA', 'REPORTES:LECTURA', 'PUBLICACION:APROBACION'],
  },
};

const RECTOR = {
  estado: 200,
  cuerpo: {
    usuario: 'rectorado.demo',
    nombre: 'Rectorado (demo)',
    roles: ['RECTOR'],
    permisos: ['DISTRIBUTIVO:LECTURA', 'REPORTES:LECTURA'],
  },
};

const HASH = 'a'.repeat(64);
const PUBLICACION = {
  periodo: '2026-I',
  version: 1,
  hash: HASH,
  publicadaEn: '2026-11-18T15:00:00Z',
  publicadaPor: 'secretaria.demo',
  enReforma: false,
};
const COLUMNAS = [
  { nombre: 'NUMERO_IDENTIFICACION', campo: 'identificacion' },
  { nombre: 'COMPONENTE', campo: 'componente' },
  { nombre: 'HORAS_SEMANALES', campo: 'horas' },
];
function version(numero: number, traducida = false) {
  return {
    numero,
    porDefecto: false,
    sha256: HASH,
    cargadoPor: 'secretaria.demo',
    cargadoEn: '2026-11-22T15:00:00Z',
    columnas: traducida
      ? COLUMNAS.map((una) =>
          una.campo === 'componente'
            ? {
                ...una,
                traduccion: [
                  { codigo: 'CLASE', valor: '01' },
                  { codigo: 'OTRAS_DOCENCIA', valor: '02' },
                  { codigo: 'INVESTIGACION', valor: '03' },
                  { codigo: 'VINCULACION', valor: '04' },
                  { codigo: 'GESTION', valor: '05' },
                ],
              }
            : una,
        )
      : COLUMNAS,
  };
}
const POR_DEFECTO = { numero: 0, porDefecto: true, sha256: HASH, columnas: COLUMNAS };

async function simularSiga(pagina: Page) {
  let layouts: unknown = { vigente: POR_DEFECTO, anteriores: [] };
  await pagina.route('**/api/reporting/publicaciones', (ruta) =>
    ruta.fulfill({ json: [PUBLICACION] }),
  );
  await pagina.route('**/api/reporting/periodos/2026-I/csv-siga', (ruta) =>
    ruta.fulfill({
      status: 200,
      contentType: 'text/csv; charset=UTF-8',
      headers: { 'Content-Disposition': 'attachment; filename="siga-2026-I-v1.csv"' },
      body: 'NUMERO_IDENTIFICACION,COMPONENTE,HORAS_SEMANALES\r\n',
    }),
  );
  await pagina.route('**/api/reporting/layout-siga/plantilla.yaml', (ruta) =>
    ruta.fulfill({
      status: 200,
      contentType: 'text/yaml; charset=UTF-8',
      headers: { 'Content-Disposition': 'attachment; filename="layout-siga-plantilla.yaml"' },
      body: '# Layout de exportación\n',
    }),
  );
  await pagina.route('**/api/reporting/layout-siga', async (ruta) => {
    if (ruta.request().method() === 'GET') {
      await ruta.fulfill({ json: layouts });
      return;
    }
    const cuerpo = ruta.request().postDataBuffer()?.toString('utf8') ?? '';
    if (cuerpo.includes('horas_semanales')) {
      await ruta.fulfill({
        status: 400,
        json: {
          titulo: 'Archivo de mapeo con problemas',
          descripcion:
            'Corrige lo que se indica en cada columna y vuelve a cargar el archivo. El layout vigente no cambió.',
          codigo: 'REPORTING_LAYOUT_INVALIDO',
          detalles: [
            {
              campo: 'columnas[3] (HORAS)',
              descripcion:
                'El campo «horas_semanales» no existe. Usa uno de la lista de la plantilla.',
            },
            {
              campo: 'columnas[4] (PERIODO)',
              descripcion:
                'El nombre «PERIODO» ya está en columnas[1]. Cada columna necesita un nombre distinto.',
            },
          ],
        },
      });
      return;
    }
    layouts = { vigente: version(1, true), anteriores: [] };
    await ruta.fulfill({ json: version(1, true) });
  });
  return {
    conAnteriores: () => {
      layouts = { vigente: version(2, true), anteriores: [version(1)] };
    },
  };
}

async function abrirComoSecretaria(pagina: Page) {
  await simularApi(pagina, { sesion: SECRETARIA });
  const simulada = await simularSiga(pagina);
  await pagina.goto('/iniciar-sesion');
  await ingresar(pagina, SECRETARIA.cuerpo.usuario);
  await pagina.getByRole('link', { name: 'Exportación a SIGA' }).click();
  await expect(pagina.getByRole('heading', { level: 1, name: 'Exportación a SIGA' })).toBeVisible();
  await expect(pagina.getByRole('table', { name: 'Layout vigente' })).toBeVisible();
  return simulada;
}

async function cargar(pagina: Page, contenido: string) {
  await pagina.getByLabel('Cargar archivo de mapeo').setInputFiles({
    name: 'layout.yaml',
    mimeType: 'text/yaml',
    buffer: Buffer.from(contenido, 'utf8'),
  });
}

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

test.describe('exportación a SIGA', () => {
  test('la Secretaría genera el CSV para SIGA y descarga la plantilla (US1)', async ({ page }) => {
    await abrirComoSecretaria(page);
    await expect(page.getByText('Se generará con el layout por defecto.').first()).toBeVisible();

    const csv = page.waitForEvent('download');
    await page.getByRole('button', { name: 'Generar CSV para SIGA' }).click();
    expect((await csv).suggestedFilename()).toBe('siga-2026-I-v1.csv');
    await expect(page.getByText('Se generó siga-2026-I-v1.csv.')).toBeVisible();

    const plantilla = page.waitForEvent('download');
    await page.getByRole('button', { name: 'Descargar plantilla' }).click();
    expect((await plantilla).suggestedFilename()).toBe('layout-siga-plantilla.yaml');
  });

  test('la Secretaría carga un layout con problemas y después uno válido (US3)', async ({
    page,
  }) => {
    await abrirComoSecretaria(page);

    await cargar(page, 'columnas:\n  - {nombre: HORAS, campo: horas_semanales}\n');
    const titulo = page.getByRole('heading', {
      name: 'El archivo tiene 2 problemas. El layout vigente no cambió.',
    });
    await expect(titulo).toBeFocused();
    await expect(page.getByRole('alert')).toContainText('columnas[3] (HORAS)');
    await sinFallas(page);

    await cargar(page, 'columnas: []\n');
    await expect(
      page.getByText(
        'Se cargó la versión 1 del layout de exportación. El próximo CSV saldrá con sus columnas.',
      ),
    ).toBeVisible();
    await expect(page.getByRole('table', { name: 'Layout vigente' })).toContainText('CLASE → 01');
  });

  test('las versiones anteriores se expanden y la pantalla no tiene fallas (SC-010)', async ({
    page,
  }) => {
    await simularApi(page, { sesion: SECRETARIA });
    const simulada = await simularSiga(page);
    simulada.conAnteriores();
    await page.goto('/iniciar-sesion');
    await ingresar(page, SECRETARIA.cuerpo.usuario);
    await page.getByRole('link', { name: 'Exportación a SIGA' }).click();

    await expect(page.getByRole('button', { name: 'Generar CSV para SIGA' })).toBeVisible();
    await sinFallas(page);
    await page.getByRole('button', { name: 'Ver las columnas de la versión 1' }).click();
    await expect(page.getByRole('table', { name: 'Columnas de la versión 1' })).toBeVisible();
    await sinFallas(page);
  });

  test('el Rector no ve la opción ni puede abrir la pantalla (FR-019)', async ({ page }) => {
    await simularApi(page, { sesion: RECTOR });
    await simularSiga(page);
    await page.goto('/iniciar-sesion');
    await ingresar(page, RECTOR.cuerpo.usuario);

    await expect(page.getByRole('link', { name: 'Reportes' })).toBeVisible();
    await expect(page.getByRole('link', { name: 'Exportación a SIGA' })).toHaveCount(0);
  });
});

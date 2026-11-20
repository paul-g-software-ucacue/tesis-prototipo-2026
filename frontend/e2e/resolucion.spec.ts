// F15-H01 en un navegador, con la API simulada: la Secretaría general abre «Resolución del OCS»,
// registra la resolución con el teclado, confirma, ve la constancia y la descarga. axe-core (WCAG
// 2.1 AA) sin fallas en EN_OCS, con la confirmación abierta y en APROBADO. F18-H01: publica con el
// teclado, confirma y ve la constancia; axe-core con el diálogo abierto y en PUBLICADO.
import AxeBuilder from '@axe-core/playwright';
import { expect, test, type Page } from '@playwright/test';
import { ingresar, simularApi } from './simulacion';

const SECRETARIA = {
  estado: 200,
  cuerpo: {
    usuario: 'secretaria.demo',
    nombre: 'Secretaría general académica (demo)',
    roles: ['SECRETARIA_GENERAL'],
    permisos: [
      'DISTRIBUTIVO:LECTURA',
      'RESOLUCION_OCS:LECTURA',
      'RESOLUCION_OCS:ESCRITURA',
      'PUBLICACION:APROBACION',
    ],
  },
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

test('la Secretaría registra la resolución con el teclado, la ve y la descarga', async ({
  page,
}) => {
  let aprobado = false;
  let registros = 0;
  let enviado = '';
  const flujo = () => ({
    periodo: '2026-2',
    etapa: aprobado ? 'APROBADO' : 'EN_OCS',
    nombreDeEtapa: aprobado ? 'Aprobado' : 'Resolución del OCS',
    version: 1,
    parciales: [{ carrera: 'TDS', enviado: true, pendienteDeConfirmar: false }],
    acciones: aprobado ? [] : ['REGISTRAR_RESOLUCION'],
    transiciones: [],
    ...(aprobado
      ? {
          resolucion: {
            numero: 'RPC-SO-12-No.123-2026',
            fecha: '2026-11-14',
            registradaPor: 'secretaria.demo',
            registradaEn: '2026-11-15T15:30:00Z',
            sha256: 'b'.repeat(64),
            nombre: 'resolucion.pdf',
            tamano: 2048,
          },
        }
      : {}),
  });
  await simularApi(page, { sesion: SECRETARIA });
  await page.route('**/api/academico/periodos', (ruta) =>
    ruta.fulfill({
      json: [
        {
          codigo: '2026-2',
          nombre: 'Periodo 2026-2',
          inicio: '2026-09-01',
          fin: '2027-02-28',
          estado: 'PLANIFICACION',
        },
      ],
    }),
  );
  await page.route('**/api/academico/carreras', (ruta) => ruta.fulfill({ json: [] }));
  await page.route('**/api/validacionocs/periodos/*/flujo', (ruta) =>
    ruta.fulfill({ json: flujo() }),
  );
  await page.route('**/api/validacionocs/periodos/*/resolucion', async (ruta) => {
    registros++;
    enviado = ruta.request().postData() ?? '';
    aprobado = true;
    await ruta.fulfill({ json: flujo() });
  });
  await page.route('**/api/validacionocs/periodos/*/resolucion/archivo', (ruta) =>
    ruta.fulfill({
      body: '%PDF-1.7\n%%EOF\n',
      headers: {
        'Content-Type': 'application/pdf',
        'Content-Disposition': 'attachment; filename="resolucion.pdf"',
      },
    }),
  );

  await page.goto('/iniciar-sesion');
  await ingresar(page, 'secretaria.demo');
  await page.getByRole('link', { name: 'Resolución del OCS' }).click();
  await expect(page.getByRole('heading', { level: 1, name: 'Resolución del OCS' })).toBeVisible();
  const estado = page.getByRole('region', { name: 'Estado del distributivo' });
  await expect(estado).toContainText('Resolución del OCS · versión 1');
  await sinFallas(page);

  // Con el teclado: el número, la fecha y el PDF.
  await page.getByLabel('Número de la resolución').focus();
  await page.keyboard.type('RPC-SO-12-No.123-2026');
  await page.getByLabel('Fecha de la resolución').fill('2026-11-14');
  await page.getByLabel('PDF de la resolución').setInputFiles({
    name: 'resolucion.pdf',
    mimeType: 'application/pdf',
    buffer: Buffer.from('%PDF-1.7\n%%EOF\n'),
  });
  await expect(estado).toContainText('resolucion.pdf');
  await page.getByRole('button', { name: 'Registrar la resolución' }).focus();
  await page.keyboard.press('Enter');
  const dialogo = page.getByRole('dialog', { name: 'Registrar la resolución del OCS' });
  await expect(dialogo).toContainText(
    'El distributivo quedará aprobado con esta resolución. Después no se corrige.',
  );
  await expect(dialogo.getByRole('button', { name: 'Cancelar' })).toBeFocused();
  // axe mide el contraste: primero, que termine la animación de apertura.
  await expect(page.locator('.MuiDialog-container')).toHaveCSS('opacity', '1');
  await sinFallas(page);
  await page.keyboard.press('Tab');
  await expect(dialogo.getByRole('button', { name: 'Registrar y aprobar' })).toBeFocused();
  await page.keyboard.press('Enter');

  await expect(estado).toContainText('Aprobado · versión 1');
  await expect(estado).toContainText('Resolución del OCS n.º RPC-SO-12-No.123-2026');
  await expect(estado).toContainText('Huella del PDF: bbbbbbbbbbbb…');
  await expect(page.getByLabel('Número de la resolución')).toHaveCount(0);
  expect(registros).toBe(1);
  expect(enviado).toContain('RPC-SO-12-No.123-2026');
  expect(enviado).toContain('2026-11-14');
  expect(enviado).toContain('filename="resolucion.pdf"');
  await sinFallas(page);

  const descarga = page.waitForEvent('download');
  await page.getByRole('button', { name: 'Descargar la resolución' }).click();
  expect((await descarga).suggestedFilename()).toBe('resolucion.pdf');
});

test('la Secretaría publica el distributivo aprobado con el teclado y ve la constancia', async ({
  page,
}) => {
  let publicado = false;
  let publicaciones = 0;
  let cuerpo: string | null = 'sin pedir';
  const flujo = () => ({
    periodo: '2026-2',
    etapa: publicado ? 'PUBLICADO' : 'APROBADO',
    nombreDeEtapa: publicado ? 'Publicado' : 'Aprobado',
    version: 2,
    parciales: [{ carrera: 'TDS', enviado: true, pendienteDeConfirmar: false }],
    acciones: publicado ? [] : ['PUBLICAR'],
    transiciones: [],
    resolucion: {
      numero: 'RPC-SO-12-No.123-2026',
      fecha: '2026-11-14',
      registradaPor: 'secretaria.demo',
      registradaEn: '2026-11-15T15:30:00Z',
      sha256: 'b'.repeat(64),
      nombre: 'resolucion.pdf',
      tamano: 2048,
    },
    ...(publicado
      ? {
          publicacion: {
            version: 2,
            publicadaPor: 'secretaria.demo',
            publicadaEn: '2026-11-18T15:30:00Z',
          },
        }
      : {}),
  });
  await simularApi(page, { sesion: SECRETARIA });
  await page.route('**/api/academico/periodos', (ruta) =>
    ruta.fulfill({
      json: [
        {
          codigo: '2026-2',
          nombre: 'Periodo 2026-2',
          inicio: '2026-09-01',
          fin: '2027-02-28',
          estado: 'PLANIFICACION',
        },
      ],
    }),
  );
  await page.route('**/api/academico/carreras', (ruta) => ruta.fulfill({ json: [] }));
  await page.route('**/api/validacionocs/periodos/*/flujo', (ruta) =>
    ruta.fulfill({ json: flujo() }),
  );
  await page.route('**/api/validacionocs/periodos/*/publicacion', async (ruta) => {
    publicaciones++;
    cuerpo = ruta.request().postData();
    publicado = true;
    await ruta.fulfill({ json: flujo() });
  });

  await page.goto('/iniciar-sesion');
  await ingresar(page, 'secretaria.demo');
  await page.getByRole('link', { name: 'Resolución del OCS' }).click();
  const estado = page.getByRole('region', { name: 'Estado del distributivo' });
  await expect(estado).toContainText('Aprobado · versión 2');

  await page.getByRole('button', { name: 'Publicar el distributivo' }).focus();
  await page.keyboard.press('Enter');
  const dialogo = page.getByRole('dialog', { name: 'Publicar el distributivo' });
  await expect(dialogo).toContainText(
    'La versión 2 de 2026-2 quedará publicada y bloqueada: no se podrá cambiar sin una reforma. Se notificará a los docentes, a Talento Humano y a las coordinaciones.',
  );
  await expect(dialogo.getByRole('button', { name: 'Cancelar' })).toBeFocused();
  // axe mide el contraste: primero, que termine la animación de apertura.
  await expect(page.locator('.MuiDialog-container')).toHaveCSS('opacity', '1');
  await sinFallas(page);
  await page.keyboard.press('Tab');
  await expect(dialogo.getByRole('button', { name: 'Publicar' })).toBeFocused();
  await page.keyboard.press('Enter');

  await expect(estado).toContainText('Publicado · versión 2');
  await expect(estado).toContainText('Versión 2 publicada por secretaria.demo el 18/11/26, 10:30');
  await expect(page.getByRole('dialog')).toHaveCount(0);
  await expect(page.getByRole('button', { name: 'Publicar el distributivo' })).toHaveCount(0);
  expect(publicaciones).toBe(1);
  expect(cuerpo ?? '').toBe('');
  await sinFallas(page);
});

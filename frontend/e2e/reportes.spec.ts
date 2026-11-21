// F20-H01 en un navegador, con la API simulada: el Rector elige un docente, ve su reporte y lo
// descarga en PDF y en CSV (US1, US2); un Coordinador de carrera no ve la opción (FR-011); y la
// pantalla no tiene fallas de accesibilidad con axe-core (WCAG 2.1 AA, SC-007).
import AxeBuilder from '@axe-core/playwright';
import { expect, test, type Page } from '@playwright/test';
import { elegirEscribiendo, ingresar, simularApi } from './simulacion';

const RECTOR = {
  estado: 200,
  cuerpo: {
    usuario: 'rectorado.demo',
    nombre: 'Rectorado (demo)',
    roles: ['RECTOR'],
    permisos: ['DISTRIBUTIVO:LECTURA', 'REPORTES:LECTURA'],
  },
};

const COORDINADOR = {
  estado: 200,
  cuerpo: {
    usuario: 'coordinacion.demo',
    nombre: 'Coordinación (demo)',
    roles: ['COORDINADOR_CARRERA'],
    permisos: ['OFERTA:LECTURA', 'DISTRIBUTIVO:LECTURA', 'DOCENTES:LECTURA'],
  },
};

const ANA = '0109990010';
const PUBLICACION = {
  periodo: '2026-I',
  version: 1,
  hash: 'a'.repeat(64),
  publicadaEn: '2026-11-18T15:00:00Z',
  publicadaPor: 'secretaria.demo',
  enReforma: false,
};
const REPORTE = {
  publicacion: PUBLICACION,
  docente: { identificacion: ANA, nombre: 'PÉREZ ANA', dedicacion: 'TC', horasContratadas: 40 },
  asignaturas: [
    { carrera: 'Software', codigo: 'BD-301', nombre: 'Bases de datos', paralelo: 'A', horas: 8 },
  ],
  actividades: [
    {
      componente: 'INVESTIGACION',
      codigo: '141-a',
      nombre: 'Proyecto de investigación',
      unidad: 'Suelos andinos',
      horas: 2.5,
    },
  ],
  horas: {
    clase: 8,
    otrasDocencia: 0,
    investigacion: 2.5,
    vinculacion: 0,
    gestion: 0,
    total: 10.5,
  },
  generadoPor: 'rectorado.demo',
  generadoEn: '2026-11-21T15:00:00Z',
};

async function simularReportes(pagina: Page) {
  await pagina.route('**/api/reporting/publicaciones', (ruta) =>
    ruta.fulfill({ json: [PUBLICACION] }),
  );
  await pagina.route('**/api/reporting/periodos/2026-I/docentes', (ruta) =>
    ruta.fulfill({
      json: {
        publicacion: PUBLICACION,
        docentes: [
          { identificacion: '0109990020', nombre: 'ANDRADE LUIS' },
          { identificacion: ANA, nombre: 'PÉREZ ANA' },
        ],
      },
    }),
  );
  await pagina.route(`**/api/reporting/periodos/2026-I/docentes/${ANA}/reporte`, (ruta) =>
    ruta.fulfill({ json: REPORTE }),
  );
  for (const [formato, tipo] of [
    ['pdf', 'application/pdf'],
    ['csv', 'text/csv; charset=UTF-8'],
  ] as const) {
    await pagina.route(
      `**/api/reporting/periodos/2026-I/docentes/${ANA}/reporte.${formato}`,
      (ruta) =>
        ruta.fulfill({
          status: 200,
          contentType: tipo,
          headers: {
            'Content-Disposition': `attachment; filename="reporte-individual-2026-I-${ANA}.${formato}"`,
          },
          body: formato === 'pdf' ? '%PDF-1.7' : '﻿"Periodo"\r\n',
        }),
    );
  }
}

async function abrirComoRector(pagina: Page) {
  await simularApi(pagina, { sesion: RECTOR });
  await simularReportes(pagina);
  await pagina.goto('/iniciar-sesion');
  await ingresar(pagina, RECTOR.cuerpo.usuario);
  await pagina.getByRole('link', { name: 'Reportes' }).click();
  await expect(pagina.getByRole('heading', { level: 1, name: 'Reportes' })).toBeVisible();
  await elegirEscribiendo(pagina.getByRole('combobox', { name: 'Docente' }), ANA);
  await expect(pagina.getByRole('heading', { level: 2, name: 'PÉREZ ANA' })).toBeVisible();
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

test.describe('reportes', () => {
  test('el Rector ve el reporte de un docente y lo descarga en PDF y en CSV (US1, US2)', async ({
    page,
  }) => {
    await abrirComoRector(page);
    await expect(page.getByRole('table', { name: 'Asignaturas' })).toContainText('Bases de datos');
    await expect(page.getByRole('table', { name: 'Actividades' })).toContainText('Suelos andinos');
    await expect(page.getByRole('table', { name: 'Horas por componente' })).toContainText('10,50');

    for (const formato of ['PDF', 'CSV']) {
      const descarga = page.waitForEvent('download');
      await page.getByRole('button', { name: `Descargar ${formato}` }).click();
      expect((await descarga).suggestedFilename()).toBe(
        `reporte-individual-2026-I-${ANA}.${formato.toLowerCase()}`,
      );
    }
    const dialogo = page.getByRole('dialog', { name: 'CSV descargado' });
    await expect(dialogo).toContainText('importa la columna «Identificación» como texto');
    await expect(dialogo.getByRole('button', { name: 'Entendido' })).toBeFocused();
    await page.keyboard.press('Enter');
    await expect(dialogo).toHaveCount(0);
    await expect(page.getByRole('button', { name: 'Descargar CSV' })).toBeFocused();
  });

  test('un Coordinador de carrera no ve la opción y la dirección muestra el aviso (FR-011)', async ({
    page,
  }) => {
    await simularApi(page, { sesion: COORDINADOR });
    let consultas = 0;
    await page.route('**/api/reporting/**', (ruta) => {
      consultas += 1;
      return ruta.fulfill({ status: 403, json: {} });
    });
    await page.goto('/reportes');
    await ingresar(page, COORDINADOR.cuerpo.usuario);

    await expect(page).toHaveURL(/\/reportes$/);
    await expect(page.getByRole('alert')).toContainText('Acceso no autorizado');
    await expect(page.getByRole('link', { name: 'Reportes' })).toHaveCount(0);
    expect(consultas).toBe(0);
  });

  test.describe('accesibilidad', () => {
    test.skip(({ browserName }) => browserName !== 'chromium', 'axe-core corre solo en Chromium');

    test('con el reporte de un docente y el aviso del CSV', async ({ page }) => {
      await abrirComoRector(page);
      await sinFallas(page);

      await page.getByRole('button', { name: 'Descargar CSV' }).click();
      const dialogo = page.getByRole('dialog', { name: 'CSV descargado' });
      await expect(dialogo.getByRole('button', { name: 'Entendido' })).toBeFocused();
      // Como en los demás diálogos: axe-core mide cuando el modal terminó de aparecer, no a mitad
      // de la animación, donde el contraste todavía no es el final.
      await expect(page.locator('.MuiDialog-container')).toHaveCSS('opacity', '1');
      await sinFallas(page);
    });
  });
});

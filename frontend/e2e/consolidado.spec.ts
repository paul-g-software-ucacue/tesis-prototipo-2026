// F10-H02 en un navegador, con la API simulada: consolidar, adjuntar el informe y su accesibilidad
// con axe-core (WCAG 2.1 AA).
import AxeBuilder from '@axe-core/playwright';
import { expect, test, type Page } from '@playwright/test';
import { ingresar, simularApi } from './simulacion';

const CONSOLIDACION = {
  estado: 200,
  cuerpo: {
    usuario: 'consolidacion.demo',
    nombre: 'Responsable de consolidación (demo)',
    roles: ['RESPONSABLE_CONSOLIDACION'],
    permisos: [
      'CONSOLIDACION:LECTURA',
      'CONSOLIDACION:ESCRITURA',
      'DISTRIBUTIVO:LECTURA',
      'OFERTA:LECTURA',
    ],
  },
};

const HORAS = { clase: 10, otrasDocencia: 6, investigacion: 0, vinculacion: 0, gestion: 2 };

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

async function abrirConsolidado(pagina: Page) {
  let consolidado = false;
  let informe = false;
  const respuesta = () => ({
    periodo: '2026-2',
    creado: true,
    modificable: true,
    version: 1,
    desactualizado: false,
    carreras: [
      {
        carrera: 'TDS',
        estado: 'EN_ELABORACION',
        paralelos: 3,
        propuestos: 2,
        completo: false,
        horas: HORAS,
      },
    ],
    totales: HORAS,
    abiertas: { duras: 1, blandas: 1 },
    ...(consolidado
      ? { consolidacion: { por: 'consolidacion.demo', en: '2026-11-02T15:00:00Z' } }
      : {}),
    ...(informe
      ? {
          informe: {
            nombre: 'informe.pdf',
            tamano: 2048,
            huella: 'a'.repeat(64),
            por: 'consolidacion.demo',
            en: '2026-11-02T15:10:00Z',
          },
        }
      : {}),
  });
  await simularApi(pagina, { sesion: CONSOLIDACION });
  await pagina.route('**/api/academico/periodos', (ruta) =>
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
  await pagina.route('**/api/academico/carreras', (ruta) => ruta.fulfill({ json: [] }));
  await pagina.route('**/api/generacion/periodos/*/consolidado', async (ruta) => {
    if (ruta.request().method() === 'POST') {
      consolidado = true;
    }
    await ruta.fulfill({ json: respuesta() });
  });
  await pagina.route('**/api/generacion/periodos/*/consolidado/informe', async (ruta) => {
    informe = true;
    await ruta.fulfill({ json: respuesta().informe });
  });
  await pagina.goto('/iniciar-sesion');
  await ingresar(pagina, CONSOLIDACION.cuerpo.usuario);
  await pagina.getByRole('link', { name: 'Consolidado del periodo' }).click();
  await expect(
    pagina.getByRole('heading', { level: 1, name: 'Consolidado del periodo' }),
  ).toBeVisible();
}

test('consolida, adjunta el informe y es accesible en cada estado', async ({ page }) => {
  await abrirConsolidado(page);

  await expect(page.getByRole('table', { name: 'Carreras del consolidado' })).toBeVisible();
  await sinFallas(page);

  await page.getByRole('button', { name: 'Consolidar' }).click();
  await expect(page.getByText(/Consolidado por consolidacion\.demo/)).toBeVisible();
  await sinFallas(page);

  await page.getByLabel('Adjuntar el informe').setInputFiles({
    name: 'informe.pdf',
    mimeType: 'application/pdf',
    buffer: Buffer.from('%PDF-1.7\n%%EOF'),
  });
  await expect(page.getByRole('button', { name: 'Descargar el informe' })).toBeVisible();
  await sinFallas(page);
});

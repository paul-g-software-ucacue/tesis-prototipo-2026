// F10-H01 en un navegador, con la API simulada: abrir el panel, seguir un enlace, justificar una
// blanda y su accesibilidad con axe-core (WCAG 2.1 AA).
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

const PARALELO = {
  tipo: 'PARALELO_SIN_PROPUESTA',
  regla: 'RF-24',
  severidad: 'DURA',
  estado: 'ABIERTA',
  objeto: { clase: 'PARALELO', clave: 'POO-204/A', nombre: 'Programación · A' },
  detalle: 'Falta el docente de 4 h de clase.',
  indicacion: 'El coordinador de la carrera debe proponer un docente.',
  pantalla: 'OFERTA',
};

const PROPORCION = {
  tipo: 'PROPORCION',
  regla: 'RN-05',
  severidad: 'BLANDA',
  estado: 'ABIERTA',
  objeto: { clase: 'DOCENTE', clave: '0109990010', nombre: 'PEREZ LUIS' },
  detalle: 'Tiene 72 % de clase y 28 % de otras actividades.',
  indicacion: 'El coordinador de la carrera debe ajustar sus horas.',
  pantalla: 'ACTIVIDADES',
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

async function abrirPanel(pagina: Page) {
  let justificada = false;
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
  await pagina.route('**/api/generacion/periodos/*/inconsistencias', (ruta) =>
    ruta.fulfill({
      json: {
        periodo: '2026-2',
        creado: true,
        modificable: true,
        version: 1,
        abiertas: { duras: 1, blandas: justificada ? 0 : 1 },
        inconsistencias: [
          PARALELO,
          justificada
            ? {
                ...PROPORCION,
                estado: 'JUSTIFICADA',
                justificacion: {
                  texto: 'Tiene horas de tronco común',
                  por: 'consolidacion.demo',
                  en: '2026-11-02T15:00:00Z',
                },
              }
            : PROPORCION,
        ],
        sinEvaluar: ['RN-03', 'RN-16'],
      },
    }),
  );
  await pagina.route('**/api/generacion/periodos/*/inconsistencias/justificacion', async (ruta) => {
    justificada = true;
    await ruta.fulfill({ json: { ...PROPORCION, estado: 'JUSTIFICADA' } });
  });
  await pagina.goto('/iniciar-sesion');
  await ingresar(pagina, CONSOLIDACION.cuerpo.usuario);
  await pagina.getByRole('link', { name: 'Panel de inconsistencias' }).click();
  await expect(
    pagina.getByRole('heading', { level: 1, name: 'Panel de inconsistencias' }),
  ).toBeVisible();
}

test('muestra las abiertas, justifica una blanda y es accesible en cada estado', async ({
  page,
}) => {
  await abrirPanel(page);

  const abiertas = page.getByRole('table', { name: 'Inconsistencias abiertas' });
  await expect(abiertas.getByText('RF-24')).toBeVisible();
  await sinFallas(page);

  await abiertas.getByRole('button', { name: 'Justificar: PEREZ LUIS' }).click();
  const dialogo = page.getByRole('dialog', { name: 'Justificar: PEREZ LUIS' });
  await expect(page.locator('.MuiDialog-container')).toHaveCSS('opacity', '1');
  await sinFallas(page);

  await dialogo.getByLabel(/Justificación/).fill('Tiene horas de tronco común');
  await dialogo.getByRole('button', { name: 'Guardar justificación' }).click();

  await expect(page.getByRole('dialog')).toBeHidden();
  const justificadas = page.getByRole('table', { name: 'Inconsistencias justificadas' });
  await expect(justificadas.getByText('Tiene horas de tronco común')).toBeVisible();
  await sinFallas(page);
});

test('el enlace lleva a la pantalla donde se corrige', async ({ page }) => {
  await abrirPanel(page);
  await page.route('**/api/academico/periodos/*/oferta', (ruta) =>
    ruta.fulfill({
      json: {
        periodo: '2026-2',
        estadoDelPeriodo: 'PLANIFICACION',
        modificable: true,
        horasACubrir: 0,
        carreras: [],
      },
    }),
  );
  await page.route('**/api/generacion/periodos/*/parciales', (ruta) =>
    ruta.fulfill({ json: { creado: false, parciales: [] } }),
  );
  await page.route('**/api/academico/unidades', (ruta) => ruta.fulfill({ json: [] }));

  await page.getByRole('link', { name: 'Ir a la oferta: Programación · A' }).click();

  await expect(page.getByRole('heading', { level: 1, name: 'Oferta del periodo' })).toBeVisible();
});

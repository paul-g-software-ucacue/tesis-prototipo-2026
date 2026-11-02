// F07-H01 en un navegador, con la API simulada: el panel de los totales al elegir y al guardar, la
// proporción y el exceso, y la accesibilidad con axe-core (WCAG 2.1 AA), también a 390 px.
import AxeBuilder from '@axe-core/playwright';
import { expect, test, type Page } from '@playwright/test';
import { ingresar, simularApi } from './simulacion';

const COORDINADOR = {
  estado: 200,
  cuerpo: {
    usuario: 'coordinacion.demo',
    nombre: 'Coordinación (demo)',
    roles: ['COORDINADOR_CARRERA'],
    permisos: [
      'OFERTA:LECTURA:PROPIO',
      'OFERTA:ESCRITURA:PROPIO',
      'DISTRIBUTIVO:LECTURA:PROPIO',
      'DOCENTES:LECTURA',
    ],
  },
};

const CARRERA = '550613A01-P-0301';
const DOCENTE = '0109990010';
const NOMBRE = 'PERALTA MALDONADO JUAN ANDRES';
const POSIBLES = {
  docente: DOCENTE,
  actividades: [
    {
      actividad: '140-b',
      nombre: 'Planificar y actualizar contenidos de clases',
      componente: 'OTRAS_DOCENCIA',
      disponible: true,
      exigeProyecto: false,
      proyectos: [],
    },
  ],
};

function carga(exceso: number) {
  return {
    docente: DOCENTE,
    nombreDocente: NOMBRE,
    horasContratadas: 20,
    totales: { clase: 8, otrasDocencia: 4, investigacion: 0, vinculacion: 0, gestion: 8 + exceso },
    asignadas: 20 + exceso,
    disponibles: 0,
    exceso,
    proporcion: { clase: 67, otrasDocencia: 33 },
    meta: { clase: 60, otrasDocencia: 40 },
  };
}

async function abrir(pagina: Page, exceso = 0) {
  await simularApi(pagina, { sesion: COORDINADOR });
  await pagina.route('**/api/academico/periodos', (ruta) =>
    ruta.fulfill({
      json: [
        {
          codigo: '2026-2',
          nombre: 'Periodo 2026-2',
          fechaInicio: '2026-10-05',
          fechaFin: '2027-02-05',
          estado: 'PLANIFICACION',
        },
      ],
    }),
  );
  await pagina.route('**/api/academico/carreras', (ruta) =>
    ruta.fulfill({ json: [{ codigo: CARRERA, nombre: 'DESARROLLO DE SOFTWARE' }] }),
  );
  await pagina.route('**/api/docentes/docentes*', (ruta) =>
    ruta.fulfill({
      json: [
        {
          identificacion: DOCENTE,
          nombreCompleto: NOMBRE,
          dedicacion: 'TC',
          horasContratadas: 20,
          estado: 'ACTIVO',
        },
      ],
    }),
  );
  await pagina.route('**/api/generacion/periodos/2026-2/actividades', (ruta) =>
    ruta.fulfill({
      json: { creado: true, carreras: [{ carrera: CARRERA, docentes: [] }] },
    }),
  );
  await pagina.route('**/docentes/*/actividades-posibles', (ruta) =>
    ruta.fulfill({ json: POSIBLES }),
  );
  await pagina.route('**/docentes/*/carga-horaria', (ruta) =>
    ruta.fulfill({ json: carga(exceso) }),
  );
  await pagina.route('**/parciales/*/docentes/*/actividades/*', (ruta) =>
    ruta.fulfill({
      json: {
        actividad: {
          actividad: '140-b',
          nombre: 'Planificar',
          componente: 'OTRAS_DOCENCIA',
          horas: 4,
        },
      },
    }),
  );
  await pagina.goto('/iniciar-sesion');
  await ingresar(pagina, 'coordinacion.demo');
  await pagina.getByRole('link', { name: 'Actividades no lectivas' }).click();
  await expect(
    pagina.getByRole('heading', { level: 1, name: 'Actividades no lectivas' }),
  ).toBeVisible();
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

async function elegirDocente(pagina: Page) {
  await pagina.getByRole('button', { name: 'Asignar actividad' }).click();
  const dialogo = pagina.getByRole('dialog', { name: 'Asignar actividad' });
  await expect(pagina.locator('.MuiDialog-container')).toHaveCSS('opacity', '1');
  await dialogo.getByLabel('Docente').selectOption(DOCENTE);
  return dialogo;
}

test('el coordinador ve los totales al elegir al docente y al guardar', async ({ page }) => {
  await abrir(page);

  const dialogo = await elegirDocente(page);
  const panel = dialogo.getByRole('region', { name: `Totales de ${NOMBRE}` });
  await expect(panel).toBeVisible();
  await expect(panel.getByText('Clase 67 % · Otras 33 % (meta 60/40)')).toBeVisible();
  await sinFallas(page);

  await dialogo.getByRole('combobox', { name: 'Actividad' }).click();
  await page.getByRole('listbox', { name: 'Actividad' }).getByRole('option').first().click();
  await dialogo.getByLabel('Horas semanales').fill('4');
  await dialogo.getByRole('button', { name: 'Asignar', exact: true }).click();

  await expect(page.getByRole('dialog')).toBeHidden();
  await expect(page.getByRole('region', { name: `Totales de ${NOMBRE}` })).toBeVisible();
  await sinFallas(page);
});

test('con más horas de las contratadas dice el exceso', async ({ page }) => {
  await abrir(page, 13);

  const dialogo = await elegirDocente(page);

  await expect(dialogo.getByText('Se pasó por 13 h.')).toBeVisible();
  await sinFallas(page);
});

test('el panel se lee completo a 390 px', async ({ page }) => {
  await page.setViewportSize({ width: 390, height: 780 });
  await abrir(page);

  const dialogo = await elegirDocente(page);
  const panel = dialogo.getByRole('region', { name: `Totales de ${NOMBRE}` });
  await expect(panel).toBeVisible();

  const caja = await panel.boundingBox();
  expect(caja).not.toBeNull();
  expect(caja!.x).toBeGreaterThanOrEqual(0);
  expect(caja!.x + caja!.width).toBeLessThanOrEqual(390);
  await sinFallas(page);
});

// F07-H02 en un navegador, con la API simulada: el error de horas en el formulario, con las horas que
// quedan, y el candidato que no se puede proponer, con accesibilidad con axe-core (WCAG 2.1 AA).
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

async function abrir(pagina: Page) {
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
    ruta.fulfill({ json: [{ identificacion: DOCENTE, nombreCompleto: NOMBRE }] }),
  );
  await pagina.route('**/api/generacion/periodos/2026-2/actividades', (ruta) =>
    ruta.fulfill({ json: { creado: true, carreras: [{ carrera: CARRERA, docentes: [] }] } }),
  );
  await pagina.route('**/docentes/*/actividades-posibles', (ruta) =>
    ruta.fulfill({
      json: {
        docente: DOCENTE,
        actividades: [
          {
            actividad: '140-b',
            nombre: 'Planificar',
            componente: 'OTRAS_DOCENCIA',
            disponible: true,
            exigeProyecto: false,
            proyectos: [],
          },
        ],
      },
    }),
  );
  await pagina.route('**/docentes/*/carga-horaria', (ruta) =>
    ruta.fulfill({
      json: {
        docente: DOCENTE,
        nombreDocente: NOMBRE,
        horasContratadas: 20,
        totales: { clase: 18, otrasDocencia: 0, investigacion: 0, vinculacion: 0, gestion: 0 },
        asignadas: 18,
        disponibles: 2,
        exceso: 0,
        proporcion: { clase: 100, otrasDocencia: 0 },
        meta: { clase: 60, otrasDocencia: 40 },
      },
    }),
  );
  await pagina.route('**/parciales/*/docentes/*/actividades/*', (ruta) =>
    ruta.fulfill({
      status: 409,
      json: {
        titulo: 'Horas laborales excedidas',
        descripcion:
          'Esta asignación pasa las horas del docente, sumando todas sus carreras. Elige a otro docente o baja sus horas.',
        codigo: 'GENERACION_HORAS_EXCEDIDAS',
        detalles: [{ campo: 'horas', descripcion: 'Le quedan 2 h.' }],
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

test('si no caben las horas, lo dice en el formulario y sigue abierto', async ({ page }) => {
  await abrir(page);

  await page.getByRole('button', { name: 'Asignar actividad' }).click();
  const dialogo = page.getByRole('dialog', { name: 'Asignar actividad' });
  await expect(page.locator('.MuiDialog-container')).toHaveCSS('opacity', '1');
  await dialogo.getByLabel('Docente').selectOption(DOCENTE);
  await dialogo.getByRole('combobox', { name: 'Actividad' }).click();
  await page.getByRole('listbox', { name: 'Actividad' }).getByRole('option').first().click();
  await dialogo.getByLabel('Horas semanales').fill('4');
  await dialogo.getByRole('button', { name: 'Asignar', exact: true }).click();

  await expect(dialogo.getByRole('alert')).toContainText('Horas laborales excedidas');
  await expect(dialogo.getByText(/Le quedan 2 h\./)).toBeVisible();
  await expect(dialogo.getByLabel('Horas semanales')).toHaveValue('4');
  await sinFallas(page);
});

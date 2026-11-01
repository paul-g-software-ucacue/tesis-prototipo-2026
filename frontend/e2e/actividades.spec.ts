// F06-H04 en un navegador, con la API simulada: el coordinador asigna una actividad con y sin
// proyecto y la quita, y la accesibilidad de la pantalla y del diálogo con axe-core (WCAG 2.1 AA).
import AxeBuilder from '@axe-core/playwright';
import { expect, test, type Locator, type Page } from '@playwright/test';
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
// El texto más largo del catálogo: en la lista del navegador se salía de la pantalla.
const NOMBRE_LARGO =
  'Prestar servicios a la sociedad que no generen beneficio económico para el instituto o conservatorio para su personal académico, tales como el análisis de laboratorio especializado, el peritaje judicial, la revisión técnica documental para las instituciones del Estado, entre otras. La participación en trabajos de consultoría institucional no se reconocerá como actividad de vinculación dentro de la dedicación horaria';
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
    {
      actividad: '141-a',
      nombre: 'Diseñar y ejecutar proyectos de investigación',
      componente: 'INVESTIGACION',
      disponible: true,
      exigeProyecto: true,
      proyectos: [{ codigo: 'INV-2025-01', nombre: 'Monitoreo de hatos' }],
    },
    {
      actividad: '142-e',
      nombre: NOMBRE_LARGO,
      componente: 'VINCULACION',
      disponible: true,
      exigeProyecto: false,
      proyectos: [],
    },
    {
      actividad: '143-d',
      nombre: 'Coordinar carreras o programas',
      componente: 'GESTION',
      disponible: false,
      exigeProyecto: false,
      proyectos: [],
      motivo: 'El docente no tiene vigente el cargo que exige esta actividad.',
    },
  ],
};

interface Guardada {
  actividad: string;
  nombre: string;
  componente: string;
  horas: number;
  proyecto?: string;
  nombreProyecto?: string;
}

async function abrir(pagina: Page) {
  const guardadas: Guardada[] = [];
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
  await pagina.route('**/actividades-posibles', (ruta) => ruta.fulfill({ json: POSIBLES }));
  await pagina.route('**/api/generacion/periodos/*/actividades', (ruta) =>
    ruta.fulfill({
      json: {
        creado: true,
        carreras: [
          {
            carrera: CARRERA,
            docentes:
              guardadas.length === 0
                ? []
                : [
                    {
                      docente: DOCENTE,
                      nombreDocente: NOMBRE,
                      actividades: guardadas,
                      horasPorComponente: {
                        otrasDocencia: guardadas
                          .filter((una) => una.componente === 'OTRAS_DOCENCIA')
                          .reduce((suma, una) => suma + una.horas, 0),
                        investigacion: guardadas
                          .filter((una) => una.componente === 'INVESTIGACION')
                          .reduce((suma, una) => suma + una.horas, 0),
                        vinculacion: 0,
                        gestion: 0,
                      },
                    },
                  ],
          },
        ],
      },
    }),
  );
  await pagina.route('**/parciales/*/docentes/*/actividades/*', async (ruta) => {
    const codigo = new URL(ruta.request().url()).pathname.split('/').pop() ?? '';
    if (ruta.request().method() === 'DELETE') {
      guardadas.splice(
        guardadas.findIndex((una) => una.actividad === codigo),
        1,
      );
      await ruta.fulfill({ status: 204 });
      return;
    }
    const { horas, proyecto } = ruta.request().postDataJSON() as {
      horas: number;
      proyecto?: string;
    };
    const posible = POSIBLES.actividades.find((una) => una.actividad === codigo);
    const guardada = {
      actividad: codigo,
      nombre: posible?.nombre ?? '',
      componente: posible?.componente ?? '',
      horas,
      ...(proyecto && { proyecto, nombreProyecto: 'Monitoreo de hatos' }),
    };
    guardadas.push(guardada);
    await ruta.fulfill({ json: { actividad: guardada, horasSuficientes: true } });
  });
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

async function elegir(dialogo: Locator, etiqueta: string, inicio: string) {
  await dialogo.getByRole('combobox', { name: etiqueta }).click();
  await dialogo
    .page()
    .getByRole('listbox', { name: etiqueta })
    .getByRole('option', { name: new RegExp(`^${inicio}`) })
    .click();
}

async function abrirElDialogo(pagina: Page) {
  await pagina.getByRole('button', { name: 'Asignar actividad' }).click();
  const dialogo = pagina.getByRole('dialog', { name: 'Asignar actividad' });
  await expect(pagina.locator('.MuiDialog-container')).toHaveCSS('opacity', '1');
  await dialogo.getByLabel('Docente').selectOption(DOCENTE);
  await expect(dialogo.getByRole('combobox', { name: 'Actividad' })).toBeEnabled();
  return dialogo;
}

test('el coordinador asigna con y sin proyecto, y quita', async ({ page }) => {
  await abrir(page);
  await sinFallas(page);

  let dialogo = await abrirElDialogo(page);
  await elegir(dialogo, 'Actividad', '140 b\\)');
  await dialogo.getByLabel('Horas semanales').fill('4,25');
  await sinFallas(page);
  await dialogo.getByRole('button', { name: 'Asignar', exact: true }).click();
  await expect(page.getByRole('dialog')).toBeHidden();
  await expect(page.getByText('Otras actividades de docencia: 4,25 h')).toBeVisible();

  dialogo = await abrirElDialogo(page);
  await elegir(dialogo, 'Actividad', '141 a\\)');
  await elegir(dialogo, 'Proyecto', 'INV-2025-01');
  await dialogo.getByLabel('Horas semanales').fill('6');
  await dialogo.getByRole('button', { name: 'Asignar', exact: true }).click();
  await expect(page.getByRole('dialog')).toBeHidden();
  await expect(page.getByText('Proyecto: Monitoreo de hatos')).toBeVisible();
  await sinFallas(page);

  await page.getByRole('button', { name: `Quitar 141-a de ${NOMBRE}` }).click();
  await page.getByRole('dialog').getByRole('button', { name: 'Quitar', exact: true }).click();
  await expect(page.getByText('Proyecto: Monitoreo de hatos')).toBeHidden();
});

test('una actividad sin el cargo dice por qué y no deja asignar', async ({ page }) => {
  await abrir(page);

  const dialogo = await abrirElDialogo(page);
  await elegir(dialogo, 'Actividad', '143 d\\)');

  await expect(dialogo.getByText(/no tiene vigente el cargo/)).toBeVisible();
  await expect(dialogo.getByRole('button', { name: 'Asignar', exact: true })).toBeDisabled();
  await sinFallas(page);
});

for (const { ancho, alto } of [
  { ancho: 1280, alto: 720 },
  { ancho: 390, alto: 780 },
]) {
  test(`las actividades largas se leen completas a ${ancho} px`, async ({ page }) => {
    await page.setViewportSize({ width: ancho, height: alto });
    await abrir(page);

    const dialogo = await abrirElDialogo(page);
    await dialogo.getByRole('combobox', { name: 'Actividad' }).click();
    const lista = page.getByRole('listbox', { name: 'Actividad' });
    const larga = lista.getByRole('option', { name: /^142 e\)/ });
    await larga.scrollIntoViewIfNeeded();

    const caja = await larga.boundingBox();
    expect(caja).not.toBeNull();
    expect(caja!.x).toBeGreaterThanOrEqual(0);
    expect(caja!.x + caja!.width).toBeLessThanOrEqual(ancho);
    expect(await larga.evaluate((uno) => uno.scrollWidth <= uno.clientWidth)).toBe(true);
    await sinFallas(page);

    await larga.click();
    await expect(dialogo.getByText(NOMBRE_LARGO, { exact: false })).toBeVisible();
    await sinFallas(page);
  });
}

// F02-H02 en un navegador, con la API simulada: el tablero de preparación del periodo con la carga
// de docentes pendiente («Crear distributivo» deshabilitado, con lo que falta) y con todo listo (el
// distributivo de F06-H03, que se crea y deja de ofrecerse), y su accesibilidad con axe-core (WCAG 2.1
// AA).
import AxeBuilder from '@axe-core/playwright';
import { expect, test, type Page } from '@playwright/test';
import { ingresar, simularApi } from './simulacion';

const VICERRECTOR = {
  estado: 200,
  cuerpo: {
    usuario: 'vicerrectorado.demo',
    nombre: 'Vicerrector académico (demo)',
    roles: ['VICERRECTOR'],
    permisos: ['DISTRIBUTIVO:LECTURA', 'DISTRIBUTIVO:ESCRITURA', 'CARGAS:LECTURA'],
  },
};

const NOMBRES = [
  'Estructura orgánica',
  'Carreras',
  'Ciclos académicos aprobados',
  'Asignaturas y malla',
  'Oferta del periodo',
  'Docentes contratados (con ficha académica)',
  'Horas según la política de contratación',
  'Cargos y autoridades',
  'Proyectos y prácticas aprobados',
  'Matriculados por carrera',
];
const CODIGOS = [
  'estructura_organica',
  'carreras',
  'ciclos_academicos',
  'asignaturas_malla',
  'oferta_periodo',
  'docentes',
  'politica_carga_horaria',
  'cargos_autoridades',
  'proyectos_practicas',
  'matriculados_carrera',
];

/** El tablero de 2026-II: todo cargado, salvo los docentes si se pide. */
function tablero(conDocentes: boolean) {
  const cargas = NOMBRES.map((nombre, indice) => ({
    numero: indice + 1,
    codigo: CODIGOS[indice],
    nombre,
    obligatoriedad:
      indice === 9
        ? 'RECOMENDADA'
        : indice === 8
          ? 'OBLIGATORIA_SALVO_SIN_PROYECTOS'
          : 'OBLIGATORIA',
    estado: !conDocentes && indice === 5 ? 'PENDIENTE' : 'CARGADA',
  }));
  return {
    periodos: [
      { codigo: '2026-II', nombre: 'Periodo académico 2026-II' },
      { codigo: '2026-I', nombre: 'Periodo académico 2026-I' },
    ],
    periodo: '2026-II',
    abierto: true,
    cerrado: false,
    cargas,
    faltan: conDocentes
      ? []
      : [
          {
            tipo: 'CARGA',
            carga: { numero: 6, codigo: 'docentes', nombre: NOMBRES[5] },
            estado: 'PENDIENTE',
          },
        ],
    listo: conDocentes,
  };
}

async function abrirPreparacion(pagina: Page, conDocentes: boolean) {
  let creado = false;
  await pagina.route('**/api/generacion/periodos/*/distributivo', async (ruta) => {
    if (ruta.request().method() === 'POST') {
      creado = true;
    }
    await (creado
      ? ruta.fulfill({
          status: ruta.request().method() === 'POST' ? 201 : 200,
          json: { periodo: '2026-II', version: 1, carreras: ['550613A01-P-0301'] },
        })
      : ruta.fulfill({
          status: 404,
          json: {
            titulo: 'Distributivo no encontrado',
            descripcion: 'El periodo todavía no tiene distributivo.',
            codigo: 'GENERACION_DISTRIBUTIVO_NO_ENCONTRADO',
          },
        }));
  });
  await simularApi(pagina, { sesion: VICERRECTOR });
  await pagina.route('**/api/cargas/preparacion*', (ruta) =>
    ruta.fulfill({ json: tablero(conDocentes) }),
  );
  await pagina.goto('/iniciar-sesion');
  await ingresar(pagina, 'vicerrectorado.demo');
  await pagina.getByRole('link', { name: 'Preparación del periodo' }).click();
  await expect(
    pagina.getByRole('heading', { level: 1, name: 'Preparación del periodo' }),
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

test('con docentes pendiente, «Crear distributivo» está deshabilitado y dice qué falta', async ({
  page,
}) => {
  await abrirPreparacion(page, false);

  const crear = page.getByRole('button', { name: 'Crear distributivo' });
  await expect(crear).toBeDisabled();
  await expect(crear).toHaveAccessibleDescription(
    'Docentes contratados (con ficha académica): está pendiente. Súbela y confírmala en «Plantillas de carga».',
  );
  await expect(page.locator('tr[data-carga="docentes"]')).toContainText('Pendiente');
  await sinFallas(page);
});

test('con las cargas obligatorias, crea el distributivo y deja de ofrecerlo', async ({ page }) => {
  await abrirPreparacion(page, true);

  await page.getByRole('button', { name: 'Crear distributivo' }).click();

  await expect(page.getByRole('status')).toContainText(
    'El periodo 2026-II ya tiene su distributivo, en la versión 1.',
  );
  await expect(page.getByRole('button', { name: 'Crear distributivo' })).toBeHidden();
  await sinFallas(page);
});

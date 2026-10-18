// F04-H01 en un navegador, con la API simulada: el Vicerrector académico abre «Normativa» y ve la
// versión 1 con su tabla de carga horaria; un docente ve «Acceso no autorizado». También la
// accesibilidad de la pantalla con axe-core (WCAG 2.1 AA).
import AxeBuilder from '@axe-core/playwright';
import { expect, test, type Page } from '@playwright/test';
import { TEXTO_TECNICO, ingresar, simularApi } from './simulacion';

const FILA = {
  dedicacionTotal: 40,
  severidad: 'DURA',
  referencia: 'POL 6',
};

// 22 filas, como la política 2024: 7 para todos los tipos y 3 perfiles por cada tipo de IST.
const TABLA = [
  ...[
    'DOCENTE_TC',
    'DOCENTE_MT',
    'COORDINADOR_ACADEMICO_INSTITUCIONAL',
    'SECRETARIA_GENERAL_ACADEMICA',
    'DOCENTE_PROYECTO_NACIONAL_SIES',
    'DOCENTE_INVESTIGACION_VINCULACION',
    'CUIDADOR_SUSTITUTO',
  ].map((perfil) => ({
    ...FILA,
    aplicaA: 'TODOS',
    perfil,
    horasClase: 4,
    horasClaseMinimo: 3,
    horasClaseMaximo: 5,
  })),
  ...['T1', 'T2', 'T3', 'T4', 'T5'].flatMap((tipo) =>
    ['COORDINADOR_CARRERA_SEDE', 'GESTOR', 'RECTOR_VICERRECTOR'].map((perfil) => ({
      ...FILA,
      aplicaA: tipo,
      perfil,
      horasClase: 8,
      horasClaseMinimo: 8,
      horasClaseMaximo: 8,
      horasOtrasDocencia: 16,
      horasGestion: 16,
    })),
  ),
];

const RESUMEN = {
  numero: 1,
  codigo: 'SENESCYT-2024-03',
  origen: 'Política SENESCYT 2024',
  vigenteDesde: '2024-03-11',
  tipoIst: 'T2',
};

async function simularNormativa(pagina: Page) {
  await pagina.route('**/api/normativa/versiones', (ruta) => ruta.fulfill({ json: [RESUMEN] }));
  await pagina.route('**/api/normativa/versiones/1', (ruta) =>
    ruta.fulfill({
      json: {
        ...RESUMEN,
        reglas: [
          {
            identificador: 'RN-02',
            severidad: 'DURA',
            parametros: [{ nombre: 'horas_maximas_docente', valor: 40 }],
          },
        ],
        tablaCargaHoraria: TABLA,
      },
    }),
  );
}

const VICERRECTOR = {
  estado: 200,
  cuerpo: {
    usuario: 'vicerrectorado.demo',
    nombre: 'Vicerrectorado académico (demo)',
    roles: ['VICERRECTOR'],
    permisos: ['DISTRIBUTIVO:LECTURA', 'PARAMETROS:LECTURA'],
  },
};

test('versión inicial: el Vicerrector abre «Normativa» y ve la política 2024', async ({ page }) => {
  await simularApi(page, { sesion: VICERRECTOR });
  await simularNormativa(page);
  await page.goto('/iniciar-sesion');
  await ingresar(page, 'vicerrectorado.demo');

  await page.getByRole('link', { name: 'Normativa' }).click();

  await expect(
    page.getByRole('heading', { level: 2, name: 'Versión 1 · Política SENESCYT 2024' }),
  ).toBeVisible();
  await expect(page.getByRole('list', { name: 'Datos de la versión' })).toContainText('Tipo 2');
  await expect(
    page.getByRole('table', { name: 'Tabla de carga horaria' }).getByRole('row'),
  ).toHaveCount(23);
});

test('sin permiso: docente.demo abre /normativa y ve «Acceso no autorizado»', async ({ page }) => {
  await simularApi(page, {
    sesion: {
      estado: 200,
      cuerpo: { usuario: 'docente.demo', roles: ['DOCENTE'], permisos: [] },
    },
  });
  const pedidas: string[] = [];
  page.on('request', (peticion) => pedidas.push(new URL(peticion.url()).pathname));
  await page.goto('/normativa');
  await ingresar(page, 'docente.demo');

  await expect(page).toHaveURL(/\/normativa$/);
  await expect(page.getByRole('alert')).toContainText('Acceso no autorizado');
  for (const tecnico of TEXTO_TECNICO) {
    await expect(page.locator('body')).not.toContainText(tecnico);
  }
  expect(pedidas.filter((ruta) => ruta.startsWith('/api/normativa'))).toEqual([]);
});

test.describe('accesibilidad de la pantalla «Normativa»', () => {
  test.skip(({ browserName }) => browserName !== 'chromium', 'axe-core corre solo en Chromium');

  test('la versión 1 con sus reglas y su tabla', async ({ page }) => {
    await simularApi(page, { sesion: VICERRECTOR });
    await simularNormativa(page);
    await page.goto('/iniciar-sesion');
    await ingresar(page, 'vicerrectorado.demo');
    await page.getByRole('link', { name: 'Normativa' }).click();
    await expect(page.getByRole('table', { name: 'Tabla de carga horaria' })).toBeVisible();

    const resultado = await new AxeBuilder({ page })
      .withTags(['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa'])
      .analyze();
    const fallas = resultado.violations.map(
      (falla) =>
        `${falla.id} (${falla.impact}): ${falla.nodes.map((nodo) => nodo.target.join(' ')).join(', ')}`,
    );
    expect(fallas, fallas.join('\n')).toEqual([]);
  });
});

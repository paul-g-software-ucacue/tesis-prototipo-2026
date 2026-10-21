// F04-H01 y F04-H02 en un navegador, con la API simulada: el Vicerrector académico abre «Normativa»
// y ve la versión 1 con su tabla de carga horaria; un docente ve «Acceso no autorizado»; con
// escritura, el Vicerrector cambia en pantalla las horas de clase del docente de medio tiempo de una
// versión nueva. F04-H03: la afinidad y la severidad de una regla del motor. También la accesibilidad
// de la pantalla y sus diálogos con axe-core (WCAG 2.1 AA).
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
  periodos: [] as string[],
};

const REGLAS = [
  {
    identificador: 'RN-02',
    severidad: 'DURA',
    parametros: [{ nombre: 'horas_maximas_docente', valor: 40 }],
  },
  {
    identificador: 'RN-05',
    severidad: 'BLANDA',
    parametros: [{ nombre: 'porcentaje_clase', valor: 60 }],
  },
  {
    identificador: 'RN-19',
    severidad: 'BLANDA',
    parametros: [{ nombre: 'minimo_afinidad', valor: 2 }],
  },
  {
    identificador: 'RN-24',
    parametros: [
      { nombre: 'cuarto_nivel_campo_amplio', valor: 2 },
      { nombre: 'cuarto_nivel_campo_especifico', valor: 3 },
      { nombre: 'curso_relacionado', valor: 1 },
      { nombre: 'experiencia_relacionada', valor: 2 },
      { nombre: 'tercer_nivel_campo_amplio', valor: 1 },
      { nombre: 'tercer_nivel_campo_especifico', valor: 2 },
      { nombre: 'tope_cursos', valor: 3 },
    ],
  },
  { identificador: 'RF-24', severidad: 'DURA', parametros: [] },
];

async function simularNormativa(pagina: Page) {
  await pagina.route('**/api/normativa/versiones', (ruta) => ruta.fulfill({ json: [RESUMEN] }));
  await pagina.route('**/api/normativa/versiones/1', (ruta) =>
    ruta.fulfill({
      json: {
        ...RESUMEN,
        reglas: REGLAS,
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

// F04-H02: la versión 1 rige 2025-1 y la 2, copiada de ella, todavía no rige ningún periodo.
const UNO_EN_USO = { ...RESUMEN, periodos: ['2025-1'] };
const DOS = {
  ...RESUMEN,
  numero: 2,
  codigo: 'SENESCYT-2025-01',
  origen: 'Reforma de la política 2025',
  vigenteDesde: '2025-01-15',
  copiadaDe: 1,
};

const ESCRITOR = {
  estado: 200,
  cuerpo: { ...VICERRECTOR.cuerpo, permisos: ['PARAMETROS:LECTURA', 'PARAMETROS:ESCRITURA'] },
};

const CLASE_DE_MEDIO_TIEMPO = 'Clase · Docente a medio tiempo · Todos';

interface CuerpoEditado {
  tablaCargaHoraria: { perfil: string; horasClase: number }[];
  reglas: {
    identificador: string;
    severidad?: string;
    parametros: { nombre: string; valor: number }[];
  }[];
}

/** Simula las dos versiones y responde el PUT de la 2 con lo que diga {@code alEditar}. */
async function simularDosVersiones(
  pagina: Page,
  alEditar: (cuerpo: CuerpoEditado) => {
    status: number;
    json: unknown;
  },
) {
  let dos = { ...DOS, reglas: REGLAS, tablaCargaHoraria: TABLA };
  await pagina.route('**/api/normativa/versiones', (ruta) =>
    ruta.fulfill({ json: [UNO_EN_USO, DOS] }),
  );
  await pagina.route('**/api/normativa/versiones/1', (ruta) =>
    ruta.fulfill({ json: { ...UNO_EN_USO, reglas: REGLAS, tablaCargaHoraria: TABLA } }),
  );
  await pagina.route('**/api/normativa/versiones/2', async (ruta) => {
    if (ruta.request().method() === 'PUT') {
      const respuesta = alEditar(ruta.request().postDataJSON() as CuerpoEditado);
      if (respuesta.status === 200) {
        dos = respuesta.json as typeof dos;
      }
      await ruta.fulfill(respuesta);
      return;
    }
    await ruta.fulfill({ json: dos });
  });
}

async function abrirComoEscritor(pagina: Page) {
  await simularApi(pagina, { sesion: ESCRITOR });
  await pagina.goto('/iniciar-sesion');
  await ingresar(pagina, 'vicerrectorado.demo');
  await pagina.getByRole('link', { name: 'Normativa' }).click();
  await expect(
    pagina.getByRole('heading', { level: 2, name: 'Versión 2 · Reforma de la política 2025' }),
  ).toBeVisible();
}

test('versión nueva: el Vicerrector cambia las horas de clase del docente de medio tiempo', async ({
  page,
}) => {
  await simularDosVersiones(page, (cuerpo) => ({
    status: 200,
    json: {
      ...DOS,
      reglas: REGLAS,
      tablaCargaHoraria: cuerpo.tablaCargaHoraria.map((fila, i) => ({ ...TABLA[i], ...fila })),
    },
  }));
  await abrirComoEscritor(page);

  await page.getByRole('button', { name: 'Editar' }).click();
  await page.getByRole('spinbutton', { name: CLASE_DE_MEDIO_TIEMPO }).fill('5');
  await page.getByRole('button', { name: 'Guardar' }).click();

  await expect(page.getByRole('button', { name: 'Guardar' })).toBeHidden();
  const tabla = page.getByRole('table', { name: 'Tabla de carga horaria' });
  await expect(tabla.getByRole('row').nth(2)).toContainText('Docente a medio tiempo');
  await expect(tabla.getByRole('row').nth(2).getByRole('cell').nth(3)).toHaveText('5');
});

test('severidad y afinidad: RN-05 pasa a «Dura» y un peso cambia en una versión nueva', async ({
  page,
}) => {
  await simularDosVersiones(page, (cuerpo) => ({
    status: 200,
    json: { ...DOS, reglas: cuerpo.reglas, tablaCargaHoraria: TABLA },
  }));
  await abrirComoEscritor(page);
  await expect(page.getByRole('list', { name: 'Afinidad' })).toContainText(
    'Experiencia relacionada: 2',
  );

  await page.getByRole('button', { name: 'Editar' }).click();
  await page.getByRole('combobox', { name: 'RN-05 · Severidad' }).selectOption('Dura');
  await page.getByRole('spinbutton', { name: 'RN-24 · Experiencia relacionada' }).fill('3');
  await page.getByRole('button', { name: 'Guardar' }).click();

  await expect(page.getByRole('button', { name: 'Guardar' })).toBeHidden();
  await expect(page.getByRole('list', { name: 'Afinidad' })).toContainText(
    'Experiencia relacionada: 3',
  );
  const reglas = page.getByRole('table', { name: 'Reglas y parámetros' });
  await expect(reglas.getByRole('row').filter({ hasText: 'RN-05' })).toContainText('Dura');
});

test('edición en pantalla: un valor fuera de rango muestra el motivo junto a su fila', async ({
  page,
}) => {
  await simularDosVersiones(page, () => ({
    status: 400,
    json: {
      titulo: 'Solicitud inválida',
      descripcion: 'Revisa los campos marcados y vuelve a intentarlo.',
      codigo: 'COMUN_SOLICITUD_INVALIDA',
      detalles: [
        {
          campo: 'tablaCargaHoraria[DOCENTE_MT/TODOS]',
          descripcion: 'Las horas de clase (9) no están entre el mínimo y el máximo (3 a 5)',
        },
      ],
    },
  }));
  await abrirComoEscritor(page);

  await page.getByRole('button', { name: 'Editar' }).click();
  const clase = page.getByRole('spinbutton', { name: CLASE_DE_MEDIO_TIEMPO });
  await clase.fill('9');
  await page.getByRole('button', { name: 'Guardar' }).click();

  await expect(page.getByRole('alert')).toContainText('Solicitud inválida');
  await expect(clase).toHaveAccessibleDescription(
    'Las horas de clase (9) no están entre el mínimo y el máximo (3 a 5)',
  );
  for (const tecnico of TEXTO_TECNICO) {
    await expect(page.locator('body')).not.toContainText(tecnico);
  }
});

test.describe('accesibilidad de la pantalla «Normativa»', () => {
  test.skip(({ browserName }) => browserName !== 'chromium', 'axe-core corre solo en Chromium');

  test('la versión 1 con su afinidad, sus reglas y su tabla', async ({ page }) => {
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

  async function sinFallas(page: Page) {
    const resultado = await new AxeBuilder({ page })
      .withTags(['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa'])
      .analyze();
    const fallas = resultado.violations.map(
      (falla) =>
        `${falla.id} (${falla.impact}): ${falla.nodes.map((nodo) => nodo.target.join(' ')).join(', ')}`,
    );
    expect(fallas, fallas.join('\n')).toEqual([]);
  }

  test('la edición en pantalla de la versión 2, con los selectores de severidad', async ({
    page,
  }) => {
    await simularDosVersiones(page, () => ({ status: 200, json: DOS }));
    await abrirComoEscritor(page);
    await page.getByRole('button', { name: 'Editar' }).click();
    await expect(page.getByRole('spinbutton', { name: CLASE_DE_MEDIO_TIEMPO })).toBeVisible();
    await expect(page.getByRole('combobox', { name: 'RN-05 · Severidad' })).toBeVisible();

    await sinFallas(page);
  });

  for (const [boton, titulo] of [
    ['Crear versión nueva', 'Versión nueva a partir de la versión 2'],
  ] as const) {
    test(`el diálogo «${titulo}»`, async ({ page }) => {
      await simularDosVersiones(page, () => ({ status: 200, json: DOS }));
      await abrirComoEscritor(page);
      await page.getByRole('button', { name: boton }).click();
      await expect(page.getByRole('dialog', { name: titulo })).toBeVisible();
      // Sin esperar a que termine la transición, axe mide el contraste a media opacidad.
      await expect(page.locator('.MuiDialog-container')).toHaveCSS('opacity', '1');

      await sinFallas(page);
    });
  }
});

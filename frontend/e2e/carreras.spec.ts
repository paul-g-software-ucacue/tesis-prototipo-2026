// F03-H02 en un navegador, con la API simulada: el Vicerrector registra una carrera, una malla y
// una asignatura en ella, y la ve en su ciclo con sus horas. También la accesibilidad de «Carreras
// y mallas», de la malla abierta y de sus diálogos con axe-core (WCAG 2.1 AA).
import AxeBuilder from '@axe-core/playwright';
import { expect, test, type Page } from '@playwright/test';
import { ingresar, simularApi } from './simulacion';

interface Malla {
  codigo: string;
  estado: string;
  asignaturas: number;
  horasSemanalesClase: number;
}

interface Carrera {
  codigo: string;
  nombre: string;
  numeroCiclos: number;
  estado: string;
  mallas: Malla[];
  [otro: string]: unknown;
}

interface Asignatura {
  codigo: string;
  carrera: string;
  malla: string;
  ciclo: number;
  horasSemanalesClase: number;
  campoAmplio: string | { codigo: string; nombre: string };
  activa: boolean;
  [otro: string]: unknown;
}

const VICERRECTOR = {
  estado: 200,
  cuerpo: {
    usuario: 'vicerrectorado.demo',
    nombre: 'Vicerrector académico (demo)',
    roles: ['VICERRECTOR'],
    permisos: ['DISTRIBUTIVO:LECTURA', 'DISTRIBUTIVO:ESCRITURA'],
  },
};

const CIENCIAS = { codigo: '05', nombre: 'Ciencias naturales, matemáticas y estadística' };

const CAMPOS = [
  CIENCIAS,
  { codigo: '06', nombre: 'Tecnologías de la información y la comunicación' },
];

const UNIDADES = [{ codigo: 'MATRIZ', tipo: 'SEDE_MATRIZ', nombre: 'Sede matriz', activa: true }];

const DS: Carrera = {
  codigo: '550613A01-P-0301',
  nombre: 'DESARROLLO DE SOFTWARE',
  siglas: 'DS',
  sede: 'MATRIZ',
  nivelFormacion: 'TECNOLOGICO_SUPERIOR',
  modalidad: 'PRESENCIAL',
  jornadas: ['NOCTURNA'],
  campoAmplio: '06',
  campoEspecifico: '061',
  numeroCiclos: 5,
  estado: 'VIGENTE',
  mallas: [{ codigo: 'DS-2019', estado: 'VIGENTE', asignaturas: 1, horasSemanalesClase: 3 }],
};

const MD: Asignatura = {
  codigo: 'DS-B-I-MD-101',
  carrera: DS.codigo,
  malla: 'DS-2019',
  nombre: 'MATEMÁTICA DISCRETA',
  ciclo: 1,
  unidadOrganizacionCurricular: 'BASICA',
  horasSemanalesClase: 3,
  troncoComun: true,
  campoAmplio: CIENCIAS,
  campoEspecifico: '054',
  palabrasClave: ['matemática', 'lógica'],
  activa: true,
};

// Las carreras, mallas y asignaturas en memoria, cambiadas como lo haría Académico: las horas de
// cada malla se suman de sus asignaturas activas.
async function simularAcademico(
  pagina: Page,
  iniciales: Carrera[],
  asignaturasIniciales: Asignatura[],
) {
  const carreras: Carrera[] = structuredClone(iniciales);
  const asignaturas: Asignatura[] = structuredClone(asignaturasIniciales);
  const conHoras = () =>
    carreras.map((carrera) => ({
      ...carrera,
      mallas: carrera.mallas.map((malla) => {
        const suyas = asignaturas.filter((una) => una.malla === malla.codigo && una.activa);
        return {
          ...malla,
          asignaturas: suyas.length,
          horasSemanalesClase: suyas.reduce((suma, una) => suma + una.horasSemanalesClase, 0),
        };
      }),
    }));
  await pagina.route('**/api/academico/campos-amplios', (ruta) => ruta.fulfill({ json: CAMPOS }));
  await pagina.route('**/api/academico/unidades', (ruta) => ruta.fulfill({ json: UNIDADES }));
  await pagina.route('**/api/academico/carreras', async (ruta) => {
    if (ruta.request().method() === 'POST') {
      const nueva = {
        ...(ruta.request().postDataJSON() as Carrera),
        estado: 'VIGENTE',
        mallas: [],
      };
      carreras.push(nueva);
      await ruta.fulfill({ status: 201, json: nueva });
      return;
    }
    await ruta.fulfill({ json: conHoras() });
  });
  await pagina.route(/\/api\/academico\/carreras\/[^/]+\/mallas$/, async (ruta) => {
    const [, codigo] = /carreras\/([^/]+)\/mallas$/.exec(ruta.request().url()) ?? [];
    const malla = {
      ...(ruta.request().postDataJSON() as { codigo: string }),
      estado: 'VIGENTE',
      asignaturas: 0,
      horasSemanalesClase: 0,
    };
    carreras.find((carrera) => carrera.codigo === codigo)?.mallas.push(malla);
    await ruta.fulfill({ status: 201, json: malla });
  });
  await pagina.route(/\/api\/academico\/carreras\/[^/]+\/asignaturas$/, async (ruta) => {
    const [, codigo] = /carreras\/([^/]+)\/asignaturas$/.exec(ruta.request().url()) ?? [];
    await ruta.fulfill({ json: asignaturas.filter((una) => una.carrera === codigo) });
  });
  await pagina.route('**/api/academico/asignaturas', async (ruta) => {
    const nueva = ruta.request().postDataJSON() as Asignatura;
    const campo = CAMPOS.find((uno) => uno.codigo === nueva.campoAmplio);
    const guardada = { ...nueva, campoAmplio: campo ?? CIENCIAS, activa: true };
    asignaturas.push(guardada);
    await ruta.fulfill({ status: 201, json: guardada });
  });
}

async function abrirCarreras(
  pagina: Page,
  carreras: Carrera[] = [],
  asignaturas: Asignatura[] = [],
) {
  await simularApi(pagina, { sesion: VICERRECTOR });
  await simularAcademico(pagina, carreras, asignaturas);
  await pagina.goto('/iniciar-sesion');
  await ingresar(pagina, 'vicerrectorado.demo');
  await pagina.getByRole('link', { name: 'Carreras y mallas' }).click();
  await expect(pagina.getByRole('heading', { level: 1, name: 'Carreras y mallas' })).toBeVisible();
}

function fila(pagina: Page, codigo: string) {
  return pagina
    .getByRole('table', { name: 'Carreras' })
    .getByRole('row')
    .filter({ has: pagina.getByRole('cell', { name: codigo, exact: true }) });
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

test('el Vicerrector registra una carrera, una malla y una asignatura, y la ve en su ciclo con sus horas', async ({
  page,
}) => {
  await abrirCarreras(page);

  await page.getByRole('button', { name: 'Registrar carrera' }).click();
  const carrera = page.getByRole('dialog', { name: 'Registrar carrera' });
  await carrera.getByLabel('Código SENESCYT').fill('550613A01-P-0301');
  await carrera.getByLabel('Nombre', { exact: true }).fill('DESARROLLO DE SOFTWARE');
  await carrera.getByLabel('Siglas').fill('DS');
  await carrera.getByLabel('Sede').selectOption('MATRIZ');
  await carrera.getByLabel('Nivel de formación').selectOption('TECNOLOGICO_SUPERIOR');
  await carrera.getByLabel('Modalidad').selectOption('PRESENCIAL');
  await carrera.getByLabel('Nocturna').check();
  await carrera.getByLabel('Campo amplio').selectOption('06');
  await carrera.getByLabel('Campo específico').fill('061');
  await carrera.getByLabel('Número de ciclos').fill('5');
  await carrera.getByRole('button', { name: 'Registrar' }).click();
  await expect(fila(page, '550613A01-P-0301')).toContainText('Sin mallas');

  await page.getByRole('button', { name: 'Registrar una malla de DESARROLLO DE SOFTWARE' }).click();
  const malla = page.getByRole('dialog', { name: 'Registrar malla' });
  await malla.getByLabel('Código de versión').fill('DS-2019');
  await malla.getByRole('button', { name: 'Registrar' }).click();
  await expect(fila(page, '550613A01-P-0301')).toContainText('DS-2019 · Vigente · 0 h');

  await page.getByRole('button', { name: 'Ver la malla DS-2019' }).click();
  await page.getByRole('button', { name: 'Registrar asignatura en DS-2019' }).click();
  const asignatura = page.getByRole('dialog', { name: 'Registrar asignatura' });
  await asignatura.getByLabel('Código', { exact: true }).fill('DS-B-I-MD-101');
  await asignatura.getByLabel('Nombre', { exact: true }).fill('MATEMÁTICA DISCRETA');
  await asignatura.getByLabel('Ciclo').fill('1');
  await asignatura.getByLabel('Unidad de organización curricular').selectOption('BASICA');
  await asignatura.getByLabel('Horas semanales de clase').fill('3');
  await asignatura.getByLabel('Tronco común').check();
  await asignatura.getByLabel('Campo amplio').selectOption('05');
  await asignatura.getByLabel('Campo específico').fill('054');
  await asignatura.getByLabel('Palabras clave').fill('matemática; lógica');
  await asignatura.getByRole('button', { name: 'Registrar' }).click();

  const region = page.getByRole('region', { name: 'Malla DS-2019' });
  await expect(region.getByRole('heading', { name: 'Ciclo 1 · 3 h' })).toBeVisible();
  await expect(region.getByRole('table', { name: 'Ciclo 1' })).toContainText(
    '05 · Ciencias naturales, matemáticas y estadística',
  );
  await expect(region).toContainText('3 horas semanales de clase');
});

test('las carreras y la malla abierta', async ({ page }) => {
  await abrirCarreras(page, [DS], [MD]);
  await page.getByRole('button', { name: 'Ver la malla DS-2019' }).click();
  await expect(page.getByRole('table', { name: 'Ciclo 1' })).toBeVisible();

  await sinFallas(page);
});

for (const [boton, titulo] of [
  ['Registrar carrera', 'Registrar carrera'],
  ['Corregir 550613A01-P-0301', 'Corregir la carrera 550613A01-P-0301'],
  ['Registrar asignatura en DS-2019', 'Registrar asignatura'],
  ['Corregir DS-B-I-MD-101', 'Corregir la asignatura DS-B-I-MD-101'],
] as const) {
  test(`el diálogo «${titulo}»`, async ({ page }) => {
    await abrirCarreras(page, [DS], [MD]);
    await page.getByRole('button', { name: 'Ver la malla DS-2019' }).click();
    await expect(page.getByRole('table', { name: 'Ciclo 1' })).toBeVisible();
    await page.getByRole('button', { name: boton }).click();
    await expect(page.getByRole('dialog', { name: titulo })).toBeVisible();
    // Sin esperar a que termine la transición, axe mide el contraste a media opacidad.
    await expect(page.locator('.MuiDialog-container')).toHaveCSS('opacity', '1');

    await sinFallas(page);
  });
}

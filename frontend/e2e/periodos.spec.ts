// F02-H01 en un navegador, con la API simulada: el Vicerrector académico registra un periodo, lo abre
// eligiendo su versión de normativa y lo pasa a En aprobación. También la accesibilidad de la
// pantalla «Periodos» y de sus diálogos con axe-core (WCAG 2.1 AA).
import AxeBuilder from '@axe-core/playwright';
import { expect, test, type Page } from '@playwright/test';
import { ingresar, simularApi } from './simulacion';

interface Periodo {
  codigo: string;
  nombre: string;
  inicio: string;
  fin: string;
  estado: string;
  version?: number;
}

const VICERRECTOR = {
  estado: 200,
  cuerpo: {
    usuario: 'vicerrectorado.demo',
    nombre: 'Vicerrectorado académico (demo)',
    roles: ['VICERRECTOR'],
    permisos: [
      'DISTRIBUTIVO:LECTURA',
      'DISTRIBUTIVO:ESCRITURA',
      'PARAMETROS:LECTURA',
      'PARAMETROS:ESCRITURA',
    ],
  },
};

const VERSIONES = [
  {
    numero: 1,
    codigo: 'SENESCYT-2024-03',
    origen: 'Política SENESCYT 2024',
    vigenteDesde: '2024-03-11',
    tipoIst: 'T2',
    periodos: ['2026-1'],
  },
  {
    numero: 2,
    codigo: 'SENESCYT-2025-01',
    origen: 'Reforma de la política 2025',
    vigenteDesde: '2025-01-15',
    tipoIst: 'T2',
    periodos: [] as string[],
    copiadaDe: 1,
  },
];

// Los periodos en memoria, cambiados por las operaciones simuladas como lo haría Académico.
async function simularAcademico(pagina: Page) {
  const periodos: Periodo[] = [
    {
      codigo: '2026-2',
      nombre: 'Periodo 2026-2',
      inicio: '2026-09-01',
      fin: '2027-02-28',
      estado: 'PLANIFICACION',
    },
    {
      codigo: '2026-1',
      nombre: 'Periodo 2026-1',
      inicio: '2026-03-01',
      fin: '2026-08-15',
      estado: 'VIGENTE',
      version: 1,
    },
  ];
  await pagina.route('**/api/normativa/versiones', (ruta) => ruta.fulfill({ json: VERSIONES }));
  // F05-H03: el Responsable de consolidación de cada periodo, que guarda Roles.
  const responsables: { periodo: string; usuario: string; designadoEn: string }[] = [];
  await pagina.route('**/api/roles/responsables', (ruta) => ruta.fulfill({ json: responsables }));
  await pagina.route('**/api/roles/docentes', (ruta) =>
    ruta.fulfill({ json: ['docente.coordinador.demo', 'docente.demo'] }),
  );
  await pagina.route(/\/api\/roles\/responsables\/[^/]+$/, async (ruta) => {
    const periodo = /responsables\/([^/]+)$/.exec(ruta.request().url())?.[1] ?? '';
    const { usuario } = ruta.request().postDataJSON() as { usuario: string };
    const designado = { periodo, usuario, designadoEn: '2026-10-21T15:00:00Z' };
    responsables.splice(
      0,
      responsables.length,
      ...responsables.filter((uno) => uno.periodo !== periodo),
      designado,
    );
    await ruta.fulfill({ json: designado });
  });
  await pagina.route('**/api/academico/periodos', async (ruta) => {
    if (ruta.request().method() === 'POST') {
      const nuevo = { ...(ruta.request().postDataJSON() as Periodo), estado: 'PLANIFICACION' };
      periodos.unshift(nuevo);
      await ruta.fulfill({ status: 201, json: nuevo });
      return;
    }
    await ruta.fulfill({ json: periodos });
  });
  await pagina.route(/\/api\/academico\/periodos\/[^/]+\/(normativa|estado)$/, async (ruta) => {
    const [, codigo, que] =
      /periodos\/([^/]+)\/(normativa|estado)$/.exec(ruta.request().url()) ?? [];
    const periodo = periodos.find((uno) => uno.codigo === codigo);
    if (!periodo) {
      await ruta.fulfill({ status: 404, json: {} });
      return;
    }
    const cuerpo = ruta.request().postDataJSON() as { version?: number; estado?: string };
    if (que === 'normativa') {
      periodo.version = cuerpo.version;
    } else {
      periodo.estado = cuerpo.estado ?? periodo.estado;
    }
    await ruta.fulfill({ json: periodo });
  });
}

async function abrirPeriodos(pagina: Page) {
  await simularApi(pagina, { sesion: VICERRECTOR });
  await simularAcademico(pagina);
  await pagina.goto('/iniciar-sesion');
  await ingresar(pagina, 'vicerrectorado.demo');
  await pagina.getByRole('link', { name: 'Periodos' }).click();
  await expect(pagina.getByRole('table', { name: 'Periodos' })).toBeVisible();
}

function fila(pagina: Page, codigo: string) {
  return pagina
    .getByRole('table', { name: 'Periodos' })
    .getByRole('row')
    .filter({ hasText: codigo });
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

test('el Vicerrector registra un periodo, lo abre con su versión y lo pasa a En aprobación', async ({
  page,
}) => {
  await abrirPeriodos(page);

  await page.getByRole('button', { name: 'Registrar periodo' }).click();
  const registrar = page.getByRole('dialog', { name: 'Registrar periodo' });
  await registrar.getByLabel('Código').fill('2027-1');
  await registrar.getByLabel('Nombre').fill('Periodo 2027-1');
  await registrar.getByLabel('Inicio').fill('2027-03-01');
  await registrar.getByLabel('Fin').fill('2027-08-15');
  await registrar.getByRole('button', { name: 'Registrar' }).click();
  await expect(fila(page, '2027-1')).toContainText('Planificación');
  await expect(fila(page, '2027-1')).toContainText('Sin versión');

  await fila(page, '2027-1').getByRole('button', { name: 'Abrir 2027-1' }).click();
  const abrir = page.getByRole('dialog', { name: 'Abrir el periodo 2027-1' });
  await abrir.getByLabel('Versión de normativa').selectOption('2');
  await abrir.getByRole('button', { name: 'Abrir' }).click();
  await expect(fila(page, '2027-1')).toContainText('Versión 2');

  await fila(page, '2027-1').getByRole('button', { name: 'Pasar 2027-1 a En aprobación' }).click();
  await expect(fila(page, '2027-1')).toContainText('En aprobación');
});

test('el Vicerrector designa al Responsable de consolidación de un periodo', async ({ page }) => {
  await abrirPeriodos(page);
  await expect(fila(page, '2026-2')).toContainText('Sin designar');

  await fila(page, '2026-2').getByRole('button', { name: 'Designar responsable 2026-2' }).click();
  const dialogo = page.getByRole('dialog', {
    name: 'Designar al Responsable de consolidación de 2026-2',
  });
  await dialogo.getByLabel('Docente').selectOption('docente.demo');
  await dialogo.getByRole('button', { name: 'Designar' }).click();

  await expect(fila(page, '2026-2')).toContainText('docente.demo');
});

test('la lista de periodos con sus acciones', async ({ page }) => {
  await abrirPeriodos(page);
  await expect(fila(page, '2026-2')).toContainText('Ábrelo antes con su versión de normativa.');

  await sinFallas(page);
});

for (const [codigo, boton, titulo] of [
  [undefined, 'Registrar periodo', 'Registrar periodo'],
  ['2026-2', 'Abrir 2026-2', 'Abrir el periodo 2026-2'],
  ['2026-2', 'Corregir 2026-2', 'Corregir el periodo 2026-2'],
  ['2026-1', 'Pasar 2026-1 a Cerrado', 'Cerrar el periodo 2026-1'],
  ['2026-2', 'Designar responsable 2026-2', 'Designar al Responsable de consolidación de 2026-2'],
] as const) {
  test(`el diálogo «${titulo}»`, async ({ page }) => {
    await abrirPeriodos(page);
    const donde = codigo ? fila(page, codigo) : page;
    await donde.getByRole('button', { name: boton }).click();
    await expect(page.getByRole('dialog', { name: titulo })).toBeVisible();
    // Sin esperar a que termine la transición, axe mide el contraste a media opacidad.
    await expect(page.locator('.MuiDialog-container')).toHaveCSS('opacity', '1');

    await sinFallas(page);
  });
}

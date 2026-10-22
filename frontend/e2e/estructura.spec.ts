// F03-H01 en un navegador, con la API simulada: el Administrador registra la sede matriz y una
// gestión debajo, e inactiva la gestión. También la accesibilidad de «Estructura orgánica» y de sus
// diálogos con axe-core (WCAG 2.1 AA).
import AxeBuilder from '@axe-core/playwright';
import { expect, test, type Page } from '@playwright/test';
import { ingresar, simularApi } from './simulacion';

interface Unidad {
  codigo: string;
  tipo: string;
  nombre: string;
  padre?: string;
  gestionAutorizada?: string;
  comisionPermanente?: boolean;
  activa: boolean;
}

const ADMINISTRADOR = {
  estado: 200,
  cuerpo: {
    usuario: 'admin.demo',
    nombre: 'Administrador del sistema (demo)',
    roles: ['ADMINISTRADOR'],
    permisos: ['CATALOGOS:LECTURA', 'CATALOGOS:ESCRITURA', 'USUARIOS_ROLES:LECTURA'],
  },
};

const GESTIONES = [
  { codigo: 'G01', nombre: 'Rector' },
  { codigo: 'G07', nombre: 'Gestor de Investigación, Desarrollo Tecnológico e Innovación' },
];

// Las unidades en memoria, cambiadas por las operaciones simuladas como lo haría Académico.
async function simularAcademico(pagina: Page, iniciales: Unidad[] = []) {
  const unidades: Unidad[] = [...iniciales];
  await pagina.route('**/api/academico/gestiones-autorizadas', (ruta) =>
    ruta.fulfill({ json: GESTIONES }),
  );
  await pagina.route('**/api/academico/unidades', async (ruta) => {
    if (ruta.request().method() === 'POST') {
      const nueva = { ...(ruta.request().postDataJSON() as Unidad), activa: true };
      unidades.push(nueva);
      await ruta.fulfill({ status: 201, json: nueva });
      return;
    }
    await ruta.fulfill({ json: [...unidades].sort((a, b) => a.codigo.localeCompare(b.codigo)) });
  });
  await pagina.route(/\/api\/academico\/unidades\/[^/]+(\/estado)?$/, async (ruta) => {
    const [, codigo] = /unidades\/([^/]+)(\/estado)?$/.exec(ruta.request().url()) ?? [];
    const unidad = unidades.find((una) => una.codigo === codigo);
    if (!unidad) {
      await ruta.fulfill({ status: 404, json: {} });
      return;
    }
    Object.assign(unidad, ruta.request().postDataJSON() as Partial<Unidad>);
    await ruta.fulfill({ json: unidad });
  });
}

async function abrirEstructura(pagina: Page, iniciales?: Unidad[]) {
  await simularApi(pagina, { sesion: ADMINISTRADOR });
  await simularAcademico(pagina, iniciales);
  await pagina.goto('/iniciar-sesion');
  await ingresar(pagina, 'admin.demo');
  await pagina.getByRole('link', { name: 'Estructura orgánica' }).click();
  await expect(
    pagina.getByRole('heading', { level: 1, name: 'Estructura orgánica' }),
  ).toBeVisible();
}

function fila(pagina: Page, codigo: string) {
  return pagina
    .getByRole('table', { name: 'Estructura orgánica' })
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

const ESTRUCTURA: Unidad[] = [
  { codigo: 'MATRIZ', tipo: 'SEDE_MATRIZ', nombre: 'Sede matriz', activa: true },
  {
    codigo: 'RECT',
    tipo: 'GOBIERNO',
    nombre: 'Rectorado',
    padre: 'MATRIZ',
    gestionAutorizada: 'G01',
    activa: true,
  },
  {
    codigo: 'COM-EVAL',
    tipo: 'COMISION',
    nombre: 'Comisión de Evaluación Interna',
    padre: 'RECT',
    comisionPermanente: true,
    activa: true,
  },
];

test('el Administrador registra la sede matriz y una gestión debajo, e inactiva la gestión', async ({
  page,
}) => {
  await abrirEstructura(page);

  await page.getByRole('button', { name: 'Registrar unidad' }).click();
  const matriz = page.getByRole('dialog', { name: 'Registrar unidad' });
  await matriz.getByLabel('Código').fill('MATRIZ');
  await matriz.getByLabel('Nombre').fill('Sede matriz');
  await matriz.getByLabel('Tipo').selectOption('SEDE_MATRIZ');
  await matriz.getByRole('button', { name: 'Registrar' }).click();
  await expect(fila(page, 'MATRIZ')).toContainText('Sede matriz');

  await page.getByRole('button', { name: 'Registrar unidad' }).click();
  const gestion = page.getByRole('dialog', { name: 'Registrar unidad' });
  await gestion.getByLabel('Código').fill('G-INV');
  await gestion.getByLabel('Nombre').fill('Gestión de Investigación');
  await gestion.getByLabel('Tipo').selectOption('GESTION');
  await gestion.getByLabel('Depende de').selectOption('MATRIZ');
  await gestion.getByLabel('Gestión autorizada').selectOption('G07');
  await gestion.getByRole('button', { name: 'Registrar' }).click();
  await expect(fila(page, 'G-INV')).toContainText('Depende de MATRIZ');
  await expect(fila(page, 'G-INV')).toContainText('G07 · Gestor de Investigación');

  await fila(page, 'G-INV').getByRole('button', { name: 'Inactivar G-INV' }).click();
  await page
    .getByRole('dialog', { name: 'Inactivar la unidad G-INV' })
    .getByRole('button', { name: 'Inactivar' })
    .click();
  await expect(fila(page, 'G-INV')).toContainText('Inactiva');
});

test('la estructura en árbol con sus acciones', async ({ page }) => {
  await abrirEstructura(page, ESTRUCTURA);
  await expect(fila(page, 'COM-EVAL')).toContainText('Informativa, sin horas');

  await sinFallas(page);
});

for (const [codigo, boton, titulo] of [
  [undefined, 'Registrar unidad', 'Registrar unidad'],
  ['RECT', 'Corregir RECT', 'Corregir la unidad RECT'],
  ['COM-EVAL', 'Inactivar COM-EVAL', 'Inactivar la unidad COM-EVAL'],
] as const) {
  test(`el diálogo «${titulo}»`, async ({ page }) => {
    await abrirEstructura(page, ESTRUCTURA);
    const donde = codigo ? fila(page, codigo) : page;
    await donde.getByRole('button', { name: boton }).click();
    const dialogo = page.getByRole('dialog', { name: titulo });
    await expect(dialogo).toBeVisible();
    // Sin esperar a que termine la transición, axe mide el contraste a media opacidad.
    await expect(page.locator('.MuiDialog-container')).toHaveCSS('opacity', '1');
    if (!codigo) {
      await dialogo.getByLabel('Tipo').selectOption('COMISION');
    }

    await sinFallas(page);
  });
}

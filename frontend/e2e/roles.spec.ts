// Los tres escenarios de F05-H02 en un navegador, con la API simulada: el administrador crea un rol
// y se lo asigna a un usuario; un usuario con dos roles ve las funciones de los dos; uno sin
// permiso sobre «Usuarios y roles» ve «Acceso no autorizado». También la accesibilidad de la
// pantalla «Roles» con axe-core (WCAG 2.1 AA).
import AxeBuilder from '@axe-core/playwright';
import { expect, test, type Page } from '@playwright/test';
import { RESPUESTAS, TEXTO_TECNICO, ingresar, simularApi } from './simulacion';

const FUNCIONALIDADES = [
  { codigo: 'USUARIOS_ROLES', nombre: 'Usuarios y roles', tipos: ['LECTURA', 'ESCRITURA'] },
  { codigo: 'CARGAS', nombre: 'Cargas de datos', tipos: ['LECTURA', 'ESCRITURA'] },
  { codigo: 'DOCENTES', nombre: 'Docentes', tipos: ['LECTURA', 'ESCRITURA'] },
];

const ADMINISTRADOR = {
  identificador: 'ADMINISTRADOR',
  nombre: 'Administrador del sistema',
  descripcion: 'Configura usuarios, roles y catálogos.',
  predefinido: true,
  permisos: [
    { funcionalidad: 'USUARIOS_ROLES', tipo: 'LECTURA', alcance: 'TODO' },
    { funcionalidad: 'USUARIOS_ROLES', tipo: 'ESCRITURA', alcance: 'TODO' },
  ],
  usuariosAsignados: 1,
};

const DOCENTE = {
  identificador: 'DOCENTE',
  nombre: 'Docente',
  descripcion: 'Consulta su distributivo.',
  predefinido: true,
  permisos: [{ funcionalidad: 'CARGAS', tipo: 'LECTURA', alcance: 'PROPIO' }],
  usuariosAsignados: 1,
};

/** La API de Roles y la lista de usuarios, con estado: lo que se crea y se asigna queda. */
async function simularRoles(pagina: Page) {
  const roles: Record<string, unknown>[] = [ADMINISTRADOR, DOCENTE];
  const asignaciones: Record<string, string[]> = {
    'admin.demo': ['ADMINISTRADOR'],
    'docente.demo': ['DOCENTE'],
  };
  await pagina.route('**/api/roles/funcionalidades', (ruta) =>
    ruta.fulfill({ json: FUNCIONALIDADES }),
  );
  await pagina.route('**/api/roles', async (ruta) => {
    if (ruta.request().method() === 'POST') {
      const nuevo = {
        ...(ruta.request().postDataJSON() as Record<string, unknown>),
        identificador: 'AUDITOR_DE_ROLES',
        predefinido: false,
        usuariosAsignados: 0,
      };
      roles.push(nuevo);
      await ruta.fulfill({ status: 201, json: nuevo });
    } else {
      await ruta.fulfill({ json: roles });
    }
  });
  await pagina.route('**/api/roles/usuarios', (ruta) =>
    ruta.fulfill({
      json: Object.entries(asignaciones).map(([usuario, suyos]) => ({ usuario, roles: suyos })),
    }),
  );
  await pagina.route('**/api/roles/usuarios/*', async (ruta) => {
    const usuario = decodeURIComponent(ruta.request().url().split('/').at(-1) ?? '');
    const { roles: nuevos } = ruta.request().postDataJSON() as { roles: string[] };
    asignaciones[usuario] = nuevos;
    await ruta.fulfill({ json: { usuario, roles: nuevos } });
  });
  await pagina.route('**/api/usuarios/usuarios', (ruta) =>
    ruta.fulfill({
      json: [
        { usuario: 'admin.demo', nombre: 'Administración del sistema (demo)', activo: true },
        { usuario: 'docente.demo', nombre: 'Docente (demo)', activo: true },
      ],
    }),
  );
  return asignaciones;
}

const SESION_ADMINISTRADOR = {
  estado: 200,
  cuerpo: {
    usuario: 'admin.demo',
    nombre: 'Administración del sistema (demo)',
    roles: ['ADMINISTRADOR'],
    permisos: ['USUARIOS_ROLES:ESCRITURA', 'USUARIOS_ROLES:LECTURA'],
  },
};

test('rol nuevo: el administrador crea un rol y se lo asigna a docente.demo', async ({ page }) => {
  await simularApi(page, { sesion: SESION_ADMINISTRADOR });
  const asignaciones = await simularRoles(page);
  await page.goto('/iniciar-sesion');
  await ingresar(page, 'admin.demo');

  await page.getByRole('link', { name: 'Roles' }).click();
  await page.getByRole('button', { name: 'Crear rol' }).click();
  const dialogo = page.getByRole('dialog', { name: 'Rol nuevo' });
  await dialogo.getByLabel('Nombre').fill('Auditor de roles');
  await dialogo.getByLabel('Descripción').fill('Ve los roles sin cambiarlos.');
  await dialogo.getByRole('combobox', { name: 'Usuarios y roles: lectura' }).selectOption('TODO');
  await dialogo.getByRole('button', { name: 'Guardar' }).click();
  await expect(dialogo).toBeHidden();
  await expect(page.getByRole('table', { name: 'Roles' })).toContainText('Auditor de roles');

  await page.getByRole('tab', { name: 'Usuarios' }).click();
  await page.getByRole('button', { name: 'Cambiar roles de docente.demo' }).click();
  const roles = page.getByRole('dialog', { name: 'Roles de docente.demo' });
  await roles.getByRole('checkbox', { name: 'Auditor de roles' }).check();
  await roles.getByRole('button', { name: 'Guardar' }).click();
  await expect(roles).toBeHidden();

  expect(asignaciones['docente.demo']).toEqual(['AUDITOR_DE_ROLES', 'DOCENTE']);
});

test('varios roles: docente.coordinador.demo ve las funciones de sus dos roles', async ({
  page,
}) => {
  await simularApi(page, {
    sesion: {
      ...RESPUESTAS.sesionDemo,
      cuerpo: {
        ...RESPUESTAS.sesionDemo.cuerpo,
        permisos: [
          'DISTRIBUTIVO:LECTURA:PROPIO',
          'DOCENTES:LECTURA',
          'MI_DISTRIBUTIVO:ESCRITURA:PROPIO',
          'MI_DISTRIBUTIVO:LECTURA:PROPIO',
          'OFERTA:APROBACION:PROPIO',
          'OFERTA:ESCRITURA:PROPIO',
          'OFERTA:LECTURA:PROPIO',
        ],
      },
    },
  });
  await page.goto('/iniciar-sesion');
  await ingresar(page, 'docente.coordinador.demo');

  const funciones = page.getByRole('list', { name: 'Tus funciones' });
  await expect(funciones.getByRole('listitem')).toHaveCount(4);
  await expect(funciones).toContainText('Mi distributivo');
  await expect(funciones).toContainText('Oferta y propuesta de docentes');
  await expect(page.getByRole('link', { name: 'Roles' })).toHaveCount(0);
});

test('solo lectura: docente.demo abre /roles y ve «Acceso no autorizado»', async ({ page }) => {
  await simularApi(page, {
    sesion: {
      estado: 200,
      cuerpo: {
        usuario: 'docente.demo',
        roles: ['DOCENTE'],
        permisos: ['MI_DISTRIBUTIVO:ESCRITURA:PROPIO', 'MI_DISTRIBUTIVO:LECTURA:PROPIO'],
      },
    },
  });
  const pedidas: string[] = [];
  page.on('request', (peticion) => pedidas.push(new URL(peticion.url()).pathname));
  await page.goto('/roles');
  await ingresar(page, 'docente.demo');

  await expect(page).toHaveURL(/\/roles$/);
  await expect(page.getByRole('alert')).toContainText('Acceso no autorizado');
  for (const tecnico of TEXTO_TECNICO) {
    await expect(page.locator('body')).not.toContainText(tecnico);
  }
  expect(pedidas.filter((ruta) => ruta.startsWith('/api/roles'))).toEqual([]);
});

test.describe('accesibilidad de la pantalla «Roles»', () => {
  test.skip(({ browserName }) => browserName !== 'chromium', 'axe-core corre solo en Chromium');

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

  test('la lista, el formulario de un rol nuevo y la pestaña Usuarios', async ({ page }) => {
    await simularApi(page, { sesion: SESION_ADMINISTRADOR });
    await simularRoles(page);
    await page.goto('/iniciar-sesion');
    await ingresar(page, 'admin.demo');
    await page.getByRole('link', { name: 'Roles' }).click();
    await expect(page.getByRole('table', { name: 'Roles' })).toBeVisible();
    await sinFallas(page);

    await page.getByRole('button', { name: 'Crear rol' }).click();
    await expect(page.getByRole('dialog', { name: 'Rol nuevo' })).toBeVisible();
    await sinFallas(page);
    await page.getByRole('button', { name: 'Cerrar' }).click();

    await page.getByRole('tab', { name: 'Usuarios' }).click();
    await expect(page.getByRole('table', { name: 'Usuarios y sus roles' })).toBeVisible();
    await sinFallas(page);
  });
});

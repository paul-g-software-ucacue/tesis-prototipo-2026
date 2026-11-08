// F17-H02 en un navegador, con la API simulada: el Vicerrector filtra la bitácora por usuario y
// fecha y ve solo esas entradas (US1-1), un Coordinador de carrera no la ve (US2), y la pantalla no
// tiene fallas de accesibilidad con axe-core (WCAG 2.1 AA).
import AxeBuilder from '@axe-core/playwright';
import { expect, test, type Page } from '@playwright/test';
import { ingresar, simularApi } from './simulacion';

const VICERRECTOR = {
  estado: 200,
  cuerpo: {
    usuario: 'vicerrectorado.demo',
    nombre: 'Vicerrectorado (demo)',
    roles: ['VICERRECTOR'],
    permisos: ['DISTRIBUTIVO:LECTURA', 'REPORTES:LECTURA', 'BITACORA:LECTURA'],
  },
};

const COORDINADOR = {
  estado: 200,
  cuerpo: {
    usuario: 'coordinacion.demo',
    nombre: 'Coordinación (demo)',
    roles: ['COORDINADOR_CARRERA'],
    permisos: ['OFERTA:LECTURA', 'DISTRIBUTIVO:LECTURA', 'DOCENTES:LECTURA'],
  },
};

const DE_COORDINACION = {
  id: '6f1c2d4e-8a9b-4c3d-9e1f-0a2b3c4d5e6f',
  servicio: 'generacion',
  usuario: 'coordinacion.demo',
  accion: 'ACTIVIDAD_GUARDADA',
  momento: '2026-11-07T17:02:14Z',
  periodo: '2026-I',
  objeto: { periodo: '2026-I', docente: '0102030405', actividad: '140-b' },
  anterior: { horas: '4' },
  nuevo: { horas: '6' },
};

const DE_OTRO = {
  ...DE_COORDINACION,
  id: '7a2d3e5f-9b0c-4d4e-8f20-1b3c4d5e6f70',
  usuario: 'admin.demo',
  servicio: 'roles',
  accion: 'EDITAR_ROL',
  periodo: undefined,
  objeto: { identificador: 'ROL_DE_PRUEBA' },
};

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

// El servidor simulado filtra como la API: por usuario y por día en la hora de Ecuador.
async function simularBitacora(pagina: Page) {
  await pagina.route('**/api/usuarios/bitacora/acciones', (ruta) =>
    ruta.fulfill({
      json: [
        { servicio: 'generacion', accion: 'ACTIVIDAD_GUARDADA' },
        { servicio: 'roles', accion: 'EDITAR_ROL' },
      ],
    }),
  );
  await pagina.route('**/api/usuarios/bitacora?*', (ruta) => {
    const parametros = new URL(ruta.request().url()).searchParams;
    const usuario = parametros.get('usuario');
    const entradas = [DE_COORDINACION, DE_OTRO].filter(
      (una) => !usuario || una.usuario === usuario,
    );
    return ruta.fulfill({
      json: {
        entradas,
        total: entradas.length,
        pagina: 1,
        tamano: 50,
        corte: '2026-11-07T20:00:00Z',
      },
    });
  });
  await pagina.route('**/api/usuarios/bitacora', (ruta) =>
    ruta.fulfill({
      json: {
        entradas: [DE_COORDINACION, DE_OTRO],
        total: 2,
        pagina: 1,
        tamano: 50,
        corte: '2026-11-07T20:00:00Z',
      },
    }),
  );
}

async function abrirComoVicerrector(pagina: Page) {
  await simularApi(pagina, { sesion: VICERRECTOR });
  await simularBitacora(pagina);
  await pagina.goto('/iniciar-sesion');
  await ingresar(pagina, VICERRECTOR.cuerpo.usuario);
  await pagina.getByRole('link', { name: 'Bitácora' }).click();
  await expect(pagina.getByRole('heading', { level: 1, name: 'Bitácora' })).toBeVisible();
  await expect(pagina.getByRole('table', { name: 'Entradas de la bitácora' })).toBeVisible();
}

test.describe('bitácora', () => {
  test('el Vicerrector filtra por usuario y fecha y ve solo esas entradas (US1-1)', async ({
    page,
  }) => {
    await abrirComoVicerrector(page);
    await expect(page.getByText('2 entradas')).toBeVisible();

    await page.getByLabel('Quién hizo el cambio').fill('coordinacion.demo');
    await page.getByLabel('Desde').fill('2026-11-07');
    await page.getByLabel('Hasta').fill('2026-11-07');
    await page.getByRole('button', { name: 'Consultar' }).click();

    await expect(page.getByText('1 entrada', { exact: true })).toBeVisible();
    await expect(page).toHaveURL(/usuario=coordinacion\.demo/);
    const tabla = page.getByRole('table', { name: 'Entradas de la bitácora' });
    await expect(tabla.getByText('admin.demo')).toHaveCount(0);
    await page.getByRole('button', { name: 'Ver valores' }).click();
    const valores = page.getByRole('region', { name: 'Valores de la entrada' });
    await expect(valores.getByRole('group', { name: 'Valor anterior' })).toContainText('horas: 4');
    await expect(valores.getByRole('group', { name: 'Valor nuevo' })).toContainText('horas: 6');
  });

  test('un Coordinador de carrera no ve la opción y la dirección muestra el aviso (US2)', async ({
    page,
  }) => {
    await simularApi(page, { sesion: COORDINADOR });
    let consultas = 0;
    await page.route('**/api/usuarios/bitacora**', (ruta) => {
      consultas += 1;
      return ruta.fulfill({ status: 403, json: {} });
    });
    // El token vive en memoria (D-51): se abre la dirección antes de iniciar sesión, y el inicio
    // de sesión lleva de vuelta a ella.
    await page.goto('/bitacora');
    await ingresar(page, COORDINADOR.cuerpo.usuario);

    await expect(page).toHaveURL(/\/bitacora$/);
    await expect(page.getByRole('alert')).toContainText('Acceso no autorizado');
    await expect(page.getByRole('navigation', { name: 'Menú principal' })).toBeVisible();
    await expect(page.getByRole('link', { name: 'Bitácora' })).toHaveCount(0);
    expect(consultas).toBe(0);
  });

  test.describe('accesibilidad', () => {
    test.skip(({ browserName }) => browserName !== 'chromium', 'axe-core corre solo en Chromium');

    test('con las entradas y una expandida', async ({ page }) => {
      await abrirComoVicerrector(page);
      await page.getByRole('button', { name: 'Ver valores' }).first().click();

      await sinFallas(page);
    });

    test('con un filtro marcado antes de enviar', async ({ page }) => {
      await abrirComoVicerrector(page);
      await page.getByLabel('Desde').fill('2026-11-07');
      await page.getByLabel('Hasta').fill('2026-11-06');
      await page.getByRole('button', { name: 'Consultar' }).click();
      await expect(
        page.getByText('La fecha hasta no puede ser anterior a la fecha desde.'),
      ).toBeVisible();

      await sinFallas(page);
    });
  });
});

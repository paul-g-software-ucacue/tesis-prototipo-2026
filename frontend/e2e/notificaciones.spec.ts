// F16-H01 en un navegador, con la API simulada: el Vicerrector ve en la campana una notificación
// sin leer, la abre con el teclado, llega a «Consolidado del periodo» y la campana deja de
// contarla. axe-core (WCAG 2.1 AA) sin fallas con la lista cerrada, abierta y vacía (SC-006).
import AxeBuilder from '@axe-core/playwright';
import { expect, test, type Page } from '@playwright/test';
import { ingresar, simularApi } from './simulacion';

const VICERRECTOR = {
  estado: 200,
  cuerpo: {
    usuario: 'vicerrectorado.demo',
    nombre: 'Vicerrectorado académico (demo)',
    roles: ['VICERRECTOR'],
    permisos: ['DISTRIBUTIVO:LECTURA', 'DISTRIBUTIVO:ESCRITURA', 'DISTRIBUTIVO:APROBACION'],
  },
};

const NOTIFICACION = {
  id: '3f2b8c1e-5d4a-4e6f-9a7b-1c2d3e4f5a6b',
  periodo: '2026-2',
  titulo: 'El distributivo llegó a «Revisión del Vicerrectorado»',
  detalle: 'Periodo 2026-2, versión 1.',
  enlace: '/consolidado',
  autor: 'consolidacion.demo',
  autorNombre: 'Responsable de consolidación (demo)',
  momento: '2026-11-15T20:04:00Z',
  leida: false,
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

async function conLaBandeja(pagina: Page, notificaciones: (typeof NOTIFICACION)[]) {
  let leida = false;
  await pagina.route('**/api/notificaciones/bandeja/sin-leer', (ruta) =>
    ruta.fulfill({
      json: { sinLeer: leida ? 0 : notificaciones.filter((una) => !una.leida).length },
    }),
  );
  await pagina.route('**/api/notificaciones/bandeja?*', (ruta) =>
    ruta.fulfill({
      json: {
        notificaciones: notificaciones.map((una) => ({ ...una, leida: una.leida || leida })),
        sinLeer: leida ? 0 : notificaciones.length,
      },
    }),
  );
  await pagina.route('**/api/notificaciones/bandeja/*/lectura', (ruta) => {
    leida = true;
    return ruta.fulfill({
      json: { ...NOTIFICACION, leida: true, leidaEn: '2026-11-15T20:05:00Z' },
    });
  });
  await pagina.route('**/api/academico/periodos', (ruta) => ruta.fulfill({ json: [] }));
}

test('el Vicerrector abre su notificación con el teclado y llega a la pantalla del hecho', async ({
  page,
}) => {
  await simularApi(page, { sesion: VICERRECTOR });
  await conLaBandeja(page, [NOTIFICACION]);

  await page.goto('/iniciar-sesion');
  await ingresar(page, 'vicerrectorado.demo');
  const campana = page.getByRole('button', { name: 'Notificaciones, 1 sin leer' });
  await expect(campana).toBeVisible();
  await sinFallas(page);

  await campana.focus();
  await page.keyboard.press('Enter');
  const lista = page.getByRole('list', { name: 'Notificaciones' });
  await expect(lista).toContainText('El distributivo llegó a «Revisión del Vicerrectorado»');
  await expect(lista).toContainText('Responsable de consolidación (demo)');
  // axe mide el contraste: primero, que termine la animación de apertura.
  await expect(page.locator('.MuiPopover-paper')).toHaveCSS('opacity', '1');
  await sinFallas(page);

  await page.keyboard.press('Tab');
  await expect(lista.getByRole('button', { name: /El distributivo llegó/ })).toBeFocused();
  await page.keyboard.press('Enter');

  await expect(page).toHaveURL(/\/consolidado\?periodo=2026-2$/);
  await expect(
    page.getByRole('heading', { level: 1, name: 'Consolidado del periodo' }),
  ).toBeVisible();
  await expect(page.getByRole('button', { name: 'Notificaciones', exact: true })).toBeVisible();
});

test('Escape cierra la lista; sin notificaciones, la lista lo dice y no tiene fallas', async ({
  page,
}) => {
  await simularApi(page, { sesion: VICERRECTOR });
  await conLaBandeja(page, []);

  await page.goto('/iniciar-sesion');
  await ingresar(page, 'vicerrectorado.demo');
  const campana = page.getByRole('button', { name: 'Notificaciones', exact: true });
  await campana.click();

  await expect(page.getByText('No tienes notificaciones')).toBeVisible();
  await expect(page.locator('.MuiPopover-paper')).toHaveCSS('opacity', '1');
  await sinFallas(page);
  await page.keyboard.press('Escape');
  await expect(page.getByText('No tienes notificaciones')).toBeHidden();
  await expect(campana).toBeFocused();
});

// Flujos de la sesión en un navegador (D-50, D-51; SC-007, SC-008): rutas protegidas, el token solo
// en memoria, la recarga, el 401 y el ancho mínimo de 1366 px (RNF-COM-01).
import { expect, test } from '@playwright/test';
import { RESPUESTAS, ingresar, simularApi } from './simulacion';

test('sin sesión, Inicio lleva al inicio de sesión y después vuelve a Inicio', async ({ page }) => {
  await simularApi(page);
  await page.goto('/');

  await expect(page).toHaveURL(/\/iniciar-sesion\?destino=%2F$/);
  await ingresar(page);

  await expect(page).toHaveURL(/\/$/);
  await expect(page.getByRole('heading', { level: 1, name: 'Hola, autor.prueba' })).toBeVisible();
  await expect(page.getByRole('listitem').filter({ hasText: 'DOCENTE' })).toBeVisible();
});

test('después de iniciar sesión, el token no está en el navegador ni en la URL', async ({
  page,
  context,
}) => {
  await simularApi(page);
  await page.goto('/iniciar-sesion');
  await ingresar(page);
  await expect(page.getByRole('heading', { level: 1, name: 'Hola, autor.prueba' })).toBeVisible();

  const almacenado = await page.evaluate(() => ({
    local: JSON.stringify({ ...window.localStorage }),
    sesion: JSON.stringify({ ...window.sessionStorage }),
    cookie: document.cookie,
  }));
  expect(almacenado).toEqual({ local: '{}', sesion: '{}', cookie: '' });
  expect(await context.cookies()).toEqual([]);
  expect(page.url()).not.toContain('cabecera.cuerpo.firma');
});

test('una recarga vuelve al inicio de sesión: el token estaba solo en memoria', async ({
  page,
}) => {
  await simularApi(page);
  await page.goto('/iniciar-sesion');
  await ingresar(page);
  await expect(page.getByRole('heading', { level: 1, name: 'Hola, autor.prueba' })).toBeVisible();

  await page.reload();

  await expect(page).toHaveURL(/\/iniciar-sesion/);
});

test('un 401 de la sesión actual cierra la sesión y lo dice', async ({ page }) => {
  await simularApi(page, { sesion: RESPUESTAS.sesionNoValida });
  await page.goto('/iniciar-sesion');

  await ingresar(page);

  await expect(page).toHaveURL(/\/iniciar-sesion\?.*motivo=sesion-terminada/);
  await expect(page.getByRole('alert')).toContainText('Tu sesión terminó. Inicia sesión de nuevo.');
});

test('Cerrar sesión vuelve al inicio de sesión sin aviso', async ({ page }) => {
  await simularApi(page);
  await page.goto('/iniciar-sesion');
  await ingresar(page);
  await page.getByRole('button', { name: 'Cerrar sesión' }).click();

  await expect(page).toHaveURL(/\/iniciar-sesion$/);
  await expect(page.getByRole('alert')).toHaveCount(0);
});

test('con 1366 px de ancho, Inicio no tiene desplazamiento horizontal', async ({ page }) => {
  await simularApi(page);
  await page.goto('/iniciar-sesion');
  await ingresar(page);
  await expect(page.getByRole('heading', { level: 1, name: 'Hola, autor.prueba' })).toBeVisible();

  const desborda = await page.evaluate(
    () => document.documentElement.scrollWidth > document.documentElement.clientWidth,
  );
  expect(desborda).toBe(false);
});

// Escenario 3 de B00-H06 (FR-013, SC-003): la pantalla de inicio de sesión no tiene fallas de
// WCAG 2.1 AA según axe-core, en un navegador real (también el contraste de color).
import AxeBuilder from '@axe-core/playwright';
import { expect, test, type Page } from '@playwright/test';
import { RESPUESTAS, ingresar, simularApi } from './simulacion';

const WCAG_21_AA = ['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa'];

async function sinFallas(pagina: Page) {
  const resultado = await new AxeBuilder({ page: pagina }).withTags(WCAG_21_AA).analyze();
  const fallas = resultado.violations.map(
    (falla) =>
      `${falla.id} (${falla.impact}): ${falla.nodes.map((nodo) => nodo.target.join(' ')).join(', ')}`,
  );
  expect(fallas, fallas.join('\n')).toEqual([]);
}

test.describe('accesibilidad del inicio de sesión', () => {
  test.skip(({ browserName }) => browserName !== 'chromium', 'axe-core corre solo en Chromium');

  test('en su estado inicial', async ({ page }) => {
    await page.goto('/iniciar-sesion');
    await expect(page.getByRole('heading', { level: 1, name: 'Iniciar sesión' })).toBeVisible();

    await sinFallas(page);
  });

  test('con el error de credenciales inválidas', async ({ page }) => {
    await simularApi(page, { inicio: RESPUESTAS.credencialesInvalidas });
    await page.goto('/iniciar-sesion');
    await ingresar(page);
    await expect(page.getByRole('alert')).toBeVisible();

    await sinFallas(page);
  });

  test('con los errores de campo vacío', async ({ page }) => {
    await page.goto('/iniciar-sesion');
    await ingresar(page, '', '');
    await expect(page.getByText('Ingresa tu contraseña.')).toBeVisible();

    await sinFallas(page);
  });
});

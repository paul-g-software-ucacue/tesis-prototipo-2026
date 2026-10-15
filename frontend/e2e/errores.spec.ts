// Escenario 2 de B00-H06 (FR-010 a FR-012, SC-002): cada respuesta de error llega al usuario con
// su título y su descripción en español, sin el código, el estado HTTP ni texto técnico.
import { expect, test, type Page } from '@playwright/test';
import {
  RESPUESTAS,
  TEXTO_TECNICO,
  ingresar,
  simularApi,
  type RespuestaSimulada,
} from './simulacion';

async function sinTextoTecnico(pagina: Page) {
  const texto = await pagina.locator('body').innerText();
  for (const patron of TEXTO_TECNICO) {
    expect(texto).not.toMatch(patron);
  }
}

const AL_INICIAR_SESION: [string, RespuestaSimulada, string, string][] = [
  [
    '401 de credenciales inválidas',
    RESPUESTAS.credencialesInvalidas,
    'Credenciales inválidas',
    'El usuario o la contraseña no son correctos.',
  ],
  [
    '400 con detalles',
    RESPUESTAS.conDetalles,
    'Solicitud inválida',
    'Revisa los campos marcados y vuelve a intentarlo.',
  ],
  [
    '500 con idIncidente',
    RESPUESTAS.errorInterno,
    'Error inesperado',
    'Ocurrió un error inesperado.',
  ],
  [
    '503 del API Gateway',
    RESPUESTAS.servicioNoDisponible,
    'Servicio no disponible',
    'El servicio no está disponible en este momento.',
  ],
  [
    'HTML de un proxy',
    RESPUESTAS.htmlDeUnProxy,
    'Error inesperado',
    'Ocurrió un error inesperado.',
  ],
  [
    'sin conexión',
    RESPUESTAS.sinConexion,
    'Servicio no disponible',
    'El servicio no está disponible en este momento.',
  ],
];

for (const [caso, respuesta, titulo, descripcion] of AL_INICIAR_SESION) {
  test(`al iniciar sesión, ${caso} se muestra en español y sin texto técnico`, async ({ page }) => {
    await simularApi(page, { inicio: respuesta });
    await page.goto('/iniciar-sesion');

    await ingresar(page);

    const aviso = page.getByRole('alert');
    await expect(aviso).toContainText(titulo);
    await expect(aviso).toContainText(descripcion);
    await sinTextoTecnico(page);
  });
}

test('el 500 muestra el identificador del incidente', async ({ page }) => {
  await simularApi(page, { inicio: RESPUESTAS.errorInterno });
  await page.goto('/iniciar-sesion');

  await ingresar(page);

  await expect(page.getByRole('alert')).toContainText(
    'Identificador del incidente: 6f1c2b9e-1d2a-4c3b-8e4f-5a6b7c8d9e0f',
  );
});

test('el 400 muestra cada detalle bajo su campo', async ({ page }) => {
  await simularApi(page, { inicio: RESPUESTAS.conDetalles });
  await page.goto('/iniciar-sesion');

  await ingresar(page);

  await expect(page.getByLabel('Usuario')).toHaveAccessibleDescription('es obligatorio');
});

test('un 404 de la sesión actual se muestra en Inicio con su título y su descripción', async ({
  page,
}) => {
  await simularApi(page, { sesion: RESPUESTAS.recursoNoEncontrado });
  await page.goto('/iniciar-sesion');

  await ingresar(page);

  const aviso = page.getByRole('alert');
  await expect(aviso).toContainText('Recurso no encontrado');
  await expect(aviso).toContainText('La dirección solicitada no existe.');
  await sinTextoTecnico(page);
});

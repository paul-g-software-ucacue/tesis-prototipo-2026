// F12-H01 en un navegador, con la API simulada: ver lo que rige, un rechazo con todos sus problemas,
// cargar un archivo y su accesibilidad con axe-core (WCAG 2.1 AA).
import AxeBuilder from '@axe-core/playwright';
import { expect, test, type Page } from '@playwright/test';
import { ingresar, simularApi } from './simulacion';

const ADMINISTRADOR = {
  estado: 200,
  cuerpo: {
    usuario: 'admin.demo',
    nombre: 'Administrador (demo)',
    roles: ['ADMINISTRADOR'],
    permisos: ['FLUJO_FIRMAS:LECTURA', 'FLUJO_FIRMAS:ESCRITURA', 'DISTRIBUTIVO:LECTURA'],
  },
};

const RUTA = '**/api/validacionocs/periodos/*/configuracion-flujo';

const CONFIGURACION = {
  periodo: '2026-2',
  porDefecto: true,
  modificable: true,
  etapas: [
    {
      tipo: 'BORRADOR_CARRERA',
      esqueleto: true,
      activa: true,
      nombre: 'Propuesta de carrera',
      rol: 'COORDINADOR_CARRERA',
      plazoDias: 15,
      admiteDevolucion: false,
    },
    {
      tipo: 'REMITIDO_ZONAL',
      esqueleto: false,
      activa: false,
      nombre: 'Remitido a la instancia zonal',
      rol: 'VICERRECTOR',
      plazoDias: 10,
      admiteDevolucion: false,
    },
  ],
  firmas: [
    {
      orden: 1,
      etiqueta: 'Elaborado',
      rol: 'RESPONSABLE_CONSOLIDACION',
      etapa: 'BORRADOR_CARRERA',
      documento: 'CONSOLIDADO',
    },
  ],
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

// advertencias: las de la carga (F12-H03); rechazarPrimero: si la primera carga se rechaza.
async function abrir(pagina: Page, advertencias: object[] = [], rechazarPrimero = true) {
  let cargada = false;
  let rechazar = rechazarPrimero;
  await simularApi(pagina, { sesion: ADMINISTRADOR });
  await pagina.route('**/api/academico/periodos', (ruta) =>
    ruta.fulfill({
      json: [
        {
          codigo: '2026-2',
          nombre: 'Periodo 2026-2',
          inicio: '2026-09-01',
          fin: '2027-02-28',
          estado: 'PLANIFICACION',
        },
      ],
    }),
  );
  await pagina.route(RUTA, async (ruta) => {
    if (ruta.request().method() === 'PUT' && rechazar) {
      rechazar = false;
      await ruta.fulfill({
        status: 400,
        json: {
          titulo: 'Configuración con problemas',
          descripcion: 'Corrige lo que se indica y vuelve a cargar el archivo.',
          codigo: 'VALIDACIONOCS_CONFIGURACION_INVALIDA',
          detalles: [
            { campo: 'archivo', descripcion: 'Falta la etapa EN_OCS.' },
            { campo: 'archivo', descripcion: 'El rol RECTOR no tiene usuarios.' },
          ],
        },
      });
      return;
    }
    if (ruta.request().method() === 'PUT') {
      cargada = true;
    }
    await ruta.fulfill({
      json: cargada
        ? {
            ...CONFIGURACION,
            porDefecto: false,
            version: 1,
            cargada: { por: 'admin.demo', en: '2026-11-02T15:00:00Z' },
            advertencias,
          }
        : CONFIGURACION,
    });
  });
  await pagina.goto('/iniciar-sesion');
  await ingresar(pagina, ADMINISTRADOR.cuerpo.usuario);
  await pagina.getByRole('link', { name: 'Flujo y firmas' }).click();
  await expect(pagina.getByRole('heading', { level: 1, name: 'Flujo y firmas' })).toBeVisible();
}

const ARCHIVO = {
  name: 'flujo.yaml',
  mimeType: 'text/yaml',
  buffer: Buffer.from('etapas: {}\n'),
};

test('ve lo que rige, un rechazo dice qué corregir y carga, accesible en cada estado', async ({
  page,
}) => {
  await abrir(page);

  await expect(page.getByRole('table', { name: 'Etapas del flujo' })).toBeVisible();
  await expect(page.getByRole('status', { name: 'Configuración por defecto' })).toBeVisible();
  await sinFallas(page);

  await page.getByLabel('Cargar un archivo').setInputFiles(ARCHIVO);
  await expect(page.getByText('Falta la etapa EN_OCS.')).toBeVisible();
  await expect(page.getByText('El rol RECTOR no tiene usuarios.')).toBeVisible();
  await expect(page.getByRole('status', { name: 'Configuración por defecto' })).toBeVisible();
  await sinFallas(page);

  await page.getByLabel('Cargar un archivo').setInputFiles(ARCHIVO);
  await expect(page.getByText(/Versión 1 · cargada por admin\.demo/)).toBeVisible();
  await sinFallas(page);
});

// F12-H03 (D-114): una carga con la advertencia de RN-23 queda vigente y lo dice; axe-core con el
// aviso a la vista.
test('una carga con la advertencia de RN-23 queda vigente, lo dice y es accesible', async ({
  page,
}) => {
  await abrir(
    page,
    [
      {
        codigo: 'RN23_MARGEN_INSUFICIENTE',
        mensaje:
          'Con estos plazos, contados desde hoy, el distributivo se aprobaría el 02/01/2027, después del 20/12/2026, que es 30 días antes del inicio del periodo (RN-23). Le faltan 13 días: acorta plazos o desactiva etapas opcionales.',
        ultimoPlazo: '2027-01-02',
        inicioDelPeriodo: '2027-01-19',
        diasAntesDelInicio: 30,
        fechaDeRemision: '2026-12-20',
        diasDeMargen: -13,
      },
    ],
    false,
  );

  await page.getByLabel('Cargar un archivo').setInputFiles(ARCHIVO);
  const aviso = page.getByRole('status', { name: 'Advertencia de RN-23' });
  await expect(aviso).toContainText('La configuración quedó vigente.');
  await expect(aviso).toContainText('Faltan 13 días de margen');
  await expect(page.getByText(/Versión 1 · cargada por admin\.demo/)).toBeVisible();
  await sinFallas(page);
});

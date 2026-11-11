// F10-H02 en un navegador, con la API simulada: consolidar, adjuntar el informe y su accesibilidad
// con axe-core (WCAG 2.1 AA). F10-H03: lo que falta para enviar a revisión, y el envío. F12-H05:
// devolver dos carreras con su observación. F13-H01: el Vicerrector aprueba el contenido y ve la
// constancia de la versión congelada.
import AxeBuilder from '@axe-core/playwright';
import { expect, test, type Page } from '@playwright/test';
import { ingresar, simularApi } from './simulacion';

const CONSOLIDACION = {
  estado: 200,
  cuerpo: {
    usuario: 'consolidacion.demo',
    nombre: 'Responsable de consolidación (demo)',
    roles: ['RESPONSABLE_CONSOLIDACION'],
    permisos: [
      'CONSOLIDACION:LECTURA',
      'CONSOLIDACION:ESCRITURA',
      'DISTRIBUTIVO:LECTURA',
      'OFERTA:LECTURA',
    ],
  },
};

const VICERRECTORADO = {
  estado: 200,
  cuerpo: {
    usuario: 'vicerrectorado.demo',
    nombre: 'Vicerrectorado (demo)',
    roles: ['VICERRECTOR'],
    permisos: [
      'DISTRIBUTIVO:LECTURA',
      'DISTRIBUTIVO:ESCRITURA',
      'DISTRIBUTIVO:APROBACION',
      'DISTRIBUTIVO:FIRMA',
    ],
  },
};

const HORAS = { clase: 10, otrasDocencia: 6, investigacion: 0, vinculacion: 0, gestion: 2 };

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

// propia: otra respuesta del consolidado, para el envío a revisión (F10-H03).
async function abrirConsolidado(
  pagina: Page,
  propia?: () => object,
  sesion: typeof CONSOLIDACION = CONSOLIDACION,
) {
  let consolidado = false;
  let informe = false;
  const comun = () => ({
    periodo: '2026-2',
    creado: true,
    modificable: true,
    version: 1,
    desactualizado: false,
    carreras: [
      {
        carrera: 'TDS',
        estado: 'EN_ELABORACION',
        paralelos: 3,
        propuestos: 2,
        completo: false,
        horas: HORAS,
      },
    ],
    totales: HORAS,
    abiertas: { duras: 1, blandas: 1 },
    ...(consolidado
      ? { consolidacion: { por: 'consolidacion.demo', en: '2026-11-02T15:00:00Z' } }
      : {}),
    ...(informe
      ? {
          informe: {
            nombre: 'informe.pdf',
            tamano: 2048,
            huella: 'a'.repeat(64),
            por: 'consolidacion.demo',
            en: '2026-11-02T15:10:00Z',
          },
        }
      : {}),
  });
  const respuesta = (): { informe?: unknown } => propia?.() ?? comun();
  await simularApi(pagina, { sesion });
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
  await pagina.route('**/api/academico/carreras', (ruta) => ruta.fulfill({ json: [] }));
  await pagina.route('**/api/generacion/periodos/*/consolidado', async (ruta) => {
    if (ruta.request().method() === 'POST') {
      consolidado = true;
    }
    await ruta.fulfill({ json: respuesta() });
  });
  await pagina.route('**/api/generacion/periodos/*/consolidado/informe', async (ruta) => {
    informe = true;
    await ruta.fulfill({ json: respuesta().informe });
  });
  await pagina.goto('/iniciar-sesion');
  await ingresar(pagina, sesion.cuerpo.usuario);
  await pagina.getByRole('link', { name: 'Consolidado del periodo' }).click();
  await expect(
    pagina.getByRole('heading', { level: 1, name: 'Consolidado del periodo' }),
  ).toBeVisible();
}

test('consolida, adjunta el informe y es accesible en cada estado', async ({ page }) => {
  await abrirConsolidado(page);

  await expect(page.getByRole('table', { name: 'Carreras del consolidado' })).toBeVisible();
  await sinFallas(page);

  await page.getByRole('button', { name: 'Consolidar' }).click();
  await expect(page.getByText(/Consolidado por consolidacion\.demo/)).toBeVisible();
  await sinFallas(page);

  await page.getByLabel('Adjuntar el informe').setInputFiles({
    name: 'informe.pdf',
    mimeType: 'application/pdf',
    buffer: Buffer.from('%PDF-1.7\n%%EOF'),
  });
  await expect(page.getByRole('button', { name: 'Descargar el informe' })).toBeVisible();
  await sinFallas(page);
});

test('dice lo que falta para enviar a revisión, envía cuando está listo y es accesible', async ({
  page,
}) => {
  let etapa = 'EN_CONSOLIDACION';
  let listo = false;
  await page.route('**/api/validacionocs/periodos/*/flujo', (ruta) =>
    ruta.fulfill({
      json: {
        periodo: '2026-2',
        etapa,
        nombreDeEtapa:
          etapa === 'EN_CONSOLIDACION' ? 'Consolidación' : 'Revisión del Vicerrectorado',
        version: 1,
        parciales: [{ carrera: 'TDS', enviado: true, pendienteDeConfirmar: false }],
        acciones: etapa === 'EN_CONSOLIDACION' ? ['ENVIAR_A_REVISION'] : [],
        transiciones: [],
      },
    }),
  );
  await page.route('**/api/validacionocs/periodos/*/revision', async (ruta) => {
    etapa = 'EN_REVISION_VRA';
    await ruta.fulfill({
      json: {
        periodo: '2026-2',
        etapa,
        nombreDeEtapa: 'Revisión del Vicerrectorado',
        version: 1,
        parciales: [],
        acciones: [],
        transiciones: [],
      },
    });
  });
  await abrirConsolidado(page, () => ({
    periodo: '2026-2',
    creado: true,
    modificable: true,
    version: 1,
    desactualizado: false,
    carreras: [
      {
        carrera: 'TDS',
        estado: listo ? 'ENVIADO' : 'EN_ELABORACION',
        paralelos: 3,
        propuestos: 3,
        completo: true,
        horas: HORAS,
      },
    ],
    totales: HORAS,
    abiertas: listo ? { duras: 0, blandas: 0 } : { duras: 1, blandas: 1 },
    consolidacion: { por: 'consolidacion.demo', en: '2026-11-02T15:00:00Z' },
    ...(listo
      ? {
          informe: {
            nombre: 'informe.pdf',
            tamano: 2048,
            huella: 'a'.repeat(64),
            por: 'consolidacion.demo',
            en: '2026-11-02T15:10:00Z',
          },
        }
      : {}),
  }));

  const falta = page.getByRole('list', { name: 'Falta para enviar a revisión' });
  await expect(falta).toContainText('Adjunta el informe sustentado.');
  await expect(falta).toContainText('Quedan 2 inconsistencias abiertas');
  await expect(page.getByRole('button', { name: 'Enviar a revisión' })).toBeDisabled();
  await sinFallas(page);

  // Volver a consolidar recarga el consolidado, que ya está listo.
  listo = true;
  await page.getByRole('button', { name: 'Volver a consolidar' }).click();
  await expect(falta).toHaveCount(0);
  await page.getByRole('button', { name: 'Enviar a revisión' }).click();
  await page.getByRole('dialog').getByRole('button', { name: 'Enviar' }).click();
  await expect(page.getByText('Revisión del Vicerrectorado · versión 1')).toBeVisible();
  await expect(page.getByRole('button', { name: 'Enviar a revisión' })).toHaveCount(0);
  await sinFallas(page);
});

// F12-H05: el Responsable devuelve dos carreras, cada una con su observación.
test('devuelve dos carreras con su observación y el diálogo es accesible', async ({ page }) => {
  let devueltas: { carrera: string; observacion: string }[] = [];
  const flujo = () => ({
    periodo: '2026-2',
    etapa: devueltas.length > 0 ? 'BORRADOR_CARRERA' : 'EN_CONSOLIDACION',
    nombreDeEtapa: devueltas.length > 0 ? 'Propuesta de carrera' : 'Consolidación',
    version: 1,
    parciales: ['TDS', 'DS', 'EL'].map((carrera) => {
      const devuelta = devueltas.find((una) => una.carrera === carrera);
      return devuelta
        ? {
            carrera,
            enviado: false,
            pendienteDeConfirmar: false,
            devolucion: {
              por: 'consolidacion.demo',
              en: '2026-11-08T22:10:00Z',
              observacion: devuelta.observacion,
            },
          }
        : { carrera, enviado: true, pendienteDeConfirmar: false };
    }),
    acciones: devueltas.length > 0 ? [] : ['ENVIAR_A_REVISION', 'DEVOLVER_PARCIALES'],
    transiciones: [],
  });
  await page.route('**/api/validacionocs/periodos/*/flujo', (ruta) =>
    ruta.fulfill({ json: flujo() }),
  );
  await page.route('**/api/validacionocs/periodos/*/devolucion', async (ruta) => {
    devueltas = (ruta.request().postDataJSON() as { devoluciones: typeof devueltas }).devoluciones;
    await ruta.fulfill({ json: flujo() });
  });
  await abrirConsolidado(page);

  await page.getByRole('button', { name: 'Devolver parciales' }).click();
  const dialogo = page.getByRole('dialog', { name: 'Devolver parciales' });
  await dialogo.getByRole('checkbox', { name: 'TDS', exact: true }).check();
  await dialogo.getByRole('checkbox', { name: 'DS', exact: true }).check();
  await dialogo.getByLabel(/^Observación para TDS/).fill('Faltan docentes en primer nivel.');
  await dialogo.getByLabel(/^Observación para DS/).fill('corta');
  await expect(dialogo.getByRole('button', { name: 'Devolver' })).toBeDisabled();
  await sinFallas(page);
  await dialogo.getByLabel(/^Observación para DS/).fill('Un docente sin clase en segundo nivel.');
  await dialogo.getByRole('button', { name: 'Devolver' }).click();

  const estado = page.getByRole('region', { name: 'Estado del distributivo' });
  await expect(estado).toContainText('Propuesta de carrera · versión 1');
  await expect(estado).toContainText('TDS: devuelto por consolidacion.demo');
  await expect(estado).toContainText('Faltan docentes en primer nivel.');
  await expect(estado).toContainText('Un docente sin clase en segundo nivel.');
  await expect(page.getByRole('button', { name: 'Devolver parciales' })).toHaveCount(0);
  expect(devueltas.map((una) => una.carrera)).toEqual(['TDS', 'DS']);
  await sinFallas(page);
});

// #215: en revisión, el consolidado se lee y el informe se descarga, pero no se consolida ni adjunta.
test('en revisión no ofrece consolidar ni adjuntar, avisa por qué y es accesible', async ({
  page,
}) => {
  await abrirConsolidado(page, () => ({
    periodo: '2026-2',
    creado: true,
    modificable: true,
    enRevision: true,
    version: 1,
    desactualizado: false,
    carreras: [
      {
        carrera: 'TDS',
        estado: 'ENVIADO',
        paralelos: 3,
        propuestos: 3,
        completo: true,
        horas: HORAS,
      },
    ],
    totales: HORAS,
    abiertas: { duras: 0, blandas: 0 },
    consolidacion: { por: 'consolidacion.demo', en: '2026-11-02T15:00:00Z' },
    informe: {
      nombre: 'informe.pdf',
      tamano: 2048,
      huella: 'a'.repeat(64),
      por: 'consolidacion.demo',
      en: '2026-11-02T15:10:00Z',
    },
  }));

  await expect(
    page.getByText(
      'El distributivo está en revisión del Vicerrectorado: el consolidado ya no cambia.',
    ),
  ).toBeVisible();
  await expect(page.getByRole('button', { name: /onsolidar/ })).toHaveCount(0);
  await expect(page.getByRole('button', { name: /Adjuntar/ })).toHaveCount(0);
  await expect(page.getByRole('button', { name: 'Descargar el informe' })).toBeVisible();
  await sinFallas(page);
});

// F13-H01: el Vicerrector cancela, aprueba y congela; la tarjeta muestra la constancia con la huella
// y las pantallas, el aviso de versión congelada.
test('el Vicerrector aprueba y congela, ve la constancia y es accesible', async ({ page }) => {
  const HASH = '0123456789abcdef'.repeat(4);
  let congelado = false;
  let aprobaciones = 0;
  const flujo = () => ({
    periodo: '2026-2',
    etapa: congelado ? 'CONGELADO_EN_FIRMA' : 'EN_REVISION_VRA',
    nombreDeEtapa: congelado ? 'En firma' : 'Revisión del Vicerrectorado',
    version: 1,
    parciales: [{ carrera: 'TDS', enviado: true, pendienteDeConfirmar: false }],
    acciones: congelado ? [] : ['APROBAR_CONTENIDO'],
    transiciones: [],
    ...(congelado
      ? {
          congelacion: {
            version: 1,
            hash: HASH,
            quien: 'vicerrectorado.demo',
            momento: '2026-11-10T15:30:00Z',
          },
        }
      : {}),
  });
  await page.route('**/api/validacionocs/periodos/*/flujo', (ruta) =>
    ruta.fulfill({ json: flujo() }),
  );
  await page.route('**/api/validacionocs/periodos/*/aprobacion', async (ruta) => {
    aprobaciones++;
    congelado = true;
    await ruta.fulfill({ json: flujo() });
  });
  await abrirConsolidado(
    page,
    () => ({
      periodo: '2026-2',
      creado: true,
      modificable: true,
      enRevision: true,
      versionCongelada: congelado,
      version: 1,
      desactualizado: false,
      carreras: [
        {
          carrera: 'TDS',
          estado: 'ENVIADO',
          paralelos: 3,
          propuestos: 3,
          completo: true,
          horas: HORAS,
        },
      ],
      totales: HORAS,
      abiertas: { duras: 0, blandas: 0 },
      consolidacion: { por: 'consolidacion.demo', en: '2026-11-02T15:00:00Z' },
      informe: {
        nombre: 'informe.pdf',
        tamano: 2048,
        huella: 'a'.repeat(64),
        por: 'consolidacion.demo',
        en: '2026-11-02T15:10:00Z',
      },
    }),
    VICERRECTORADO,
  );

  await page.getByRole('button', { name: 'Aprobar el contenido' }).click();
  const dialogo = page.getByRole('dialog', { name: 'Aprobar el contenido' });
  await expect(dialogo).toContainText(
    'La versión 1 queda congelada: es la que se firmará. Todo cambio posterior exigirá una versión nueva.',
  );
  await expect(dialogo.getByRole('button', { name: 'Cancelar' })).toBeFocused();
  await sinFallas(page);
  await dialogo.getByRole('button', { name: 'Cancelar' }).click();
  await expect(dialogo).toHaveCount(0);
  expect(aprobaciones).toBe(0);

  await page.getByRole('button', { name: 'Aprobar el contenido' }).click();
  await page
    .getByRole('dialog', { name: 'Aprobar el contenido' })
    .getByRole('button', { name: 'Aprobar y congelar' })
    .click();

  const estado = page.getByRole('region', { name: 'Estado del distributivo' });
  await expect(estado).toContainText('En firma · versión 1');
  await expect(estado).toContainText(
    'Versión 1 congelada por vicerrectorado.demo el 10/11/26, 10:30',
  );
  await expect(estado).toContainText('Huella: 0123456789ab…');
  await expect(page.getByRole('button', { name: 'Aprobar el contenido' })).toHaveCount(0);
  await expect(
    page.getByText('La versión 1 está congelada para la firma: ya no cambia.'),
  ).toBeVisible();
  await expect(page.getByRole('button', { name: /onsolidar/ })).toHaveCount(0);
  await expect(page.getByRole('button', { name: 'Descargar el informe' })).toBeVisible();
  expect(aprobaciones).toBe(1);
  await sinFallas(page);
});

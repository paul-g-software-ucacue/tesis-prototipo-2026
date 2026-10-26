// F01-H01 en un navegador, con la API simulada: el Responsable de consolidación descarga la
// plantilla de docentes y el diccionario de la estructura orgánica; un gestor no ve la pantalla.
// F01-H02: sube el CSV de docentes y ve los registros leídos; uno de carreras sin una columna
// muestra cuál falta. También la accesibilidad de «Plantillas de carga» con axe-core (WCAG 2.1 AA).
import { readFile } from 'node:fs/promises';
import AxeBuilder from '@axe-core/playwright';
import { expect, test, type Page } from '@playwright/test';
import { ingresar, simularApi } from './simulacion';

const CONSOLIDACION = {
  estado: 200,
  cuerpo: {
    usuario: 'consolidacion.demo',
    nombre: 'Responsable de consolidación (demo)',
    roles: ['RESPONSABLE_CONSOLIDACION'],
    // D-71: el Responsable de consolidación también lee y escribe las cargas.
    permisos: [
      'DISTRIBUTIVO:LECTURA',
      'CONSOLIDACION:LECTURA',
      'CARGAS:LECTURA',
      'CARGAS:ESCRITURA',
    ],
  },
};

const GESTOR = {
  estado: 200,
  cuerpo: {
    usuario: 'gestion.demo',
    nombre: 'Gestor (demo)',
    roles: ['GESTOR_UNIDAD'],
    permisos: ['PROYECTOS:LECTURA:PROPIO'],
  },
};

// Los 26 encabezados de 06_docentes.csv de los anexos de la ERS.
const DOCENTES =
  'tipo_identificacion,numero_identificacion,primer_apellido,segundo_apellido,primer_nombre,segundo_nombre,correo_institucional,telefono,tipo_personal,categoria_escalafon,dedicacion,horas_contratadas,fecha_ingreso,inicio_contrato,fin_contrato,condicion_especial,codigo_carrera_base,titulo_tercer_nivel,registro_senescyt_3n,campo_amplio_3n,campo_especifico_3n,titulos_cuarto_nivel,cursos_capacitaciones,experiencia_laboral,asignaturas_afines_declaradas,estado'.split(
    ',',
  );

const NOMBRES = [
  'Estructura orgánica',
  'Carreras',
  'Ciclos académicos aprobados',
  'Asignaturas y malla',
  'Oferta del periodo',
  'Docentes contratados (con ficha académica)',
  'Horas según la política de contratación',
  'Cargos y autoridades',
  'Proyectos y prácticas aprobados',
  'Matriculados por carrera',
];

const CODIGOS = [
  'estructura_organica',
  'carreras',
  'ciclos_academicos',
  'asignaturas_malla',
  'oferta_periodo',
  'docentes',
  'politica_carga_horaria',
  'cargos_autoridades',
  'proyectos_practicas',
  'matriculados_carrera',
];

function columna(campo: string) {
  return {
    campo,
    descripcion: `Descripción de ${campo}`,
    tipo: 'Texto',
    obligatorio: 'Sí',
    ejemplo: 'A1',
  };
}

const DICCIONARIO = {
  convenciones: [{ tema: 'Fechas', regla: 'aaaa-mm-dd (ISO 8601).' }],
  cargas: NOMBRES.map((nombre, indice) => ({
    numero: indice + 1,
    codigo: CODIGOS[indice],
    nombre,
    archivo: `${String(indice + 1).padStart(2, '0')}_${CODIGOS[indice]}.csv`,
    dependeDe: indice === 0 ? [] : [indice],
    obligatoriedad: indice === 9 ? 'RECOMENDADA' : 'OBLIGATORIA',
    columnas:
      indice === 5
        ? DOCENTES.map(columna)
        : indice === 0
          ? [
              columna('codigo_unidad'),
              {
                ...columna('codigo_gestion_autorizada'),
                obligatorio: 'Sí si tipo_unidad = GESTION',
                valores: [
                  { codigo: 'G01', descripcion: 'Rector' },
                  { codigo: 'G02', descripcion: 'Vicerrector' },
                ],
              },
            ]
          : [columna('codigo')],
  })),
};

function csv(nombre: string, cuerpo: string) {
  return {
    status: 200,
    headers: {
      'Content-Type': 'text/csv;charset=UTF-8',
      'Content-Disposition': `attachment; filename="${nombre}"`,
    },
    body: `\uFEFF${cuerpo}\r\n`,
  };
}

async function simularCargas(pagina: Page) {
  await pagina.route('**/api/cargas/plantillas', (ruta) => ruta.fulfill({ json: DICCIONARIO }));
  await pagina.route('**/api/cargas/plantillas/docentes/plantilla', (ruta) =>
    ruta.fulfill(csv('06_docentes.csv', DOCENTES.join(','))),
  );
  await pagina.route('**/api/cargas/plantillas/estructura_organica/diccionario', (ruta) =>
    ruta.fulfill(
      csv(
        '01_estructura_organica_diccionario.csv',
        'numero,campo,descripcion\r\n1,codigo_unidad,Código único de la unidad',
      ),
    ),
  );
}

// Lo que recibió el servidor simulado en cada subida, para comprobar el pedido real del navegador.
interface Subida {
  autorizacion: string | null;
  tipo: string | null;
  cuerpo: string;
}

// El problema que el servidor real da a una cédula de 9 dígitos (F01-H03, RF-08).
const CEDULA_SIN_CERO = {
  fila: 2,
  campo: 'numero_identificacion',
  valor: '109990010',
  motivo: 'Cédula inválida o sin cero inicial: le falta el 0 inicial.',
  tipo: 'ERROR',
  sugerencia: '0109990010',
};

// El informe en CSV, como lo escribe el servidor: con el BOM y la celda neutralizada.
async function simularInforme(pagina: Page): Promise<Subida[]> {
  const informes: Subida[] = [];
  await pagina.route('**/api/cargas/plantillas/*/archivo/informe', async (ruta) => {
    const peticion = ruta.request();
    informes.push({
      autorizacion: await peticion.headerValue('Authorization'),
      tipo: await peticion.headerValue('Content-Type'),
      cuerpo: peticion.postDataBuffer()?.toString('utf8') ?? '',
    });
    await ruta.fulfill({
      status: 200,
      headers: {
        'Content-Type': 'text/csv;charset=UTF-8',
        'Content-Disposition': 'attachment; filename="informe-docentes.csv"',
      },
      body:
        '\uFEFFfila,campo,valor,motivo,tipo,sugerencia\r\n' +
        '2,numero_identificacion,109990010,Cédula inválida o sin cero inicial: le falta el 0 inicial.,ERROR,0109990010\r\n',
    });
  });
  return informes;
}

async function simularSubidas(pagina: Page): Promise<Subida[]> {
  const subidas: Subida[] = [];
  await pagina.route('**/api/cargas/plantillas/*/archivo', async (ruta) => {
    const peticion = ruta.request();
    subidas.push({
      autorizacion: await peticion.headerValue('Authorization'),
      tipo: await peticion.headerValue('Content-Type'),
      cuerpo: peticion.postDataBuffer()?.toString('utf8') ?? '',
    });
    if (peticion.url().includes('/docentes/')) {
      const conCedulaSinCero = (peticion.postDataBuffer()?.toString('utf8') ?? '').includes(
        '109990010,',
      );
      await ruta.fulfill({
        json: {
          carga: 'docentes',
          archivo: '06_docentes.csv',
          formato: 'CSV',
          registros: 47,
          valido: !conCedulaSinCero,
          errores: conCedulaSinCero ? 1 : 0,
          advertencias: 0,
          problemas: conCedulaSinCero ? [CEDULA_SIN_CERO] : [],
        },
      });
      return;
    }
    await ruta.fulfill({
      status: 400,
      json: {
        titulo: 'Encabezados distintos de la plantilla',
        descripcion:
          'Las columnas del archivo no son las de la plantilla de la carga, en su orden. Revisa las columnas marcadas.',
        codigo: 'CARGAS_ENCABEZADOS_DISTINTOS',
        detalles: [{ campo: 'codigo_carrera', descripcion: 'Falta en el archivo.' }],
      },
    });
  });
  return subidas;
}

async function abrirPlantillas(pagina: Page) {
  await simularApi(pagina, { sesion: CONSOLIDACION });
  await simularCargas(pagina);
  await pagina.goto('/iniciar-sesion');
  await ingresar(pagina, 'consolidacion.demo');
  await pagina.getByRole('link', { name: 'Plantillas de carga' }).click();
  await expect(
    pagina.getByRole('heading', { level: 1, name: 'Plantillas de carga' }),
  ).toBeVisible();
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

test('el Responsable de consolidación descarga la plantilla de docentes y un diccionario', async ({
  page,
}) => {
  await abrirPlantillas(page);

  const plantilla = page.waitForEvent('download');
  await page
    .getByRole('button', {
      name: 'Descargar la plantilla de Docentes contratados (con ficha académica)',
    })
    .click();
  const docentes = await plantilla;
  expect(docentes.suggestedFilename()).toBe('06_docentes.csv');
  const contenido = await readFile(await docentes.path(), 'utf8');
  expect(contenido.replace('\uFEFF', '').trim().split(',')).toHaveLength(26);

  const diccionario = page.waitForEvent('download');
  await page
    .getByRole('button', { name: 'Descargar el diccionario de Estructura orgánica' })
    .click();
  expect((await diccionario).suggestedFilename()).toBe('01_estructura_organica_diccionario.csv');
});

test('un gestor no ve «Plantillas de carga» en el menú', async ({ page }) => {
  await simularApi(page, { sesion: GESTOR });
  await page.goto('/iniciar-sesion');
  await ingresar(page, 'gestion.demo');

  await expect(page.getByRole('link', { name: 'Inicio' })).toBeVisible();
  await expect(page.getByRole('link', { name: 'Plantillas de carga' })).toHaveCount(0);
});

test('«Plantillas de carga» no tiene fallas WCAG 2.1 AA, ni con un diccionario desplegado', async ({
  page,
}) => {
  await abrirPlantillas(page);
  await sinFallas(page);

  await page.getByRole('button', { name: 'Ver el diccionario de Estructura orgánica' }).click();
  await expect(
    page.getByRole('table', { name: 'Diccionario de Estructura orgánica' }),
  ).toBeVisible();
  await page.getByRole('button', { name: 'Convenciones de todas las cargas' }).click();
  await sinFallas(page);
});

test('el Responsable de consolidación sube el CSV de docentes y ve los registros leídos', async ({
  page,
}) => {
  const subidas = await simularSubidas(page);
  await abrirPlantillas(page);

  await page
    .getByLabel('Archivo de Docentes contratados (con ficha académica)', { exact: true })
    .setInputFiles({
      name: '06_docentes.csv',
      mimeType: 'text/csv',
      buffer: Buffer.from(DOCENTES.join(',') + '\r\n'),
    });

  await expect(page.getByRole('status')).toHaveText(
    '06_docentes.csv es válido: 47 registros leídos.',
  );
  expect(subidas).toHaveLength(1);
  expect(subidas[0]?.autorizacion).toMatch(/^Bearer /);
  expect(subidas[0]?.tipo).toMatch(/^multipart\/form-data; boundary=/);
  expect(subidas[0]?.cuerpo).toContain('name="archivo"; filename="06_docentes.csv"');
  await sinFallas(page);
});

test('un archivo de carreras sin una columna muestra cuál falta', async ({ page }) => {
  await simularSubidas(page);
  await abrirPlantillas(page);

  await page.getByLabel('Archivo de Carreras', { exact: true }).setInputFiles({
    name: 'carreras.csv',
    mimeType: 'text/csv',
    buffer: Buffer.from('nombre,siglas\r\nUno,U\r\n'),
  });

  const aviso = page.getByRole('alert');
  await expect(aviso).toContainText('Encabezados distintos de la plantilla');
  await expect(aviso).toContainText('codigo_carrera: Falta en el archivo.');
  await sinFallas(page);
});

test('una cédula sin el cero inicial se ve con su sugerencia y el informe se descarga', async ({
  page,
}) => {
  await simularSubidas(page);
  const informes = await simularInforme(page);
  await abrirPlantillas(page);
  const fila = ['CEDULA', '109990010', ...DOCENTES.slice(2).map(() => 'X')].join(',');

  await page
    .getByLabel('Archivo de Docentes contratados (con ficha académica)', { exact: true })
    .setInputFiles({
      name: '06_docentes.csv',
      mimeType: 'text/csv',
      buffer: Buffer.from(DOCENTES.join(',') + '\r\n' + fila + '\r\n'),
    });

  await expect(page.getByRole('status')).toHaveText(
    '06_docentes.csv está rechazado: 1 error y 0 advertencias. Corríjalo y vuelva a subirlo.',
  );
  const problemas = page.getByRole('table', { name: 'Problemas de 06_docentes.csv' });
  await expect(problemas.getByRole('row').nth(1)).toContainText('0109990010');
  await sinFallas(page);

  const descarga = page.waitForEvent('download');
  await page.getByRole('button', { name: 'Descargar el informe de 06_docentes.csv' }).click();
  const informe = await descarga;
  expect(informe.suggestedFilename()).toBe('informe-docentes.csv');
  const contenido = await readFile(await informe.path(), 'utf8');
  expect(contenido.replace('\uFEFF', '').split('\r\n')[0]).toBe(
    'fila,campo,valor,motivo,tipo,sugerencia',
  );
  expect(informes).toHaveLength(1);
  expect(informes[0]?.autorizacion).toMatch(/^Bearer /);
  expect(informes[0]?.cuerpo).toContain('name="archivo"; filename="06_docentes.csv"');
});

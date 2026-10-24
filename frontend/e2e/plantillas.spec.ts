// F01-H01 en un navegador, con la API simulada: el Responsable de consolidación descarga la
// plantilla de docentes y el diccionario de la estructura orgánica; un gestor no ve la pantalla.
// También la accesibilidad de «Plantillas de carga» con axe-core (WCAG 2.1 AA).
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
    permisos: ['DISTRIBUTIVO:LECTURA', 'CONSOLIDACION:LECTURA'],
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

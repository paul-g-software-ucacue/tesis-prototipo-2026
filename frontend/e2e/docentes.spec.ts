// F03-H04 en un navegador, con la API simulada: Talento Humano registra un docente con un curso, lo
// busca por su apellido en minúsculas y abre su ficha con las horas del curso. También la
// accesibilidad de «Docentes», de la ficha y de los diálogos con axe-core (WCAG 2.1 AA).
import AxeBuilder from '@axe-core/playwright';
import { expect, test, type Page } from '@playwright/test';
import { ingresar, simularApi } from './simulacion';

const TALENTO_HUMANO = {
  estado: 200,
  cuerpo: {
    usuario: 'datos.demo',
    nombre: 'Talento Humano (demo)',
    roles: ['OPERADOR_DATOS'],
    permisos: ['CARGAS:LECTURA', 'CARGAS:ESCRITURA', 'DOCENTES:LECTURA', 'DOCENTES:ESCRITURA'],
  },
};

const REFERENCIAS = {
  carreras: [{ codigo: '550613A01-P-0301', nombre: 'DESARROLLO DE SOFTWARE', siglas: 'DS' }],
  asignaturas: [
    {
      codigo: 'DS-P-II-POO-204',
      nombre: 'PROGRAMACIÓN ORIENTADA A OBJETOS',
      carrera: '550613A01-P-0301',
    },
  ],
  camposAmplios: [
    { codigo: '01', nombre: 'Educación' },
    { codigo: '06', nombre: 'Tecnologías de la información y la comunicación' },
  ],
  unidades: [],
  periodos: [
    {
      codigo: '2026-I',
      nombre: 'Periodo académico 2026-I',
      inicio: '2026-04-06',
      fin: '2026-08-07',
    },
    {
      codigo: '2026-II',
      nombre: 'Periodo académico 2026-II',
      inicio: '2026-10-05',
      fin: '2027-02-05',
    },
  ],
};

// Lo que respondería Generación: 2026-I abierto, 2026-II sin abrir (F06-H01).
const AFINIDAD = {
  periodo: '2026-I',
  version: 1,
  puntajes: [
    {
      asignatura: 'DS-P-II-POO-204',
      nombre: 'PROGRAMACIÓN ORIENTADA A OBJETOS',
      total: 3,
      topeAplicado: true,
      desglose: [
        {
          tipo: 'TERCER_NIVEL',
          nombre: 'INGENIERO DE SISTEMAS',
          coincidencia: 'CAMPO_ESPECIFICO',
          aporte: 2,
        },
        {
          tipo: 'CURSO',
          nombre: 'PROGRAMACIÓN EN JAVA',
          coincidencia: 'CAMPO_ESPECIFICO',
          aporte: 1,
        },
      ],
    },
    {
      asignatura: 'DS-P-I-ANA-101',
      nombre: 'ANATOMÍA',
      total: 0,
      topeAplicado: false,
      desglose: [],
    },
  ],
};

const PERIODO_SIN_ABRIR = {
  titulo: 'Periodo sin abrir',
  descripcion:
    'El periodo todavía no tiene su versión de normativa. Ábrelo en «Periodos» para calcular la afinidad.',
  codigo: 'GENERACION_PERIODO_SIN_ABRIR',
};

type Cuerpo = Record<string, unknown> & {
  identificacion: string;
  primerApellido: string;
  primerNombre: string;
  carreraBase: string;
  ficha: { asignaturasAfines?: string[] } & Record<string, unknown>;
};

// Sin tildes ni mayúsculas, como la búsqueda de Docentes.
const normal = (texto: string) => texto.normalize('NFD').replace(/\p{M}/gu, '').toUpperCase();

// Los docentes en memoria, con lo que respondería Docentes.
async function simularDocentes(pagina: Page) {
  const docentes: Cuerpo[] = [];
  const respuesta = (uno: Cuerpo) => {
    const nombreCompleto = [uno.primerApellido, uno.primerNombre]
      .map((parte) => parte.toUpperCase())
      .join(' ');
    return {
      ...uno,
      primerApellido: uno.primerApellido.toUpperCase(),
      primerNombre: uno.primerNombre.toUpperCase(),
      nombreCompleto,
      nombreDeCarreraBase: 'DESARROLLO DE SOFTWARE',
      estado: (uno.estado as string | undefined) ?? 'ACTIVO',
      condicionEspecial: (uno.condicionEspecial as string | undefined) ?? 'NINGUNA',
      camposAmplios: REFERENCIAS.camposAmplios,
      asignaturasAfines: (uno.ficha.asignaturasAfines ?? []).map((codigo) => ({
        codigo,
        nombre: REFERENCIAS.asignaturas.find((una) => una.codigo === codigo)?.nombre,
      })),
    };
  };
  await pagina.route('**/api/docentes/referencias', (ruta) => ruta.fulfill({ json: REFERENCIAS }));
  await pagina.route('**/api/docentes/docentes?*', (ruta) => {
    const texto = new URL(ruta.request().url()).searchParams.get('texto') ?? '';
    return ruta.fulfill({
      json: docentes
        .map(respuesta)
        .filter((uno) => normal(uno.nombreCompleto).includes(normal(texto))),
    });
  });
  await pagina.route('**/api/docentes/docentes', async (ruta) => {
    if (ruta.request().method() === 'POST') {
      const nuevo = ruta.request().postDataJSON() as Cuerpo;
      docentes.push(nuevo);
      await ruta.fulfill({ status: 201, json: respuesta(nuevo) });
      return;
    }
    await ruta.fulfill({ json: docentes.map(respuesta) });
  });
  await pagina.route(/\/api\/docentes\/docentes\/[^/?]+(\/estado)?$/, async (ruta) => {
    const [, identificacion, estado] =
      /docentes\/docentes\/([^/?]+)(\/estado)?$/.exec(ruta.request().url()) ?? [];
    const uno = docentes.find((candidato) => candidato.identificacion === identificacion);
    if (!uno) {
      await ruta.fulfill({ status: 404, json: {} });
      return;
    }
    if (estado) {
      uno.estado = (ruta.request().postDataJSON() as { estado: string }).estado;
    }
    await ruta.fulfill({ json: respuesta(uno) });
  });
}

async function abrirDocentes(pagina: Page) {
  await simularApi(pagina, { sesion: TALENTO_HUMANO });
  await simularDocentes(pagina);
  await pagina.route('**/api/generacion/docentes/*/afinidad*', async (ruta) => {
    const periodo = new URL(ruta.request().url()).searchParams.get('periodo');
    await ruta.fulfill(
      periodo === '2026-I' ? { json: AFINIDAD } : { status: 409, json: PERIODO_SIN_ABRIR },
    );
  });
  await pagina.goto('/iniciar-sesion');
  await ingresar(pagina, 'datos.demo');
  await pagina.getByRole('link', { name: 'Docentes' }).click();
  await expect(pagina.getByRole('heading', { level: 1, name: 'Docentes' })).toBeVisible();
}

async function registrar(pagina: Page) {
  await pagina.getByRole('button', { name: 'Registrar docente' }).click();
  const dialogo = pagina.getByRole('dialog', { name: 'Registrar docente' });
  await dialogo.getByLabel('Identificación', { exact: true }).fill('0109990010');
  await dialogo.getByLabel('Primer apellido').fill('Peralta');
  await dialogo.getByLabel('Primer nombre').fill('Juan');
  await dialogo.getByLabel('Correo institucional').fill('juan.peralta@instituto-prueba.edu.ec');
  await dialogo.getByLabel('Tipo de personal').selectOption('OCASIONAL');
  await dialogo.getByLabel('Dedicación').selectOption('TC');
  await dialogo.getByLabel('Horas contratadas').fill('40');
  await dialogo.getByLabel('Fecha de ingreso').fill('2019-10-01');
  await dialogo.getByLabel('Inicio del contrato').fill('2026-04-01');
  await dialogo.getByLabel('Fin del contrato').fill('2026-08-31');
  await dialogo.getByLabel('Carrera base').selectOption('550613A01-P-0301');
  await dialogo.getByLabel('Título', { exact: true }).fill('INGENIERO DE SISTEMAS');
  await dialogo.getByLabel('Campo amplio').selectOption('06');
  await dialogo.getByLabel('Campo específico').first().fill('061');
  await dialogo.getByRole('button', { name: 'Agregar curso' }).click();
  const cursos = dialogo.getByRole('group', { name: 'Cursos y capacitaciones' });
  await cursos.getByLabel('Nombre').fill('PROGRAMACIÓN EN JAVA');
  await cursos.getByLabel('Campo específico').fill('061');
  await cursos.getByLabel('Horas').fill('40');
  await cursos.getByLabel('Año').fill('2023');
  await dialogo.getByRole('button', { name: 'Registrar' }).click();
  await expect(dialogo).toBeHidden();
}

async function sinFallas(pagina: Page) {
  // Sin esperar a que termine la transición, axe mide el contraste a media opacidad.
  for (const contenedor of await pagina.locator('.MuiDialog-container').all()) {
    await expect(contenedor).toHaveCSS('opacity', '1');
  }
  const resultado = await new AxeBuilder({ page: pagina })
    .withTags(['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa'])
    .analyze();
  const fallas = resultado.violations.map(
    (falla) =>
      `${falla.id} (${falla.impact}): ${falla.nodes.map((nodo) => nodo.target.join(' ')).join(', ')}`,
  );
  expect(fallas, fallas.join('\n')).toEqual([]);
}

test('Talento Humano registra un docente, lo busca por su apellido en minúsculas y ve las horas de su curso', async ({
  page,
}) => {
  await abrirDocentes(page);
  await registrar(page);

  await page.getByLabel('Buscar por nombre o identificación').fill('peralta');
  const tabla = page.getByRole('table', { name: 'Docentes' });
  await expect(tabla).toContainText('PERALTA JUAN');
  await expect(page.getByText('1 docente', { exact: true })).toBeVisible();

  await page.getByRole('button', { name: 'Ver la ficha de PERALTA JUAN' }).click();
  const ficha = page.getByRole('dialog', { name: 'Ficha de PERALTA JUAN' });
  await expect(ficha).toContainText('PROGRAMACIÓN EN JAVA · 40 h (2023)');
  await expect(ficha).toContainText(
    'Tercer nivel: INGENIERO DE SISTEMAS · campo amplio 06 (Tecnologías de la información y la comunicación), específico 061',
  );
});

test('la lista de docentes', async ({ page }) => {
  await abrirDocentes(page);
  await registrar(page);
  await expect(page.getByRole('table', { name: 'Docentes' })).toContainText('PERALTA JUAN');

  await sinFallas(page);
});

for (const [boton, titulo] of [
  ['Registrar docente', 'Registrar docente'],
  ['Ver la ficha de PERALTA JUAN', 'Ficha de PERALTA JUAN'],
  ['Cambiar el estado de PERALTA JUAN', 'Estado laboral de PERALTA JUAN'],
] as const) {
  test(`el diálogo «${titulo}»`, async ({ page }) => {
    await abrirDocentes(page);
    await registrar(page);
    await page.getByRole('button', { name: boton }).click();
    const dialogo = page.getByRole('dialog', { name: titulo });
    await expect(dialogo).toBeVisible();
    if (boton === 'Ver la ficha de PERALTA JUAN') {
      await expect(dialogo).toContainText('PROGRAMACIÓN EN JAVA');
    }

    await sinFallas(page);
  });
}

test('la afinidad por asignatura en la ficha, con su desglose', async ({ page }) => {
  await abrirDocentes(page);
  await registrar(page);
  await page.getByRole('button', { name: 'Ver la ficha de PERALTA JUAN' }).click();
  const ficha = page.getByRole('dialog', { name: 'Ficha de PERALTA JUAN' });

  await ficha.getByRole('combobox', { name: 'Periodo' }).click();
  await page.getByRole('option', { name: /^2026-I ·/ }).click();
  const asignatura = ficha.getByRole('button', { name: /PROGRAMACIÓN ORIENTADA A OBJETOS/ });
  await expect(asignatura).toContainText('3 puntos');
  await asignatura.click();

  await expect(
    ficha.getByText('Curso: PROGRAMACIÓN EN JAVA · campo específico · 1 punto'),
  ).toBeVisible();
  await expect(ficha.getByText(/Se aplicó el tope de cursos/)).toBeVisible();
  await sinFallas(page);

  await ficha.getByRole('combobox', { name: 'Periodo' }).click();
  await page.getByRole('option', { name: /^2026-II ·/ }).click();
  await expect(ficha.getByText(/Ábrelo en «Periodos»/)).toBeVisible();
  await expect(asignatura).toBeHidden();
  await sinFallas(page);
});

test('el diálogo «Registrar docente» con los campos marcados en rojo', async ({ page }) => {
  await abrirDocentes(page);
  await page.getByRole('button', { name: 'Registrar docente' }).click();
  const dialogo = page.getByRole('dialog', { name: 'Registrar docente' });
  await dialogo.getByLabel('Identificación', { exact: true }).fill('0109990011');
  await dialogo.getByRole('button', { name: 'Registrar' }).click();
  await expect(dialogo.getByRole('alert')).toContainText('marcados en rojo');
  await expect(dialogo).toContainText('el último dígito no corresponde a los demás');

  await sinFallas(page);
});

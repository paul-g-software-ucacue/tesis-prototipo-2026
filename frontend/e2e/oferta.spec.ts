// F03-H03 en un navegador, con la API simulada: el Vicerrector oferta una asignatura de 4 horas en
// 3 paralelos y ve 12 horas a cubrir; retira uno y ve 8. También la accesibilidad de «Oferta del
// periodo» y de sus diálogos con axe-core (WCAG 2.1 AA). Y F06-H02: la sugerencia de docentes de una
// asignatura-paralelo, con su desglose. Y F06-H03: proponer con y sin justificación.
import AxeBuilder from '@axe-core/playwright';
import { expect, test, type Page } from '@playwright/test';
import { ingresar, simularApi } from './simulacion';

interface Paralelo {
  periodo: string;
  asignatura: string;
  paralelo: string;
  jornada: string;
  modalidad: string;
  estudiantes: number;
  horasSemanales?: number;
  sede?: string;
  estado: string;
}

const VICERRECTOR = {
  estado: 200,
  cuerpo: {
    usuario: 'vicerrectorado.demo',
    nombre: 'Vicerrector académico (demo)',
    roles: ['VICERRECTOR'],
    permisos: ['DISTRIBUTIVO:LECTURA', 'DISTRIBUTIVO:ESCRITURA'],
  },
};

const PERIODOS = [
  {
    codigo: '2026-2',
    nombre: 'Periodo 2026-2',
    inicio: '2026-09-01',
    fin: '2027-02-28',
    estado: 'PLANIFICACION',
  },
];

const DS = {
  codigo: '550613A01-P-0301',
  nombre: 'DESARROLLO DE SOFTWARE',
  siglas: 'DS',
  sede: 'MATRIZ',
  nivelFormacion: 'TECNOLOGICO_SUPERIOR',
  modalidad: 'PRESENCIAL',
  jornadas: ['NOCTURNA'],
  campoAmplio: '06',
  campoEspecifico: '061',
  numeroCiclos: 5,
  estado: 'VIGENTE',
  mallas: [{ codigo: 'DS-2019', estado: 'VIGENTE', asignaturas: 1, horasSemanalesClase: 4 }],
};

const CAMPO = { codigo: '06', nombre: 'Tecnologías de la información y la comunicación' };

const POO = {
  codigo: 'DS-P-II-POO-204',
  carrera: DS.codigo,
  malla: 'DS-2019',
  nombre: 'PROGRAMACIÓN ORIENTADA A OBJETOS',
  ciclo: 2,
  unidadOrganizacionCurricular: 'PROFESIONAL',
  horasSemanalesClase: 4,
  troncoComun: false,
  campoAmplio: CAMPO,
  campoEspecifico: '061',
  palabrasClave: ['programación', 'objetos'],
  activa: true,
};

// La oferta en memoria, con los totales que calcularía Académico.
async function simularAcademico(pagina: Page, iniciales: Paralelo[]) {
  const oferta: Paralelo[] = structuredClone(iniciales);
  const respuesta = (una: Paralelo) => ({
    ...una,
    nombre: POO.nombre,
    carrera: DS.codigo,
    malla: POO.malla,
    ciclo: POO.ciclo,
    sede: una.sede ?? DS.sede,
    sedeIndicada: una.sede !== undefined,
    horasSemanales: una.horasSemanales ?? POO.horasSemanalesClase,
    horasIndicadas: una.horasSemanales !== undefined,
    horasDeLaAsignatura: POO.horasSemanalesClase,
    porEncimaDeLaAsignatura: false,
    troncoComun: POO.troncoComun,
    campoAmplio: CAMPO,
    campoEspecifico: POO.campoEspecifico,
    palabrasClave: POO.palabrasClave,
  });
  const delPeriodo = () => {
    const ofertadas = oferta.filter((una) => una.estado === 'OFERTADA');
    const total = ofertadas.reduce(
      (suma, una) => suma + (una.horasSemanales ?? POO.horasSemanalesClase),
      0,
    );
    return {
      periodo: '2026-2',
      estadoDelPeriodo: 'PLANIFICACION',
      modificable: true,
      horasACubrir: total,
      carreras:
        oferta.length === 0
          ? []
          : [
              {
                carrera: DS.codigo,
                nombre: DS.nombre,
                horasACubrir: total,
                asignaturas: [
                  {
                    asignatura: POO.codigo,
                    nombre: POO.nombre,
                    ciclo: POO.ciclo,
                    paralelos: ofertadas.length,
                    horasACubrir: total,
                  },
                ],
              },
            ],
      asignaturasParalelo: [...oferta]
        .sort((a, b) => a.paralelo.localeCompare(b.paralelo))
        .map(respuesta),
    };
  };
  await pagina.route('**/api/generacion/periodos/*/parciales', (ruta) =>
    ruta.fulfill({ json: { creado: false, parciales: [] } }),
  );
  await pagina.route('**/api/academico/periodos', (ruta) => ruta.fulfill({ json: PERIODOS }));
  await pagina.route('**/api/academico/carreras', (ruta) => ruta.fulfill({ json: [DS] }));
  await pagina.route('**/api/academico/unidades', (ruta) =>
    ruta.fulfill({
      json: [{ codigo: 'MATRIZ', tipo: 'SEDE_MATRIZ', nombre: 'Sede matriz', activa: true }],
    }),
  );
  await pagina.route(/\/api\/academico\/carreras\/[^/]+\/asignaturas$/, (ruta) =>
    ruta.fulfill({ json: [POO] }),
  );
  await pagina.route('**/api/academico/periodos/2026-2/oferta', async (ruta) => {
    if (ruta.request().method() === 'POST') {
      const nueva = {
        ...(ruta.request().postDataJSON() as Omit<Paralelo, 'periodo' | 'estado'>),
        periodo: '2026-2',
        estado: 'OFERTADA',
      };
      oferta.push(nueva);
      await ruta.fulfill({ status: 201, json: respuesta(nueva) });
      return;
    }
    await ruta.fulfill({ json: delPeriodo() });
  });
  await pagina.route(/\/oferta\/[^/]+\/[^/]+\/estado$/, async (ruta) => {
    const [, asignatura, paralelo] =
      /oferta\/([^/]+)\/([^/]+)\/estado$/.exec(ruta.request().url()) ?? [];
    const una = oferta.find(
      (candidata) => candidata.asignatura === asignatura && candidata.paralelo === paralelo,
    );
    if (!una) {
      await ruta.fulfill({ status: 404, json: {} });
      return;
    }
    una.estado = (ruta.request().postDataJSON() as { estado: string }).estado;
    await ruta.fulfill({ json: respuesta(una) });
  });
}

function nocturna(paralelo: string): Paralelo {
  return {
    periodo: '2026-2',
    asignatura: POO.codigo,
    paralelo,
    jornada: 'NOCTURNA',
    modalidad: 'PRESENCIAL',
    estudiantes: 28,
    estado: 'OFERTADA',
  };
}

async function abrirOferta(
  pagina: Page,
  iniciales: Paralelo[] = [],
  sesion: typeof VICERRECTOR = VICERRECTOR,
) {
  await simularApi(pagina, { sesion });
  await simularAcademico(pagina, iniciales);
  await pagina.goto('/iniciar-sesion');
  await ingresar(pagina, sesion.cuerpo.usuario);
  await pagina.getByRole('link', { name: 'Oferta del periodo' }).click();
  await expect(pagina.getByRole('heading', { level: 1, name: 'Oferta del periodo' })).toBeVisible();
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

test('el Vicerrector oferta una asignatura de 4 horas en 3 paralelos, ve 12 horas y con uno retirado 8', async ({
  page,
}) => {
  await abrirOferta(page);

  for (const paralelo of ['A_NOCTURNA', 'B_NOCTURNA', 'C_NOCTURNA']) {
    await page.getByRole('button', { name: 'Ofertar asignatura' }).click();
    const dialogo = page.getByRole('dialog', { name: 'Ofertar asignatura' });
    await dialogo.getByLabel('Carrera').selectOption(DS.codigo);
    await dialogo.getByRole('combobox', { name: 'Asignatura' }).selectOption(POO.codigo);
    await dialogo.getByLabel('Paralelo').fill(paralelo);
    await dialogo.getByLabel('Estudiantes previstos').fill('28');
    await dialogo.getByRole('button', { name: 'Ofertar' }).click();
    await expect(dialogo).toBeHidden();
  }

  await expect(page.getByText('Horas a cubrir: 12 h')).toBeVisible();
  const tabla = page.getByRole('table', { name: 'Oferta de DESARROLLO DE SOFTWARE' });
  await expect(tabla).toContainText('3 paralelos');

  await page.getByRole('button', { name: `Retirar ${POO.codigo} C_NOCTURNA` }).click();
  await page
    .getByRole('dialog', { name: `Retirar ${POO.codigo} de C_NOCTURNA` })
    .getByRole('button', { name: 'Retirar' })
    .click();
  await expect(page.getByText('Horas a cubrir: 8 h')).toBeVisible();
  await expect(tabla).toContainText(
    'C_NOCTURNA · Nocturna · Presencial · 28 estudiantes · 4 h · Retirada',
  );
});

test('la oferta del periodo', async ({ page }) => {
  await abrirOferta(page, [
    nocturna('A_NOCTURNA'),
    { ...nocturna('B_NOCTURNA'), estado: 'RETIRADA' },
  ]);
  await expect(page.getByRole('table', { name: 'Oferta de DESARROLLO DE SOFTWARE' })).toBeVisible();

  await sinFallas(page);
});

for (const [boton, titulo] of [
  ['Ofertar asignatura', 'Ofertar asignatura'],
  [`Corregir ${POO.codigo} A_NOCTURNA`, `Corregir ${POO.codigo} en A_NOCTURNA`],
  [`Retirar ${POO.codigo} A_NOCTURNA`, `Retirar ${POO.codigo} de A_NOCTURNA`],
] as const) {
  test(`el diálogo «${titulo}»`, async ({ page }) => {
    await abrirOferta(page, [nocturna('A_NOCTURNA')]);
    await expect(
      page.getByRole('table', { name: 'Oferta de DESARROLLO DE SOFTWARE' }),
    ).toBeVisible();
    await page.getByRole('button', { name: boton }).click();
    const dialogo = page.getByRole('dialog', { name: titulo });
    await expect(dialogo).toBeVisible();
    // Sin esperar a que termine la transición, axe mide el contraste a media opacidad.
    await expect(page.locator('.MuiDialog-container')).toHaveCSS('opacity', '1');
    if (boton === 'Ofertar asignatura') {
      await dialogo.getByLabel('Carrera').selectOption(DS.codigo);
      await expect(dialogo.getByRole('combobox', { name: 'Asignatura' })).toBeVisible();
    }

    await sinFallas(page);
  });
}

const SUGERENCIA = {
  periodo: '2026-2',
  asignatura: POO.codigo,
  nombre: POO.nombre,
  paralelo: 'A_NOCTURNA',
  horasDelParalelo: 4,
  troncoComun: false,
  version: 2,
  minimoAfinidad: 2,
  fueraPorElMinimo: 1,
  candidatos: [
    {
      identificacion: '0109990010',
      nombre: 'PERALTA MALDONADO JUAN ANDRES',
      dedicacion: 'TC',
      horasContratadas: 40,
      horasDisponibles: 37.5,
      horasEnOtrasCarreras: 2.5,
      horasSuficientes: true,
      puntaje: {
        asignatura: POO.codigo,
        nombre: POO.nombre,
        total: 7,
        topeAplicado: false,
        desglose: [
          {
            tipo: 'CUARTO_NIVEL',
            nombre: 'MÁSTER EN TELEMÁTICA',
            coincidencia: 'CAMPO_ESPECIFICO',
            aporte: 7,
          },
        ],
      },
    },
    {
      identificacion: '0109990020',
      nombre: 'TORRES LEON LUIS',
      dedicacion: 'TP',
      horasContratadas: 10,
      horasDisponibles: 2,
      horasEnOtrasCarreras: 8,
      horasSuficientes: false,
      puntaje: {
        asignatura: POO.codigo,
        nombre: POO.nombre,
        total: 9,
        topeAplicado: false,
        desglose: [],
      },
    },
  ],
  bajoElMinimo: [
    {
      identificacion: '0109990040',
      nombre: 'VERA CAMPOS ROSA',
      dedicacion: 'TC',
      horasContratadas: 40,
      horasDisponibles: 40,
      horasEnOtrasCarreras: 0,
      horasSuficientes: true,
      puntaje: {
        asignatura: POO.codigo,
        nombre: POO.nombre,
        total: 1,
        topeAplicado: false,
        desglose: [],
      },
    },
  ],
  distributivoCreado: true,
};

test('la sugerencia de docentes de una asignatura-paralelo, con su desglose', async ({ page }) => {
  await page.route('**/api/generacion/periodos/*/oferta/*/*/sugerencia', (ruta) =>
    ruta.fulfill({ json: SUGERENCIA }),
  );
  await abrirOferta(page, [nocturna('A_NOCTURNA')]);

  await page.getByRole('button', { name: `Sugerencia de ${POO.codigo} A_NOCTURNA` }).click();

  const dialogo = page.getByRole('dialog', {
    name: `Docentes sugeridos para ${POO.nombre} · A_NOCTURNA`,
  });
  await expect(page.locator('.MuiDialog-container')).toHaveCSS('opacity', '1');
  await expect(dialogo.getByRole('button', { name: 'Docentes bajo el mínimo (1)' })).toBeVisible();
  await expect(
    dialogo.getByText(
      'Tiempo completo · 40 h contratadas · 37,5 h disponibles · 2,5 h en otras carreras y unidades',
    ),
  ).toBeVisible();
  await expect(dialogo.getByText('Sin horas suficientes')).toBeVisible();

  await dialogo.getByRole('button', { name: /PERALTA MALDONADO/ }).click();
  await expect(
    dialogo.getByText('Título de cuarto nivel: MÁSTER EN TELEMÁTICA · campo específico · 7 puntos'),
  ).toBeVisible();

  await sinFallas(page);
});

const COORDINADOR = {
  estado: 200,
  cuerpo: {
    usuario: 'coordinacion.demo',
    nombre: 'Coordinación (demo)',
    roles: ['COORDINADOR_CARRERA'],
    permisos: [
      'OFERTA:LECTURA:PROPIO',
      'OFERTA:ESCRITURA:PROPIO',
      'DISTRIBUTIVO:LECTURA:PROPIO',
      'DOCENTES:LECTURA',
    ],
  },
};

/** El coordinador abre la sugerencia; cada propuesta queda en los parciales simulados. */
async function abrirComoCoordinador(pagina: Page) {
  const propuestas: unknown[] = [];
  await pagina.route('**/api/generacion/periodos/*/oferta/*/*/sugerencia', (ruta) =>
    ruta.fulfill({ json: SUGERENCIA }),
  );
  await pagina.route('**/api/generacion/periodos/*/oferta/*/*/propuesta', async (ruta) => {
    const { docente, texto } = ruta.request().postDataJSON() as {
      docente: string;
      texto?: string;
    };
    const propuesta = {
      asignatura: POO.codigo,
      paralelo: 'A_NOCTURNA',
      docente,
      nombreDocente: 'PROPUESTO DEL SERVIDOR',
      justificacion: texto ? 'EXCEPCION' : 'AFINIDAD',
      texto,
      retirada: false,
      horasSuficientes: true,
    };
    propuestas.push(propuesta);
    await ruta.fulfill({ json: propuesta });
  });
  await abrirOferta(pagina, [nocturna('A_NOCTURNA')], COORDINADOR);
  await pagina.route('**/api/generacion/periodos/*/parciales', (ruta) =>
    ruta.fulfill({
      json: {
        creado: true,
        parciales: [
          {
            carrera: DS.codigo,
            estado: 'EN_ELABORACION',
            ofertadas: 1,
            conPropuesta: propuestas.length,
            completo: propuestas.length === 1,
            propuestas,
          },
        ],
      },
    }),
  );
  await pagina.getByRole('button', { name: `Sugerencia de ${POO.codigo} A_NOCTURNA` }).click();
  await expect(pagina.locator('.MuiDialog-container').first()).toHaveCSS('opacity', '1');
  return pagina.getByRole('dialog', { name: /^Docentes sugeridos para/ });
}

test('el coordinador propone a un candidato y ve el parcial completo', async ({ page }) => {
  const dialogo = await abrirComoCoordinador(page);

  await dialogo.getByRole('button', { name: /^Proponer a PERALTA/ }).click();

  await expect(page.getByRole('dialog')).toBeHidden();
  await expect(page.getByText('Propuesto: PROPUESTO DEL SERVIDOR · Afinidad')).toBeVisible();
  await expect(page.getByText('1 de 1 propuestas')).toBeVisible();
  await expect(page.getByText('Parcial completo')).toBeVisible();
  await sinFallas(page);
});

test('proponer bajo el mínimo exige la justificación, y el diálogo es accesible', async ({
  page,
}) => {
  const dialogo = await abrirComoCoordinador(page);

  await dialogo.getByRole('button', { name: 'Docentes bajo el mínimo (1)' }).click();
  await sinFallas(page);
  await dialogo
    .getByRole('button', { name: /^Proponer a / })
    .last()
    .click();

  const justificar = page.getByRole('dialog', { name: /^Justificar a / });
  await expect(page.locator('.MuiDialog-container').last()).toHaveCSS('opacity', '1');
  await justificar.getByRole('button', { name: 'Guardar propuesta' }).click();
  await expect(justificar.getByText(/en 10 a 500 caracteres/)).toBeVisible();
  await sinFallas(page);

  await justificar.getByLabel(/Justificación/).fill('Dictó esta materia durante tres años');
  await justificar.getByRole('button', { name: 'Guardar propuesta' }).click();

  await expect(page.getByRole('dialog')).toBeHidden();
  await expect(page.getByText('Propuesto: PROPUESTO DEL SERVIDOR · Excepción')).toBeVisible();
  await expect(page.getByText('Dictó esta materia durante tres años')).toBeVisible();
  await sinFallas(page);
});

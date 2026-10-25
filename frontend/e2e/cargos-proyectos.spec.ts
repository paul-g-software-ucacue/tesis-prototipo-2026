// F03-H05 en un navegador, con la API simulada: Talento Humano registra un coordinador sin título de
// cuarto nivel y ve la advertencia de RN-20; registra un proyecto y lo ve asignable en 2026-I y no
// asignable en 2026-II. También la accesibilidad de «Cargos», de «Proyectos» y de sus diálogos con
// axe-core (WCAG 2.1 AA), también con los campos marcados en rojo.
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

const PERIODOS = [
  { codigo: '2026-I', nombre: 'Periodo 2026-I', inicio: '2026-04-06', fin: '2026-08-07' },
  { codigo: '2026-II', nombre: 'Periodo 2026-II', inicio: '2026-10-05', fin: '2027-02-05' },
];

const REFERENCIAS = {
  carreras: [{ codigo: '550613A01-P-0301', nombre: 'DESARROLLO DE SOFTWARE', siglas: 'DS' }],
  asignaturas: [],
  camposAmplios: [],
  unidades: [
    {
      codigo: 'G-INV',
      nombre: 'Gestión de Investigación',
      tipo: 'GESTION',
      gestionAutorizada: 'G07',
      activa: true,
    },
  ],
  periodos: PERIODOS,
};

const DOCENTES = [
  {
    identificacion: '0109990028',
    nombreCompleto: 'CABRERA VAZQUEZ ANA GABRIELA',
    carreraBase: '550613A01-P-0301',
    tipoPersonal: 'TITULAR',
    dedicacion: 'TC',
    horasContratadas: 40,
    condicionEspecial: 'NINGUNA',
    estado: 'ACTIVO',
  },
];

type Cuerpo = Record<string, unknown>;

// Los cargos y los proyectos en memoria, con lo que respondería Docentes.
async function simularCargosYProyectos(pagina: Page) {
  const cargos: Cuerpo[] = [];
  const proyectos: Cuerpo[] = [];
  await pagina.route('**/api/docentes/referencias', (ruta) => ruta.fulfill({ json: REFERENCIAS }));
  await pagina.route('**/api/docentes/docentes*', (ruta) => ruta.fulfill({ json: DOCENTES }));
  await pagina.route('**/api/docentes/cargos*', async (ruta) => {
    if (ruta.request().method() === 'POST') {
      const nuevo = ruta.request().postDataJSON() as Cuerpo;
      const cargo = {
        ...nuevo,
        nombreDelDocente: 'CABRERA VAZQUEZ ANA GABRIELA',
        nombreDeCarrera: 'DESARROLLO DE SOFTWARE',
        advertencias: [
          {
            codigo: 'CUARTO_NIVEL',
            descripcion: 'Le falta un título de cuarto nivel registrado (RN-20).',
          },
        ],
      };
      cargos.push(cargo);
      await ruta.fulfill({ status: 201, json: cargo });
      return;
    }
    await ruta.fulfill({ json: cargos });
  });
  await pagina.route('**/api/docentes/proyectos*', async (ruta) => {
    if (ruta.request().method() === 'POST') {
      const nuevo = ruta.request().postDataJSON() as Cuerpo;
      proyectos.push(nuevo);
      await ruta.fulfill({ status: 201, json: { ...nuevo, nombresDeCarreras: [] } });
      return;
    }
    const periodo = PERIODOS.find(
      (uno) => uno.codigo === new URL(ruta.request().url()).searchParams.get('periodo'),
    );
    await ruta.fulfill({
      json: proyectos.map((proyecto) => {
        if (!periodo) {
          return { ...proyecto, nombresDeCarreras: [] };
        }
        const cruza =
          String(proyecto.vigenciaInicio) <= periodo.fin &&
          String(proyecto.vigenciaFin) >= periodo.inicio;
        return {
          ...proyecto,
          nombresDeCarreras: [],
          asignable: cruza,
          motivos: cruza ? [] : ['VIGENCIA'],
        };
      }),
    });
  });
}

async function abrir(pagina: Page, enlace: 'Cargos' | 'Proyectos') {
  await simularApi(pagina, { sesion: TALENTO_HUMANO });
  await simularCargosYProyectos(pagina);
  await pagina.goto('/iniciar-sesion');
  await ingresar(pagina, 'datos.demo');
  await pagina.getByRole('link', { name: enlace }).click();
  await expect(pagina.getByRole('heading', { level: 1, name: enlace })).toBeVisible();
}

async function registrarCoordinador(pagina: Page) {
  await pagina.getByRole('button', { name: 'Registrar cargo' }).click();
  const dialogo = pagina.getByRole('dialog', { name: 'Registrar cargo' });
  await dialogo.getByLabel('Docente').selectOption('0109990028');
  await dialogo.getByLabel('Cargo', { exact: true }).selectOption('COORDINADOR_CARRERA');
  await dialogo.getByLabel('Carrera que coordina').selectOption('550613A01-P-0301');
  await dialogo.getByLabel('Fecha de designación').fill('2025-04-01');
  await dialogo.getByLabel('Documento de designación').fill('RES-PRUEBA-2025-004');
  await dialogo.getByRole('button', { name: 'Registrar' }).click();
  await expect(dialogo).toBeHidden();
}

async function registrarProyecto(pagina: Page) {
  await pagina.getByRole('button', { name: 'Registrar proyecto' }).click();
  const dialogo = pagina.getByRole('dialog', { name: 'Registrar proyecto' });
  await dialogo.getByLabel('Código').fill('ppp-2026-ds');
  await dialogo.getByLabel('Nombre').fill('Prácticas de Desarrollo de Software');
  await dialogo.getByLabel('Tipo').selectOption('PRACTICAS_PREPROFESIONALES');
  await dialogo.getByLabel('Resolución del OCS').fill('RES-OCS-PRUEBA-2026-011');
  await dialogo.getByLabel('Fecha de la resolución').fill('2026-02-26');
  await dialogo.getByLabel('Inicio de la vigencia').fill('2026-04-06');
  await dialogo.getByLabel('Fin de la vigencia').fill('2026-08-14');
  await dialogo.getByLabel('Director').selectOption('0109990028');
  await dialogo.getByLabel('Horas semanales aprobadas').fill('2');
  const carreras = dialogo.getByRole('group', { name: 'Carreras' });
  await carreras.getByLabel('Carrera', { exact: true }).selectOption('550613A01-P-0301');
  const participantes = dialogo.getByRole('group', { name: 'Participantes' });
  await participantes.getByLabel('Docente', { exact: true }).selectOption('0109990028');
  await participantes.getByLabel('Rol', { exact: true }).selectOption('TUTOR_PRACTICAS');
  await participantes.getByLabel('Horas', { exact: true }).fill('2');
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

test('Talento Humano registra un coordinador sin cuarto nivel y ve la advertencia de RN-20', async ({
  page,
}) => {
  await abrir(page, 'Cargos');
  await registrarCoordinador(page);

  await expect(page.getByRole('alert').filter({ hasText: 'se guardó' })).toContainText(
    'Le falta un título de cuarto nivel registrado (RN-20).',
  );
  await expect(page.getByRole('table', { name: 'Cargos' })).toContainText(
    'Le falta un título de cuarto nivel registrado (RN-20).',
  );
  await sinFallas(page);
});

test('Talento Humano registra un proyecto: es asignable en 2026-I y no en 2026-II', async ({
  page,
}) => {
  await abrir(page, 'Proyectos');
  await registrarProyecto(page);
  const tabla = page.getByRole('table', { name: 'Proyectos' });

  await expect(page.getByLabel('Periodo')).toHaveValue('2026-II');
  await expect(tabla).toContainText('PPP-2026-DS');
  await expect(tabla).toContainText('No asignable por su vigencia');
  await page.getByLabel('Periodo').selectOption('2026-I');
  await expect(tabla).toContainText('Asignable');
  await expect(tabla).not.toContainText('No asignable');
  await sinFallas(page);
});

for (const [enlace, boton, titulo] of [
  ['Cargos', 'Registrar cargo', 'Registrar cargo'],
  ['Proyectos', 'Registrar proyecto', 'Registrar proyecto'],
] as const) {
  test(`el diálogo «${titulo}», vacío y con los campos marcados en rojo`, async ({ page }) => {
    await abrir(page, enlace);
    await page.getByRole('button', { name: boton }).click();
    const dialogo = page.getByRole('dialog', { name: titulo });
    await expect(dialogo).toBeVisible();
    await sinFallas(page);

    await dialogo.getByRole('button', { name: 'Registrar' }).click();
    await expect(dialogo.getByRole('alert')).toContainText('marcados en rojo');
    await sinFallas(page);
  });
}

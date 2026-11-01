// La pantalla «Docentes» (F03-H04, FR-011): buscar por nombre o identificación, filtrar, ver la
// ficha y, con «Docentes», «Catálogos» o «Distributivo» de escritura, registrar, corregir con las
// listas de la ficha y cambiar el estado laboral (D-70).
import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { instalarApiSimulada, json, type ApiSimulada } from '../../pruebas/apiSimulada';
import { montarSpa } from '../../pruebas/montarSpa';
import { cerrarSesion, iniciarSesion } from '../../sesion/almacen';
import { AFINIDAD, PERALTA, REFERENCIAS, RESUMENES } from './datosDePrueba';

const TALENTO_HUMANO = ['DOCENTES:LECTURA', 'DOCENTES:ESCRITURA'];
const CONSOLIDACION = ['OFERTA:LECTURA', 'DISTRIBUTIVO:LECTURA'];
const LISTA = '/api/docentes/docentes';
const PERALTA_RUTA = `${LISTA}/0109990010`;

function sesionCon(simulada: ApiSimulada, usuario: string, permisos: string[]) {
  simulada.responder(
    'GET',
    '/api/usuarios/sesion',
    json(200, { usuario, nombre: `${usuario} (demo)`, roles: [], permisos }),
  );
}

async function cuerpoDe(simulada: ApiSimulada, metodo: string, ruta: string): Promise<unknown> {
  const peticion = simulada.peticiones.find(
    (candidata) => candidata.method === metodo && new URL(candidata.url).pathname === ruta,
  );
  if (!peticion) {
    throw new Error(`No hubo ${metodo} ${ruta}`);
  }
  return (await peticion.json()) as unknown;
}

/**
 * Escribe un texto de una vez: pegarlo es un solo evento, y el formulario del docente tiene muchos
 * campos (escribir letra por letra pasaba de 15 s con la cobertura activa).
 */
async function escribir(
  usuario: ReturnType<typeof userEvent.setup>,
  campo: HTMLElement,
  texto: string,
) {
  await usuario.click(campo);
  await usuario.paste(texto);
}

describe('Docentes', () => {
  let simulada: ApiSimulada;

  beforeEach(() => {
    simulada = instalarApiSimulada();
    simulada.responder('GET', LISTA, json(200, RESUMENES));
    simulada.responder('GET', PERALTA_RUTA, json(200, PERALTA));
    simulada.responder('GET', '/api/docentes/referencias', json(200, REFERENCIAS));
    iniciarSesion('cabecera.cuerpo.firma', new Date(Date.now() + 60_000));
  });

  afterEach(() => {
    cerrarSesion();
  });

  it('lista los docentes con su carrera, su tipo, su dedicación y su estado en español', async () => {
    sesionCon(simulada, 'consolidacion.demo', CONSOLIDACION);
    montarSpa('/docentes');

    expect(await screen.findByRole('heading', { level: 1, name: 'Docentes' })).toBeInTheDocument();
    const tabla = await screen.findByRole('table', { name: 'Docentes' });
    expect(screen.getByText('2 docentes')).toBeInTheDocument();
    const peralta = within(tabla).getByText('PERALTA MALDONADO JUAN ANDRES').closest('tr');
    expect(peralta).toHaveTextContent('0109990010');
    expect(peralta).toHaveTextContent('DESARROLLO DE SOFTWARE');
    expect(peralta).toHaveTextContent('Ocasional');
    expect(peralta).toHaveTextContent('Tiempo completo · 40 h');
    expect(peralta).toHaveTextContent('Activo');
    const cabrera = within(tabla).getByText('CABRERA VAZQUEZ ANA GABRIELA').closest('tr');
    expect(cabrera).toHaveTextContent('Medio tiempo · 20 h');
    expect(cabrera).toHaveTextContent('En licencia');
  });

  it('busca por nombre o identificación y filtra por carrera base y estado', async () => {
    sesionCon(simulada, 'consolidacion.demo', CONSOLIDACION);
    const usuario = userEvent.setup();
    montarSpa('/docentes');

    await screen.findByRole('table', { name: 'Docentes' });
    await usuario.type(screen.getByLabelText('Buscar por nombre o identificación'), 'peraltá');
    await usuario.selectOptions(screen.getByLabelText('Carrera base'), '550613A01-P-0301');
    await usuario.selectOptions(screen.getByLabelText('Estado laboral'), 'ACTIVO');

    await waitFor(() => {
      const busquedas = simulada.peticiones
        .filter((peticion) => new URL(peticion.url).pathname === LISTA)
        .map((peticion) => new URL(peticion.url).searchParams);
      const ultima = busquedas.at(-1);
      expect(ultima?.get('texto')).toBe('peraltá');
      expect(ultima?.get('carrera')).toBe('550613A01-P-0301');
      expect(ultima?.get('estado')).toBe('ACTIVO');
    });
  });

  it('muestra la ficha: cada título con su campo amplio y específico, y cada curso con sus horas', async () => {
    sesionCon(simulada, 'consolidacion.demo', CONSOLIDACION);
    const usuario = userEvent.setup();
    montarSpa('/docentes');

    await usuario.click(
      await screen.findByRole('button', { name: 'Ver la ficha de PERALTA MALDONADO JUAN ANDRES' }),
    );
    const ficha = await screen.findByRole('dialog', {
      name: 'Ficha de PERALTA MALDONADO JUAN ANDRES',
    });
    expect(
      await within(ficha).findByText(
        /Tercer nivel: INGENIERO DE SISTEMAS · campo amplio 06 \(Tecnologías de la información y la comunicación\), específico 061/,
      ),
    ).toBeInTheDocument();
    expect(
      within(ficha).getByText(
        /Maestría: MÁSTER EN TELEMÁTICA \(2015\) · campo amplio 06 .*específico 061/,
      ),
    ).toBeInTheDocument();
    expect(
      within(ficha).getByText(
        'PROGRAMACIÓN EN JAVA · 40 h (2023) · campo amplio 06 (Tecnologías de la información y la comunicación), específico 061',
      ),
    ).toBeInTheDocument();
    expect(
      within(ficha).getByText(
        /DOCENCIA UNIVERSITARIA · 60 h \(2021\) · campo amplio 01 \(Educación\)/,
      ),
    ).toBeInTheDocument();
    expect(within(ficha).getByText(/DESARROLLADOR en EMPRESA XYZ · 3 años/)).toBeInTheDocument();
    expect(
      within(ficha).getByText('DS-P-II-POO-204 · PROGRAMACIÓN ORIENTADA A OBJETOS'),
    ).toBeInTheDocument();
    expect(within(ficha).getByText('Ocasional')).toBeInTheDocument();
    expect(within(ficha).getByText('Tiempo completo · 40 h contratadas')).toBeInTheDocument();
    expect(within(ficha).getByText('Ninguna')).toBeInTheDocument();
  });

  it('sin escritura no ofrece acciones y dice por qué', async () => {
    sesionCon(simulada, 'consolidacion.demo', CONSOLIDACION);
    montarSpa('/docentes');

    expect(
      await screen.findByText('Solo lectura: tu rol no registra ni corrige docentes.'),
    ).toBeInTheDocument();
    await screen.findByRole('table', { name: 'Docentes' });
    for (const nombre of [/^Registrar docente/, /^Corregir/, /^Cambiar el estado/]) {
      expect(screen.queryByRole('button', { name: nombre })).not.toBeInTheDocument();
    }
  });

  // Llena unos 20 campos con user-event: con cobertura y la suite en paralelo pasa de los 15 s.
  it('Talento Humano registra un docente: la categoría sigue al tipo de personal y dice qué falta', async () => {
    sesionCon(simulada, 'datos.demo', TALENTO_HUMANO);
    simulada.responder('POST', LISTA, json(201, PERALTA));
    const usuario = userEvent.setup();
    montarSpa('/docentes');

    await usuario.click(await screen.findByRole('button', { name: 'Registrar docente' }));
    const dialogo = await screen.findByRole('dialog', { name: 'Registrar docente' });
    await usuario.click(within(dialogo).getByRole('button', { name: 'Registrar' }));
    expect(
      within(dialogo).getByText(/^Falta: Identificación, Primer apellido/),
    ).toBeInTheDocument();

    await escribir(usuario, within(dialogo).getByLabelText('Identificación'), '0109990010');
    await escribir(usuario, within(dialogo).getByLabelText('Primer apellido'), 'peralta');
    await escribir(usuario, within(dialogo).getByLabelText('Primer nombre'), 'juan');
    await escribir(
      usuario,
      within(dialogo).getByLabelText('Correo institucional'),
      'juan.peralta@instituto-prueba.edu.ec',
    );
    await usuario.selectOptions(within(dialogo).getByLabelText('Tipo de personal'), 'TITULAR');
    expect(within(dialogo).getByLabelText('Categoría de escalafón')).toBeEnabled();
    await usuario.selectOptions(within(dialogo).getByLabelText('Tipo de personal'), 'OCASIONAL');
    expect(within(dialogo).getByLabelText('Categoría de escalafón')).toBeDisabled();
    expect(within(dialogo).getByLabelText('Categoría de escalafón')).toHaveValue('NO_APLICA');
    await usuario.selectOptions(within(dialogo).getByLabelText('Dedicación'), 'TC');
    await escribir(usuario, within(dialogo).getByLabelText('Horas contratadas'), '40');
    await escribir(usuario, within(dialogo).getByLabelText('Fecha de ingreso'), '2019-10-01');
    await escribir(usuario, within(dialogo).getByLabelText('Inicio del contrato'), '2026-04-01');
    await escribir(usuario, within(dialogo).getByLabelText('Fin del contrato'), '2026-08-31');
    await usuario.selectOptions(within(dialogo).getByLabelText('Carrera base'), '550613A01-P-0301');
    await escribir(usuario, within(dialogo).getByLabelText('Título'), 'INGENIERO DE SISTEMAS');
    await usuario.selectOptions(within(dialogo).getByLabelText('Campo amplio'), '06');
    await escribir(usuario, within(dialogo).getByLabelText('Campo específico'), '061');
    await usuario.click(within(dialogo).getByRole('button', { name: 'Agregar curso' }));
    const curso = within(dialogo).getByRole('group', { name: 'Cursos y capacitaciones' });
    await escribir(usuario, within(curso).getByLabelText('Nombre'), 'PROGRAMACIÓN EN JAVA');
    await escribir(usuario, within(curso).getByLabelText('Campo específico'), '061');
    await escribir(usuario, within(curso).getByLabelText('Horas'), '40');
    await escribir(usuario, within(curso).getByLabelText('Año'), '2023');
    await usuario.click(within(dialogo).getByRole('button', { name: 'Agregar asignatura afín' }));
    await usuario.selectOptions(within(dialogo).getByLabelText('Asignatura'), 'DS-P-II-POO-204');
    await usuario.click(within(dialogo).getByRole('button', { name: 'Registrar' }));

    await waitFor(() =>
      expect(screen.queryByRole('dialog', { name: 'Registrar docente' })).not.toBeInTheDocument(),
    );
    expect(await cuerpoDe(simulada, 'POST', LISTA)).toEqual({
      tipoIdentificacion: 'CEDULA',
      identificacion: '0109990010',
      primerApellido: 'peralta',
      primerNombre: 'juan',
      correoInstitucional: 'juan.peralta@instituto-prueba.edu.ec',
      tipoPersonal: 'OCASIONAL',
      categoriaEscalafon: 'NO_APLICA',
      dedicacion: 'TC',
      horasContratadas: 40,
      fechaIngreso: '2019-10-01',
      inicioContrato: '2026-04-01',
      finContrato: '2026-08-31',
      condicionEspecial: 'NINGUNA',
      carreraBase: '550613A01-P-0301',
      ficha: {
        tercerNivel: {
          titulo: 'INGENIERO DE SISTEMAS',
          campoAmplio: '06',
          campoEspecifico: '061',
        },
        cuartoNivel: [],
        cursos: [{ nombre: 'PROGRAMACIÓN EN JAVA', campoEspecifico: '061', horas: 40, anio: 2023 }],
        experiencia: [],
        asignaturasAfines: ['DS-P-II-POO-204'],
      },
    });
  }, 30_000);

  it('cada campo admite solo lo suyo y, al registrar, marca de una vez lo que está mal sin enviar', async () => {
    sesionCon(simulada, 'datos.demo', TALENTO_HUMANO);
    const usuario = userEvent.setup();
    montarSpa('/docentes');

    await usuario.click(await screen.findByRole('button', { name: 'Registrar docente' }));
    const dialogo = await screen.findByRole('dialog', { name: 'Registrar docente' });
    const identificacion = within(dialogo).getByLabelText('Identificación');
    await escribir(usuario, identificacion, '01a0999001099');
    expect(identificacion).toHaveValue('0109990010');
    expect(identificacion).toHaveAttribute('inputmode', 'numeric');
    await escribir(usuario, within(dialogo).getByLabelText('Primer apellido'), 'Peralta 2');
    expect(within(dialogo).getByLabelText('Primer apellido')).toHaveValue('Peralta ');
    await escribir(usuario, within(dialogo).getByLabelText('Teléfono (opcional)'), '099-12');
    expect(within(dialogo).getByLabelText('Teléfono (opcional)')).toHaveValue('09912');
    await usuario.click(within(dialogo).getByRole('button', { name: 'Agregar curso' }));
    const curso = within(dialogo).getByRole('group', { name: 'Cursos y capacitaciones' });
    await escribir(usuario, within(curso).getByLabelText('Campo específico'), 'SERVIDORES');
    expect(within(curso).getByLabelText('Campo específico')).toHaveValue('');
    await escribir(usuario, within(curso).getByLabelText('Horas'), '60h');
    expect(within(curso).getByLabelText('Horas')).toHaveValue('60');

    await usuario.click(within(dialogo).getByRole('button', { name: 'Registrar' }));
    expect(within(dialogo).getByRole('alert')).toHaveTextContent(
      /^Falta: Primer nombre, .* Revisa \d+ campos marcados en rojo\.$/,
    );
    expect(
      within(dialogo).getByText('El teléfono tiene de 7 a 10 dígitos; escribiste 5.'),
    ).toBeInTheDocument();
    expect(within(curso).getByLabelText('Campo específico')).toHaveAttribute(
      'aria-invalid',
      'true',
    );
    expect(
      within(curso).getByText(/^Escribe el código del campo específico, de 3 dígitos/),
    ).toBeInTheDocument();
    expect(simulada.peticiones.some((peticion) => peticion.method === 'POST')).toBe(false);

    // Las marcas se actualizan mientras se corrige.
    await escribir(usuario, within(dialogo).getByLabelText('Teléfono (opcional)'), '00');
    expect(
      within(dialogo).queryByText(/^El teléfono tiene de 7 a 10 dígitos/),
    ).not.toBeInTheDocument();
  });

  it('corrige con la identificación fija, agrega un curso y quita la experiencia', async () => {
    sesionCon(simulada, 'datos.demo', TALENTO_HUMANO);
    simulada.responder('PUT', PERALTA_RUTA, json(200, PERALTA));
    const usuario = userEvent.setup();
    montarSpa('/docentes');

    await usuario.click(
      await screen.findByRole('button', { name: 'Corregir a PERALTA MALDONADO JUAN ANDRES' }),
    );
    const dialogo = await screen.findByRole('dialog', {
      name: 'Corregir PERALTA MALDONADO JUAN ANDRES',
    });
    expect(within(dialogo).getByLabelText('Identificación')).toBeDisabled();
    expect(within(dialogo).getByLabelText('Identificación')).toHaveValue('0109990010');
    await usuario.selectOptions(within(dialogo).getByLabelText('Dedicación'), 'MT');
    const horas = within(dialogo).getByLabelText('Horas contratadas');
    await usuario.clear(horas);
    await escribir(usuario, horas, '20');
    await usuario.click(within(dialogo).getByRole('button', { name: 'Quitar experiencia 1' }));
    await usuario.click(within(dialogo).getByRole('button', { name: 'Agregar curso' }));
    const cursos = within(dialogo).getByRole('group', { name: 'Cursos y capacitaciones' });
    const nombres = within(cursos).getAllByLabelText('Nombre');
    await escribir(usuario, nombres[2] as HTMLElement, 'SCRUM');
    await escribir(
      usuario,
      within(cursos).getAllByLabelText('Campo específico')[2] as HTMLElement,
      '061',
    );
    await escribir(usuario, within(cursos).getAllByLabelText('Horas')[2] as HTMLElement, '20');
    await escribir(usuario, within(cursos).getAllByLabelText('Año')[2] as HTMLElement, '2024');
    await usuario.click(within(dialogo).getByRole('button', { name: 'Guardar' }));

    await waitFor(() =>
      expect(
        screen.queryByRole('dialog', { name: 'Corregir PERALTA MALDONADO JUAN ANDRES' }),
      ).not.toBeInTheDocument(),
    );
    const cuerpo = (await cuerpoDe(simulada, 'PUT', PERALTA_RUTA)) as Record<string, unknown>;
    expect(cuerpo).toMatchObject({ dedicacion: 'MT', horasContratadas: 20 });
    expect(cuerpo).not.toHaveProperty('identificacion');
    expect(cuerpo.ficha).toMatchObject({
      experiencia: [],
      cursos: [
        { nombre: 'PROGRAMACIÓN EN JAVA', horas: 40 },
        { nombre: 'DOCENCIA UNIVERSITARIA', horas: 60 },
        { nombre: 'SCRUM', campoEspecifico: '061', horas: 20, anio: 2024 },
      ],
    });
  });

  it('un 400 marca su campo y, en una lista, su fila; un 409 muestra su título', async () => {
    sesionCon(simulada, 'datos.demo', TALENTO_HUMANO);
    simulada.responder(
      'PUT',
      PERALTA_RUTA,
      json(400, {
        titulo: 'Solicitud inválida',
        descripcion: 'Revisa los datos marcados.',
        codigo: 'COMUN_SOLICITUD_INVALIDA',
        detalles: [
          { campo: 'dedicacion', descripcion: 'no es un valor permitido: usa TC, MT o TP' },
          {
            campo: 'ficha.cursos[1].horas',
            descripcion: 'Las horas del curso son enteras, de 1 a 2000',
          },
        ],
      }),
    );
    const usuario = userEvent.setup();
    montarSpa('/docentes');

    await usuario.click(
      await screen.findByRole('button', { name: 'Corregir a PERALTA MALDONADO JUAN ANDRES' }),
    );
    const dialogo = await screen.findByRole('dialog', {
      name: 'Corregir PERALTA MALDONADO JUAN ANDRES',
    });
    await usuario.click(within(dialogo).getByRole('button', { name: 'Guardar' }));

    expect(
      await within(dialogo).findByText('No es un valor permitido: usa TC, MT o TP.'),
    ).toBeInTheDocument();
    const cursos = within(dialogo).getByRole('group', { name: 'Cursos y capacitaciones' });
    expect(within(cursos).getAllByLabelText('Horas')[1]).toHaveAttribute('aria-invalid', 'true');
    expect(within(cursos).getAllByLabelText('Horas')[0]).toHaveAttribute('aria-invalid', 'false');
    expect(
      within(cursos).getByText('Las horas del curso son enteras, de 1 a 2000.'),
    ).toBeInTheDocument();

    simulada.responder(
      'PUT',
      PERALTA_RUTA,
      json(409, {
        titulo: 'Correo repetido',
        descripcion: 'Otro docente ya usa ese correo institucional. Revisa el correo.',
        codigo: 'DOCENTES_CORREO_REPETIDO',
      }),
    );
    await usuario.click(within(dialogo).getByRole('button', { name: 'Guardar' }));
    expect(await within(dialogo).findByText('Correo repetido')).toBeInTheDocument();
  });

  it('cambia el estado laboral de un docente', async () => {
    sesionCon(simulada, 'datos.demo', TALENTO_HUMANO);
    simulada.responder(
      'PUT',
      `${PERALTA_RUTA}/estado`,
      json(200, { ...PERALTA, estado: 'LICENCIA' }),
    );
    const usuario = userEvent.setup();
    montarSpa('/docentes');

    await usuario.click(
      await screen.findByRole('button', {
        name: 'Cambiar el estado de PERALTA MALDONADO JUAN ANDRES',
      }),
    );
    const dialogo = await screen.findByRole('dialog', {
      name: 'Estado laboral de PERALTA MALDONADO JUAN ANDRES',
    });
    await usuario.selectOptions(within(dialogo).getByLabelText('Estado laboral'), 'LICENCIA');
    await usuario.click(within(dialogo).getByRole('button', { name: 'Guardar' }));

    await waitFor(() =>
      expect(
        screen.queryByRole('dialog', { name: 'Estado laboral de PERALTA MALDONADO JUAN ANDRES' }),
      ).not.toBeInTheDocument(),
    );
    expect(await cuerpoDe(simulada, 'PUT', `${PERALTA_RUTA}/estado`)).toEqual({
      estado: 'LICENCIA',
    });
  });

  it('el menú lleva a «Docentes» con la lectura de los docentes, el distributivo o los catálogos', async () => {
    sesionCon(simulada, 'consolidacion.demo', CONSOLIDACION);
    montarSpa('/');

    expect(await screen.findByRole('link', { name: 'Docentes' })).toHaveAttribute(
      'href',
      '/docentes',
    );
  });

  it('sin ninguna de esas lecturas no muestra «Docentes» en el menú', async () => {
    sesionCon(simulada, 'gestion.demo', ['PROYECTOS:LECTURA']);
    montarSpa('/');

    await screen.findByRole('link', { name: 'Inicio' });
    expect(screen.queryByRole('link', { name: 'Docentes' })).not.toBeInTheDocument();
  });
});

// La afinidad en la ficha (F06-H01, RF-17): la calcula Generación con la versión del periodo.
describe('Afinidad por asignatura', () => {
  const COORDINACION = ['DOCENTES:LECTURA'];
  const RUTA_AFINIDAD = '/api/generacion/docentes/0109990010/afinidad';
  let simulada: ApiSimulada;

  beforeEach(() => {
    simulada = instalarApiSimulada();
    sesionCon(simulada, 'coordinacion.demo', COORDINACION);
    simulada.responder('GET', LISTA, json(200, RESUMENES));
    simulada.responder('GET', '/api/docentes/referencias', json(200, REFERENCIAS));
    simulada.responder('GET', PERALTA_RUTA, json(200, PERALTA));
    iniciarSesion('cabecera.cuerpo.firma', new Date(Date.now() + 60_000));
  });

  afterEach(() => {
    cerrarSesion();
  });

  async function abrirFicha(usuario: ReturnType<typeof userEvent.setup>) {
    montarSpa('/docentes');
    await usuario.click(
      await screen.findByRole('button', {
        name: 'Ver la ficha de PERALTA MALDONADO JUAN ANDRES',
      }),
    );
    return screen.findByRole('dialog', { name: 'Ficha de PERALTA MALDONADO JUAN ANDRES' });
  }

  async function elegirPeriodo(
    usuario: ReturnType<typeof userEvent.setup>,
    ficha: HTMLElement,
    periodo: RegExp,
  ) {
    await usuario.click(await within(ficha).findByRole('combobox', { name: 'Periodo' }));
    await usuario.click(await screen.findByRole('option', { name: periodo }));
  }

  it('no consulta hasta que eliges un periodo', async () => {
    const usuario = userEvent.setup();
    const ficha = await abrirFicha(usuario);

    expect(await within(ficha).findByText(/Elige un periodo/)).toBeInTheDocument();
    expect(simulada.peticiones.some((una) => new URL(una.url).pathname === RUTA_AFINIDAD)).toBe(
      false,
    );
  });

  it('muestra las asignaturas de la oferta en el orden de Generación, con su puntaje', async () => {
    simulada.responder('GET', RUTA_AFINIDAD, json(200, AFINIDAD));
    const usuario = userEvent.setup();
    const ficha = await abrirFicha(usuario);

    await elegirPeriodo(usuario, ficha, /^2026-I ·/);

    const asignaturas = await within(ficha).findAllByRole('button', { name: /punto/ });
    expect(asignaturas.map((una) => una.textContent)).toEqual([
      'PROGRAMACIÓN ORIENTADA A OBJETOS7,5 puntos',
      'ANATOMÍA0 puntos',
    ]);
    expect(within(ficha).getByText(/versión 2 de la normativa/)).toBeInTheDocument();
    const pedida = simulada.peticiones.find((una) => new URL(una.url).pathname === RUTA_AFINIDAD);
    expect(new URL(pedida?.url ?? 'http://x').searchParams.get('periodo')).toBe('2026-I');
  });

  it('despliega de dónde sale cada punto, con el tope aplicado', async () => {
    simulada.responder('GET', RUTA_AFINIDAD, json(200, AFINIDAD));
    const usuario = userEvent.setup();
    const ficha = await abrirFicha(usuario);
    await elegirPeriodo(usuario, ficha, /^2026-I ·/);

    await usuario.click(
      await within(ficha).findByRole('button', { name: /PROGRAMACIÓN ORIENTADA A OBJETOS/ }),
    );

    expect(
      await within(ficha).findByText(
        'Título de cuarto nivel: MÁSTER EN TELEMÁTICA · solo campo amplio · 2 puntos',
      ),
    ).toBeVisible();
    expect(
      within(ficha).getByText('Curso: PROGRAMACIÓN EN JAVA · campo específico · 1,5 puntos'),
    ).toBeVisible();
    expect(within(ficha).getByText(/Se aplicó el tope de cursos/)).toBeVisible();
  });

  it('dice que nada coincide en una asignatura con puntaje 0', async () => {
    simulada.responder('GET', RUTA_AFINIDAD, json(200, AFINIDAD));
    const usuario = userEvent.setup();
    const ficha = await abrirFicha(usuario);
    await elegirPeriodo(usuario, ficha, /^2026-I ·/);

    await usuario.click(await within(ficha).findByRole('button', { name: /ANATOMÍA/ }));

    expect(await within(ficha).findByText(/Nada de la ficha coincide/)).toBeVisible();
  });

  it('un periodo sin abrir muestra el mensaje del catálogo, sin puntajes', async () => {
    simulada.responder(
      'GET',
      RUTA_AFINIDAD,
      json(409, {
        titulo: 'Periodo sin abrir',
        descripcion:
          'El periodo todavía no tiene su versión de normativa. Ábrelo en «Periodos» para calcular la afinidad.',
        codigo: 'GENERACION_PERIODO_SIN_ABRIR',
      }),
    );
    const usuario = userEvent.setup();
    const ficha = await abrirFicha(usuario);

    await elegirPeriodo(usuario, ficha, /^2026-II ·/);

    expect(await within(ficha).findByText(/Ábrelo en «Periodos»/)).toBeInTheDocument();
    expect(within(ficha).queryByRole('button', { name: /punto/ })).not.toBeInTheDocument();
  });

  it('un periodo sin oferta lo dice', async () => {
    simulada.responder('GET', RUTA_AFINIDAD, json(200, { ...AFINIDAD, puntajes: [] }));
    const usuario = userEvent.setup();
    const ficha = await abrirFicha(usuario);

    await elegirPeriodo(usuario, ficha, /^2026-I ·/);

    expect(
      await within(ficha).findByText('No hay oferta para mostrar en este periodo.'),
    ).toBeInTheDocument();
  });

  it('sin carrera indicada, el coordinador ve el aviso y ningún puntaje', async () => {
    simulada.responder(
      'GET',
      RUTA_AFINIDAD,
      json(403, {
        titulo: 'Falta tu carrera',
        descripcion:
          'Todavía no tienes una carrera indicada. Pide al Administrador del sistema que la indique en «Roles de usuarios».',
        codigo: 'COMUN_CARRERA_SIN_INDICAR',
      }),
    );
    const usuario = userEvent.setup();
    const ficha = await abrirFicha(usuario);

    await elegirPeriodo(usuario, ficha, /^2026-I ·/);

    expect(await within(ficha).findByText('Falta tu carrera')).toBeInTheDocument();
    expect(within(ficha).queryByText(/puntos?$/)).not.toBeInTheDocument();
  });

  it.each([
    [502, 'COMUN_PROCESO_CON_ERROR', 'Servicio con problemas'],
    [504, 'COMUN_PROCESO_SIN_RESPUESTA', 'Servicio sin respuesta'],
  ])('un %i muestra el aviso sin detalles técnicos', async (estado, codigo, titulo) => {
    simulada.responder(
      'GET',
      RUTA_AFINIDAD,
      json(estado, { titulo, descripcion: 'Intenta de nuevo en unos minutos.', codigo }),
    );
    const usuario = userEvent.setup();
    const ficha = await abrirFicha(usuario);

    await elegirPeriodo(usuario, ficha, /^2026-I ·/);

    expect(await within(ficha).findByText(titulo)).toBeInTheDocument();
    expect(within(ficha).queryByText(new RegExp(codigo))).not.toBeInTheDocument();
    expect(within(ficha).queryByRole('button', { name: /punto/ })).not.toBeInTheDocument();
  });
});

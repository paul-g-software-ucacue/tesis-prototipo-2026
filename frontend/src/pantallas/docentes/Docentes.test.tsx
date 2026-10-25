// La pantalla «Docentes» (F03-H04, FR-011): buscar por nombre o identificación, filtrar, ver la
// ficha y, con «Docentes», «Catálogos» o «Distributivo» de escritura, registrar, corregir con las
// listas de la ficha y cambiar el estado laboral (D-70).
import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { instalarApiSimulada, json, type ApiSimulada } from '../../pruebas/apiSimulada';
import { montarSpa } from '../../pruebas/montarSpa';
import { cerrarSesion, iniciarSesion } from '../../sesion/almacen';
import { PERALTA, REFERENCIAS, RESUMENES } from './datosDePrueba';

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

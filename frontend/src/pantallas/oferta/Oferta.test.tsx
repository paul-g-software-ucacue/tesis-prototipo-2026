// La pantalla «Oferta del periodo» (F03-H03, FR-011): la oferta del periodo elegido por carrera,
// con las horas a cubrir de cada asignatura, de cada carrera y del periodo. Con «Catálogos:
// escritura» o «Distributivo: escritura» y el periodo en Planificación se oferta, se corrige, se
// retira y se vuelve a ofertar (D-70).
import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { instalarApiSimulada, json, type ApiSimulada } from '../../pruebas/apiSimulada';
import { montarSpa } from '../../pruebas/montarSpa';
import { cerrarSesion, iniciarSesion } from '../../sesion/almacen';
import { ASIGNATURAS, CARRERAS } from '../carreras/datosDePrueba';
import { UNIDADES } from '../estructura/datosDePrueba';
import { PERIODOS } from '../periodos/datosDePrueba';
import {
  OFERTA_EN_PLANIFICACION,
  OFERTA_VIGENTE,
  PARCIALES,
  PROPUESTA,
  SIN_PARCIALES,
  SUGERENCIA,
} from './datosDePrueba';

const VICERRECTOR = ['DISTRIBUTIVO:LECTURA', 'DISTRIBUTIVO:ESCRITURA'];
const CONSOLIDACION = ['OFERTA:LECTURA', 'DISTRIBUTIVO:LECTURA'];
const RUTA = '/api/academico/periodos/2026-2/oferta';
const COORDINACION = ['OFERTA:LECTURA', 'DISTRIBUTIVO:LECTURA', 'DOCENTES:LECTURA'];
const SOLO_OFERTA = ['OFERTA:LECTURA'];
const RUTA_SUGERENCIA =
  '/api/generacion/periodos/2026-2/oferta/DS-P-II-POO-204/A_NOCTURNA/sugerencia';
const RUTA_PARCIALES = '/api/generacion/periodos/2026-2/parciales';
const RUTA_PROPUESTA =
  '/api/generacion/periodos/2026-2/oferta/DS-P-II-POO-204/A_NOCTURNA/propuesta';
const COORDINADOR = [...COORDINACION, 'OFERTA:ESCRITURA:PROPIO'];
const TEXTO = 'Dictó esta materia durante tres años';
const CARRERA_DE_LA_PRUEBA = 'Oferta de DESARROLLO DE SOFTWARE';

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

describe('Oferta del periodo', () => {
  let simulada: ApiSimulada;

  beforeEach(() => {
    simulada = instalarApiSimulada();
    simulada.responder('GET', '/api/academico/periodos', json(200, PERIODOS));
    simulada.responder('GET', RUTA, json(200, OFERTA_EN_PLANIFICACION));
    simulada.responder('GET', '/api/academico/periodos/2026-1/oferta', json(200, OFERTA_VIGENTE));
    simulada.responder('GET', '/api/academico/carreras', json(200, CARRERAS));
    simulada.responder('GET', '/api/academico/unidades', json(200, UNIDADES));
    simulada.responder(
      'GET',
      '/api/academico/carreras/550613A01-P-0301/asignaturas',
      json(200, ASIGNATURAS),
    );
    simulada.responder('GET', RUTA_PARCIALES, json(200, SIN_PARCIALES));
    simulada.responder(
      'GET',
      '/api/generacion/periodos/2026-1/parciales',
      json(200, SIN_PARCIALES),
    );
    iniciarSesion('cabecera.cuerpo.firma', new Date(Date.now() + 60_000));
  });

  afterEach(() => {
    cerrarSesion();
  });

  it('elige el periodo en Planificación y muestra las horas a cubrir por asignatura, carrera y periodo', async () => {
    sesionCon(simulada, 'consolidacion.demo', CONSOLIDACION);
    montarSpa('/oferta');

    expect(
      await screen.findByRole('heading', { level: 1, name: 'Oferta del periodo' }),
    ).toBeInTheDocument();
    expect(await screen.findByLabelText('Periodo')).toHaveValue('2026-2');
    expect(await screen.findByText('Horas a cubrir: 12 h')).toBeInTheDocument();
    const tabla = await screen.findByRole('table', { name: 'Oferta de DESARROLLO DE SOFTWARE' });
    expect(
      screen.getByRole('heading', { name: 'DESARROLLO DE SOFTWARE · 12 h a cubrir' }),
    ).toBeInTheDocument();
    const poo = within(tabla).getByText('DS-P-II-POO-204').closest('tr') as HTMLElement;
    expect(poo).toHaveTextContent('12 h');
    expect(poo).toHaveTextContent('3 paralelos');
    expect(poo).toHaveTextContent('A_NOCTURNA · Nocturna · Presencial · 28 estudiantes · 4 h');
    const md = within(tabla).getByText('DS-B-I-MD-101').closest('tr') as HTMLElement;
    expect(md).toHaveTextContent('Retirada');
    expect(md).toHaveTextContent('0 h');
  });

  it('sin escritura no ofrece acciones y dice por qué', async () => {
    sesionCon(simulada, 'consolidacion.demo', CONSOLIDACION);
    montarSpa('/oferta');

    expect(
      await screen.findByText('Solo lectura: tu rol no cambia la oferta.'),
    ).toBeInTheDocument();
    await screen.findByRole('table', { name: 'Oferta de DESARROLLO DE SOFTWARE' });
    for (const nombre of [/^Ofertar/, /^Corregir/, /^Retirar/, /^Volver a ofertar/]) {
      expect(screen.queryByRole('button', { name: nombre })).not.toBeInTheDocument();
    }
  });

  it('con el periodo fuera de Planificación queda en solo lectura y dice por qué', async () => {
    sesionCon(simulada, 'vicerrectorado.demo', VICERRECTOR);
    const usuario = userEvent.setup();
    montarSpa('/oferta');

    await usuario.selectOptions(await screen.findByLabelText('Periodo'), '2026-1');

    expect(
      await screen.findByText(
        'El periodo está en Vigente: la oferta solo cambia en Planificación.',
      ),
    ).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Ofertar asignatura' })).not.toBeInTheDocument();
  });

  it('el Vicerrector oferta una asignatura con la carrera, la asignatura, el paralelo y la jornada de la carrera', async () => {
    sesionCon(simulada, 'vicerrectorado.demo', VICERRECTOR);
    simulada.responder('POST', RUTA, json(201, OFERTA_EN_PLANIFICACION.asignaturasParalelo[1]));
    const usuario = userEvent.setup();
    montarSpa('/oferta');

    await usuario.click(await screen.findByRole('button', { name: 'Ofertar asignatura' }));
    const dialogo = await screen.findByRole('dialog', { name: 'Ofertar asignatura' });
    await usuario.selectOptions(within(dialogo).getByLabelText('Carrera'), '550613A01-P-0301');
    await usuario.selectOptions(
      await within(dialogo).findByRole('combobox', { name: 'Asignatura' }),
      'DS-B-I-MD-101',
    );
    expect(within(dialogo).getByText(/Vacío: las de la asignatura, 3 h/)).toBeInTheDocument();
    await usuario.type(within(dialogo).getByLabelText('Paralelo'), 'd_nocturna');
    expect(within(dialogo).getByLabelText('Modalidad')).toHaveValue('PRESENCIAL');
    await usuario.selectOptions(within(dialogo).getByLabelText('Jornada'), 'NOCTURNA');
    await usuario.type(within(dialogo).getByLabelText('Estudiantes previstos'), '30');
    await usuario.click(within(dialogo).getByRole('button', { name: 'Ofertar' }));

    await waitFor(() =>
      expect(screen.queryByRole('dialog', { name: 'Ofertar asignatura' })).not.toBeInTheDocument(),
    );
    expect(await cuerpoDe(simulada, 'POST', RUTA)).toEqual({
      asignatura: 'DS-B-I-MD-101',
      paralelo: 'D_NOCTURNA',
      jornada: 'NOCTURNA',
      modalidad: 'PRESENCIAL',
      estudiantes: 30,
    });
  });

  it('antes de elegir la carrera, la asignatura y la jornada están desactivadas y dicen por qué', async () => {
    sesionCon(simulada, 'vicerrectorado.demo', VICERRECTOR);
    const usuario = userEvent.setup();
    montarSpa('/oferta');

    await usuario.click(await screen.findByRole('button', { name: 'Ofertar asignatura' }));
    const dialogo = await screen.findByRole('dialog', { name: 'Ofertar asignatura' });

    for (const nombre of ['Asignatura', 'Jornada']) {
      const campo = within(dialogo).getByRole('combobox', { name: nombre });
      expect(campo).toBeDisabled();
      expect(campo).toHaveAccessibleDescription('Elige primero la carrera.');
    }
    await usuario.selectOptions(within(dialogo).getByLabelText('Carrera'), '550613A01-P-0301');
    expect(within(dialogo).getByRole('combobox', { name: 'Asignatura' })).toBeEnabled();
    expect(within(dialogo).getByRole('combobox', { name: 'Jornada' })).toBeEnabled();
    expect(within(dialogo).getByRole('combobox', { name: 'Jornada' })).toHaveAccessibleDescription(
      'Una de las jornadas de la carrera: Matutina, Nocturna.',
    );
  });

  it('un 400 muestra el motivo junto a la pista y un 409 su título', async () => {
    sesionCon(simulada, 'admin.demo', [
      'CATALOGOS:LECTURA',
      'CATALOGOS:ESCRITURA',
      'DISTRIBUTIVO:LECTURA',
    ]);
    simulada.responder(
      'POST',
      RUTA,
      json(400, {
        titulo: 'Solicitud inválida',
        descripcion: 'Revisa los campos marcados y vuelve a intentarlo.',
        codigo: 'COMUN_SOLICITUD_INVALIDA',
        detalles: [
          {
            campo: 'horasSemanales',
            descripcion: 'No pueden pasar las 3 horas de la asignatura (RN-18)',
          },
        ],
      }),
    );
    const usuario = userEvent.setup();
    montarSpa('/oferta');

    await usuario.click(await screen.findByRole('button', { name: 'Ofertar asignatura' }));
    const dialogo = await screen.findByRole('dialog', { name: 'Ofertar asignatura' });
    await usuario.selectOptions(within(dialogo).getByLabelText('Carrera'), '550613A01-P-0301');
    await usuario.selectOptions(
      await within(dialogo).findByRole('combobox', { name: 'Asignatura' }),
      'DS-B-I-MD-101',
    );
    await usuario.type(within(dialogo).getByLabelText('Paralelo'), 'D_NOCTURNA');
    await usuario.selectOptions(within(dialogo).getByLabelText('Jornada'), 'NOCTURNA');
    await usuario.type(within(dialogo).getByLabelText('Estudiantes previstos'), '30');
    await usuario.type(within(dialogo).getByLabelText('Horas semanales'), '5');
    await usuario.click(within(dialogo).getByRole('button', { name: 'Ofertar' }));

    expect(
      await within(dialogo).findByText(
        'No pueden pasar las 3 horas de la asignatura (RN-18). Vacío: las de la asignatura, 3 h; si las indicas, no pueden pasarlas.',
      ),
    ).toBeInTheDocument();
    expect(within(dialogo).getByLabelText('Horas semanales')).toHaveAttribute(
      'aria-invalid',
      'true',
    );
  });

  it('corrige, retira con confirmación y vuelve a ofertar', async () => {
    sesionCon(simulada, 'vicerrectorado.demo', VICERRECTOR);
    const ruta = `${RUTA}/DS-P-II-POO-204/A_NOCTURNA`;
    simulada.responder('PUT', ruta, json(200, OFERTA_EN_PLANIFICACION.asignaturasParalelo[1]));
    simulada.responder(
      'PUT',
      `${ruta}/estado`,
      json(200, OFERTA_EN_PLANIFICACION.asignaturasParalelo[1]),
    );
    simulada.responder(
      'PUT',
      `${RUTA}/DS-B-I-MD-101/A_NOCTURNA/estado`,
      json(200, OFERTA_EN_PLANIFICACION.asignaturasParalelo[0]),
    );
    const usuario = userEvent.setup();
    montarSpa('/oferta');

    await usuario.click(
      await screen.findByRole('button', { name: 'Corregir DS-P-II-POO-204 A_NOCTURNA' }),
    );
    const corregir = await screen.findByRole('dialog', {
      name: 'Corregir DS-P-II-POO-204 en A_NOCTURNA',
    });
    await usuario.selectOptions(within(corregir).getByLabelText('Modalidad'), 'DUAL');
    await usuario.clear(within(corregir).getByLabelText('Estudiantes previstos'));
    await usuario.type(within(corregir).getByLabelText('Estudiantes previstos'), '31');
    await usuario.click(within(corregir).getByRole('button', { name: 'Guardar' }));
    expect(await cuerpoDe(simulada, 'PUT', ruta)).toEqual({
      jornada: 'NOCTURNA',
      modalidad: 'DUAL',
      estudiantes: 31,
    });

    await usuario.click(
      await screen.findByRole('button', { name: 'Retirar DS-P-II-POO-204 A_NOCTURNA' }),
    );
    await usuario.click(
      within(
        await screen.findByRole('dialog', { name: 'Retirar DS-P-II-POO-204 de A_NOCTURNA' }),
      ).getByRole('button', { name: 'Retirar' }),
    );
    expect(await cuerpoDe(simulada, 'PUT', `${ruta}/estado`)).toEqual({ estado: 'RETIRADA' });

    await usuario.click(
      await screen.findByRole('button', { name: 'Volver a ofertar DS-B-I-MD-101 A_NOCTURNA' }),
    );
    expect(await cuerpoDe(simulada, 'PUT', `${RUTA}/DS-B-I-MD-101/A_NOCTURNA/estado`)).toEqual({
      estado: 'OFERTADA',
    });
  });

  it('si la lista de periodos responde 403, lo dice sin romperse', async () => {
    sesionCon(simulada, 'catalogos.demo', ['CATALOGOS:LECTURA']);
    simulada.responder(
      'GET',
      '/api/academico/periodos',
      json(403, {
        titulo: 'Acceso no autorizado',
        descripcion: 'No tienes permiso para esta acción.',
        codigo: 'SEGURIDAD_ACCESO_NO_AUTORIZADO',
      }),
    );
    montarSpa('/oferta');

    expect(
      await screen.findByRole('heading', { level: 1, name: 'Oferta del periodo' }),
    ).toBeInTheDocument();
    expect(await screen.findByText('Acceso no autorizado')).toBeInTheDocument();
  });

  it.each([['OFERTA:LECTURA'], ['DISTRIBUTIVO:LECTURA'], ['CATALOGOS:LECTURA']])(
    'con %s está en el menú',
    async (permiso) => {
      sesionCon(simulada, 'lector.demo', [permiso]);
      montarSpa('/');

      expect(await screen.findByRole('link', { name: 'Oferta del periodo' })).toBeInTheDocument();
    },
  );

  it('sin oferta para mostrar, lo dice sin hablar del periodo entero', async () => {
    simulada.responder(
      'GET',
      RUTA,
      json(200, {
        ...OFERTA_EN_PLANIFICACION,
        horasACubrir: 0,
        carreras: [],
        asignaturasParalelo: [],
      }),
    );
    sesionCon(simulada, 'coordinacion.demo', COORDINACION);
    montarSpa('/oferta');

    expect(
      await screen.findByText('No hay oferta para mostrar en este periodo.'),
    ).toBeInTheDocument();
  });

  it('sin carrera indicada, el coordinador ve el aviso y ninguna asignatura', async () => {
    simulada.responder(
      'GET',
      RUTA,
      json(403, {
        titulo: 'Falta tu carrera',
        descripcion:
          'Todavía no tienes una carrera indicada. Pide al Administrador del sistema que la indique en «Roles de usuarios».',
        codigo: 'COMUN_CARRERA_SIN_INDICAR',
      }),
    );
    sesionCon(simulada, 'coordinacion.demo', COORDINACION);
    montarSpa('/oferta');

    expect(await screen.findByText('Falta tu carrera')).toBeInTheDocument();
    expect(screen.getByText(/«Roles de usuarios»/)).toBeInTheDocument();
    expect(screen.queryByRole('table')).not.toBeInTheDocument();
  });

  describe('sugerencia', () => {
    async function abrirSugerencia() {
      sesionCon(simulada, 'coordinacion.demo', COORDINACION);
      montarSpa('/oferta');
      const boton = await screen.findByRole('button', {
        name: 'Sugerencia de DS-P-II-POO-204 A_NOCTURNA',
      });
      await userEvent.click(boton);
      return screen.findByRole('dialog', {
        name: 'Docentes sugeridos para PROGRAMACIÓN ORIENTADA A OBJETOS · A_NOCTURNA',
      });
    }

    it('ofrece la acción en cada fila ofertada a quien lee docentes, aunque no cambie la oferta', async () => {
      sesionCon(simulada, 'coordinacion.demo', COORDINACION);
      montarSpa('/oferta');

      expect(
        await screen.findByRole('button', { name: 'Sugerencia de DS-P-II-POO-204 B_NOCTURNA' }),
      ).toBeInTheDocument();
      expect(screen.queryByRole('button', { name: /^Corregir/ })).not.toBeInTheDocument();
    });

    it('una fila retirada no la ofrece', async () => {
      sesionCon(simulada, 'coordinacion.demo', COORDINACION);
      montarSpa('/oferta');

      await screen.findByRole('button', { name: 'Sugerencia de DS-P-II-POO-204 A_NOCTURNA' });
      expect(
        screen.queryByRole('button', { name: 'Sugerencia de DS-B-I-MD-101 A_NOCTURNA' }),
      ).not.toBeInTheDocument();
    });

    it('quien lee la oferta sin leer docentes no la ve', async () => {
      sesionCon(simulada, 'lector.demo', SOLO_OFERTA);
      montarSpa('/oferta');

      await screen.findByRole('table', { name: 'Oferta de DESARROLLO DE SOFTWARE' });
      expect(screen.queryByRole('button', { name: /^Sugerencia/ })).not.toBeInTheDocument();
    });

    it('con el periodo fuera de Planificación también se puede abrir', async () => {
      sesionCon(simulada, 'coordinacion.demo', COORDINACION);
      simulada.responder('GET', '/api/academico/periodos', json(200, [PERIODOS[1]]));
      montarSpa('/oferta');

      expect(
        await screen.findByRole('button', { name: 'Sugerencia de DS-P-II-POO-204 A_NOCTURNA' }),
      ).toBeInTheDocument();
    });

    it('muestra los candidatos en el orden en que llegan, con sus horas y sin datos de más', async () => {
      simulada.responder('GET', RUTA_SUGERENCIA, json(200, SUGERENCIA));
      const dialogo = await abrirSugerencia();

      expect(
        await within(dialogo).findByText(
          'Con el mínimo de 2 puntos de la versión 2 de la normativa.',
        ),
      ).toBeInTheDocument();
      const nombres = within(dialogo)
        .getAllByRole('button', { expanded: false })
        .map((uno) => uno.textContent);
      expect(nombres[0]).toContain('PERALTA MALDONADO JUAN ANDRES');
      expect(nombres[1]).toContain('SALINAS VEGA ANA MARIA');
      expect(nombres[2]).toContain('TORRES LEON LUIS');
      expect(
        within(dialogo).getByText(
          'Tiempo completo · 40 h contratadas · 40 h disponibles · 0 h en otras carreras y unidades',
        ),
      ).toBeInTheDocument();
      expect(within(dialogo).queryByText(/0109990010/)).not.toBeInTheDocument();
      expect(within(dialogo).queryByRole('button', { name: /^Proponer/ })).not.toBeInTheDocument();
    });

    it('las horas con decimales se muestran con coma y sin redondear', async () => {
      simulada.responder('GET', RUTA_SUGERENCIA, json(200, SUGERENCIA));
      const dialogo = await abrirSugerencia();

      expect(
        await within(dialogo).findByText(
          'Medio tiempo · 20 h contratadas · 14,25 h disponibles · 5,75 h en otras carreras y unidades',
        ),
      ).toBeInTheDocument();
    });

    it('marca a quien no le alcanzan las horas y solo a él', async () => {
      simulada.responder('GET', RUTA_SUGERENCIA, json(200, SUGERENCIA));
      const dialogo = await abrirSugerencia();

      expect(await within(dialogo).findAllByText('Sin horas suficientes')).toHaveLength(1);
    });

    it('despliega el desglose del candidato', async () => {
      simulada.responder('GET', RUTA_SUGERENCIA, json(200, SUGERENCIA));
      const dialogo = await abrirSugerencia();

      await userEvent.click(
        await within(dialogo).findByRole('button', { name: /PERALTA MALDONADO/ }),
      );

      expect(
        await within(dialogo).findByText(
          'Título de cuarto nivel: MÁSTER EN TELEMÁTICA · campo específico · 7 puntos',
        ),
      ).toBeInTheDocument();
    });

    it('deja a los docentes bajo el mínimo en una sección cerrada, con su puntaje', async () => {
      simulada.responder('GET', RUTA_SUGERENCIA, json(200, SUGERENCIA));
      const dialogo = await abrirSugerencia();

      const seccion = await within(dialogo).findByRole('button', {
        name: 'Docentes bajo el mínimo (3)',
      });
      expect(seccion).toHaveAttribute('aria-expanded', 'false');
      expect(within(dialogo).getByText('VERA CAMPOS ROSA')).not.toBeVisible();

      await userEvent.click(seccion);

      expect(await within(dialogo).findByText('VERA CAMPOS ROSA')).toBeVisible();
      expect(within(dialogo).getByText('1,5 puntos')).toBeVisible();
    });

    it('en tronco común no hay sección de docentes bajo el mínimo', async () => {
      simulada.responder(
        'GET',
        RUTA_SUGERENCIA,
        json(200, { ...SUGERENCIA, troncoComun: true, bajoElMinimo: [], fueraPorElMinimo: 0 }),
      );
      const dialogo = await abrirSugerencia();

      await within(dialogo).findByText('Es de tronco común: el mínimo de afinidad no aplica.');
      expect(within(dialogo).queryByText(/Docentes bajo el mínimo/)).not.toBeInTheDocument();
    });

    it('en tronco común dice que el mínimo no aplica', async () => {
      simulada.responder(
        'GET',
        RUTA_SUGERENCIA,
        json(200, { ...SUGERENCIA, troncoComun: true, fueraPorElMinimo: 0 }),
      );
      const dialogo = await abrirSugerencia();

      expect(
        await within(dialogo).findByText('Es de tronco común: el mínimo de afinidad no aplica.'),
      ).toBeInTheDocument();
      expect(within(dialogo).queryByText(/quedaron fuera|quedó fuera/)).not.toBeInTheDocument();
    });

    it('sin candidatos dice que nadie alcanza el mínimo', async () => {
      simulada.responder(
        'GET',
        RUTA_SUGERENCIA,
        json(200, { ...SUGERENCIA, candidatos: [], fueraPorElMinimo: 1 }),
      );
      const dialogo = await abrirSugerencia();

      expect(
        await within(dialogo).findByText('Ningún docente alcanza el mínimo en esta asignatura.'),
      ).toBeInTheDocument();
      expect(
        within(dialogo).getByRole('button', { name: 'Docentes bajo el mínimo (3)' }),
      ).toBeInTheDocument();
    });

    it('sin docentes activos lo dice', async () => {
      simulada.responder(
        'GET',
        RUTA_SUGERENCIA,
        json(200, { ...SUGERENCIA, candidatos: [], fueraPorElMinimo: 0 }),
      );
      const dialogo = await abrirSugerencia();

      expect(await within(dialogo).findByText('No hay docentes activos.')).toBeInTheDocument();
    });

    it.each([
      [
        409,
        'GENERACION_PERIODO_SIN_ABRIR',
        'Periodo sin abrir',
        'El periodo todavía no tiene su versión de normativa. Ábrelo en «Periodos» para calcular la afinidad.',
      ],
      [
        502,
        'COMUN_PROCESO_CON_ERROR',
        'Servicio con problemas',
        'Una parte del sistema tuvo un problema y no se pudo completar la consulta. Intenta de nuevo en unos minutos.',
      ],
      [
        504,
        'COMUN_PROCESO_SIN_RESPUESTA',
        'Servicio sin respuesta',
        'Una parte del sistema tardó demasiado en responder. Intenta de nuevo en unos minutos.',
      ],
    ])(
      'un %i muestra el aviso del catálogo, sin una lista',
      async (estado, codigo, titulo, descripcion) => {
        simulada.responder('GET', RUTA_SUGERENCIA, json(estado, { titulo, descripcion, codigo }));
        const dialogo = await abrirSugerencia();

        expect(await within(dialogo).findByText(titulo)).toBeInTheDocument();
        expect(within(dialogo).getByText(descripcion)).toBeInTheDocument();
        expect(within(dialogo).queryByText(/puntos?$/)).not.toBeInTheDocument();
      },
    );

    it('se cierra', async () => {
      simulada.responder('GET', RUTA_SUGERENCIA, json(200, SUGERENCIA));
      const dialogo = await abrirSugerencia();

      await userEvent.click(within(dialogo).getByRole('button', { name: 'Cerrar' }));

      await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
    });
  });

  describe('propuesta', () => {
    async function abrirComoCoordinador(permisos = COORDINADOR) {
      sesionCon(simulada, 'coordinacion.demo', permisos);
      montarSpa('/oferta');
      await userEvent.click(
        await screen.findByRole('button', { name: 'Sugerencia de DS-P-II-POO-204 A_NOCTURNA' }),
      );
      return screen.findByRole('dialog', { name: /^Docentes sugeridos para/ });
    }

    function conSugerencia(cambios = {}) {
      simulada.responder('GET', RUTA_SUGERENCIA, json(200, { ...SUGERENCIA, ...cambios }));
    }

    it('ofrece «Proponer» en cada candidato a quien escribe la oferta', async () => {
      conSugerencia();
      const dialogo = await abrirComoCoordinador();

      expect(
        await within(dialogo).findByRole('button', {
          name: 'Proponer a PERALTA MALDONADO JUAN ANDRES',
        }),
      ).toBeEnabled();
      // Los de la sección cerrada también se ofrecen, pero no se ven hasta abrirla.
      expect(within(dialogo).getAllByRole('button', { name: /^Proponer a/ })).toHaveLength(3);
    });

    it('sin distributivo lo dice y no ofrece proponer', async () => {
      conSugerencia({ distributivoCreado: false });
      const dialogo = await abrirComoCoordinador();

      expect(
        await within(dialogo).findByText(/todavía no tiene distributivo\. Pide al Vicerrector/),
      ).toBeVisible();
      expect(
        within(dialogo).queryByRole('button', { name: /^Proponer a/ }),
      ).not.toBeInTheDocument();
    });

    it('quien no escribe la oferta ve la lista sin proponer', async () => {
      conSugerencia();
      const dialogo = await abrirComoCoordinador(COORDINACION);

      await within(dialogo).findByText('PERALTA MALDONADO JUAN ANDRES');
      expect(
        within(dialogo).queryByRole('button', { name: /^Proponer a/ }),
      ).not.toBeInTheDocument();
      expect(within(dialogo).queryByText(/Pide al Vicerrector/)).not.toBeInTheDocument();
    });

    it('con el periodo cerrado no ofrece proponer', async () => {
      conSugerencia();
      simulada.responder(
        'GET',
        RUTA,
        json(200, { ...OFERTA_EN_PLANIFICACION, estadoDelPeriodo: 'CERRADO' }),
      );
      const dialogo = await abrirComoCoordinador();

      await within(dialogo).findByText('PERALTA MALDONADO JUAN ANDRES');
      expect(
        within(dialogo).queryByRole('button', { name: /^Proponer a/ }),
      ).not.toBeInTheDocument();
    });

    it('proponer manda solo el docente, cierra y recarga la oferta', async () => {
      conSugerencia();
      simulada.responder('PUT', RUTA_PROPUESTA, json(200, PROPUESTA));
      const dialogo = await abrirComoCoordinador();

      simulada.responder('GET', RUTA_PARCIALES, json(200, PARCIALES));
      await userEvent.click(
        await within(dialogo).findByRole('button', {
          name: 'Proponer a PERALTA MALDONADO JUAN ANDRES',
        }),
      );

      await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
      expect(await cuerpoDe(simulada, 'PUT', RUTA_PROPUESTA)).toEqual({ docente: '0109990010' });
      expect(
        await screen.findByText('Propuesto: PERALTA MALDONADO JUAN ANDRES · Afinidad'),
      ).toBeVisible();
      expect(screen.getByText('1 de 3 propuestas')).toBeVisible();
    });

    it('un docente propuesto sin horas suficientes queda avisado en la fila', async () => {
      simulada.responder(
        'GET',
        RUTA_PARCIALES,
        json(200, {
          ...PARCIALES,
          parciales: [
            {
              ...PARCIALES.parciales[0],
              propuestas: [{ ...PROPUESTA, horasSuficientes: false }],
            },
          ],
        }),
      );
      sesionCon(simulada, 'coordinacion.demo', COORDINADOR);
      montarSpa('/oferta');

      expect(await screen.findByText('Sin horas suficientes')).toBeVisible();
    });

    it('un error del catálogo no cierra el diálogo', async () => {
      conSugerencia();
      simulada.responder(
        'PUT',
        RUTA_PROPUESTA,
        json(409, {
          titulo: 'Docente no activo',
          descripcion: 'Ese docente no está activo. Elige otro.',
          codigo: 'GENERACION_DOCENTE_NO_ACTIVO',
        }),
      );
      const dialogo = await abrirComoCoordinador();

      await userEvent.click(
        await within(dialogo).findByRole('button', {
          name: 'Proponer a PERALTA MALDONADO JUAN ANDRES',
        }),
      );

      expect(await within(dialogo).findByText('Docente no activo')).toBeVisible();
      expect(within(dialogo).getByText('SALINAS VEGA ANA MARIA')).toBeVisible();
    });

    it('muestra la propuesta actual y no ofrece volver a proponer a ese docente', async () => {
      conSugerencia({ propuesta: PROPUESTA });
      const dialogo = await abrirComoCoordinador();

      expect(
        await within(dialogo).findByText(
          'Propuesta actual: PERALTA MALDONADO JUAN ANDRES · Afinidad',
        ),
      ).toBeVisible();
      expect(within(dialogo).getByText('Propuesto')).toBeVisible();
      expect(
        within(dialogo).queryByRole('button', { name: 'Proponer a PERALTA MALDONADO JUAN ANDRES' }),
      ).not.toBeInTheDocument();
    });

    async function pedirJustificacion() {
      conSugerencia();
      const dialogo = await abrirComoCoordinador();
      await userEvent.click(
        await within(dialogo).findByRole('button', { name: 'Docentes bajo el mínimo (3)' }),
      );
      await userEvent.click(
        await within(dialogo).findByRole('button', { name: 'Proponer a VERA CAMPOS ROSA' }),
      );
      return screen.findByRole('dialog', { name: 'Justificar a VERA CAMPOS ROSA' });
    }

    it('proponer bajo el mínimo pide el texto, con su contador y su largo', async () => {
      const justificar = await pedirJustificacion();

      const campo = within(justificar).getByLabelText(/Justificación/);
      expect(campo).toHaveAttribute('maxlength', '500');
      expect(within(justificar).getByText('0 de 500 caracteres.')).toBeVisible();

      await userEvent.type(campo, 'Hola');

      expect(within(justificar).getByText('4 de 500 caracteres.')).toBeVisible();
    });

    it('sin un texto de 10 caracteres no se guarda', async () => {
      const justificar = await pedirJustificacion();

      await userEvent.type(within(justificar).getByLabelText(/Justificación/), ' corto   ');
      await userEvent.click(within(justificar).getByRole('button', { name: 'Guardar propuesta' }));

      expect(
        await within(justificar).findByText(/en 10 a 500 caracteres\. 9 de 500 caracteres\./),
      ).toBeVisible();
      expect(simulada.peticiones.some((peticion) => peticion.method === 'PUT')).toBe(false);
    });

    it('con el texto guarda el docente y el texto limpio', async () => {
      simulada.responder('PUT', RUTA_PROPUESTA, json(200, PROPUESTA));
      const justificar = await pedirJustificacion();

      await userEvent.type(within(justificar).getByLabelText(/Justificación/), `  ${TEXTO}  `);
      await userEvent.click(within(justificar).getByRole('button', { name: 'Guardar propuesta' }));

      await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
      expect(await cuerpoDe(simulada, 'PUT', RUTA_PROPUESTA)).toEqual({
        docente: '0109990040',
        texto: TEXTO,
      });
    });

    it('un 400 del servidor se muestra en el campo y no cierra', async () => {
      simulada.responder(
        'PUT',
        RUTA_PROPUESTA,
        json(400, {
          titulo: 'Solicitud inválida',
          descripcion: 'Revisa los campos marcados y vuelve a intentarlo.',
          codigo: 'COMUN_SOLICITUD_INVALIDA',
          detalles: [{ campo: 'texto', descripcion: 'Escribe por qué propones a este docente' }],
        }),
      );
      const justificar = await pedirJustificacion();

      await userEvent.type(within(justificar).getByLabelText(/Justificación/), TEXTO);
      await userEvent.click(within(justificar).getByRole('button', { name: 'Guardar propuesta' }));

      expect(
        await within(justificar).findByText(/Escribe por qué propones a este docente\./),
      ).toBeVisible();
      expect(within(justificar).getByLabelText(/Justificación/)).toHaveAttribute(
        'aria-invalid',
        'true',
      );
    });

    it('cancelar la justificación vuelve a la lista', async () => {
      const justificar = await pedirJustificacion();

      await userEvent.click(within(justificar).getByRole('button', { name: 'Cancelar' }));

      await waitFor(() =>
        expect(
          screen.queryByRole('dialog', { name: 'Justificar a VERA CAMPOS ROSA' }),
        ).not.toBeInTheDocument(),
      );
      expect(screen.getByRole('dialog', { name: /^Docentes sugeridos para/ })).toBeVisible();
    });
  });

  describe('parcial', () => {
    function conParcial(cambios = {}, propuesta = {}) {
      simulada.responder(
        'GET',
        RUTA_PARCIALES,
        json(200, {
          creado: true,
          parciales: [
            {
              ...PARCIALES.parciales[0],
              ...cambios,
              propuestas: [{ ...PROPUESTA, ...propuesta }],
            },
          ],
        }),
      );
    }

    it('cada fila muestra el docente propuesto, su justificación y su texto', async () => {
      conParcial({}, { justificacion: 'EXCEPCION', texto: TEXTO });
      sesionCon(simulada, 'consolidacion.demo', CONSOLIDACION);
      montarSpa('/oferta');

      const tabla = await screen.findByRole('table', { name: CARRERA_DE_LA_PRUEBA });
      expect(
        await within(tabla).findByText('Propuesto: PERALTA MALDONADO JUAN ANDRES · Excepción'),
      ).toBeVisible();
      expect(within(tabla).getByText(TEXTO)).toBeVisible();
    });

    it('sin distributivo no habla de propuestas', async () => {
      sesionCon(simulada, 'consolidacion.demo', CONSOLIDACION);
      montarSpa('/oferta');

      await screen.findByRole('table', { name: CARRERA_DE_LA_PRUEBA });
      expect(screen.queryByText(/propuestas$/)).not.toBeInTheDocument();
    });

    it('dice cuando el parcial está completo', async () => {
      conParcial({ ofertadas: 3, conPropuesta: 3, completo: true });
      sesionCon(simulada, 'consolidacion.demo', CONSOLIDACION);
      montarSpa('/oferta');

      expect(await screen.findByText('3 de 3 propuestas')).toBeVisible();
      expect(screen.getByText('Parcial completo')).toBeVisible();
    });

    it('marca la propuesta de un paralelo retirado', async () => {
      conParcial({}, { retirada: true, horasSuficientes: false });
      sesionCon(simulada, 'coordinacion.demo', COORDINADOR);
      montarSpa('/oferta');

      expect(
        await screen.findByText('Paralelo retirado: no cuenta para el parcial.'),
      ).toBeVisible();
      expect(screen.queryByText('Sin horas suficientes')).not.toBeInTheDocument();
    });

    it('quita la propuesta con confirmación y recarga', async () => {
      conParcial();
      simulada.responder('DELETE', RUTA_PROPUESTA, json(204));
      sesionCon(simulada, 'coordinacion.demo', COORDINADOR);
      montarSpa('/oferta');

      await userEvent.click(
        await screen.findByRole('button', {
          name: 'Quitar propuesta de DS-P-II-POO-204 A_NOCTURNA',
        }),
      );
      const confirmar = await screen.findByRole('dialog', {
        name: 'Quitar la propuesta de DS-P-II-POO-204 A_NOCTURNA',
      });
      simulada.responder('GET', RUTA_PARCIALES, json(200, SIN_PARCIALES));
      await userEvent.click(within(confirmar).getByRole('button', { name: 'Quitar' }));

      await waitFor(() =>
        expect(screen.queryByText(/^Propuesto: PERALTA/)).not.toBeInTheDocument(),
      );
      expect(
        simulada.peticiones.some(
          (peticion) =>
            peticion.method === 'DELETE' && new URL(peticion.url).pathname === RUTA_PROPUESTA,
        ),
      ).toBe(true);
    });

    it('un error al quitar se muestra', async () => {
      conParcial();
      simulada.responder(
        'DELETE',
        RUTA_PROPUESTA,
        json(409, {
          titulo: 'Parcial cerrado',
          descripcion: 'Este parcial ya no admite cambios.',
          codigo: 'GENERACION_PARCIAL_NO_EDITABLE',
        }),
      );
      sesionCon(simulada, 'coordinacion.demo', COORDINADOR);
      montarSpa('/oferta');

      await userEvent.click(
        await screen.findByRole('button', {
          name: 'Quitar propuesta de DS-P-II-POO-204 A_NOCTURNA',
        }),
      );
      await userEvent.click(
        within(await screen.findByRole('dialog')).getByRole('button', { name: 'Quitar' }),
      );

      expect(await screen.findByText('Parcial cerrado')).toBeVisible();
    });

    it('quien no escribe la oferta no ve «Quitar propuesta»', async () => {
      conParcial();
      sesionCon(simulada, 'coordinacion.demo', COORDINACION);
      montarSpa('/oferta');

      await screen.findByText(/^Propuesto: PERALTA/);
      expect(screen.queryByRole('button', { name: /^Quitar propuesta/ })).not.toBeInTheDocument();
    });

    it('si Generación no responde, la oferta se sigue viendo con el aviso', async () => {
      simulada.responder(
        'GET',
        RUTA_PARCIALES,
        json(503, {
          titulo: 'Servicio no disponible',
          descripcion: 'Intenta de nuevo en unos minutos.',
          codigo: 'COMUN_SERVICIO_NO_DISPONIBLE',
        }),
      );
      sesionCon(simulada, 'consolidacion.demo', CONSOLIDACION);
      montarSpa('/oferta');

      expect(await screen.findByText('Servicio no disponible')).toBeVisible();
      expect(screen.getByRole('table', { name: CARRERA_DE_LA_PRUEBA })).toBeVisible();
    });
  });

  it('muestra las horas con hasta dos decimales', async () => {
    simulada.responder('GET', RUTA, json(200, { ...OFERTA_EN_PLANIFICACION, horasACubrir: 12.25 }));
    sesionCon(simulada, 'consolidacion.demo', CONSOLIDACION);
    montarSpa('/oferta');

    expect(await screen.findByText('Horas a cubrir: 12,25 h')).toBeVisible();
  });

  it('sin ninguno de los tres permisos no está en el menú y la ruta no la muestra', async () => {
    sesionCon(simulada, 'docente.demo', ['MI_DISTRIBUTIVO:LECTURA:PROPIO']);
    montarSpa('/oferta');

    expect(await screen.findByRole('alert')).toHaveTextContent('Acceso no autorizado');
    expect(screen.queryByRole('link', { name: 'Oferta del periodo' })).not.toBeInTheDocument();
  });
});

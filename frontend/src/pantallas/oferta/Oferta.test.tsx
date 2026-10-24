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
import { OFERTA_EN_PLANIFICACION, OFERTA_VIGENTE } from './datosDePrueba';

const VICERRECTOR = ['DISTRIBUTIVO:LECTURA', 'DISTRIBUTIVO:ESCRITURA'];
const CONSOLIDACION = ['OFERTA:LECTURA', 'DISTRIBUTIVO:LECTURA'];
const RUTA = '/api/academico/periodos/2026-2/oferta';

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
    expect(await screen.findByText('Horas a cubrir del periodo: 12 h')).toBeInTheDocument();
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

  it('sin ninguno de los tres permisos no está en el menú y la ruta no la muestra', async () => {
    sesionCon(simulada, 'docente.demo', ['MI_DISTRIBUTIVO:LECTURA:PROPIO']);
    montarSpa('/oferta');

    expect(await screen.findByRole('alert')).toHaveTextContent('Acceso no autorizado');
    expect(screen.queryByRole('link', { name: 'Oferta del periodo' })).not.toBeInTheDocument();
  });
});

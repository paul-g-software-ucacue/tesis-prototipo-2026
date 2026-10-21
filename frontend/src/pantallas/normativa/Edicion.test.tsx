// La pantalla «Normativa» con escritura (F04-H02): crear una versión nueva, editar en pantalla la
// de sin periodo y ver en solo lectura la que ya rige uno (asociarla a un periodo se hace en «Periodos», D-69).
import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { instalarApiSimulada, json, type ApiSimulada } from '../../pruebas/apiSimulada';
import { montarSpa } from '../../pruebas/montarSpa';
import { cerrarSesion, iniciarSesion } from '../../sesion/almacen';
import { DOS_VERSIONES, VERSION_1_EN_USO, VERSION_2 } from './datosDePrueba';

async function cuerpoDe(simulada: ApiSimulada, metodo: string, ruta: string): Promise<unknown> {
  const peticion = simulada.peticiones.find(
    (candidata) => candidata.method === metodo && new URL(candidata.url).pathname === ruta,
  );
  if (!peticion) {
    throw new Error(`No hubo ${metodo} ${ruta}`);
  }
  return (await peticion.json()) as unknown;
}

function sesionCon(simulada: ApiSimulada, permisos: string[]) {
  simulada.responder(
    'GET',
    '/api/usuarios/sesion',
    json(200, {
      usuario: 'vicerrectorado.demo',
      nombre: 'Vicerrectorado académico (demo)',
      roles: ['VICERRECTOR'],
      permisos,
    }),
  );
}

const ESCRITURA = ['PARAMETROS:LECTURA', 'PARAMETROS:ESCRITURA'];

describe('Normativa con escritura', () => {
  let simulada: ApiSimulada;

  beforeEach(() => {
    simulada = instalarApiSimulada();
    simulada.responder('GET', '/api/normativa/versiones', json(200, DOS_VERSIONES));
    simulada.responder('GET', '/api/normativa/versiones/1', json(200, VERSION_1_EN_USO));
    simulada.responder('GET', '/api/normativa/versiones/2', json(200, VERSION_2));
    iniciarSesion('cabecera.cuerpo.firma', new Date(Date.now() + 60_000));
  });

  afterEach(() => {
    cerrarSesion();
  });

  it('al abrir queda elegida la versión de número más alto', async () => {
    sesionCon(simulada, ESCRITURA);
    montarSpa('/normativa');

    expect(
      await screen.findByRole('heading', {
        level: 2,
        name: 'Versión 2 · Reforma de la política 2025',
      }),
    ).toBeInTheDocument();
    expect(screen.getByRole('list', { name: 'Datos de la versión' })).toHaveTextContent(
      'Copiada de la versión 1',
    );
  });

  it('crea una versión nueva a partir de la elegida y la deja elegida', async () => {
    sesionCon(simulada, ESCRITURA);
    const tres = { ...VERSION_2, numero: 3, codigo: 'SENESCYT-2025-02', copiadaDe: 2 };
    simulada.responder('POST', '/api/normativa/versiones', json(201, tres));
    simulada.responder('GET', '/api/normativa/versiones/3', json(200, tres));
    const usuario = userEvent.setup();
    montarSpa('/normativa');

    await usuario.click(await screen.findByRole('button', { name: 'Crear versión nueva' }));
    const dialogo = await screen.findByRole('dialog', {
      name: 'Versión nueva a partir de la versión 2',
    });
    await usuario.type(within(dialogo).getByLabelText('Código'), 'SENESCYT-2025-02');
    await usuario.type(within(dialogo).getByLabelText('Origen'), 'Reforma 2025');
    await usuario.type(within(dialogo).getByLabelText('Vigente desde'), '2025-02-01');
    await usuario.click(within(dialogo).getByRole('button', { name: 'Crear' }));

    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
    expect(await cuerpoDe(simulada, 'POST', '/api/normativa/versiones')).toEqual({
      copiarDe: 2,
      codigo: 'SENESCYT-2025-02',
      origen: 'Reforma 2025',
      vigenteDesde: '2025-02-01',
    });
    expect(
      await screen.findByRole('heading', { level: 2, name: /^Versión 3/ }),
    ).toBeInTheDocument();
  });

  it('edita en pantalla las horas y los parámetros y envía la versión completa', async () => {
    sesionCon(simulada, ESCRITURA);
    simulada.responder('PUT', '/api/normativa/versiones/2', json(200, VERSION_2));
    const usuario = userEvent.setup();
    montarSpa('/normativa');

    await usuario.click(await screen.findByRole('button', { name: 'Editar' }));
    const clase = screen.getByRole('spinbutton', {
      name: 'Clase · Docente a tiempo completo · Todos',
    });
    await usuario.clear(clase);
    await usuario.type(clase, '20');
    const maximo = screen.getByRole('spinbutton', { name: 'RN-02 · Máximo por docente (h)' });
    await usuario.clear(maximo);
    await usuario.type(maximo, '38.5');
    await usuario.click(screen.getByRole('button', { name: 'Guardar' }));

    await waitFor(() =>
      expect(screen.queryByRole('button', { name: 'Guardar' })).not.toBeInTheDocument(),
    );
    const cuerpo = (await cuerpoDe(simulada, 'PUT', '/api/normativa/versiones/2')) as {
      codigo: string;
      reglas: { identificador: string; parametros: { nombre: string; valor: number }[] }[];
      tablaCargaHoraria: Record<string, unknown>[];
    };
    expect(cuerpo.codigo).toBe('SENESCYT-2025-01');
    expect(cuerpo.tablaCargaHoraria).toEqual([
      {
        aplicaA: 'TODOS',
        perfil: 'DOCENTE_TC',
        dedicacionTotal: 40,
        horasClase: 20,
        horasClaseMinimo: 18,
        horasClaseMaximo: 24,
        horasOtrasDocencia: 18,
        horasGestion: 0,
      },
      {
        aplicaA: 'TODOS',
        perfil: 'DOCENTE_INVESTIGACION_VINCULACION',
        dedicacionTotal: 40,
        horasClase: 16,
        horasClaseMinimo: 0,
        horasClaseMaximo: 16,
        horasInvestigacionVinculacionMaximo: 24,
      },
      {
        aplicaA: 'T2',
        perfil: 'GESTOR',
        dedicacionTotal: 40,
        horasClase: 14,
        horasClaseMinimo: 14,
        horasClaseMaximo: 14,
        horasOtrasDocencia: 16,
        horasGestion: 10,
      },
    ]);
    // F04-H03: las reglas del motor envían su severidad; las de solo parámetros, no.
    expect(cuerpo.reglas.find((regla) => regla.identificador === 'RN-02')).toEqual({
      identificador: 'RN-02',
      severidad: 'DURA',
      parametros: [{ nombre: 'horas_maximas_docente', valor: 38.5 }],
    });
  });

  it('cambia la severidad de una regla del motor; las de solo parámetros no tienen selector', async () => {
    sesionCon(simulada, ESCRITURA);
    simulada.responder('PUT', '/api/normativa/versiones/2', json(200, VERSION_2));
    const usuario = userEvent.setup();
    montarSpa('/normativa');

    await usuario.click(await screen.findByRole('button', { name: 'Editar' }));
    await usuario.selectOptions(
      screen.getByRole('combobox', { name: 'RN-05 · Severidad' }),
      'Dura',
    );
    expect(screen.queryByRole('combobox', { name: 'RN-01 · Severidad' })).not.toBeInTheDocument();
    expect(screen.queryByRole('combobox', { name: 'RN-24 · Severidad' })).not.toBeInTheDocument();
    await usuario.click(screen.getByRole('button', { name: 'Guardar' }));

    await waitFor(() =>
      expect(screen.queryByRole('button', { name: 'Guardar' })).not.toBeInTheDocument(),
    );
    const cuerpo = (await cuerpoDe(simulada, 'PUT', '/api/normativa/versiones/2')) as {
      reglas: { identificador: string; severidad?: string; parametros: unknown[] }[];
    };
    expect(cuerpo.reglas.map((regla) => [regla.identificador, regla.severidad ?? null])).toEqual([
      ['RN-01', null],
      ['RN-02', 'DURA'],
      ['RN-05', 'DURA'],
      ['RN-19', 'BLANDA'],
      ['RN-24', null],
      ['RF-24', 'DURA'],
    ]);
    expect(cuerpo.reglas.find((regla) => regla.identificador === 'RF-24')?.parametros).toEqual([]);
  });

  it('un 400 de una severidad o de un peso se muestra junto a su campo', async () => {
    sesionCon(simulada, ESCRITURA);
    simulada.responder(
      'PUT',
      '/api/normativa/versiones/2',
      json(400, {
        titulo: 'Solicitud inválida',
        descripcion: 'Revisa los campos marcados y vuelve a intentarlo.',
        codigo: 'COMUN_SOLICITUD_INVALIDA',
        detalles: [
          {
            campo: 'reglas[RN-24].cuarto_nivel_campo_amplio',
            descripcion:
              'El peso del campo amplio (4) no puede superar al del campo específico (3)',
          },
          {
            campo: 'reglas[RN-05].severidad',
            descripcion: 'Esta regla solo da parámetros: no tiene severidad',
          },
        ],
      }),
    );
    const usuario = userEvent.setup();
    montarSpa('/normativa');

    await usuario.click(await screen.findByRole('button', { name: 'Editar' }));
    await usuario.click(screen.getByRole('button', { name: 'Guardar' }));

    expect(
      await screen.findByRole('spinbutton', {
        name: 'RN-24 · Título de cuarto nivel, campo amplio',
        description: 'El peso del campo amplio (4) no puede superar al del campo específico (3)',
      }),
    ).toBeInTheDocument();
    expect(
      screen.getByRole('combobox', {
        name: 'RN-05 · Severidad',
        description: 'Esta regla solo da parámetros: no tiene severidad',
      }),
    ).toBeInTheDocument();
    expect(document.body).not.toHaveTextContent('reglas[');
  });

  it('un 400 marca la fila con su motivo, sin códigos, y un 409 se muestra con su título', async () => {
    sesionCon(simulada, ESCRITURA);
    simulada.responder(
      'PUT',
      '/api/normativa/versiones/2',
      json(400, {
        titulo: 'Solicitud inválida',
        descripcion: 'Revisa los campos marcados y vuelve a intentarlo.',
        codigo: 'COMUN_SOLICITUD_INVALIDA',
        detalles: [
          {
            campo: 'tablaCargaHoraria[DOCENTE_TC/TODOS]',
            descripcion: 'Las horas de clase (25) no están entre el mínimo y el máximo (18 a 24)',
          },
        ],
      }),
    );
    const usuario = userEvent.setup();
    montarSpa('/normativa');

    await usuario.click(await screen.findByRole('button', { name: 'Editar' }));
    await usuario.click(screen.getByRole('button', { name: 'Guardar' }));

    const tabla = screen.getByRole('table', { name: 'Tabla de carga horaria' });
    const docente = within(tabla).getAllByRole('row')[1];
    expect(docente).toBeDefined();
    expect(
      await within(docente as HTMLElement).findByText(
        'Las horas de clase (25) no están entre el mínimo y el máximo (18 a 24)',
      ),
    ).toBeInTheDocument();
    expect(screen.getByRole('alert')).toHaveTextContent('Solicitud inválida');
    expect(document.body).not.toHaveTextContent('COMUN_');
    expect(document.body).not.toHaveTextContent('tablaCargaHoraria');

    simulada.responder(
      'PUT',
      '/api/normativa/versiones/2',
      json(409, {
        titulo: 'Versión en uso',
        descripcion:
          'La versión rige al menos un periodo y ya no se puede cambiar. Crea una versión nueva a partir de ella.',
        codigo: 'NORMATIVA_VERSION_EN_USO',
      }),
    );
    await usuario.click(screen.getByRole('button', { name: 'Guardar' }));
    expect(await screen.findByRole('alert')).toHaveTextContent('Versión en uso');
    expect(screen.getByRole('alert')).toHaveTextContent('Crea una versión nueva a partir de ella.');
  });

  it('«Cancelar» descarta la edición sin pedir nada', async () => {
    sesionCon(simulada, ESCRITURA);
    const usuario = userEvent.setup();
    montarSpa('/normativa');

    await usuario.click(await screen.findByRole('button', { name: 'Editar' }));
    await usuario.clear(
      screen.getByRole('spinbutton', { name: 'Clase · Docente a tiempo completo · Todos' }),
    );
    await usuario.click(screen.getByRole('button', { name: 'Cancelar' }));

    expect(screen.queryByRole('spinbutton')).not.toBeInTheDocument();
    expect(simulada.peticiones.some((peticion) => peticion.method === 'PUT')).toBe(false);
  });

  it('con escritura, ya no ofrece «Asociar a un periodo»: el periodo se abre en «Periodos» (D-69)', async () => {
    sesionCon(simulada, ESCRITURA);
    montarSpa('/normativa');

    expect(await screen.findByRole('button', { name: 'Crear versión nueva' })).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Asociar a un periodo' })).not.toBeInTheDocument();
  });

  it('una versión que rige un periodo se ve en solo lectura, con «Crear versión nueva»', async () => {
    sesionCon(simulada, ESCRITURA);
    const usuario = userEvent.setup();
    montarSpa('/normativa');

    await usuario.click(await screen.findByRole('button', { name: /^Versión 1/ }));

    expect(
      await screen.findByRole('heading', { level: 2, name: 'Versión 1 · Política SENESCYT 2024' }),
    ).toBeInTheDocument();
    expect(screen.getByRole('list', { name: 'Datos de la versión' })).toHaveTextContent(
      'Rige: 2025-1',
    );
    expect(screen.queryByRole('button', { name: 'Editar' })).not.toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Crear versión nueva' })).toBeInTheDocument();
    expect(screen.getByText(/ya no se puede cambiar/)).toBeInTheDocument();
  });

  it('con solo lectura no aparece ninguna acción', async () => {
    sesionCon(simulada, ['PARAMETROS:LECTURA']);
    montarSpa('/normativa');

    expect(
      await screen.findByRole('heading', { level: 2, name: /^Versión 2/ }),
    ).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Crear versión nueva' })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Editar' })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Asociar a un periodo' })).not.toBeInTheDocument();
    // F04-H03: la afinidad se ve, y no hay selectores de severidad.
    expect(screen.getByRole('list', { name: 'Afinidad' })).toBeInTheDocument();
    expect(screen.queryAllByRole('combobox')).toEqual([]);
  });
});

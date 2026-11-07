// La pantalla «Bitácora» (F17-H02, RF-44, contracts/spa.md): filtrar por fecha, usuario, tipo de
// cambio, identificador y periodo; ver cada entrada con sus valores; y solo con el permiso.
import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { instalarApiSimulada, json, type ApiSimulada } from '../../pruebas/apiSimulada';
import { montarSpa } from '../../pruebas/montarSpa';
import { cerrarSesion, iniciarSesion } from '../../sesion/almacen';
import { ACCIONES, CORTE, CREADA, DE_4_A_6, QUITADA, pagina } from './datosDePrueba';

const RUTA = '/api/usuarios/bitacora';
const VICERRECTOR = ['DISTRIBUTIVO:LECTURA', 'REPORTES:LECTURA', 'BITACORA:LECTURA'];

function sesionCon(simulada: ApiSimulada, permisos: string[]) {
  simulada.responder(
    'GET',
    '/api/usuarios/sesion',
    json(200, {
      usuario: 'vicerrectorado.demo',
      nombre: 'Vicerrectorado (demo)',
      roles: [],
      permisos,
    }),
  );
}

function consultas(simulada: ApiSimulada) {
  return simulada.peticiones
    .filter((una) => new URL(una.url).pathname === RUTA)
    .map((una) => new URL(una.url).searchParams);
}

describe('Bitácora', () => {
  let simulada: ApiSimulada;

  beforeEach(() => {
    simulada = instalarApiSimulada();
    simulada.responder('GET', RUTA, json(200, pagina([DE_4_A_6, CREADA, QUITADA], 3)));
    simulada.responder('GET', `${RUTA}/acciones`, json(200, ACCIONES));
    iniciarSesion('cabecera.cuerpo.firma', new Date(Date.now() + 60_000));
  });

  afterEach(() => {
    cerrarSesion('cierre');
    vi.restoreAllMocks();
  });

  async function abrir(ruta = '/bitacora', permisos = VICERRECTOR) {
    sesionCon(simulada, permisos);
    const montada = montarSpa(ruta);
    await screen.findByRole('table', { name: 'Entradas de la bitácora' });
    return montada;
  }

  it('muestra las entradas, de la más reciente a la más antigua, con el total', async () => {
    await abrir();

    expect(screen.getByText('3 entradas')).toBeVisible();
    const filas = within(screen.getByRole('table', { name: 'Entradas de la bitácora' }))
      .getAllByRole('row')
      .slice(1)
      .filter((fila) => fila.querySelector('td[data-momento]'));
    expect(filas).toHaveLength(3);
    const primera = filas[0] as HTMLElement;
    expect(within(primera).getByText('7/11/26, 12:02:14')).toBeVisible();
    expect(within(primera).getByText('coordinacion.demo')).toBeVisible();
    expect(within(primera).getByText('Generación')).toBeVisible();
    expect(within(primera).getByText('ACTIVIDAD_GUARDADA')).toBeVisible();
    expect(within(primera).getByText('2026-I')).toBeVisible();
    expect(
      within(primera).getByText('periodo: 2026-I · docente: 0102030405 · actividad: 140-b'),
    ).toBeVisible();
  });

  it('al expandir una entrada muestra el objeto, el valor anterior y el nuevo', async () => {
    await abrir();
    const usuario = userEvent.setup();

    await usuario.click(screen.getAllByRole('button', { name: 'Ver valores' })[0] as HTMLElement);

    const detalle = screen.getByRole('region', { name: 'Valores de la entrada' });
    expect(within(detalle).getByText('actividad: 140-b')).toBeVisible();
    expect(
      within(within(detalle).getByRole('group', { name: 'Valor anterior' })).getByText('horas: 4'),
    ).toBeVisible();
    expect(
      within(within(detalle).getByRole('group', { name: 'Valor nuevo' })).getByText('horas: 6'),
    ).toBeVisible();
  });

  it('dice cuándo falta el valor anterior (se creó) o el nuevo (se quitó)', async () => {
    await abrir();
    const usuario = userEvent.setup();
    const botones = screen.getAllByRole('button', { name: 'Ver valores' });

    await usuario.click(botones[1] as HTMLElement);
    await usuario.click(botones[2] as HTMLElement);

    expect(screen.getByText('Sin valor anterior (se creó)')).toBeVisible();
    expect(screen.getByText('Sin valor nuevo (se quitó)')).toBeVisible();
    expect(screen.getByText('camposPersonales: telefono')).toBeVisible();
  });

  it('filtra por usuario y fecha, y deja los filtros en la dirección (US1-1)', async () => {
    const { enrutador } = await abrir();
    const usuario = userEvent.setup();

    await usuario.type(screen.getByLabelText('Quién hizo el cambio'), 'coordinacion.demo');
    await usuario.type(screen.getByLabelText('Desde'), '2026-11-07');
    await usuario.type(screen.getByLabelText('Hasta'), '2026-11-07');
    await usuario.click(screen.getByRole('button', { name: 'Consultar' }));

    await waitFor(() =>
      expect(consultas(simulada).at(-1)?.toString()).toBe(
        'desde=2026-11-07&hasta=2026-11-07&usuario=coordinacion.demo',
      ),
    );
    expect(enrutador.state.location.search).toBe(
      '?desde=2026-11-07&hasta=2026-11-07&usuario=coordinacion.demo',
    );
  });

  it('dice que el usuario es quien hizo el cambio y qué va en el identificador', async () => {
    await abrir();

    expect(screen.getByLabelText('Quién hizo el cambio')).toHaveAccessibleDescription(
      'El usuario del sistema, por ejemplo coordinacion.demo, o sistema.',
    );
    expect(screen.getByLabelText('Identificador')).toHaveAccessibleDescription(
      'La cédula del docente o el código de la asignatura, actividad, cargo o carrera.',
    );
  });

  it('lee los filtros de la dirección al abrir', async () => {
    await abrir('/bitacora?usuario=sistema&periodo=2026-I');

    expect(screen.getByLabelText('Quién hizo el cambio')).toHaveValue('sistema');
    expect(screen.getByLabelText('Periodo')).toHaveValue('2026-I');
    expect(consultas(simulada).at(-1)?.toString()).toBe('usuario=sistema&periodo=2026-I');
  });

  it('marca la fecha hasta antes de enviar si es anterior a desde, sin consultar', async () => {
    await abrir();
    const usuario = userEvent.setup();
    const antes = consultas(simulada).length;

    await usuario.type(screen.getByLabelText('Desde'), '2026-11-07');
    await usuario.type(screen.getByLabelText('Hasta'), '2026-11-06');
    await usuario.click(screen.getByRole('button', { name: 'Consultar' }));

    expect(
      await screen.findByText('La fecha hasta no puede ser anterior a la fecha desde.'),
    ).toBeVisible();
    expect(consultas(simulada)).toHaveLength(antes);
  });

  it('la acción se elige del servicio y está deshabilitada sin él', async () => {
    await abrir();
    const usuario = userEvent.setup();
    const accion = screen.getByLabelText('Acción');

    expect(accion).toBeDisabled();
    await usuario.selectOptions(screen.getByLabelText('Servicio'), 'generacion');
    expect(accion).toBeEnabled();
    expect(
      within(accion)
        .getAllByRole('option')
        .map((una) => una.textContent),
    ).toEqual(['Todas', 'ACTIVIDAD_GUARDADA', 'ACTIVIDAD_QUITADA']);
  });

  it('el identificador solo admite sus caracteres mientras se escribe', async () => {
    await abrir();

    await userEvent.setup().type(screen.getByLabelText('Identificador'), 'DOC/01 tildé');

    expect(screen.getByLabelText('Identificador')).toHaveValue('DOC/01tild');
  });

  it('al cambiar de página envía el corte de la primera respuesta', async () => {
    simulada.responder('GET', RUTA, json(200, pagina([DE_4_A_6], 120)));
    await abrir();
    const usuario = userEvent.setup();

    await usuario.click(screen.getByRole('button', { name: 'Página siguiente' }));

    await waitFor(() =>
      expect(consultas(simulada).at(-1)?.toString()).toBe(
        `pagina=2&corte=${encodeURIComponent(CORTE)}`,
      ),
    );
  });

  it('una consulta nueva vuelve a la página 1 sin corte', async () => {
    await abrir(`/bitacora?pagina=3&corte=${encodeURIComponent(CORTE)}`);
    const usuario = userEvent.setup();

    await usuario.type(screen.getByLabelText('Periodo'), '2026-I');
    await usuario.click(screen.getByRole('button', { name: 'Consultar' }));

    await waitFor(() => expect(consultas(simulada).at(-1)?.toString()).toBe('periodo=2026-I'));
  });

  it('sin entradas lo dice, sin un error (US1-5)', async () => {
    simulada.responder('GET', RUTA, json(200, pagina([], 0)));
    sesionCon(simulada, VICERRECTOR);
    montarSpa('/bitacora');

    expect(await screen.findByText('No hay entradas con estos filtros.')).toBeVisible();
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
  });

  it('marca en su campo el detalle de un 400 del servidor', async () => {
    simulada.responder(
      'GET',
      RUTA,
      json(400, {
        titulo: 'Solicitud inválida',
        descripcion: 'Revisa los campos marcados y vuelve a intentarlo.',
        codigo: 'COMUN_SOLICITUD_INVALIDA',
        detalles: [{ campo: 'periodo', descripcion: 'Usa de 1 a 20 letras sin tilde' }],
      }),
    );
    sesionCon(simulada, VICERRECTOR);
    montarSpa('/bitacora?periodo=x');

    expect(await screen.findByRole('alert')).toHaveTextContent('Solicitud inválida');
    expect(screen.getByLabelText('Periodo')).toHaveAccessibleDescription(
      'Usa de 1 a 20 letras sin tilde.',
    );
  });

  it('no tiene ningún botón que cambie una entrada (FR-006)', async () => {
    await abrir();

    const nombres = screen.getAllByRole('button').map((uno) => uno.textContent);
    expect(
      nombres.filter((uno) => /borrar|eliminar|editar|guardar|corregir/i.test(uno ?? '')),
    ).toEqual([]);
  });

  it('sin el permiso muestra el aviso de acceso no autorizado sin consultar (US2)', async () => {
    sesionCon(simulada, ['OFERTA:LECTURA', 'DISTRIBUTIVO:LECTURA']);
    montarSpa('/bitacora');

    expect(await screen.findByRole('alert')).toHaveTextContent('Acceso no autorizado');
    expect(consultas(simulada)).toHaveLength(0);
  });
});

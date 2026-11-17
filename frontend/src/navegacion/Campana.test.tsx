import { act, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { instalarApiSimulada, json, type ApiSimulada } from '../pruebas/apiSimulada';
import { montarSpa } from '../pruebas/montarSpa';
import { cerrarSesion, iniciarSesion, suscribirActividad } from '../sesion/almacen';

const SIN_LEER = '/api/notificaciones/bandeja/sin-leer';
const BANDEJA = '/api/notificaciones/bandeja';

const NUEVA = {
  id: '3f2b8c1e-5d4a-4e6f-9a7b-1c2d3e4f5a6b',
  periodo: '2026-I',
  titulo: 'El distributivo llegó a «Revisión del Vicerrectorado»',
  detalle: 'Periodo 2026-I, versión 1.',
  enlace: '/consolidado',
  autor: 'consolidacion.demo',
  autorNombre: 'Responsable de consolidación',
  momento: '2026-11-15T20:04:00Z',
  leida: false,
};
const LEIDA = {
  ...NUEVA,
  id: '9a7b1c2d-3e4f-4a6b-8c1e-5d4a3f2b8c1e',
  titulo: 'La carrera DS envió su parcial',
  momento: '2026-11-14T15:00:00Z',
  leida: true,
  leidaEn: '2026-11-14T16:00:00Z',
};

describe('La campana de notificaciones (F16-H01, US2)', () => {
  let api: ApiSimulada;

  beforeEach(() => {
    api = instalarApiSimulada();
    api.responder(
      'GET',
      '/api/usuarios/sesion',
      json(200, { usuario: 'vicerrectorado.demo', roles: ['VICERRECTOR'], permisos: [] }),
    );
    iniciarSesion('cabecera.cuerpo.firma', new Date(Date.now() + 600_000));
  });

  afterEach(() => {
    cerrarSesion();
    vi.useRealTimers();
  });

  function conSinLeer(numero: number) {
    api.responder('GET', SIN_LEER, () => json(200, { sinLeer: numero }));
  }

  function conBandeja(notificaciones: unknown[], sinLeer: number) {
    api.responder('GET', BANDEJA, () => json(200, { notificaciones, sinLeer }));
  }

  it('muestra cuántas hay sin leer en su nombre accesible, y nada si son cero', async () => {
    conSinLeer(3);
    montarSpa('/');

    expect(await screen.findByRole('button', { name: 'Notificaciones, 3 sin leer' })).toBeVisible();
    expect(screen.getByText('3')).toBeVisible();
  });

  it('sin notificaciones sin leer, el botón se llama «Notificaciones» sin número', async () => {
    conSinLeer(0);
    montarSpa('/');

    await waitFor(() =>
      expect(api.peticiones.some((una) => new URL(una.url).pathname === SIN_LEER)).toBe(true),
    );
    expect(await screen.findByRole('button', { name: 'Notificaciones' })).toBeVisible();
  });

  it('la consulta periódica del número es automática: no cuenta como actividad (ADR-020)', async () => {
    vi.useFakeTimers({ shouldAdvanceTime: true });
    conSinLeer(1);
    montarSpa('/');
    expect(await screen.findByRole('button', { name: 'Notificaciones, 1 sin leer' })).toBeVisible();
    const consultasAntes = api.peticiones.filter((una) => new URL(una.url).pathname === SIN_LEER);
    let actividades = 0;
    const dejar = suscribirActividad(() => {
      actividades += 1;
    });

    await act(() => vi.advanceTimersByTimeAsync(30_000));

    await waitFor(() =>
      expect(
        api.peticiones.filter((una) => new URL(una.url).pathname === SIN_LEER).length,
      ).toBeGreaterThan(consultasAntes.length),
    );
    dejar();
    expect(actividades).toBe(0);
    const ultima = api.peticiones.filter((una) => new URL(una.url).pathname === SIN_LEER).at(-1);
    expect(ultima?.headers.has('X-MDD-Automatica')).toBe(false);
    expect(ultima?.headers.get('Authorization')).toBe('Bearer cabecera.cuerpo.firma');
  });

  it('vuelve a contar cada 30 s, sin recargar', async () => {
    vi.useFakeTimers({ shouldAdvanceTime: true });
    let numero = 1;
    api.responder('GET', SIN_LEER, () => json(200, { sinLeer: numero }));
    montarSpa('/');
    expect(await screen.findByRole('button', { name: 'Notificaciones, 1 sin leer' })).toBeVisible();

    numero = 2;
    await act(() => vi.advanceTimersByTimeAsync(30_000));

    expect(await screen.findByRole('button', { name: 'Notificaciones, 2 sin leer' })).toBeVisible();
  });

  it('abre la lista con la más nueva primero, distingue las no leídas y dice quién y cuándo', async () => {
    conSinLeer(1);
    conBandeja([NUEVA, LEIDA], 1);
    montarSpa('/');

    await userEvent
      .setup()
      .click(await screen.findByRole('button', { name: 'Notificaciones, 1 sin leer' }));

    const lista = await screen.findByRole('list', { name: 'Notificaciones' });
    const elementos = within(lista).getAllByRole('button');
    expect(elementos[0]).toHaveTextContent(/^Sin leer:/);
    expect(elementos[0]).toHaveTextContent('El distributivo llegó a «Revisión del Vicerrectorado»');
    expect(elementos[0]).toHaveTextContent('Periodo 2026-I, versión 1.');
    expect(elementos[0]).toHaveTextContent(
      'Responsable de consolidación · 15/11/26, 15:04 · 2026-I',
    );
    expect(elementos[1]).toHaveTextContent('La carrera DS envió su parcial');
    expect(elementos[1]).not.toHaveTextContent('Sin leer');
  });

  it('abrir una notificación la marca, cierra la lista, lleva a su pantalla y baja el número', async () => {
    let numero = 1;
    api.responder('GET', SIN_LEER, () => json(200, { sinLeer: numero }));
    conBandeja([NUEVA], 1);
    api.responder('POST', `/api/notificaciones/bandeja/${NUEVA.id}/lectura`, () => {
      numero = 0;
      return json(200, { ...NUEVA, leida: true, leidaEn: '2026-11-15T20:05:00Z' });
    });
    const { enrutador } = montarSpa('/');
    const usuario = userEvent.setup();

    await usuario.click(await screen.findByRole('button', { name: 'Notificaciones, 1 sin leer' }));
    await usuario.click(await screen.findByRole('button', { name: /El distributivo llegó/ }));

    await waitFor(() => expect(enrutador.state.location.pathname).toBe('/consolidado'));
    // La pantalla abre el periodo de la notificación, no el de por omisión.
    expect(enrutador.state.location.search).toBe('?periodo=2026-I');
    expect(screen.queryByRole('list', { name: 'Notificaciones' })).not.toBeInTheDocument();
    expect(
      api.peticiones.some(
        (una) => una.method === 'POST' && new URL(una.url).pathname.endsWith(`${NUEVA.id}/lectura`),
      ),
    ).toBe(true);
    expect(await screen.findByRole('button', { name: 'Notificaciones' })).toBeVisible();
  });

  it('si marcar falla, lleva igual a la pantalla del hecho', async () => {
    conSinLeer(1);
    conBandeja([NUEVA], 1);
    api.responder(
      'POST',
      `/api/notificaciones/bandeja/${NUEVA.id}/lectura`,
      json(500, {
        titulo: 'Error inesperado',
        descripcion: 'Error',
        codigo: 'COMUN_ERROR_INTERNO',
      }),
    );
    const { enrutador } = montarSpa('/');
    const usuario = userEvent.setup();

    await usuario.click(await screen.findByRole('button', { name: 'Notificaciones, 1 sin leer' }));
    await usuario.click(await screen.findByRole('button', { name: /El distributivo llegó/ }));

    await waitFor(() => expect(enrutador.state.location.pathname).toBe('/consolidado'));
  });

  it('sin notificaciones, la lista lo dice', async () => {
    conSinLeer(0);
    conBandeja([], 0);
    montarSpa('/');

    await userEvent.setup().click(await screen.findByRole('button', { name: 'Notificaciones' }));

    expect(await screen.findByText('No tienes notificaciones')).toBeVisible();
  });

  it('si la lista no carga, lo dice y deja reintentar', async () => {
    conSinLeer(1);
    let falla = true;
    api.responder('GET', BANDEJA, () =>
      falla
        ? json(503, {
            titulo: 'Servicio no disponible',
            descripcion: 'Intenta de nuevo.',
            codigo: 'COMUN_SERVICIO_NO_DISPONIBLE',
          })
        : json(200, { notificaciones: [NUEVA], sinLeer: 1 }),
    );
    montarSpa('/');
    const usuario = userEvent.setup();

    await usuario.click(await screen.findByRole('button', { name: 'Notificaciones, 1 sin leer' }));
    expect(await screen.findByText('No se pudieron cargar las notificaciones.')).toBeVisible();
    falla = false;
    await usuario.click(screen.getByRole('button', { name: 'Reintentar' }));

    expect(await screen.findByRole('button', { name: /El distributivo llegó/ })).toBeVisible();
  });

  it('Escape cierra la lista y devuelve el foco a la campana', async () => {
    conSinLeer(1);
    conBandeja([NUEVA], 1);
    montarSpa('/');
    const usuario = userEvent.setup();
    const campana = await screen.findByRole('button', { name: 'Notificaciones, 1 sin leer' });

    await usuario.click(campana);
    await screen.findByRole('list', { name: 'Notificaciones' });
    await usuario.keyboard('{Escape}');

    await waitFor(() =>
      expect(screen.queryByRole('list', { name: 'Notificaciones' })).not.toBeInTheDocument(),
    );
    expect(campana).toHaveFocus();
  });
});

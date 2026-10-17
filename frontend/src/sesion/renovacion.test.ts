import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { api, llamar } from '../api/cliente';
import { instalarApiSimulada, json, type ApiSimulada } from '../pruebas/apiSimulada';
import {
  cerrarSesion,
  estadoDeSesion,
  iniciarSesion,
  marcarActividad,
  obtenerToken,
} from './almacen';
import { activarRenovacion } from './renovacion';

const MINUTO = 60_000;

/** Deja correr los temporizadores y las promesas pendientes (la renovación es asíncrona). */
async function esperar(milisegundos: number) {
  await vi.advanceTimersByTimeAsync(milisegundos);
}

describe('renovación de la sesión por actividad (ADR-020)', () => {
  let simulada: ApiSimulada;
  let desactivar: () => void;
  let renovaciones: number;

  beforeEach(() => {
    vi.useFakeTimers();
    simulada = instalarApiSimulada();
    renovaciones = 0;
    simulada.responder('POST', '/api/usuarios/sesion/renovacion', () => {
      renovaciones++;
      return json(200, {
        token: `renovado-${renovaciones}`,
        tipo: 'Bearer',
        venceEn: new Date(Date.now() + 30 * MINUTO).toISOString(),
      });
    });
    simulada.responder('GET', '/api/usuarios/sesion', json(200, { usuario: 'x', roles: [] }));
    desactivar = activarRenovacion();
    iniciarSesion('inicial', new Date(Date.now() + 30 * MINUTO));
  });

  afterEach(() => {
    desactivar();
    cerrarSesion();
    vi.useRealTimers();
  });

  it('sin actividad, no renueva y la sesión se cierra a los 30 minutos como terminada', async () => {
    await esperar(30 * MINUTO + 1);

    expect(renovaciones).toBe(0);
    expect(estadoDeSesion()).toEqual({ activa: false, motivo: 'sesion-terminada' });
  });

  it('con actividad, renueva una vez al entrar en los últimos 20 minutos y sigue activa', async () => {
    await esperar(5 * MINUTO);
    marcarActividad();
    await esperar(5 * MINUTO);

    expect(renovaciones).toBe(1);
    expect(obtenerToken()).toBe('renovado-1');

    await esperar(25 * MINUTO);
    expect(estadoDeSesion().activa).toBe(true);
  });

  it('una actividad dentro de los últimos 20 minutos renueva en el momento, una sola vez', async () => {
    await esperar(12 * MINUTO);
    marcarActividad();
    marcarActividad();
    await esperar(0);

    expect(renovaciones).toBe(1);
  });

  it('después de una renovación, sin actividad del usuario, no renueva otra vez y la sesión vence', async () => {
    await esperar(12 * MINUTO);
    marcarActividad();
    await esperar(0);
    expect(renovaciones).toBe(1);

    await esperar(30 * MINUTO + 1);

    expect(renovaciones).toBe(1);
    expect(estadoDeSesion()).toEqual({ activa: false, motivo: 'sesion-terminada' });
  });

  it('la petición de renovación no cuenta como actividad del usuario', async () => {
    await esperar(12 * MINUTO);
    marcarActividad();
    await esperar(0);

    const renovacion = simulada.peticiones.find((p) => p.url.endsWith('/renovacion'));
    expect(renovacion?.headers.get('X-MDD-Automatica')).toBeNull();

    await esperar(11 * MINUTO);
    expect(renovaciones).toBe(1);
  });

  it('una petición del usuario a la API sí cuenta como actividad', async () => {
    await esperar(12 * MINUTO);
    await llamar(api.GET('/api/usuarios/sesion'));
    await esperar(0);

    expect(renovaciones).toBe(1);
  });

  it('si la renovación responde 401, la sesión se cierra como terminada', async () => {
    simulada.responder(
      'POST',
      '/api/usuarios/sesion/renovacion',
      json(401, {
        titulo: 'Sesión no válida',
        descripcion: 'Inicia sesión de nuevo para continuar.',
        codigo: 'SEGURIDAD_NO_AUTENTICADO',
      }),
    );
    await esperar(12 * MINUTO);
    marcarActividad();
    await esperar(0);

    expect(estadoDeSesion()).toEqual({ activa: false, motivo: 'sesion-terminada' });
  });

  it('si la renovación falla por la red, la sesión sigue hasta que vence', async () => {
    simulada.responder('POST', '/api/usuarios/sesion/renovacion', () => {
      throw new TypeError('sin red');
    });
    await esperar(12 * MINUTO);
    marcarActividad();
    await esperar(0);

    expect(estadoDeSesion().activa).toBe(true);
    await esperar(18 * MINUTO + 1);
    expect(estadoDeSesion()).toEqual({ activa: false, motivo: 'sesion-terminada' });
  });

  it('el token renovado tampoco llega al almacenamiento del navegador (D-51)', async () => {
    await esperar(12 * MINUTO);
    marcarActividad();
    await esperar(0);

    expect(obtenerToken()).toBe('renovado-1');
    expect(window.localStorage.length).toBe(0);
    expect(window.sessionStorage.length).toBe(0);
    expect(document.cookie).toBe('');
  });
});

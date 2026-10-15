import { afterEach, beforeEach, describe, expect, expectTypeOf, it } from 'vitest';
import { instalarApiSimulada, json, type ApiSimulada } from '../pruebas/apiSimulada';
import { cerrarSesion, estadoDeSesion, iniciarSesion } from '../sesion/almacen';
import { api, llamar } from './cliente';
import type { paths } from './generado/usuarios';
import { FalloDeApi } from '../errores/traducir';

describe('cliente de la API', () => {
  let simulada: ApiSimulada;

  beforeEach(() => {
    simulada = instalarApiSimulada();
    simulada.responder(
      'GET',
      '/api/usuarios/sesion',
      json(200, { usuario: 'autor.prueba', roles: [] }),
    );
  });

  afterEach(() => {
    cerrarSesion();
  });

  it('con sesión, cada petición lleva el token en Authorization (ADR-014)', async () => {
    iniciarSesion('cabecera.cuerpo.firma', new Date('2099-01-01T00:00:00Z'));

    await llamar(api.GET('/api/usuarios/sesion'));

    expect(simulada.peticiones[0]?.headers.get('Authorization')).toBe(
      'Bearer cabecera.cuerpo.firma',
    );
  });

  it('sin sesión, la petición no lleva Authorization', async () => {
    await llamar(api.GET('/api/usuarios/sesion'));

    expect(simulada.peticiones[0]?.headers.has('Authorization')).toBe(false);
  });

  it('un 401 con sesión la cierra con el motivo de sesión terminada (D-50)', async () => {
    iniciarSesion('vencido', new Date('2099-01-01T00:00:00Z'));
    simulada.responder(
      'GET',
      '/api/usuarios/sesion',
      json(401, { titulo: 't', descripcion: 'd', codigo: 'SEGURIDAD_NO_AUTENTICADO' }),
    );

    await expect(llamar(api.GET('/api/usuarios/sesion'))).rejects.toBeInstanceOf(FalloDeApi);

    expect(estadoDeSesion()).toEqual({ activa: false, motivo: 'sesion-terminada' });
  });

  it('una respuesta de error rechaza con su estado y su cuerpo', async () => {
    simulada.responder(
      'GET',
      '/api/usuarios/sesion',
      json(503, { titulo: 'x', descripcion: 'y', codigo: 'COMUN_SERVICIO_NO_DISPONIBLE' }),
    );

    const fallo = await llamar(api.GET('/api/usuarios/sesion')).catch((e: unknown) => e);

    expect(fallo).toBeInstanceOf(FalloDeApi);
    expect((fallo as FalloDeApi).estado).toBe(503);
    expect((fallo as FalloDeApi).cuerpo).toMatchObject({ titulo: 'x' });
  });

  it('las rutas y los cuerpos salen de los tipos generados del contrato', () => {
    type Respuesta =
      paths['/api/usuarios/sesion']['post']['responses']['200']['content']['application/json'];
    expectTypeOf<Respuesta>().toHaveProperty('token').toEqualTypeOf<string>();
    expectTypeOf<Respuesta>().toHaveProperty('venceEn').toEqualTypeOf<string>();
  });
});

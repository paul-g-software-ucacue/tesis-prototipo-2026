import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { instalarApiSimulada, json } from '../pruebas/apiSimulada';
import { montarSpa } from '../pruebas/montarSpa';
import { cerrarSesion, iniciarSesion, obtenerToken } from '../sesion/almacen';

describe('Marco', () => {
  beforeEach(() => {
    instalarApiSimulada().responder(
      'GET',
      '/api/usuarios/sesion',
      json(200, { usuario: 'autor.prueba', roles: ['DOCENTE'] }),
    );
    iniciarSesion('cabecera.cuerpo.firma', new Date(Date.now() + 60_000));
  });

  afterEach(() => {
    cerrarSesion();
  });

  it('tiene el encabezado con el usuario, el menú con Inicio y la región principal', async () => {
    montarSpa('/');

    expect(screen.getByRole('banner')).toHaveTextContent('MDD · Distributivo docente');
    expect(
      await screen.findByText('autor.prueba', { selector: '[data-usuario]' }),
    ).toBeInTheDocument();
    expect(screen.getByRole('navigation', { name: 'Menú principal' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Inicio' })).toHaveAttribute('aria-current', 'page');
    expect(screen.getByRole('main')).toBeInTheDocument();
  });

  it('Cerrar sesión borra el token y vuelve al inicio de sesión', async () => {
    const { enrutador } = montarSpa('/');

    await userEvent.setup().click(screen.getByRole('button', { name: 'Cerrar sesión' }));

    await waitFor(() => expect(obtenerToken()).toBeNull());
    expect(enrutador.state.location.pathname).toBe('/iniciar-sesion');
    expect(enrutador.state.location.search).toBe('');
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
  });

  describe('la opción «Bitácora» (F17-H02, FR-009)', () => {
    // Los permisos de lectura de cada rol de la matriz D-16 (V2__matriz_d16.sql).
    const CON_BITACORA = {
      ADMINISTRADOR: ['USUARIOS_ROLES:LECTURA', 'PARAMETROS:LECTURA', 'BITACORA:LECTURA'],
      VICERRECTOR: ['DISTRIBUTIVO:LECTURA', 'REPORTES:LECTURA', 'BITACORA:LECTURA'],
    };
    const SIN_BITACORA = {
      OPERADOR_DATOS: ['CARGAS:LECTURA', 'DOCENTES:LECTURA'],
      COORDINADOR_CARRERA: ['OFERTA:LECTURA', 'DISTRIBUTIVO:LECTURA', 'DOCENTES:LECTURA'],
      GESTOR_UNIDAD: ['PROYECTOS:LECTURA'],
      RESPONSABLE_CONSOLIDACION: ['DISTRIBUTIVO:LECTURA', 'CONSOLIDACION:LECTURA'],
      RECTOR: ['DISTRIBUTIVO:LECTURA', 'REPORTES:LECTURA'],
      MIEMBRO_OCS: ['DISTRIBUTIVO:LECTURA', 'REPORTES:LECTURA'],
      SECRETARIA_GENERAL: ['DISTRIBUTIVO:LECTURA', 'RESOLUCION_OCS:LECTURA'],
      DOCENTE: ['MI_DISTRIBUTIVO:LECTURA'],
    };

    function sesionCon(permisos: string[]) {
      instalarApiSimulada().responder(
        'GET',
        '/api/usuarios/sesion',
        json(200, { usuario: 'demo', roles: [], permisos }),
      );
    }

    it.each(Object.entries(CON_BITACORA))('aparece para %s', async (_, permisos) => {
      sesionCon(permisos);
      montarSpa('/');

      expect(await screen.findByRole('link', { name: 'Bitácora' })).toHaveAttribute(
        'href',
        '/bitacora',
      );
    });

    it.each(Object.entries(SIN_BITACORA))('no aparece para %s', async (_, permisos) => {
      sesionCon(permisos);
      montarSpa('/');

      await screen.findByText('demo', { selector: '[data-usuario]' });
      expect(screen.queryByRole('link', { name: 'Bitácora' })).not.toBeInTheDocument();
    });
  });
});

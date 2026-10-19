import { screen, within } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { instalarApiSimulada, json, type ApiSimulada } from '../../pruebas/apiSimulada';
import { montarSpa } from '../../pruebas/montarSpa';
import { cerrarSesion, iniciarSesion } from '../../sesion/almacen';
import { nombreDelPerfil } from './perfiles';
import { nombreDeLaRegla, nombreDelParametro } from './reglas';
import { VERSION_1, VERSIONES } from './datosDePrueba';

// La fila i de una tabla, con su tipo: falla si no existe.
function fila(filas: HTMLElement[], i: number): HTMLElement {
  const encontrada = filas[i];
  if (!encontrada) {
    throw new Error(`La tabla no tiene la fila ${i}`);
  }
  return encontrada;
}

describe('Normativa', () => {
  let simulada: ApiSimulada;

  beforeEach(() => {
    simulada = instalarApiSimulada();
    simulada.responder('GET', '/api/normativa/versiones', json(200, VERSIONES));
    simulada.responder('GET', '/api/normativa/versiones/1', json(200, VERSION_1));
    iniciarSesion('cabecera.cuerpo.firma', new Date(Date.now() + 60_000));
  });

  afterEach(() => {
    cerrarSesion();
  });

  describe('con el permiso de lectura de «Parámetros y normativa»', () => {
    beforeEach(() => {
      simulada.responder(
        'GET',
        '/api/usuarios/sesion',
        json(200, {
          usuario: 'vicerrectorado.demo',
          nombre: 'Vicerrectorado académico (demo)',
          roles: ['VICERRECTOR'],
          permisos: ['DISTRIBUTIVO:LECTURA', 'PARAMETROS:LECTURA'],
        }),
      );
    });

    it('muestra la versión 1 con su origen, su código, su vigencia y el tipo de IST', async () => {
      montarSpa('/normativa');

      expect(
        await screen.findByRole('heading', {
          level: 2,
          name: 'Versión 1 · Política SENESCYT 2024',
        }),
      ).toBeInTheDocument();
      expect(screen.getByRole('list', { name: 'Versiones' })).toHaveTextContent('Versión 1');
      const cabecera = screen.getByRole('list', { name: 'Datos de la versión' });
      expect(cabecera).toHaveTextContent('SENESCYT-2024-03');
      expect(cabecera).toHaveTextContent('11/03/2024');
      expect(cabecera).toHaveTextContent('Tipo 2');
      expect(document.title).toBe('Normativa · MDD');
      expect(screen.getByRole('link', { name: 'Normativa' })).toHaveAttribute(
        'aria-current',
        'page',
      );
    });

    it('agrupa los parámetros por regla, con la severidad en español', async () => {
      montarSpa('/normativa');

      const reglas = await screen.findByRole('table', { name: 'Reglas y parámetros' });
      const filas = within(reglas).getAllByRole('row');
      expect(filas[1]).toHaveTextContent('RN-01');
      expect(filas[1]).toHaveTextContent('Dedicaciones');
      expect(filas[1]).toHaveTextContent('—');
      expect(filas[1]).toHaveTextContent('Tiempo completo (h): 40');
      expect(filas[1]).toHaveTextContent('Medio tiempo (h): 20');
      expect(filas[2]).toHaveTextContent('Dura');
      expect(filas[3]).toHaveTextContent('Blanda');
      expect(filas[3]).toHaveTextContent('Clase (%): 60');
    });

    it('muestra la tabla de carga horaria con los perfiles en español y «—» sin valor', async () => {
      montarSpa('/normativa');

      const tabla = await screen.findByRole('table', { name: 'Tabla de carga horaria' });
      const filas = within(tabla).getAllByRole('row');
      expect(filas).toHaveLength(4);
      const docente = within(fila(filas, 1)).getAllByRole('cell');
      expect(filas[1]).toHaveTextContent('Docente a tiempo completo');
      expect(docente.map((celda) => celda.textContent)).toEqual([
        'Docente a tiempo completo',
        'Todos',
        '40',
        '22',
        '18–24',
        '18',
        '0',
        '—',
        'Dura',
        'POL 6; RCE arts. 146 y 149',
        'Por defecto 22 h clase + 18 otras de docencia (C-01)',
      ]);
      expect(filas[2]).toHaveTextContent('Docente de investigación o vinculación');
      expect(within(fila(filas, 2)).getAllByRole('cell')[5]).toHaveTextContent('—');
      expect(within(fila(filas, 3)).getAllByRole('cell')[1]).toHaveTextContent('Tipo 2');
      expect(document.body).not.toHaveTextContent('DOCENTE_TC');
    });

    it('un perfil, una regla o un parámetro que la SPA no conoce se muestra legible', () => {
      expect(nombreDelPerfil('DOCENTE_TC')).toBe('Docente a tiempo completo');
      expect(nombreDelPerfil('PERFIL_NUEVO')).toBe('Perfil nuevo');
      expect(nombreDeLaRegla('RN-10')).toBe('Docentes con cargo de gestión');
      expect(nombreDeLaRegla('RN-26')).toBe('RN-26');
      expect(nombreDelParametro('t2_hasta')).toBe('Tipo 2, hasta (estudiantes)');
      expect(nombreDelParametro('t5_minimo')).toBe('Tipo 5, mínimo (docentes)');
      expect(nombreDelParametro('parametro_nuevo')).toBe('Parametro nuevo');
    });

    it('con un error de la API, muestra el aviso sin el código', async () => {
      simulada.responder(
        'GET',
        '/api/normativa/versiones',
        json(500, {
          titulo: 'Error inesperado',
          descripcion: 'Ocurrió un error inesperado.',
          codigo: 'COMUN_ERROR_INTERNO',
          idIncidente: '6f1c2b9e-1d2a-4c3b-8e4f-5a6b7c8d9e0f',
        }),
      );
      montarSpa('/normativa');

      expect(await screen.findByRole('alert')).toHaveTextContent('Error inesperado');
      expect(document.body).not.toHaveTextContent('COMUN_');
    });
  });

  describe('sin el permiso', () => {
    beforeEach(() => {
      simulada.responder(
        'GET',
        '/api/usuarios/sesion',
        json(200, {
          usuario: 'docente.demo',
          roles: ['DOCENTE'],
          permisos: ['MI_DISTRIBUTIVO:LECTURA:PROPIO'],
        }),
      );
    });

    it('/normativa muestra «Acceso no autorizado» sin pedir nada a la API ni ofrecerla', async () => {
      montarSpa('/normativa');

      expect(await screen.findByRole('alert')).toHaveTextContent('Acceso no autorizado');
      expect(screen.queryByRole('link', { name: 'Normativa' })).not.toBeInTheDocument();
      expect(
        simulada.peticiones.some((peticion) =>
          new URL(peticion.url).pathname.startsWith('/api/normativa'),
        ),
      ).toBe(false);
    });
  });
});

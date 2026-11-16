// La pantalla «Resolución del OCS» (F15-H01, D-107): quien tiene el permiso de la resolución elige
// el periodo y ve el estado del flujo, con el formulario o la constancia de la resolución.
import { screen } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { instalarApiSimulada, json, type ApiSimulada } from '../../pruebas/apiSimulada';
import { montarSpa } from '../../pruebas/montarSpa';
import { cerrarSesion, iniciarSesion } from '../../sesion/almacen';

const PERIODOS = [
  {
    codigo: '2026-I',
    nombre: 'Periodo 2026-I',
    inicio: '2026-04-01',
    fin: '2026-08-31',
    estado: 'PLANIFICACION',
  },
];

const EN_OCS = {
  periodo: '2026-I',
  etapa: 'EN_OCS',
  nombreDeEtapa: 'Resolución del OCS',
  version: 1,
  parciales: [],
  acciones: ['REGISTRAR_RESOLUCION'],
  transiciones: [],
};

function sesionCon(simulada: ApiSimulada, permisos: string[]) {
  simulada.responder(
    'GET',
    '/api/usuarios/sesion',
    json(200, { usuario: 'secretaria.demo', nombre: 'demo (demo)', roles: [], permisos }),
  );
}

describe('Resolución del OCS', () => {
  let simulada: ApiSimulada;

  beforeEach(() => {
    simulada = instalarApiSimulada();
    simulada.responder('GET', '/api/academico/periodos', json(200, PERIODOS));
    simulada.responder('GET', '/api/academico/carreras', json(200, []));
    simulada.responder('GET', '/api/validacionocs/periodos/2026-I/flujo', json(200, EN_OCS));
    iniciarSesion('cabecera.cuerpo.firma', new Date(Date.now() + 60_000));
  });

  afterEach(() => {
    cerrarSesion('cierre');
    vi.restoreAllMocks();
  });

  it('la Secretaría la abre desde el menú y ve el formulario del periodo en EN_OCS', async () => {
    sesionCon(simulada, [
      'DISTRIBUTIVO:LECTURA',
      'RESOLUCION_OCS:LECTURA',
      'RESOLUCION_OCS:ESCRITURA',
    ]);
    montarSpa('/resolucion-ocs');

    expect(
      await screen.findByRole('heading', { level: 1, name: 'Resolución del OCS' }),
    ).toBeVisible();
    expect(await screen.findByLabelText('Número de la resolución')).toBeVisible();
    expect(screen.getByRole('link', { name: 'Resolución del OCS' })).toBeVisible();
  });

  it('sin el permiso de la resolución no está en el menú ni se abre', async () => {
    sesionCon(simulada, ['DISTRIBUTIVO:LECTURA']);
    montarSpa('/resolucion-ocs');

    expect(await screen.findByText(/No tienes permiso/)).toBeVisible();
    expect(screen.queryByRole('link', { name: 'Resolución del OCS' })).not.toBeInTheDocument();
  });
});

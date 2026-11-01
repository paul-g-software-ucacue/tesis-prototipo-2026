// El panel de los totales del docente (F07-H01, RF-27, RN-05): muestra lo que calcula el servidor.
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it } from 'vitest';
import { instalarApiSimulada, json, type ApiSimulada } from '../../pruebas/apiSimulada';
import type { CargaHoraria } from './cargaHoraria';
import { PanelDeCarga } from './PanelDeCarga';

const DOCENTE = '0109990010';
const RUTA = `/api/generacion/periodos/2026-2/docentes/${DOCENTE}/carga-horaria`;
const CARGA: CargaHoraria = {
  docente: DOCENTE,
  nombreDocente: 'PERALTA MALDONADO JUAN ANDRES',
  horasContratadas: 20,
  totales: { clase: 8, otrasDocencia: 4, investigacion: 2.5, vinculacion: 0, gestion: 1 },
  asignadas: 15.5,
  disponibles: 4.5,
  exceso: 0,
  proporcion: { clase: 67, otrasDocencia: 33 },
  meta: { clase: 60, otrasDocencia: 40 },
};

function montar(guardada = false) {
  const consultas = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  render(
    <QueryClientProvider client={consultas}>
      <PanelDeCarga periodo="2026-2" docente={DOCENTE} guardada={guardada} />
    </QueryClientProvider>,
  );
}

describe('Panel de los totales del docente', () => {
  let simulada: ApiSimulada;

  beforeEach(() => {
    simulada = instalarApiSimulada();
  });

  it('muestra los cinco componentes y las horas contratadas, asignadas y disponibles', async () => {
    simulada.responder('GET', RUTA, json(200, CARGA));
    montar();

    expect(
      await screen.findByRole('region', { name: 'Totales de PERALTA MALDONADO JUAN ANDRES' }),
    ).toBeVisible();
    expect(screen.getByText('Clase: 8 h')).toBeVisible();
    expect(screen.getByText('Otras actividades de docencia: 4 h')).toBeVisible();
    expect(screen.getByText('Investigación: 2,5 h')).toBeVisible();
    expect(screen.getByText('Vinculación: 0 h')).toBeVisible();
    expect(screen.getByText('Gestión: 1 h')).toBeVisible();
    expect(
      screen.getByText('Contratadas: 20 h · Asignadas: 15,5 h · Disponibles: 4,5 h'),
    ).toBeVisible();
  });

  it('muestra la proporción con la meta, sin aviso de error', async () => {
    simulada.responder('GET', RUTA, json(200, CARGA));
    montar();

    expect(await screen.findByText('Clase 67 % · Otras 33 % (meta 60/40)')).toBeVisible();
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
  });

  it('sin horas de docencia dice que todavía no hay proporción', async () => {
    simulada.responder('GET', RUTA, json(200, { ...CARGA, proporcion: undefined }));
    montar();

    expect(await screen.findByText('Todavía no hay proporción (meta 60/40).')).toBeVisible();
  });

  it('muestra la meta que trae la normativa del periodo', async () => {
    simulada.responder(
      'GET',
      RUTA,
      json(200, { ...CARGA, meta: { clase: 70.5, otrasDocencia: 29.5 } }),
    );
    montar();

    expect(await screen.findByText('Clase 67 % · Otras 33 % (meta 70,5/29,5)')).toBeVisible();
  });

  it('con exceso dice por cuántas horas se pasó', async () => {
    simulada.responder(
      'GET',
      RUTA,
      json(200, { ...CARGA, asignadas: 33, disponibles: 0, exceso: 13 }),
    );
    montar();

    expect(await screen.findByText('Se pasó por 13 h.')).toBeVisible();
    expect(screen.getByText(/Disponibles: 0 h/)).toBeVisible();
  });

  it('sin exceso no dice nada de eso', async () => {
    simulada.responder('GET', RUTA, json(200, CARGA));
    montar();

    await screen.findByText('Clase: 8 h');
    expect(screen.queryByText(/Se pasó/)).not.toBeInTheDocument();
  });

  it('si falla muestra el mensaje y deja volver a intentar', async () => {
    simulada.responder(
      'GET',
      RUTA,
      json(409, {
        titulo: 'Sin distributivo',
        descripcion: 'El periodo todavía no tiene distributivo.',
        codigo: 'GENERACION_SIN_DISTRIBUTIVO',
      }),
    );
    montar();

    expect(await screen.findByRole('alert')).toHaveTextContent('Sin distributivo');
    simulada.responder('GET', RUTA, json(200, CARGA));
    await userEvent.click(screen.getByRole('button', { name: 'Volver a intentar' }));
    expect(await screen.findByText('Clase: 8 h')).toBeVisible();
  });

  it('si la asignación se guardó y los totales fallan, aclara que sí se guardó', async () => {
    simulada.responder(
      'GET',
      RUTA,
      json(504, { titulo: 'Sin respuesta', descripcion: 'No responde.', codigo: 'PRUEBA' }),
    );
    montar(true);

    expect(
      await screen.findByText('La asignación se guardó. Solo faltó cargar los totales.'),
    ).toBeVisible();
  });
});

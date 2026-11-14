// El documento firmable en la tarjeta del flujo (F13-H02, contracts/spa.md): generado con su
// huella, generándose, o fallido con el motivo y «Generar el PDF» para el rol de la etapa.
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it } from 'vitest';
import { instalarApiSimulada, json, type ApiSimulada } from '../../pruebas/apiSimulada';
import { DocumentoFirmable } from './DocumentoFirmable';
import type { EstadoDelDocumento } from './documento';

const RUTA = '/api/firma/periodos/2026-I/documento';
const SHA = '0123456789abcdef'.repeat(4);

const generado: EstadoDelDocumento = {
  periodo: '2026-I',
  version: 1,
  estado: 'GENERADO',
  documento: {
    sha256: SHA,
    hashContenido: 'e'.repeat(64),
    generadoPor: 'sistema',
    generadoEn: '2026-11-11T15:30:05Z',
  },
  acciones: [],
};
const fallido: EstadoDelDocumento = {
  periodo: '2026-I',
  version: 1,
  estado: 'FALLIDA',
  motivo: 'Falta el nombre del Rector.',
  acciones: ['GENERAR'],
};
const enCurso: EstadoDelDocumento = {
  periodo: '2026-I',
  version: 1,
  estado: 'EN_CURSO',
  acciones: [],
};

function montar() {
  const consultas = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  render(
    <QueryClientProvider client={consultas}>
      <DocumentoFirmable periodo="2026-I" intervalo={20} />
    </QueryClientProvider>,
  );
}

function posts(simulada: ApiSimulada) {
  return simulada.peticiones.filter((una) => una.method === 'POST');
}

describe('DocumentoFirmable', () => {
  let simulada: ApiSimulada;

  beforeEach(() => {
    simulada = instalarApiSimulada();
  });

  it('generado: dice cuándo y la huella del PDF, que se copia completa', async () => {
    simulada.responder('GET', RUTA, json(200, generado));
    montar();
    const usuario = userEvent.setup();

    expect(await screen.findByText('Documento firmable generado el 11/11/26, 10:30')).toBeVisible();
    expect(screen.getByText('Huella del PDF: 0123456789ab…')).toBeVisible();
    await usuario.click(screen.getByRole('button', { name: 'Copiar la huella' }));
    expect(await navigator.clipboard.readText()).toBe(SHA);
    expect(screen.queryByRole('button', { name: 'Generar el PDF' })).not.toBeInTheDocument();
  });

  it('en curso: lo dice y vuelve a consultar hasta que está generado', async () => {
    let consultas = 0;
    simulada.responder('GET', RUTA, () => {
      consultas++;
      return json(200, consultas < 3 ? enCurso : generado);
    });
    montar();

    expect(await screen.findByText('Generando el documento firmable…')).toBeVisible();
    expect(await screen.findByText('Documento firmable generado el 11/11/26, 10:30')).toBeVisible();
    expect(consultas).toBeGreaterThanOrEqual(3);
  });

  it('fallido: el aviso con el motivo y «Generar el PDF», que llama una vez', async () => {
    let generadoYa = false;
    simulada.responder('GET', RUTA, () => json(200, generadoYa ? generado : fallido));
    simulada.responder('POST', RUTA, () => {
      generadoYa = true;
      return json(200, generado);
    });
    montar();
    const usuario = userEvent.setup();

    const aviso = await screen.findByRole('alert');
    expect(aviso).toHaveTextContent('El PDF no se pudo generar');
    expect(aviso).toHaveTextContent('Falta el nombre del Rector.');
    await usuario.click(screen.getByRole('button', { name: 'Generar el PDF' }));

    expect(await screen.findByText('Documento firmable generado el 11/11/26, 10:30')).toBeVisible();
    expect(posts(simulada)).toHaveLength(1);
  });

  it('un 409 al generar muestra el mensaje del catálogo y su detalle', async () => {
    simulada.responder('GET', RUTA, json(200, fallido));
    simulada.responder(
      'POST',
      RUTA,
      json(409, {
        titulo: 'El PDF no se pudo generar',
        descripcion:
          'El documento firmable no se pudo generar. Corrige lo que dice el detalle y vuelve a intentarlo.',
        codigo: 'FIRMA_GENERACION_FALLIDA',
        detalles: [{ campo: 'motivo', descripcion: 'Falta el nombre del Vicerrector académico.' }],
      }),
    );
    montar();
    const usuario = userEvent.setup();

    await usuario.click(await screen.findByRole('button', { name: 'Generar el PDF' }));

    await waitFor(() =>
      expect(screen.getByText('Falta el nombre del Vicerrector académico.')).toBeVisible(),
    );
    expect(screen.getByRole('button', { name: 'Generar el PDF' })).toBeEnabled();
  });

  it('sin la acción no hay botón, aunque haya fallado', async () => {
    simulada.responder('GET', RUTA, json(200, { ...fallido, acciones: [] }));
    montar();

    const aviso = await screen.findByRole('alert');
    expect(within(aviso).getByText(/Falta el nombre del Rector/)).toBeVisible();
    expect(screen.queryByRole('button', { name: 'Generar el PDF' })).not.toBeInTheDocument();
  });
});

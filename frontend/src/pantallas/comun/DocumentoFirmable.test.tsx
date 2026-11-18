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
  cadena: [],
};
const fallido: EstadoDelDocumento = {
  periodo: '2026-I',
  version: 1,
  estado: 'FALLIDA',
  motivo: 'Falta el nombre del Rector.',
  acciones: ['GENERAR'],
  cadena: [],
};
const enCurso: EstadoDelDocumento = {
  periodo: '2026-I',
  version: 1,
  estado: 'EN_CURSO',
  acciones: [],
  cadena: [],
};

// F16-H02: F1 firmada, F2 en turno y F3 pendiente, con F3 sin nombre impreso.
const conCadena: EstadoDelDocumento = {
  ...generado,
  cadena: [
    {
      orden: 1,
      rotulo: 'Elaborado por',
      firmante: 'PÉREZ ANA',
      estado: 'FIRMADO',
      firmadoEn: '2026-11-18T15:04:05Z',
    },
    { orden: 2, rotulo: 'Revisado por', firmante: 'LÓPEZ LUIS', estado: 'EN_TURNO' },
    { orden: 3, rotulo: 'Aprobado por', estado: 'PENDIENTE' },
  ],
};

function montar(
  version?: number,
  consultas = new QueryClient({ defaultOptions: { queries: { retry: false } } }),
) {
  return render(
    <QueryClientProvider client={consultas}>
      <DocumentoFirmable periodo="2026-I" intervalo={20} version={version} />
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

  // F12-H04: al aprobar la versión 2, Firma todavía responde con el documento de la 1 hasta que
  // empieza a generar el de la 2; mientras tanto, la tarjeta espera y vuelve a consultar.
  it('con el documento de otra versión, dice que se está generando y espera el de la congelada', async () => {
    const deLaDos: EstadoDelDocumento = {
      ...generado,
      version: 2,
      documento: {
        sha256: 'f'.repeat(64),
        hashContenido: 'd'.repeat(64),
        generadoPor: 'sistema',
        generadoEn: '2026-11-15T21:27:29Z',
      },
    };
    let consultas = 0;
    simulada.responder('GET', RUTA, () => {
      consultas++;
      return json(200, consultas < 3 ? generado : deLaDos);
    });
    montar(2);

    expect(await screen.findByText('Generando el documento firmable…')).toBeVisible();
    expect(screen.queryByText(/11\/11\/26/)).not.toBeInTheDocument();
    expect(await screen.findByText('Documento firmable generado el 15/11/26, 16:27')).toBeVisible();
    expect(screen.getByText('Huella del PDF: ffffffffffff…')).toBeVisible();
    expect(consultas).toBeGreaterThanOrEqual(3);
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

  describe('la cadena de firmas (F16-H02)', () => {
    function pasos() {
      const lista = screen.getByRole('list', { name: 'Cadena de firmas' });
      return within(lista)
        .getAllByRole('listitem')
        .map((paso) => paso.textContent);
    }

    it('cada paso con su rótulo, su firmante y su estado en texto', async () => {
      simulada.responder('GET', RUTA, json(200, conCadena));
      montar();

      await screen.findByRole('list', { name: 'Cadena de firmas' });
      expect(pasos()).toEqual([
        'F1 · Elaborado por · PÉREZ ANAFirmado el 18/11/26, 10:04',
        'F2 · Revisado por · LÓPEZ LUISEn turno',
        'F3 · Aprobado porPendiente',
      ]);
    });

    it('el paso en turno se resalta, y su estado también se dice con texto', async () => {
      simulada.responder('GET', RUTA, json(200, conCadena));
      montar();

      const lista = await screen.findByRole('list', { name: 'Cadena de firmas' });
      const enTurno = within(lista).getByText('F2 · Revisado por · LÓPEZ LUIS');
      expect(enTurno).toHaveStyle({ fontWeight: 700 });
      expect(within(lista).getByText('En turno')).toBeVisible();
    });

    it('un paso rechazado lo dice', async () => {
      simulada.responder(
        'GET',
        RUTA,
        json(200, {
          ...conCadena,
          cadena: [
            conCadena.cadena[0],
            { orden: 2, rotulo: 'Revisado por', firmante: 'LÓPEZ LUIS', estado: 'RECHAZADO' },
            conCadena.cadena[2],
          ],
        }),
      );
      montar();

      await screen.findByRole('list', { name: 'Cadena de firmas' });
      expect(pasos()[1]).toBe('F2 · Revisado por · LÓPEZ LUISRechazado');
    });

    it('sin cadena no hay lista', async () => {
      simulada.responder('GET', RUTA, json(200, generado));
      montar();

      await screen.findByText(/Documento firmable generado/);
      expect(screen.queryByRole('list', { name: 'Cadena de firmas' })).not.toBeInTheDocument();
    });

    it('con el documento de otra versión no hay lista', async () => {
      simulada.responder('GET', RUTA, json(200, conCadena));
      montar(2);

      await screen.findByText('Generando el documento firmable…');
      expect(screen.queryByRole('list', { name: 'Cadena de firmas' })).not.toBeInTheDocument();
    });

    // FR-014: al volver a montar la tarjeta, la consulta se repite y muestra la firma nueva.
    it('al volver a abrir la pantalla muestra la última firma aceptada', async () => {
      let consultas = 0;
      simulada.responder('GET', RUTA, () => {
        consultas++;
        return json(
          200,
          consultas === 1
            ? conCadena
            : {
                ...conCadena,
                cadena: [
                  conCadena.cadena[0],
                  { ...conCadena.cadena[1], estado: 'FIRMADO', firmadoEn: '2026-11-18T16:00:00Z' },
                  { ...conCadena.cadena[2], estado: 'EN_TURNO' },
                ],
              },
        );
      });
      const cache = new QueryClient({ defaultOptions: { queries: { retry: false } } });
      const primera = montar(undefined, cache);
      await screen.findByRole('list', { name: 'Cadena de firmas' });
      primera.unmount();

      montar(undefined, cache);

      await waitFor(() => expect(pasos()[2]).toBe('F3 · Aprobado porEn turno'));
    });
  });
});

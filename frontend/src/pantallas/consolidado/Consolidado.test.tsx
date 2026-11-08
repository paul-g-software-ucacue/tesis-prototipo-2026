// La pantalla «Consolidado del periodo» (F10-H02, RF-32): las carreras con su estado, los totales,
// el aviso de desactualizado y el informe. Consolidación escribe y el Vicerrectorado lee.
import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { instalarApiSimulada, json, type ApiSimulada } from '../../pruebas/apiSimulada';
import { montarSpa } from '../../pruebas/montarSpa';
import { cerrarSesion, iniciarSesion } from '../../sesion/almacen';
import { CARRERAS } from '../carreras/datosDePrueba';
import { PERIODOS } from '../periodos/datosDePrueba';
import {
  CON_INFORME,
  CONSOLIDADO,
  INFORME,
  SIN_CONSOLIDAR,
  SIN_DISTRIBUTIVO,
} from './datosDePrueba';

const CONSOLIDACION = [
  'CONSOLIDACION:LECTURA',
  'CONSOLIDACION:ESCRITURA',
  'DISTRIBUTIVO:LECTURA',
  'OFERTA:LECTURA',
];
const VICERRECTOR = ['DISTRIBUTIVO:LECTURA', 'DISTRIBUTIVO:ESCRITURA'];
const COORDINADOR = [
  'OFERTA:LECTURA:PROPIO',
  'OFERTA:ESCRITURA:PROPIO',
  'DISTRIBUTIVO:LECTURA:PROPIO',
];
const RUTA = '/api/generacion/periodos/2026-2/consolidado';
const RUTA_INFORME = `${RUTA}/informe`;

function sesionCon(simulada: ApiSimulada, permisos: string[]) {
  simulada.responder(
    'GET',
    '/api/usuarios/sesion',
    json(200, { usuario: 'demo', nombre: 'demo (demo)', roles: [], permisos }),
  );
}

function pdf(nombre = 'informe.pdf', tamano = 100) {
  const archivo = new File(['%PDF-1.7'], nombre, { type: 'application/pdf' });
  Object.defineProperty(archivo, 'size', { value: tamano });
  return archivo;
}

describe('Consolidado del periodo', () => {
  let simulada: ApiSimulada;

  beforeEach(() => {
    simulada = instalarApiSimulada();
    simulada.responder('GET', '/api/academico/periodos', json(200, PERIODOS));
    simulada.responder('GET', '/api/academico/carreras', json(200, CARRERAS));
    simulada.responder('GET', RUTA, json(200, CONSOLIDADO));
    iniciarSesion('cabecera.cuerpo.firma', new Date(Date.now() + 60_000));
  });

  afterEach(() => {
    cerrarSesion('cierre');
    vi.restoreAllMocks();
  });

  async function abrir(permisos = CONSOLIDACION) {
    sesionCon(simulada, permisos);
    montarSpa('/consolidado');
    return screen.findByRole('table', { name: 'Carreras del consolidado' });
  }

  it('muestra cada carrera con su nombre, estado, avance y horas', async () => {
    const tabla = await abrir();

    expect(await within(tabla).findByText('CONTABILIDAD')).toBeVisible();
    expect(within(tabla).getByText('En elaboración')).toBeVisible();
    expect(within(tabla).getByText('9 de 12')).toBeVisible();
  });

  it('muestra los totales institucionales y dice que las unidades no tienen parcial', async () => {
    await abrir();

    const totales = screen.getByRole('table', { name: 'Totales institucionales' });
    expect(within(totales).getByText('Investigación')).toBeVisible();
    expect(within(totales).getByText('Vinculación')).toBeVisible();
    expect(within(totales).getByText('Gestión')).toBeVisible();
    expect(screen.getByText(/unidades todavía no tienen parcial propio/)).toBeVisible();
  });

  it('dice cuántas inconsistencias hay abiertas, con un enlace al panel', async () => {
    await abrir();

    expect(screen.getByText('3 duras y 1 blanda abiertas')).toBeVisible();
    expect(screen.getByRole('link', { name: 'Ir al panel de inconsistencias' })).toHaveAttribute(
      'href',
      '/inconsistencias',
    );
  });

  it('dice quién consolidó y cuándo', async () => {
    await abrir();

    expect(screen.getByText(/Consolidado por consolidacion\.demo/)).toBeVisible();
  });

  it('sin consolidar lo dice y ofrece hacerlo', async () => {
    simulada.responder('GET', RUTA, json(200, SIN_CONSOLIDAR));
    await abrir();

    expect(screen.getByText('Este periodo todavía no se consolida.')).toBeVisible();
    expect(screen.getByRole('button', { name: 'Consolidar' })).toBeVisible();
  });

  it('consolidar guarda y recarga', async () => {
    simulada.responder('GET', RUTA, json(200, SIN_CONSOLIDAR));
    simulada.responder('POST', RUTA, json(200, CONSOLIDADO));
    await abrir();
    simulada.responder('GET', RUTA, json(200, CONSOLIDADO));

    await userEvent.click(screen.getByRole('button', { name: 'Consolidar' }));

    expect(await screen.findByText(/Consolidado por consolidacion\.demo/)).toBeVisible();
  });

  it('ya consolidado ofrece volver a consolidar', async () => {
    await abrir();

    expect(screen.getByRole('button', { name: 'Volver a consolidar' })).toBeVisible();
  });

  it('sin distributivo avisa, sin error ni la opción de consolidar', async () => {
    simulada.responder('GET', RUTA, json(200, SIN_DISTRIBUTIVO));
    sesionCon(simulada, CONSOLIDACION);
    montarSpa('/consolidado');

    expect(await screen.findByText(/todavía no tiene distributivo/)).toBeVisible();
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /onsolidar/ })).not.toBeInTheDocument();
  });

  it('un error no se presenta como un consolidado vacío', async () => {
    simulada.responder(
      'GET',
      RUTA,
      json(504, {
        titulo: 'Servicio sin respuesta',
        descripcion: 'Una parte del sistema no respondió a tiempo.',
        codigo: 'COMUN_PROCESO_SIN_RESPUESTA',
      }),
    );
    sesionCon(simulada, CONSOLIDACION);
    montarSpa('/consolidado');

    expect(await screen.findByRole('alert')).toHaveTextContent('Servicio sin respuesta');
    expect(screen.queryByRole('table')).not.toBeInTheDocument();
  });

  it('un periodo cerrado se lee, sin consolidar ni adjuntar', async () => {
    simulada.responder('GET', RUTA, json(200, { ...CON_INFORME, modificable: false }));
    await abrir();

    expect(screen.queryByRole('button', { name: /onsolidar/ })).not.toBeInTheDocument();
    expect(screen.queryByLabelText(/Adjuntar/)).not.toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Descargar el informe' })).toBeVisible();
  });

  describe('desactualizado', () => {
    it('avisa que hay que repetirlo', async () => {
      simulada.responder('GET', RUTA, json(200, { ...CONSOLIDADO, desactualizado: true }));
      await abrir();

      expect(screen.getByRole('status', { name: 'Consolidado desactualizado' })).toHaveTextContent(
        /vuelve a consolidar/,
      );
    });

    it('sin cambios no avisa', async () => {
      await abrir();

      expect(
        screen.queryByRole('status', { name: 'Consolidado desactualizado' }),
      ).not.toBeInTheDocument();
    });
  });

  describe('enviar a revisión (F10-H03)', () => {
    const FLUJO = '/api/validacionocs/periodos/2026-2/flujo';
    const REVISION = '/api/validacionocs/periodos/2026-2/revision';
    const EN_CONSOLIDACION = {
      periodo: '2026-2',
      etapa: 'EN_CONSOLIDACION',
      nombreDeEtapa: 'Consolidación',
      version: 1,
      parciales: [],
      acciones: ['ENVIAR_A_REVISION'],
      transiciones: [],
    };
    const LISTO = {
      ...CON_INFORME,
      carreras: CON_INFORME.carreras.map((carrera) => ({
        ...carrera,
        estado: 'ENVIADO' as const,
      })),
      abiertas: { duras: 0, blandas: 0 },
    };

    async function faltan() {
      const lista = await screen.findByRole('list', { name: 'Falta para enviar a revisión' });
      return within(lista)
        .getAllByRole('listitem')
        .map((uno) => uno.textContent);
    }

    beforeEach(() => {
      simulada.responder('GET', FLUJO, json(200, EN_CONSOLIDACION));
    });

    it('dice cada condición que falta y deja el botón deshabilitado', async () => {
      await abrir();

      expect(await faltan()).toEqual([
        'Hay parciales sin enviar: su carrera tiene que enviarlos.',
        'Adjunta el informe sustentado.',
        'Quedan 4 inconsistencias abiertas: corrige las duras y justifica las blandas en el panel.',
      ]);
      expect(screen.getByRole('button', { name: 'Enviar a revisión' })).toBeDisabled();
    });

    it('sin consolidar pide consolidar', async () => {
      simulada.responder('GET', RUTA, json(200, { ...LISTO, consolidacion: undefined }));
      await abrir();

      expect(await faltan()).toEqual(['Consolida el periodo.']);
    });

    it('desactualizado pide consolidar otra vez', async () => {
      simulada.responder('GET', RUTA, json(200, { ...LISTO, desactualizado: true }));
      await abrir();

      expect(await faltan()).toEqual(['Consolida otra vez: algo cambió después de consolidar.']);
    });

    it('una sola abierta lo dice en singular', async () => {
      simulada.responder('GET', RUTA, json(200, { ...LISTO, abiertas: { duras: 0, blandas: 1 } }));
      await abrir();

      expect(await faltan()).toEqual([
        'Queda 1 inconsistencia abierta: corrige las duras y justifica las blandas en el panel.',
      ]);
    });

    it('con todo cumplido envía y recarga el consolidado', async () => {
      simulada.responder('GET', RUTA, json(200, LISTO));
      simulada.responder(
        'POST',
        REVISION,
        json(200, { ...EN_CONSOLIDACION, etapa: 'EN_REVISION_VRA', acciones: [] }),
      );
      await abrir();
      const usuario = userEvent.setup();

      const boton = await screen.findByRole('button', { name: 'Enviar a revisión' });
      expect(boton).toBeEnabled();
      expect(
        screen.queryByRole('list', { name: 'Falta para enviar a revisión' }),
      ).not.toBeInTheDocument();
      const antes = simulada.peticiones.filter((una) => new URL(una.url).pathname === RUTA).length;
      await usuario.click(boton);
      await usuario.click(
        within(screen.getByRole('dialog')).getByRole('button', { name: 'Enviar' }),
      );

      await waitFor(() =>
        expect(
          simulada.peticiones.filter((una) => new URL(una.url).pathname === RUTA).length,
        ).toBeGreaterThan(antes),
      );
      expect(
        simulada.peticiones.some(
          (una) => una.method === 'POST' && new URL(una.url).pathname === REVISION,
        ),
      ).toBe(true);
    });
  });

  describe('el informe', () => {
    it('sin consolidar no se puede adjuntar, y lo dice', async () => {
      simulada.responder('GET', RUTA, json(200, SIN_CONSOLIDAR));
      await abrir();

      expect(screen.queryByLabelText(/Adjuntar/)).not.toBeInTheDocument();
      expect(screen.getByText(/Primero consolida/)).toBeVisible();
    });

    it('adjunta el PDF y recarga', async () => {
      simulada.responder('PUT', RUTA_INFORME, json(200, INFORME));
      await abrir();
      simulada.responder('GET', RUTA, json(200, CON_INFORME));

      await userEvent.upload(screen.getByLabelText('Adjuntar el informe'), pdf());

      expect(await screen.findByText('informe-2026.pdf')).toBeVisible();
    });

    it('muestra nombre, tamaño, quién y cuándo', async () => {
      simulada.responder('GET', RUTA, json(200, CON_INFORME));
      await abrir();

      expect(screen.getByText('informe-2026.pdf')).toBeVisible();
      expect(screen.getByText(/2 MB · consolidacion\.demo/)).toBeVisible();
    });

    it('con informe ofrece reemplazarlo', async () => {
      simulada.responder('GET', RUTA, json(200, CON_INFORME));
      await abrir();

      expect(screen.getByLabelText('Reemplazar el informe')).toBeInTheDocument();
    });

    it('más de 10 MB se avisa sin subirlo', async () => {
      await abrir();

      await userEvent.upload(
        screen.getByLabelText('Adjuntar el informe'),
        pdf('grande.pdf', 10 * 1024 * 1024 + 1),
      );

      expect(await screen.findByRole('alert')).toHaveTextContent('Informe demasiado grande');
      expect(simulada.peticiones.some((una) => una.method === 'PUT')).toBe(false);
    });

    it('un error del servidor se muestra', async () => {
      simulada.responder(
        'PUT',
        RUTA_INFORME,
        json(400, {
          titulo: 'El archivo no es un PDF',
          descripcion: 'Adjunta el informe en PDF.',
          codigo: 'GENERACION_INFORME_NO_ES_PDF',
        }),
      );
      await abrir();

      await userEvent.upload(screen.getByLabelText('Adjuntar el informe'), pdf());

      expect(await screen.findByRole('alert')).toHaveTextContent('El archivo no es un PDF');
    });

    it('descarga con el token', async () => {
      const crear = vi.fn(() => 'blob:informe');
      Object.assign(URL, { createObjectURL: crear, revokeObjectURL: vi.fn() });
      simulada.responder('GET', RUTA, json(200, CON_INFORME));
      simulada.responder(
        'GET',
        RUTA_INFORME,
        new Response(new Blob(['%PDF-1.7']), { status: 200 }),
      );
      await abrir();

      await userEvent.click(screen.getByRole('button', { name: 'Descargar el informe' }));

      await waitFor(() => expect(crear).toHaveBeenCalled());
    });

    it('un archivo ilegible muestra el error', async () => {
      simulada.responder('GET', RUTA, json(200, CON_INFORME));
      simulada.responder(
        'GET',
        RUTA_INFORME,
        json(500, {
          titulo: 'Informe no disponible',
          descripcion: 'No se pudo leer el informe guardado.',
          codigo: 'GENERACION_INFORME_ILEGIBLE',
        }),
      );
      await abrir();

      await userEvent.click(screen.getByRole('button', { name: 'Descargar el informe' }));

      expect(await screen.findByRole('alert')).toHaveTextContent('Informe no disponible');
    });
  });

  describe('permisos', () => {
    it('el Vicerrectorado lee y descarga, sin consolidar ni adjuntar', async () => {
      simulada.responder('GET', RUTA, json(200, CON_INFORME));
      await abrir(VICERRECTOR);

      expect(screen.queryByRole('button', { name: /onsolidar/ })).not.toBeInTheDocument();
      expect(screen.queryByLabelText(/Reemplazar|Adjuntar/)).not.toBeInTheDocument();
      expect(screen.getByRole('button', { name: 'Descargar el informe' })).toBeVisible();
    });

    it('un coordinador no lo ve en el menú ni puede abrirlo', async () => {
      sesionCon(simulada, COORDINADOR);
      montarSpa('/consolidado');

      await waitFor(() =>
        expect(screen.queryByRole('link', { name: 'Oferta del periodo' })).toBeInTheDocument(),
      );
      expect(
        screen.queryByRole('link', { name: 'Consolidado del periodo' }),
      ).not.toBeInTheDocument();
      expect(screen.queryByRole('table')).not.toBeInTheDocument();
    });
  });
});

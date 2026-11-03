// La pantalla «Flujo y firmas» (F12-H01, RF-34, RF-58): muestra lo que rige, sin editarlo, y carga
// un archivo. Un rechazo dice todos los problemas y no cambia lo que se ve.
import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { instalarApiSimulada, json, type ApiSimulada } from '../../pruebas/apiSimulada';
import { montarSpa } from '../../pruebas/montarSpa';
import { cerrarSesion, iniciarSesion } from '../../sesion/almacen';
import { PERIODOS } from '../periodos/datosDePrueba';
import { CARGADA, EN_CURSO, POR_DEFECTO } from './datosDePrueba';

const ADMINISTRADOR = ['FLUJO_FIRMAS:LECTURA', 'FLUJO_FIRMAS:ESCRITURA', 'DISTRIBUTIVO:LECTURA'];
const SOLO_LECTURA = ['FLUJO_FIRMAS:LECTURA', 'DISTRIBUTIVO:LECTURA'];
const RUTA = '/api/validacionocs/periodos/2026-2/configuracion-flujo';
const RUTA_DEL_VIGENTE = '/api/validacionocs/periodos/2026-1/configuracion-flujo';
const ACCION = 'Cargar un archivo';

function sesionCon(simulada: ApiSimulada, permisos: string[]) {
  simulada.responder(
    'GET',
    '/api/usuarios/sesion',
    json(200, { usuario: 'demo', nombre: 'demo (demo)', roles: [], permisos }),
  );
}

function yaml(tamano = 100) {
  const archivo = new File(['etapas: {}'], 'flujo.yaml', { type: 'text/yaml' });
  Object.defineProperty(archivo, 'size', { value: tamano });
  return archivo;
}

describe('Flujo y firmas', () => {
  let simulada: ApiSimulada;

  beforeEach(() => {
    simulada = instalarApiSimulada();
    simulada.responder('GET', '/api/academico/periodos', json(200, PERIODOS));
    simulada.responder('GET', RUTA, json(200, POR_DEFECTO));
    iniciarSesion('cabecera.cuerpo.firma', new Date(Date.now() + 60_000));
  });

  afterEach(() => {
    cerrarSesion('cierre');
    vi.restoreAllMocks();
  });

  async function abrir(permisos = ADMINISTRADOR) {
    sesionCon(simulada, permisos);
    montarSpa('/flujo');
    return screen.findByRole('table', { name: 'Etapas del flujo' });
  }

  describe('qué rige', () => {
    it('muestra cada etapa con su estado, rol, plazo y devolución', async () => {
      const tabla = await abrir();

      const firma = within(tabla).getByText('En firma').closest('tr') as HTMLElement;
      expect(within(firma).getByText('Activa')).toBeVisible();
      expect(within(firma).getByText('Esqueleto fijo')).toBeVisible();
      expect(within(firma).getByText('Responsable de consolidación')).toBeVisible();
      expect(within(firma).getByText('5 días')).toBeVisible();
      expect(within(firma).getByText('Sí')).toBeVisible();

      const unidades = within(tabla)
        .getByText('Planificación de investigación y vinculación')
        .closest('tr') as HTMLElement;
      expect(within(unidades).getByText('Desactivada')).toBeVisible();
      expect(within(unidades).getByText('Opcional')).toBeVisible();
      expect(within(unidades).getByText('No')).toBeVisible();
    });

    it('una etapa sin plazo no inventa uno', async () => {
      const tabla = await abrir();

      const publicado = within(tabla).getByText('Publicado').closest('tr') as HTMLElement;
      expect(within(publicado).getByText('Sin plazo')).toBeVisible();
    });

    it('muestra las firmas por su orden, con el nombre de la etapa y del documento', async () => {
      await abrir();

      const tabla = await screen.findByRole('table', { name: 'Árbol de firmas' });
      const filas = within(tabla).getAllByRole('row');
      expect(within(filas[1] as HTMLElement).getByText('Elaborado')).toBeVisible();
      expect(within(filas[1] as HTMLElement).getByText('En firma')).toBeVisible();
      expect(within(filas[1] as HTMLElement).getByText('Consolidado')).toBeVisible();
      expect(within(filas[2] as HTMLElement).getByText('Rector')).toBeVisible();
    });

    it('sin archivo, dice que rige la configuración por defecto', async () => {
      await abrir();

      expect(screen.getByRole('status', { name: 'Configuración por defecto' })).toBeVisible();
    });

    it('con archivo, dice su versión, quién y cuándo', async () => {
      simulada.responder('GET', RUTA, json(200, CARGADA));
      await abrir();

      expect(await screen.findByText(/Versión 2 · cargada por admin\.demo/)).toBeVisible();
      expect(screen.queryByRole('status', { name: 'Configuración por defecto' })).toBeNull();
    });

    it('no se edita en la pantalla: el único campo es el periodo', async () => {
      await abrir();

      expect(screen.queryByRole('textbox')).toBeNull();
      expect(screen.getAllByRole('combobox')).toHaveLength(1);
    });

    it('al cambiar de periodo muestra el suyo', async () => {
      simulada.responder('GET', RUTA_DEL_VIGENTE, json(200, EN_CURSO));
      await abrir();

      await userEvent.selectOptions(screen.getByRole('combobox', { name: 'Periodo' }), '2026-1');

      expect(await screen.findByText(/Versión 2 · cargada por admin\.demo/)).toBeVisible();
    });

    it('si falla, muestra el error del catálogo', async () => {
      simulada.responder(
        'GET',
        RUTA,
        json(502, {
          titulo: 'Servicio con problemas',
          descripcion: 'Intenta de nuevo en unos minutos.',
          codigo: 'COMUN_PROCESO_CON_ERROR',
        }),
      );
      sesionCon(simulada, ADMINISTRADOR);
      montarSpa('/flujo');

      expect(await screen.findByText('Servicio con problemas')).toBeVisible();
    });
  });

  describe('plantilla', () => {
    it('la descarga', async () => {
      const crear = vi.fn(() => 'blob:plantilla');
      Object.assign(URL, { createObjectURL: crear, revokeObjectURL: vi.fn() });
      simulada.responder(
        'GET',
        '/api/validacionocs/plantilla-flujo',
        new Response(new Blob(['# plantilla']), { status: 200 }),
      );
      await abrir();

      await userEvent.click(screen.getByRole('button', { name: 'Descargar la plantilla' }));

      await waitFor(() => expect(crear).toHaveBeenCalled());
    });

    it('si falla la descarga, lo dice', async () => {
      simulada.responder(
        'GET',
        '/api/validacionocs/plantilla-flujo',
        json(500, { titulo: 'Error interno', descripcion: 'Intenta de nuevo.', codigo: 'X' }),
      );
      await abrir();

      await userEvent.click(screen.getByRole('button', { name: 'Descargar la plantilla' }));

      expect(await screen.findByText('Error interno')).toBeVisible();
    });
  });

  describe('cargar', () => {
    it('sube el archivo y muestra la versión nueva', async () => {
      simulada.responder('PUT', RUTA, json(200, CARGADA));
      await abrir();
      simulada.responder('GET', RUTA, json(200, CARGADA));

      await userEvent.upload(screen.getByLabelText(ACCION), yaml());

      expect(await screen.findByText(/Versión 2 · cargada por admin\.demo/)).toBeVisible();
    });

    it('un rechazo dice todos los problemas y lo que rige no cambia', async () => {
      simulada.responder(
        'PUT',
        RUTA,
        json(400, {
          titulo: 'Configuración con problemas',
          descripcion: 'Corrige lo que se indica y vuelve a cargar el archivo.',
          codigo: 'VALIDACIONOCS_CONFIGURACION_INVALIDA',
          detalles: [
            { campo: 'archivo', descripcion: 'Falta la etapa EN_OCS.' },
            { campo: 'archivo', descripcion: 'El rol RECTOR no tiene usuarios.' },
          ],
        }),
      );
      await abrir();

      await userEvent.upload(screen.getByLabelText(ACCION), yaml());

      expect(await screen.findByText('Configuración con problemas')).toBeVisible();
      expect(screen.getByText(/Falta la etapa EN_OCS\./)).toBeVisible();
      expect(screen.getByText(/El rol RECTOR no tiene usuarios\./)).toBeVisible();
      expect(screen.getByRole('status', { name: 'Configuración por defecto' })).toBeVisible();
      expect(screen.getByRole('table', { name: 'Etapas del flujo' })).toBeVisible();
    });

    it('un archivo ilegible dice qué hacer', async () => {
      simulada.responder(
        'PUT',
        RUTA,
        json(400, {
          titulo: 'Archivo no válido',
          descripcion: 'No se pudo leer el archivo. Parte de la plantilla y vuelve a cargarlo.',
          codigo: 'VALIDACIONOCS_ARCHIVO_ILEGIBLE',
          detalles: [{ campo: 'archivo', descripcion: 'Revisa la línea 3.' }],
        }),
      );
      await abrir();

      await userEvent.upload(screen.getByLabelText(ACCION), yaml());

      expect(await screen.findByText('Archivo no válido')).toBeVisible();
      expect(screen.getByText(/Revisa la línea 3\./)).toBeVisible();
    });

    it('más de 1 MB se rechaza sin subirlo', async () => {
      await abrir();

      await userEvent.upload(screen.getByLabelText(ACCION), yaml(1024 * 1024 + 1));

      expect(await screen.findByText('Archivo demasiado grande')).toBeVisible();
      expect(simulada.peticiones.filter((una) => una.method === 'PUT')).toHaveLength(0);
    });

    it('justo 1 MB sí se sube', async () => {
      simulada.responder('PUT', RUTA, json(200, CARGADA));
      await abrir();

      await userEvent.upload(screen.getByLabelText(ACCION), yaml(1024 * 1024));

      await waitFor(() =>
        expect(simulada.peticiones.filter((una) => una.method === 'PUT')).toHaveLength(1),
      );
    });

    it('un rechazo no sigue al cambiar de periodo', async () => {
      simulada.responder(
        'PUT',
        RUTA,
        json(400, { titulo: 'Archivo no válido', descripcion: 'Revísalo.', codigo: 'X' }),
      );
      simulada.responder('GET', RUTA_DEL_VIGENTE, json(200, EN_CURSO));
      await abrir();
      await userEvent.upload(screen.getByLabelText(ACCION), yaml());
      await screen.findByText('Archivo no válido');

      await userEvent.selectOptions(screen.getByRole('combobox', { name: 'Periodo' }), '2026-1');

      await waitFor(() => expect(screen.queryByText('Archivo no válido')).toBeNull());
    });

    it('una carga nueva quita el error anterior', async () => {
      simulada.responder(
        'PUT',
        RUTA,
        json(400, { titulo: 'Archivo no válido', descripcion: 'Revísalo.', codigo: 'X' }),
      );
      await abrir();
      await userEvent.upload(screen.getByLabelText(ACCION), yaml());
      await screen.findByText('Archivo no válido');
      simulada.responder('PUT', RUTA, json(200, CARGADA));

      await userEvent.upload(screen.getByLabelText(ACCION), yaml());

      await waitFor(() => expect(screen.queryByText('Archivo no válido')).toBeNull());
    });
  });

  describe('quién y cuándo', () => {
    it('sin permiso de escritura no sale cargar, pero sí la plantilla', async () => {
      await abrir(SOLO_LECTURA);

      expect(screen.queryByRole('button', { name: ACCION })).toBeNull();
      expect(screen.getByRole('button', { name: 'Descargar la plantilla' })).toBeVisible();
    });

    it('fuera de Planificación se ve, y no sale cargar', async () => {
      simulada.responder('GET', RUTA, json(200, EN_CURSO));
      await abrir();

      expect(
        await screen.findByText(/solo cambia mientras el periodo está en Planificación/),
      ).toBeVisible();
      expect(screen.queryByRole('button', { name: ACCION })).toBeNull();
    });

    it('está en el menú de quien tiene el permiso', async () => {
      await abrir(SOLO_LECTURA);

      expect(screen.getByRole('link', { name: 'Flujo y firmas' })).toBeVisible();
    });

    it('no está en el menú de los demás y su ruta no abre', async () => {
      sesionCon(simulada, ['DISTRIBUTIVO:LECTURA']);
      montarSpa('/flujo');

      expect(await screen.findByRole('alert')).toHaveTextContent('Acceso no autorizado');
      expect(screen.queryByRole('link', { name: 'Flujo y firmas' })).toBeNull();
    });
  });
});

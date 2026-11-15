// «Pendientes de firma» (F14-H01, D-102): el firmante en turno ve sus documentos y descarga el PDF
// vigente; sin pendientes, lo dice; un error en la descarga se muestra con el aviso común. Desde
// F14-H02 (D-103), sube el PDF firmado y ve el resultado de cada comprobación.
import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { instalarApiSimulada, json, type ApiSimulada } from '../../pruebas/apiSimulada';
import { montarSpa } from '../../pruebas/montarSpa';
import { cerrarSesion, iniciarSesion } from '../../sesion/almacen';

const DOCUMENTO = '3f2504e0-4f89-41d3-9a0c-0305e82c3301';
const ARCHIVO = `/api/firma/documentos/${DOCUMENTO}/archivo`;
const FIRMAS = `/api/firma/documentos/${DOCUMENTO}/firmas`;
const PENDIENTE = {
  documento: DOCUMENTO,
  periodo: '2026-I',
  version: 1,
  orden: 1,
  rotulo: 'Elaborado por',
  generadoEn: '2026-11-14T08:43:00Z',
};

function sesionCon(simulada: ApiSimulada, permisos: string[]) {
  simulada.responder(
    'GET',
    '/api/usuarios/sesion',
    json(200, { usuario: 'docente.demo', nombre: 'Docente (demo)', roles: [], permisos }),
  );
}

describe('Pendientes de firma', () => {
  let simulada: ApiSimulada;

  beforeEach(() => {
    simulada = instalarApiSimulada();
    simulada.responder('GET', '/api/firma/pendientes', json(200, [PENDIENTE]));
    iniciarSesion('cabecera.cuerpo.firma', new Date(Date.now() + 60_000));
  });

  afterEach(() => {
    cerrarSesion('cierre');
    vi.restoreAllMocks();
  });

  async function abrir(permisos = ['DISTRIBUTIVO:FIRMA']) {
    sesionCon(simulada, permisos);
    montarSpa('/firmas-pendientes');
    return screen.findByRole('table', { name: 'Firmas pendientes' });
  }

  it('muestra cada documento en turno con su periodo, versión, paso y fecha', async () => {
    const tabla = await abrir();

    const fila = within(tabla).getByText('2026-I').closest('tr') as HTMLElement;
    expect(within(fila).getByText('1')).toBeVisible();
    expect(within(fila).getByText('Elaborado por')).toBeVisible();
    expect(within(fila).getByText(/14\/11\/2026|14 nov/)).toBeVisible();
    expect(
      within(fila).getByRole('button', { name: 'Descargar el PDF de 2026-I, versión 1' }),
    ).toBeEnabled();
    expect(screen.getByRole('heading', { name: 'Pendientes de firma' })).toBeVisible();
  });

  it('sin pendientes, lo dice', async () => {
    simulada.responder('GET', '/api/firma/pendientes', json(200, []));
    sesionCon(simulada, ['DISTRIBUTIVO:FIRMA']);
    montarSpa('/firmas-pendientes');

    expect(await screen.findByText('No tienes firmas pendientes.')).toBeVisible();
    expect(screen.queryByRole('table', { name: 'Firmas pendientes' })).not.toBeInTheDocument();
  });

  it('descarga el PDF una vez, con el nombre del servidor, y se desactiva mientras tanto', async () => {
    const crear = vi.fn(() => 'blob:pdf');
    Object.assign(URL, { createObjectURL: crear, revokeObjectURL: vi.fn() });
    let soltar: (respuesta: Response) => void = () => {};
    simulada.responder(
      'GET',
      ARCHIVO,
      () =>
        new Promise<Response>((resolver) => {
          soltar = resolver;
        }),
    );
    // El nombre con que se guarda: el del enlace temporal que se pulsa.
    const enlaces: string[] = [];
    vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(function (
      this: HTMLAnchorElement,
    ) {
      enlaces.push(this.download);
    });
    await abrir();
    const boton = screen.getByRole('button', { name: 'Descargar el PDF de 2026-I, versión 1' });

    await userEvent.click(boton);
    await waitFor(() => expect(boton).toBeDisabled());
    soltar(
      new Response(new Blob(['%PDF-1.7']), {
        status: 200,
        headers: {
          'Content-Type': 'application/pdf',
          'Content-Disposition': 'attachment; filename="distributivo-2026-I-v1.pdf"',
        },
      }),
    );

    await waitFor(() => expect(crear).toHaveBeenCalledTimes(1));
    expect(enlaces).toEqual(['distributivo-2026-I-v1.pdf']);
    await waitFor(() => expect(boton).toBeEnabled());
    expect(simulada.peticiones.filter((una) => new URL(una.url).pathname === ARCHIVO)).toHaveLength(
      1,
    );
  });

  it('si la descarga responde 403, lo dice con el aviso común', async () => {
    simulada.responder(
      'GET',
      ARCHIVO,
      json(403, {
        titulo: 'Acceso no autorizado',
        descripcion: 'No tienes permiso para esta operación.',
        codigo: 'SEGURIDAD_ACCESO_NO_AUTORIZADO',
      }),
    );
    await abrir();

    await userEvent.click(
      screen.getByRole('button', { name: 'Descargar el PDF de 2026-I, versión 1' }),
    );

    expect(await screen.findByText('Acceso no autorizado')).toBeVisible();
  });

  it('si el archivo no está disponible, lo dice', async () => {
    simulada.responder(
      'GET',
      ARCHIVO,
      json(500, {
        titulo: 'Archivo no disponible',
        descripcion:
          'El archivo del documento firmable no está disponible. Avisa al administrador con el identificador del incidente.',
        codigo: 'FIRMA_ARCHIVO_NO_DISPONIBLE',
      }),
    );
    await abrir();

    await userEvent.click(
      screen.getByRole('button', { name: 'Descargar el PDF de 2026-I, versión 1' }),
    );

    expect(await screen.findByText('Archivo no disponible')).toBeVisible();
  });

  describe('subir el PDF firmado (F14-H02)', () => {
    const ACEPTADA = {
      id: '5d0c6f1e-2222-4333-8444-955566667777',
      documento: DOCUMENTO,
      orden: 1,
      aceptada: true,
      sha256Subido: 'e'.repeat(64),
      subidoEn: '2026-11-14T10:00:00Z',
      comprobaciones: [
        { comprobacion: 'MISMO_DOCUMENTO', resultado: 'APROBADA' },
        { comprobacion: 'UNA_FIRMA_NUEVA', resultado: 'APROBADA' },
        { comprobacion: 'INTEGRIDAD', resultado: 'APROBADA' },
        { comprobacion: 'CONFIANZA', resultado: 'APROBADA' },
        { comprobacion: 'VIGENCIA', resultado: 'APROBADA' },
        { comprobacion: 'ORDEN', resultado: 'APROBADA' },
        { comprobacion: 'FIRMANTE', resultado: 'APROBADA' },
      ],
      certificado: {
        nombre: 'Firmante Uno Prueba (F1)',
        emisor: 'CN=CA de Prueba MDD Demo,O=MDD Demo,C=EC',
        serie: '19a2b3',
        validoDesde: '2026-10-13T10:00:00Z',
        validoHasta: '2029-10-12T10:00:00Z',
      },
    };
    const NO_INTEGRA = {
      titulo: 'Firma no íntegra',
      descripcion:
        'La firma nueva no corresponde al contenido del PDF: el archivo cambió después de firmarlo. Vuelve a firmar el PDF descargado.',
      codigo: 'FIRMA_NO_INTEGRA',
      detalles: [
        { campo: 'MISMO_DOCUMENTO', descripcion: 'Aprobada.' },
        { campo: 'UNA_FIRMA_NUEVA', descripcion: 'Aprobada.' },
        { campo: 'INTEGRIDAD', descripcion: 'Rechazada. La firma nueva no es íntegra.' },
        { campo: 'CONFIANZA', descripcion: 'No evaluada. La firma nueva no es íntegra.' },
        { campo: 'VIGENCIA', descripcion: 'No evaluada. La firma nueva no es íntegra.' },
        { campo: 'ORDEN', descripcion: 'No evaluada. La firma nueva no es íntegra.' },
        { campo: 'FIRMANTE', descripcion: 'No evaluada. La firma nueva no es íntegra.' },
      ],
    };
    const firmado = () =>
      new File(['%PDF-1.7 firmado'], 'firmado.pdf', { type: 'application/pdf' });

    it('cada fila tiene «Subir el PDF firmado», con el nombre de su fila', async () => {
      const tabla = await abrir();

      expect(
        within(tabla).getByRole('button', { name: 'Subir el PDF firmado de 2026-I, versión 1' }),
      ).toBeEnabled();
    });

    it('sube el archivo elegido y, si se acepta, muestra el resultado y vuelve a pedir la lista', async () => {
      simulada.responder('POST', FIRMAS, json(201, ACEPTADA));
      await abrir();
      simulada.responder('GET', '/api/firma/pendientes', json(200, []));

      await userEvent.upload(
        screen.getByLabelText('Archivo firmado de 2026-I, versión 1'),
        firmado(),
      );

      const aviso = await screen.findByRole('status', { name: /Firma registrada/ });
      expect(within(aviso).getByText('Es la versión vigente')).toBeVisible();
      expect(within(aviso).getByText('Tiene una sola firma nueva')).toBeVisible();
      expect(within(aviso).getByText('La firma es íntegra')).toBeVisible();
      expect(within(aviso).getByText('El certificado es de confianza')).toBeVisible();
      expect(within(aviso).getByText('El certificado está vigente')).toBeVisible();
      expect(within(aviso).getByText('La firma está en su orden')).toBeVisible();
      expect(within(aviso).getByText('Firma la persona del paso')).toBeVisible();
      expect(within(aviso).getAllByText('Aprobada')).toHaveLength(7);
      expect(within(aviso).getByText(/Firmante Uno Prueba \(F1\)/)).toBeVisible();
      const subida = simulada.peticiones.find(
        (una) => una.method === 'POST' && new URL(una.url).pathname === FIRMAS,
      );
      expect(subida?.headers.get('Content-Type')).toMatch(/^multipart\/form-data/);
      expect(await screen.findByText('No tienes firmas pendientes.')).toBeVisible();
    });

    it('si se rechaza, muestra el motivo y las siete comprobaciones, y la fila sigue', async () => {
      simulada.responder('POST', FIRMAS, json(422, NO_INTEGRA));
      await abrir();

      await userEvent.upload(
        screen.getByLabelText('Archivo firmado de 2026-I, versión 1'),
        firmado(),
      );

      const aviso = await screen.findByRole('alert');
      expect(within(aviso).getByText('Firma no íntegra')).toBeVisible();
      expect(within(aviso).getByText('La firma es íntegra')).toBeVisible();
      expect(within(aviso).getByText('Rechazada. La firma nueva no es íntegra.')).toBeVisible();
      expect(within(aviso).getAllByText('No evaluada. La firma nueva no es íntegra.')).toHaveLength(4);
      expect(within(aviso).getByText('Firma la persona del paso')).toBeVisible();
      expect(screen.getByRole('table', { name: 'Firmas pendientes' })).toBeVisible();
    });

    it('otro error se muestra con el aviso común', async () => {
      simulada.responder(
        'POST',
        FIRMAS,
        json(403, {
          titulo: 'Acceso no autorizado',
          descripcion: 'No tienes permiso para esta operación.',
          codigo: 'SEGURIDAD_ACCESO_NO_AUTORIZADO',
        }),
      );
      await abrir();

      await userEvent.upload(
        screen.getByLabelText('Archivo firmado de 2026-I, versión 1'),
        firmado(),
      );

      expect(await screen.findByText('Acceso no autorizado')).toBeVisible();
    });

    it('mientras sube, los dos botones de la fila se desactivan', async () => {
      let soltar: (respuesta: Response) => void = () => {};
      simulada.responder(
        'POST',
        FIRMAS,
        () =>
          new Promise<Response>((resolver) => {
            soltar = resolver;
          }),
      );
      await abrir();

      await userEvent.upload(
        screen.getByLabelText('Archivo firmado de 2026-I, versión 1'),
        firmado(),
      );

      const subir = screen.getByRole('button', {
        name: 'Subir el PDF firmado de 2026-I, versión 1',
      });
      const descargar = screen.getByRole('button', {
        name: 'Descargar el PDF de 2026-I, versión 1',
      });
      await waitFor(() => expect(subir).toBeDisabled());
      expect(descargar).toBeDisabled();
      soltar(json(422, NO_INTEGRA));
      await waitFor(() => expect(subir).toBeEnabled());
      expect(descargar).toBeEnabled();
    });
  });

  it('el menú ofrece «Pendientes de firma» a quien firma', async () => {
    await abrir();

    expect(screen.getByRole('link', { name: 'Pendientes de firma' })).toBeVisible();
  });

  it('el menú no lo ofrece a quien no firma', async () => {
    sesionCon(simulada, ['OFERTA:LECTURA']);
    montarSpa('/');

    await screen.findByRole('link', { name: 'Inicio' });
    expect(screen.queryByRole('link', { name: 'Pendientes de firma' })).not.toBeInTheDocument();
  });
});

// El estado del flujo del distributivo (F12-H02, contracts/spa.md): la etapa, el envío de cada
// parcial y solo las acciones que la API dice que el usuario puede hacer.
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { instalarApiSimulada, json, type ApiSimulada } from '../../pruebas/apiSimulada';
import { EstadoDelFlujoDelPeriodo } from './EstadoDelFlujo';
import type { EstadoDelFlujo } from './flujo';

const RUTA = '/api/validacionocs/periodos/2026-I/flujo';
const ENVIO = '/api/validacionocs/periodos/2026-I/parciales/DS/envio';
const AVANCE = '/api/validacionocs/periodos/2026-I/avance';
const REVISION = '/api/validacionocs/periodos/2026-I/revision';
const DEVOLUCION = '/api/validacionocs/periodos/2026-I/devolucion';
const APROBACION = '/api/validacionocs/periodos/2026-I/aprobacion';

function flujo(cambios: Partial<EstadoDelFlujo> = {}): EstadoDelFlujo {
  return {
    periodo: '2026-I',
    etapa: 'BORRADOR_CARRERA',
    nombreDeEtapa: 'Propuesta de carrera',
    version: 1,
    parciales: [
      { carrera: 'DS', enviado: false, pendienteDeConfirmar: false },
      {
        carrera: 'TDS',
        enviado: true,
        pendienteDeConfirmar: false,
        enviadoPor: 'otra.coordinacion',
        enviadoEn: '2026-11-07T17:02:14Z',
      },
    ],
    acciones: [],
    transiciones: [],
    ...cambios,
  };
}

function montar(carrera?: string, faltan?: string[]) {
  const consultas = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  const alCambiar = vi.fn();
  render(
    <QueryClientProvider client={consultas}>
      <EstadoDelFlujoDelPeriodo
        periodo="2026-I"
        carrera={carrera}
        alCambiar={alCambiar}
        faltan={faltan}
      />
    </QueryClientProvider>,
  );
  return { alCambiar };
}

describe('EstadoDelFlujo', () => {
  let simulada: ApiSimulada;

  beforeEach(() => {
    simulada = instalarApiSimulada();
  });

  it('muestra la etapa, la versión y el envío de cada parcial', async () => {
    simulada.responder('GET', RUTA, json(200, flujo()));
    montar();

    const region = await screen.findByRole('region', { name: 'Estado del distributivo' });
    expect(within(region).getByText('Propuesta de carrera · versión 1')).toBeVisible();
    expect(within(region).getByText('DS: pendiente')).toBeVisible();
    expect(
      within(region).getByText('TDS: enviado por otra.coordinacion el 7/11/26, 12:02'),
    ).toBeVisible();
    expect(within(region).queryByRole('button')).not.toBeInTheDocument();
  });

  it('el coordinador envía su parcial con una confirmación', async () => {
    simulada.responder('GET', RUTA, json(200, flujo({ acciones: ['ENVIAR_PARCIAL'] })));
    simulada.responder(
      'POST',
      ENVIO,
      json(
        200,
        flujo({ parciales: [{ carrera: 'DS', enviado: true, pendienteDeConfirmar: false }] }),
      ),
    );
    const { alCambiar } = montar('DS');
    const usuario = userEvent.setup();

    await usuario.click(await screen.findByRole('button', { name: 'Enviar parcial' }));
    const confirmar = screen.getByRole('dialog', { name: 'Enviar el parcial de DS' });
    expect(confirmar).toHaveTextContent(
      'Después de enviarlo, no podrás cambiar las propuestas ni las actividades de tu carrera.',
    );
    await usuario.click(within(confirmar).getByRole('button', { name: 'Enviar' }));

    await waitFor(() =>
      expect(simulada.peticiones.some((una) => una.method === 'POST')).toBe(true),
    );
    await waitFor(() => expect(alCambiar).toHaveBeenCalled());
  });

  it('un parcial pendiente de confirmar dice que falta confirmar y deja reenviarlo', async () => {
    simulada.responder(
      'GET',
      RUTA,
      json(
        200,
        flujo({
          acciones: ['ENVIAR_PARCIAL'],
          parciales: [{ carrera: 'DS', enviado: false, pendienteDeConfirmar: true }],
        }),
      ),
    );
    montar('DS');

    expect(await screen.findByText('DS: enviado, falta confirmar')).toBeVisible();
    expect(screen.getByRole('button', { name: 'Reenviar parcial' })).toBeVisible();
  });

  it('el gestor avanza BORRADOR_UNIDADES', async () => {
    simulada.responder(
      'GET',
      RUTA,
      json(
        200,
        flujo({ etapa: 'BORRADOR_UNIDADES', nombreDeEtapa: 'Unidades', acciones: ['AVANZAR'] }),
      ),
    );
    simulada.responder('POST', AVANCE, json(200, flujo({ etapa: 'EN_CONSOLIDACION' })));
    montar();
    const usuario = userEvent.setup();

    await usuario.click(
      await screen.findByRole('button', { name: 'Avanzar a la siguiente etapa' }),
    );
    await usuario.click(
      within(screen.getByRole('dialog')).getByRole('button', { name: 'Avanzar' }),
    );

    await waitFor(() =>
      expect(simulada.peticiones.some((una) => new URL(una.url).pathname === AVANCE)).toBe(true),
    );
  });

  it('un 409 de ya enviado se muestra con el aviso común', async () => {
    simulada.responder('GET', RUTA, json(200, flujo({ acciones: ['ENVIAR_PARCIAL'] })));
    simulada.responder(
      'POST',
      ENVIO,
      json(409, {
        titulo: 'Parcial ya enviado',
        descripcion: 'Este parcial ya se envió.',
        codigo: 'VALIDACIONOCS_PARCIAL_YA_ENVIADO',
      }),
    );
    montar('DS');
    const usuario = userEvent.setup();

    await usuario.click(await screen.findByRole('button', { name: 'Enviar parcial' }));
    await usuario.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Enviar' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('Parcial ya enviado');
  });

  describe('enviar a revisión (F10-H03)', () => {
    const enConsolidacion = flujo({
      etapa: 'EN_CONSOLIDACION',
      nombreDeEtapa: 'Consolidación',
      acciones: ['ENVIAR_A_REVISION'],
    });

    it('el Responsable envía a revisión con una confirmación', async () => {
      simulada.responder('GET', RUTA, json(200, enConsolidacion));
      simulada.responder('POST', REVISION, json(200, flujo({ etapa: 'EN_REVISION_VRA' })));
      const { alCambiar } = montar(undefined, []);
      const usuario = userEvent.setup();

      await usuario.click(await screen.findByRole('button', { name: 'Enviar a revisión' }));
      const confirmar = screen.getByRole('dialog', { name: 'Enviar a revisión' });
      expect(confirmar).toHaveTextContent(
        'El distributivo pasa a revisión del Vicerrectorado. Ya no se podrá consolidar ni justificar en esta etapa.',
      );
      await usuario.click(within(confirmar).getByRole('button', { name: 'Enviar' }));

      await waitFor(() =>
        expect(simulada.peticiones.some((una) => new URL(una.url).pathname === REVISION)).toBe(
          true,
        ),
      );
      await waitFor(() => expect(alCambiar).toHaveBeenCalled());
    });

    it('si falta algo, el botón queda deshabilitado y dice qué falta', async () => {
      simulada.responder('GET', RUTA, json(200, enConsolidacion));
      montar(undefined, ['Consolida el periodo.', 'Adjunta el informe sustentado.']);

      const boton = await screen.findByRole('button', { name: 'Enviar a revisión' });
      expect(boton).toBeDisabled();
      const lista = screen.getByRole('list', { name: 'Falta para enviar a revisión' });
      expect(
        within(lista)
          .getAllByRole('listitem')
          .map((uno) => uno.textContent),
      ).toEqual(['Consolida el periodo.', 'Adjunta el informe sustentado.']);
      expect(boton).toHaveAttribute('aria-describedby', lista.id);
    });

    it('sin la acción no hay botón', async () => {
      simulada.responder('GET', RUTA, json(200, flujo({ etapa: 'EN_CONSOLIDACION' })));
      montar(undefined, []);

      await screen.findByRole('region', { name: 'Estado del distributivo' });
      expect(screen.queryByRole('button', { name: 'Enviar a revisión' })).not.toBeInTheDocument();
    });

    it('el 409 de inconsistencias abiertas dice cuántas quedan', async () => {
      simulada.responder('GET', RUTA, json(200, enConsolidacion));
      simulada.responder(
        'POST',
        REVISION,
        json(409, {
          titulo: 'Inconsistencias abiertas',
          descripcion:
            'Quedan inconsistencias abiertas. Corrige las duras y justifica las blandas en el panel de inconsistencias.',
          codigo: 'VALIDACIONOCS_INCONSISTENCIAS_ABIERTAS',
          detalles: [
            {
              campo: 'inconsistencias',
              descripcion: 'Quedan 3: 2 duras y 1 blanda sin justificar.',
            },
          ],
        }),
      );
      montar(undefined, []);
      const usuario = userEvent.setup();

      await usuario.click(await screen.findByRole('button', { name: 'Enviar a revisión' }));
      await usuario.click(
        within(screen.getByRole('dialog')).getByRole('button', { name: 'Enviar' }),
      );

      const aviso = await screen.findByRole('alert');
      expect(aviso).toHaveTextContent('Inconsistencias abiertas');
      expect(aviso).toHaveTextContent('Quedan 3: 2 duras y 1 blanda sin justificar.');
    });
  });

  describe('devolver parciales (F12-H05)', () => {
    const enConsolidacion = flujo({
      etapa: 'EN_CONSOLIDACION',
      nombreDeEtapa: 'Consolidación',
      parciales: [
        {
          carrera: 'DS',
          enviado: true,
          pendienteDeConfirmar: false,
          enviadoPor: 'coordinacion.demo',
          enviadoEn: '2026-11-07T17:02:14Z',
        },
        {
          carrera: 'TDS',
          enviado: true,
          pendienteDeConfirmar: false,
          enviadoPor: 'otra.coordinacion',
          enviadoEn: '2026-11-07T17:02:14Z',
        },
      ],
      acciones: ['ENVIAR_A_REVISION', 'DEVOLVER_PARCIALES'],
    });
    const devuelto = flujo({
      parciales: [
        {
          carrera: 'DS',
          enviado: false,
          pendienteDeConfirmar: false,
          devolucion: {
            por: 'consolidacion.demo',
            en: '2026-11-08T22:10:00Z',
            observacion: 'Faltan docentes en primer nivel.',
          },
        },
      ],
    });

    it('un parcial devuelto dice quién, cuándo y la observación', async () => {
      simulada.responder('GET', RUTA, json(200, devuelto));
      montar();

      const region = await screen.findByRole('region', { name: 'Estado del distributivo' });
      expect(
        within(region).getByText('DS: devuelto por consolidacion.demo el 8/11/26, 17:10'),
      ).toBeVisible();
      expect(within(region).getByText('Faltan docentes en primer nivel.')).toBeVisible();
      expect(within(region).getByText('Observación')).toBeVisible();
    });

    it('el coordinador con su parcial devuelto lo reenvía', async () => {
      simulada.responder('GET', RUTA, json(200, { ...devuelto, acciones: ['ENVIAR_PARCIAL'] }));
      montar('DS');

      expect(await screen.findByRole('button', { name: 'Reenviar parcial' })).toBeVisible();
    });

    it('sin la acción no hay botón de devolver', async () => {
      simulada.responder(
        'GET',
        RUTA,
        json(200, { ...enConsolidacion, acciones: ['ENVIAR_A_REVISION'] }),
      );
      montar(undefined, []);

      await screen.findByRole('button', { name: 'Enviar a revisión' });
      expect(screen.queryByRole('button', { name: 'Devolver parciales' })).not.toBeInTheDocument();
    });

    it('el Responsable elige carreras y escribe la observación de cada una', async () => {
      simulada.responder('GET', RUTA, json(200, enConsolidacion));
      simulada.responder('POST', DEVOLUCION, json(200, flujo()));
      const { alCambiar } = montar(undefined, ['Consolida el periodo.']);
      const usuario = userEvent.setup();

      // Devolver no depende de lo que falta para enviar a revisión.
      await usuario.click(await screen.findByRole('button', { name: 'Devolver parciales' }));
      const dialogo = screen.getByRole('dialog', { name: 'Devolver parciales' });
      const devolver = within(dialogo).getByRole('button', { name: 'Devolver' });
      expect(devolver).toBeDisabled();
      expect(within(dialogo).queryByLabelText(/Observación para/)).not.toBeInTheDocument();

      await usuario.click(within(dialogo).getByRole('checkbox', { name: 'DS' }));
      const observacion = within(dialogo).getByLabelText(/^Observación para DS/);
      await usuario.type(observacion, 'corta');
      expect(within(dialogo).getByText(/5 de 500/)).toBeVisible();
      expect(observacion).toHaveAttribute('aria-invalid', 'true');
      expect(devolver).toBeDisabled();

      await usuario.type(observacion, ' pero ya no tanto.');
      expect(devolver).toBeEnabled();
      await usuario.click(devolver);

      await waitFor(() => expect(alCambiar).toHaveBeenCalled());
      const peticion = simulada.peticiones.find((una) => new URL(una.url).pathname === DEVOLUCION);
      expect(await peticion?.json()).toEqual({
        devoluciones: [{ carrera: 'DS', observacion: 'corta pero ya no tanto.' }],
      });
      expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    });

    it('nombra cada carrera por su nombre, no por su código', async () => {
      simulada.responder('GET', RUTA, json(200, enConsolidacion));
      simulada.responder(
        'GET',
        '/api/academico/carreras',
        json(200, [
          { codigo: 'DS', nombre: 'DESARROLLO DE SOFTWARE', estado: 'VIGENTE' },
          { codigo: 'TDS', nombre: 'TOPOGRAFÍA', estado: 'VIGENTE' },
        ]),
      );
      montar(undefined, []);
      const usuario = userEvent.setup();

      const region = await screen.findByRole('region', { name: 'Estado del distributivo' });
      expect(
        await within(region).findByText(/^DESARROLLO DE SOFTWARE: enviado por coordinacion.demo/),
      ).toBeVisible();
      await usuario.click(screen.getByRole('button', { name: 'Devolver parciales' }));
      const dialogo = screen.getByRole('dialog', { name: 'Devolver parciales' });
      await usuario.click(within(dialogo).getByRole('checkbox', { name: 'TOPOGRAFÍA' }));
      expect(within(dialogo).getByLabelText(/^Observación para TOPOGRAFÍA/)).toBeVisible();
      expect(within(dialogo).queryByText('DS')).not.toBeInTheDocument();
    });

    it('el 409 se muestra en el diálogo', async () => {
      simulada.responder('GET', RUTA, json(200, enConsolidacion));
      simulada.responder(
        'POST',
        DEVOLUCION,
        json(409, {
          titulo: 'Etapa sin devolución',
          descripcion: 'Solo se devuelven parciales desde la consolidación.',
          codigo: 'VALIDACIONOCS_ETAPA_NO_ADMITE_DEVOLUCION',
        }),
      );
      montar(undefined, []);
      const usuario = userEvent.setup();

      await usuario.click(await screen.findByRole('button', { name: 'Devolver parciales' }));
      const dialogo = screen.getByRole('dialog', { name: 'Devolver parciales' });
      await usuario.click(within(dialogo).getByRole('checkbox', { name: 'TDS' }));
      await usuario.type(
        within(dialogo).getByLabelText(/^Observación para TDS/),
        'Un docente sin clase.',
      );
      await usuario.click(within(dialogo).getByRole('button', { name: 'Devolver' }));

      expect(await within(dialogo).findByRole('alert')).toHaveTextContent('Etapa sin devolución');
    });
  });

  describe('aprobar el contenido (F13-H01)', () => {
    const HASH = '0123456789abcdef'.repeat(4);
    const enRevision = flujo({
      etapa: 'EN_REVISION_VRA',
      nombreDeEtapa: 'Revisión del Vicerrectorado',
      parciales: [{ carrera: 'DS', enviado: true, pendienteDeConfirmar: false }],
    });
    const congelado = flujo({
      etapa: 'CONGELADO_EN_FIRMA',
      nombreDeEtapa: 'En firma',
      version: 2,
      parciales: [{ carrera: 'DS', enviado: true, pendienteDeConfirmar: false }],
      congelacion: {
        version: 2,
        hash: HASH,
        quien: 'vicerrectorado.demo',
        momento: '2026-11-10T15:30:00Z',
      },
    });

    it('con la acción, el Vicerrector ve «Aprobar el contenido»; sin ella, no', async () => {
      simulada.responder('GET', RUTA, json(200, { ...enRevision, acciones: [] }));
      montar();

      await screen.findByText('Revisión del Vicerrectorado · versión 1');
      expect(
        screen.queryByRole('button', { name: 'Aprobar el contenido' }),
      ).not.toBeInTheDocument();
    });

    it('el Vicerrector aprueba con el diálogo y la pantalla se recarga', async () => {
      simulada.responder(
        'GET',
        RUTA,
        json(200, { ...enRevision, acciones: ['APROBAR_CONTENIDO'] }),
      );
      simulada.responder('POST', APROBACION, json(200, congelado));
      const { alCambiar } = montar();
      const usuario = userEvent.setup();

      await usuario.click(await screen.findByRole('button', { name: 'Aprobar el contenido' }));
      const dialogo = screen.getByRole('dialog', { name: 'Aprobar el contenido' });
      expect(dialogo).toHaveTextContent('La versión 1 queda congelada');
      await usuario.click(within(dialogo).getByRole('button', { name: 'Aprobar y congelar' }));

      await waitFor(() => expect(alCambiar).toHaveBeenCalled());
      expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    });

    it('la constancia dice la versión, quién, cuándo y la huella abreviada', async () => {
      simulada.responder('GET', RUTA, json(200, congelado));
      montar();

      const region = await screen.findByRole('region', { name: 'Estado del distributivo' });
      expect(
        await within(region).findByText(
          'Versión 2 congelada por vicerrectorado.demo el 10/11/26, 10:30',
        ),
      ).toBeVisible();
      expect(within(region).getByText('Huella: 0123456789ab…')).toBeVisible();
      expect(within(region).queryByText(HASH)).not.toBeInTheDocument();
    });

    it('«Copiar la huella» copia los 64 caracteres y lo confirma', async () => {
      simulada.responder('GET', RUTA, json(200, congelado));
      montar();
      const usuario = userEvent.setup();

      await usuario.click(await screen.findByRole('button', { name: 'Copiar la huella' }));

      expect(await navigator.clipboard.readText()).toBe(HASH);
      expect(await screen.findByText('Huella copiada')).toBeVisible();
    });

    it('en firma, la tarjeta muestra el documento firmable (F13-H02)', async () => {
      simulada.responder('GET', RUTA, json(200, congelado));
      simulada.responder(
        'GET',
        '/api/firma/periodos/2026-I/documento',
        json(200, {
          periodo: '2026-I',
          version: 2,
          estado: 'GENERADO',
          documento: {
            sha256: 'f'.repeat(64),
            hashContenido: HASH,
            generadoPor: 'sistema',
            generadoEn: '2026-11-10T15:30:20Z',
          },
          acciones: [],
          cadena: [],
        }),
      );
      montar();

      const region = await screen.findByRole('region', { name: 'Estado del distributivo' });
      expect(
        await within(region).findByText('Documento firmable generado el 10/11/26, 10:30'),
      ).toBeVisible();
      expect(within(region).getByText('Huella del PDF: ffffffffffff…')).toBeVisible();
    });

    it('antes de la firma no consulta el documento firmable', async () => {
      simulada.responder(
        'GET',
        RUTA,
        json(200, { ...enRevision, acciones: ['APROBAR_CONTENIDO'] }),
      );
      montar();

      await screen.findByRole('button', { name: 'Aprobar el contenido' });
      expect(simulada.peticiones.some((una) => una.url.includes('/api/firma/'))).toBe(false);
    });

    it('a mitad de camino: sin constancia y con «Aprobar el contenido»', async () => {
      simulada.responder(
        'GET',
        RUTA,
        json(200, { ...enRevision, acciones: ['APROBAR_CONTENIDO'] }),
      );
      montar();

      expect(await screen.findByRole('button', { name: 'Aprobar el contenido' })).toBeVisible();
      expect(screen.queryByText(/congelada por/)).not.toBeInTheDocument();
    });
  });

  describe('rechazo en firma (F14-H04)', () => {
    const rechazado = flujo({
      etapa: 'EN_REVISION_VRA',
      nombreDeEtapa: 'Revisión del Vicerrectorado',
      parciales: [{ carrera: 'DS', enviado: true, pendienteDeConfirmar: false }],
      rechazoEnFirma: {
        orden: 3,
        rol: 'RECTOR',
        por: 'rectorado.demo',
        en: '2026-11-15T15:30:00Z',
        observacion: 'Falta la carga de un docente de Software.',
        versionAnulada: 1,
      },
    });

    it('dice quién rechazó, en qué paso, cuándo, la observación y que hace falta otra versión', async () => {
      simulada.responder('GET', RUTA, json(200, rechazado));
      montar();

      const aviso = await screen.findByRole('status', { name: 'Rechazado en firma' });
      expect(
        within(aviso).getByText('Paso 3 · RECTOR, por rectorado.demo el 15/11/26, 10:30'),
      ).toBeVisible();
      expect(within(aviso).getByText('Falta la carga de un docente de Software.')).toBeVisible();
      expect(
        within(aviso).getByText('Para aprobar otra vez hace falta una versión nueva.'),
      ).toBeVisible();
      expect(
        screen.queryByRole('button', { name: 'Aprobar el contenido' }),
      ).not.toBeInTheDocument();
    });

    // F12-H04 (D-106): con la versión nueva, el aviso dice cuál se anuló y con cuál se sigue.
    it('con la versión nueva, dice que la anterior quedó anulada y deja aprobar', async () => {
      simulada.responder(
        'GET',
        RUTA,
        json(200, {
          ...rechazado,
          version: 2,
          acciones: ['APROBAR_CONTENIDO'],
          congelacion: {
            version: 1,
            hash: 'b'.repeat(64),
            quien: 'vicerrectorado.demo',
            momento: '2026-11-15T15:00:00Z',
          },
          rechazoEnFirma: { ...rechazado.rechazoEnFirma, versionNueva: 2 },
        }),
      );
      montar();

      const aviso = await screen.findByRole('status', { name: 'Rechazado en firma' });
      expect(
        within(aviso).getByText(
          'La versión 1 quedó anulada con sus firmas; el distributivo sigue con la versión 2.',
        ),
      ).toBeVisible();
      expect(within(aviso).queryByText(/hace falta una versión nueva/)).not.toBeInTheDocument();
      expect(within(aviso).getByText('Falta la carga de un docente de Software.')).toBeVisible();
      expect(await screen.findByRole('button', { name: 'Aprobar el contenido' })).toBeVisible();
      // La constancia es de la versión anulada: no se muestra con la versión 2 vigente.
      expect(screen.queryByText(/congelada por/)).not.toBeInTheDocument();
    });

    it('sin rechazo no muestra el aviso', async () => {
      simulada.responder(
        'GET',
        RUTA,
        json(200, { ...rechazado, rechazoEnFirma: undefined, acciones: ['APROBAR_CONTENIDO'] }),
      );
      montar();

      expect(await screen.findByRole('button', { name: 'Aprobar el contenido' })).toBeVisible();
      expect(screen.queryByRole('status', { name: 'Rechazado en firma' })).not.toBeInTheDocument();
    });
  });

  // La resolución del OCS (F15-H01, D-107, contracts/spa.md).
  describe('resolución del OCS (F15-H01)', () => {
    const RESOLUCION = '/api/validacionocs/periodos/2026-I/resolucion';
    const enOcs = flujo({
      etapa: 'EN_OCS',
      nombreDeEtapa: 'Resolución del OCS',
      version: 2,
      parciales: [{ carrera: 'DS', enviado: true, pendienteDeConfirmar: false }],
      acciones: ['REGISTRAR_RESOLUCION'],
    });
    const pdf = () =>
      new File(['%PDF-1.7\nresolucion\n%%EOF\n'], 'resolucion 123.pdf', {
        type: 'application/pdf',
      });

    async function completar() {
      await userEvent.type(
        await screen.findByLabelText('Número de la resolución'),
        'RPC-SO-12-No.123-2026',
      );
      await userEvent.type(screen.getByLabelText('Fecha de la resolución'), '2026-11-14');
      await userEvent.upload(screen.getByLabelText('PDF de la resolución'), pdf());
    }

    it('con la acción, completa el formulario, confirma y envía los tres datos', async () => {
      simulada.responder('GET', RUTA, json(200, enOcs));
      simulada.responder(
        'POST',
        RESOLUCION,
        json(200, { ...enOcs, etapa: 'APROBADO', acciones: [] }),
      );
      const { alCambiar } = montar();

      const registrar = await screen.findByRole('button', { name: 'Registrar la resolución' });
      expect(registrar).toBeDisabled();
      await completar();
      expect(registrar).toBeEnabled();
      await userEvent.click(registrar);
      const dialogo = await screen.findByRole('dialog', {
        name: 'Registrar la resolución del OCS',
      });
      expect(
        within(dialogo).getByText(
          'El distributivo quedará aprobado con esta resolución. Después no se corrige.',
        ),
      ).toBeVisible();
      await userEvent.click(within(dialogo).getByRole('button', { name: 'Registrar y aprobar' }));

      await waitFor(() => expect(alCambiar).toHaveBeenCalled());
      const envio = simulada.peticiones.find(
        (una) => una.method === 'POST' && new URL(una.url).pathname === RESOLUCION,
      );
      // En jsdom, formData() no lee el archivo de la petición: se mira el cuerpo multipart.
      expect(envio?.headers.get('Content-Type')).toMatch(/^multipart\/form-data/);
      const cuerpo = (await envio?.text()) ?? '';
      expect(cuerpo).toMatch(/name="numero"\r\n\r\nRPC-SO-12-No\.123-2026\r\n/);
      expect(cuerpo).toMatch(/name="fecha"\r\n\r\n2026-11-14\r\n/);
      // El nombre del archivo se pierde al copiar la petición de jsdom a undici en la API
      // simulada («blob»); en el navegador llega. Aquí basta con que vaya la parte del archivo.
      expect(cuerpo).toMatch(/name="archivo"; filename="[^"]+"\r\nContent-Type: application\/pdf/);
    });

    it('un 400 del servidor se muestra en el formulario, sin cerrar la etapa', async () => {
      simulada.responder('GET', RUTA, json(200, enOcs));
      simulada.responder(
        'POST',
        RESOLUCION,
        json(400, {
          titulo: 'Fecha fuera de rango',
          descripcion:
            'La fecha de la resolución va desde el día en que el distributivo llegó al OCS hasta hoy.',
          codigo: 'VALIDACIONOCS_FECHA_DE_RESOLUCION_FUERA_DE_RANGO',
        }),
      );
      const { alCambiar } = montar();
      await completar();

      await userEvent.click(screen.getByRole('button', { name: 'Registrar la resolución' }));
      await userEvent.click(
        within(await screen.findByRole('dialog')).getByRole('button', {
          name: 'Registrar y aprobar',
        }),
      );

      expect(
        await screen.findByText(
          'La fecha de la resolución va desde el día en que el distributivo llegó al OCS hasta hoy.',
        ),
      ).toBeVisible();
      expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
      expect(screen.getByLabelText('Número de la resolución')).toHaveValue('RPC-SO-12-No.123-2026');
      expect(alCambiar).not.toHaveBeenCalled();
    });

    it('sin la acción no hay formulario', async () => {
      simulada.responder('GET', RUTA, json(200, { ...enOcs, acciones: [] }));
      montar();

      expect(await screen.findByText('Etapa EN_OCS')).toBeVisible();
      expect(screen.queryByLabelText('Número de la resolución')).not.toBeInTheDocument();
    });

    it('con la resolución, muestra su constancia y la descarga', async () => {
      const crear = vi.fn(() => 'blob:pdf');
      Object.assign(URL, { createObjectURL: crear, revokeObjectURL: vi.fn() });
      simulada.responder(
        'GET',
        RUTA,
        json(200, {
          ...enOcs,
          etapa: 'APROBADO',
          nombreDeEtapa: 'Aprobado',
          acciones: [],
          resolucion: {
            numero: 'RPC-SO-12-No.123-2026',
            fecha: '2026-11-14',
            registradaPor: 'secretaria.demo',
            registradaEn: '2026-11-15T15:30:00Z',
            sha256: 'b'.repeat(64),
            nombre: 'resolucion 123.pdf',
            tamano: 2048,
          },
        }),
      );
      simulada.responder(
        'GET',
        `${RESOLUCION}/archivo`,
        new Response(new Blob(['%PDF-1.7']), {
          status: 200,
          headers: {
            'Content-Type': 'application/pdf',
            'Content-Disposition': 'attachment; filename="resolucion 123.pdf"',
          },
        }),
      );
      montar();

      expect(await screen.findByText('Resolución del OCS n.º RPC-SO-12-No.123-2026')).toBeVisible();
      expect(
        screen.getByText('Del 14/11/26, registrada por secretaria.demo el 15/11/26, 10:30'),
      ).toBeVisible();
      expect(screen.getByText(/Huella del PDF: bbbbbbbbbbbb…/)).toBeVisible();
      await userEvent.click(screen.getByRole('button', { name: 'Descargar la resolución' }));
      await waitFor(() => expect(crear).toHaveBeenCalled());
    });
  });

  it('sin distributivo no muestra nada', async () => {
    simulada.responder(
      'GET',
      RUTA,
      json(404, {
        titulo: 'Sin distributivo',
        descripcion: 'El periodo todavía no tiene distributivo.',
        codigo: 'VALIDACIONOCS_SIN_DISTRIBUTIVO',
      }),
    );
    montar();

    await waitFor(() =>
      expect(simulada.peticiones.some((una) => new URL(una.url).pathname === RUTA)).toBe(true),
    );
    expect(
      screen.queryByRole('region', { name: 'Estado del distributivo' }),
    ).not.toBeInTheDocument();
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
  });
});

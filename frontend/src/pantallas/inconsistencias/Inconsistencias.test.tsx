// La pantalla «Panel de inconsistencias» (F10-H01, RF-29): las abiertas primero, con su regla y qué
// hacer; las justificadas aparte. Consolidación justifica las blandas y el Vicerrectorado solo lee.
import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { instalarApiSimulada, json, type ApiSimulada } from '../../pruebas/apiSimulada';
import { montarSpa } from '../../pruebas/montarSpa';
import { cerrarSesion, iniciarSesion } from '../../sesion/almacen';
import { CARRERAS } from '../carreras/datosDePrueba';
import { PERIODOS } from '../periodos/datosDePrueba';
import { GESTORES, PANEL, PROPORCION, SIN_ABIERTAS, SIN_DISTRIBUTIVO } from './datosDePrueba';

const CONSOLIDACION = [
  'CONSOLIDACION:LECTURA',
  'CONSOLIDACION:ESCRITURA',
  'DISTRIBUTIVO:LECTURA',
  'OFERTA:LECTURA',
];
const VICERRECTOR = ['DISTRIBUTIVO:LECTURA', 'DISTRIBUTIVO:ESCRITURA'];
const SIN_DOCENTES = ['CONSOLIDACION:LECTURA', 'OFERTA:LECTURA'];
const COORDINADOR = [
  'OFERTA:LECTURA:PROPIO',
  'OFERTA:ESCRITURA:PROPIO',
  'DISTRIBUTIVO:LECTURA:PROPIO',
];
const RUTA = '/api/generacion/periodos/2026-2/inconsistencias';
const RUTA_JUSTIFICAR = `${RUTA}/justificacion`;
const TEXTO = 'Tiene horas de tronco común este periodo';

function sesionCon(simulada: ApiSimulada, permisos: string[]) {
  simulada.responder(
    'GET',
    '/api/usuarios/sesion',
    json(200, { usuario: 'demo', nombre: 'demo (demo)', roles: [], permisos }),
  );
}

describe('Panel de inconsistencias', () => {
  let simulada: ApiSimulada;

  beforeEach(() => {
    simulada = instalarApiSimulada();
    simulada.responder('GET', '/api/academico/periodos', json(200, PERIODOS));
    simulada.responder('GET', '/api/academico/carreras', json(200, CARRERAS));
    simulada.responder('GET', RUTA, json(200, PANEL));
    iniciarSesion('cabecera.cuerpo.firma', new Date(Date.now() + 60_000));
  });

  afterEach(() => {
    cerrarSesion('cierre');
  });

  async function abrir(permisos = CONSOLIDACION) {
    sesionCon(simulada, permisos);
    montarSpa('/inconsistencias');
    return screen.findByRole('table', { name: 'Inconsistencias abiertas' });
  }

  it('muestra los totales y cada abierta con su regla, lo que afecta y qué hacer', async () => {
    const tabla = await abrir();

    expect(screen.getByText('2 duras y 1 blanda abiertas')).toBeVisible();
    const fila = within(tabla).getByRole('row', { name: /Programación · A/ });
    expect(within(fila).getByText('RF-24')).toBeVisible();
    expect(within(fila).getByText('Dura')).toBeVisible();
    expect(within(fila).getByText('Falta el docente de 4 h de clase.')).toBeVisible();
    expect(
      within(fila).getByText('El coordinador de la carrera debe proponer un docente.'),
    ).toBeVisible();
  });

  it('sus títulos tienen el tamaño de las demás pantallas', async () => {
    await abrir();

    expect(screen.getByRole('heading', { level: 1 })).toHaveClass('MuiTypography-h4');
    expect(screen.getByRole('heading', { level: 2, name: /abiertas/ })).toHaveClass(
      'MuiTypography-h5',
    );
    expect(screen.getByRole('heading', { level: 2, name: 'Justificadas' })).toHaveClass(
      'MuiTypography-h5',
    );
  });

  it('pone primero las duras, en el orden que dice el servidor', async () => {
    const tabla = await abrir();

    const filas = within(tabla).getAllByRole('row').slice(1);
    expect(filas[0]).toHaveTextContent('RF-24');
    expect(filas[1]).toHaveTextContent('RN-12');
    expect(filas[2]).toHaveTextContent('RN-05');
  });

  it('muestra las justificadas aparte, con el texto, quién y cuándo', async () => {
    await abrir();

    const justificadas = await screen.findByRole('table', { name: 'Inconsistencias justificadas' });
    expect(within(justificadas).getByText('El instituto es pequeño este año')).toBeVisible();
    expect(within(justificadas).getByText(/consolidacion\.demo/)).toBeVisible();
    expect(within(justificadas).getByText('Dictó esta materia durante tres años')).toBeVisible();
    expect(
      within(screen.getByRole('table', { name: 'Inconsistencias abiertas' })).queryByText(/RN-10/),
    ).not.toBeInTheDocument();
  });

  it('muestra el nombre de la carrera, no su código', async () => {
    const tabla = await abrir();

    expect(await within(tabla).findByText('CONTABILIDAD')).toBeVisible();
  });

  it('dice qué reglas no evalúa todavía', async () => {
    await abrir();

    expect(screen.getByText(/todavía no evalúa/)).toHaveTextContent('RN-03');
    expect(screen.getByText(/todavía no evalúa/)).toHaveTextContent('RN-16');
  });

  it('sin abiertas lo dice, y recuerda lo que no evalúa', async () => {
    simulada.responder('GET', RUTA, json(200, SIN_ABIERTAS));
    sesionCon(simulada, CONSOLIDACION);
    montarSpa('/inconsistencias');

    expect(await screen.findByText('No hay inconsistencias abiertas.')).toBeVisible();
    expect(screen.getByText(/todavía no evalúa/)).toBeVisible();
    expect(screen.queryByRole('table')).not.toBeInTheDocument();
  });

  it('sin distributivo avisa, sin error', async () => {
    simulada.responder('GET', RUTA, json(200, SIN_DISTRIBUTIVO));
    sesionCon(simulada, CONSOLIDACION);
    montarSpa('/inconsistencias');

    expect(await screen.findByText(/todavía no tiene distributivo/)).toBeVisible();
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
    expect(screen.queryByText('No hay inconsistencias abiertas.')).not.toBeInTheDocument();
  });

  it('un error no se presenta como un panel vacío', async () => {
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
    montarSpa('/inconsistencias');

    expect(await screen.findByRole('alert')).toHaveTextContent('Servicio sin respuesta');
    expect(screen.queryByText('No hay inconsistencias abiertas.')).not.toBeInTheDocument();
  });

  it('cada inconsistencia lleva a la pantalla donde se corrige', async () => {
    const tabla = await abrir();

    expect(
      within(tabla).getByRole('link', { name: 'Ir a la oferta: Programación · A' }),
    ).toHaveAttribute('href', '/oferta');
    expect(
      within(tabla).getByRole('link', { name: 'Ir a las actividades: PEREZ LUIS' }),
    ).toHaveAttribute('href', '/actividades');
    expect(
      await within(tabla).findByRole('link', { name: 'Ir a los cargos: CONTABILIDAD' }),
    ).toHaveAttribute('href', '/cargos');
  });

  it('sin permiso para la pantalla sale la indicación sin el enlace', async () => {
    const tabla = await abrir(SIN_DOCENTES);

    expect(within(tabla).queryByRole('link', { name: /Ir a los cargos/ })).not.toBeInTheDocument();
    expect(within(tabla).getByText(/dejar un solo coordinador/)).toBeVisible();
  });

  describe('justificar', () => {
    async function abrirDialogo() {
      const tabla = await abrir();
      await userEvent.click(within(tabla).getByRole('button', { name: 'Justificar: PEREZ LUIS' }));
      return screen.findByRole('dialog', { name: 'Justificar: PEREZ LUIS' });
    }

    it('guarda el texto y recarga el panel', async () => {
      simulada.responder(
        'PUT',
        RUTA_JUSTIFICAR,
        json(200, { ...PROPORCION, estado: 'JUSTIFICADA' }),
      );
      const dialogo = await abrirDialogo();
      simulada.responder('GET', RUTA, json(200, SIN_ABIERTAS));

      await userEvent.type(within(dialogo).getByLabelText(/Justificación/), TEXTO);
      await userEvent.click(within(dialogo).getByRole('button', { name: 'Guardar justificación' }));

      expect(await screen.findByText('No hay inconsistencias abiertas.')).toBeVisible();
      expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
      const peticion = simulada.peticiones.find((una) => una.method === 'PUT');
      expect(await peticion?.clone().json()).toEqual({
        tipo: 'PROPORCION',
        objeto: '0109990010',
        texto: TEXTO,
      });
    });

    it('un texto corto se corrige sin ir al servidor', async () => {
      const dialogo = await abrirDialogo();

      await userEvent.type(within(dialogo).getByLabelText(/Justificación/), 'corto');
      await userEvent.click(within(dialogo).getByRole('button', { name: 'Guardar justificación' }));

      expect(await within(dialogo).findByText(/de 10 a 500 caracteres/)).toBeVisible();
      expect(simulada.peticiones.some((una) => una.method === 'PUT')).toBe(false);
    });

    it('si el servidor responde un error, lo escrito sigue', async () => {
      simulada.responder(
        'PUT',
        RUTA_JUSTIFICAR,
        json(409, {
          titulo: 'Inconsistencia resuelta',
          descripcion: 'Esta inconsistencia ya no existe.',
          codigo: 'GENERACION_INCONSISTENCIA_NO_ENCONTRADA',
        }),
      );
      const dialogo = await abrirDialogo();

      await userEvent.type(within(dialogo).getByLabelText(/Justificación/), TEXTO);
      await userEvent.click(within(dialogo).getByRole('button', { name: 'Guardar justificación' }));

      expect(await within(dialogo).findByRole('alert')).toHaveTextContent(
        'Inconsistencia resuelta',
      );
      expect(within(dialogo).getByLabelText(/Justificación/)).toHaveValue(TEXTO);
    });

    it('una dura no lo ofrece', async () => {
      const tabla = await abrir();

      const fila = within(tabla).getByRole('row', { name: /Programación · A/ });
      expect(within(fila).queryByRole('button', { name: /Justificar/ })).not.toBeInTheDocument();
    });

    it('una justificada se puede cambiar, con su texto', async () => {
      await abrir();
      const justificadas = await screen.findByRole('table', {
        name: 'Inconsistencias justificadas',
      });

      await userEvent.click(
        within(justificadas).getByRole('button', { name: 'Cambiar la justificación: 2026-2' }),
      );

      const dialogo = await screen.findByRole('dialog');
      expect(within(dialogo).getByLabelText(/Justificación/)).toHaveValue(
        GESTORES.justificacion?.texto,
      );
    });

    it('una excepción de RN-19 no se cambia aquí', async () => {
      await abrir();
      const justificadas = await screen.findByRole('table', {
        name: 'Inconsistencias justificadas',
      });

      const fila = within(justificadas).getByRole('row', { name: /Bases de datos · B/ });
      expect(within(fila).queryByRole('button')).not.toBeInTheDocument();
    });

    it('en un periodo cerrado no sale «Justificar» en ninguna', async () => {
      simulada.responder('GET', RUTA, json(200, { ...PANEL, modificable: false }));
      await abrir();

      expect(screen.queryByRole('button', { name: /Justificar/ })).not.toBeInTheDocument();
      expect(screen.queryByRole('button', { name: /Cambiar/ })).not.toBeInTheDocument();
    });
  });

  describe('en revisión (#215)', () => {
    it('no sale «Justificar» en ninguna y avisa por qué', async () => {
      simulada.responder('GET', RUTA, json(200, { ...PANEL, enRevision: true }));
      await abrir();

      expect(
        screen.getByText(
          'El distributivo está en revisión del Vicerrectorado: el consolidado ya no cambia.',
        ),
      ).toBeVisible();
      expect(screen.queryByRole('button', { name: /Justificar/ })).not.toBeInTheDocument();
      expect(screen.queryByRole('button', { name: /Cambiar/ })).not.toBeInTheDocument();
    });
  });

  describe('con la versión congelada (F13-H01)', () => {
    it('no sale «Justificar» y el aviso es el de la versión congelada', async () => {
      simulada.responder(
        'GET',
        RUTA,
        json(200, { ...PANEL, enRevision: true, versionCongelada: true }),
      );
      await abrir();

      expect(
        screen.getByText('La versión 1 está congelada para la firma: ya no cambia.'),
      ).toBeVisible();
      expect(
        screen.queryByText(
          'El distributivo está en revisión del Vicerrectorado: el consolidado ya no cambia.',
        ),
      ).not.toBeInTheDocument();
      expect(screen.queryByRole('button', { name: /Justificar/ })).not.toBeInTheDocument();
      expect(screen.queryByRole('button', { name: /Cambiar/ })).not.toBeInTheDocument();
    });
  });

  describe('permisos', () => {
    it('el Vicerrectorado lee el mismo panel, sin justificar', async () => {
      const tabla = await abrir(VICERRECTOR);

      expect(within(tabla).getByText('RF-24')).toBeVisible();
      expect(screen.queryByRole('button', { name: /Justificar/ })).not.toBeInTheDocument();
      expect(screen.queryByRole('button', { name: /Cambiar/ })).not.toBeInTheDocument();
    });

    it('un coordinador no lo ve en el menú ni puede abrirlo', async () => {
      sesionCon(simulada, COORDINADOR);
      montarSpa('/inconsistencias');

      await waitFor(() =>
        expect(screen.queryByRole('link', { name: 'Oferta del periodo' })).toBeInTheDocument(),
      );
      expect(
        screen.queryByRole('link', { name: 'Panel de inconsistencias' }),
      ).not.toBeInTheDocument();
      expect(screen.queryByRole('table')).not.toBeInTheDocument();
    });

    it('Consolidación lo ve en el menú', async () => {
      await abrir();

      expect(screen.getByRole('link', { name: 'Panel de inconsistencias' })).toBeVisible();
    });
  });
});

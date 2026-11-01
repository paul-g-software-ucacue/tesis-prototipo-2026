// La pantalla «Actividades no lectivas» (F06-H04, RF-26): asignar, reemplazar y quitar, con el
// proyecto y el cargo que exige cada actividad. La regla está en el servidor: aquí solo se comprueba
// que la pantalla muestra lo que recibe y valida sus campos (principio XIV).
import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { instalarApiSimulada, json, type ApiSimulada } from '../../pruebas/apiSimulada';
import { montarSpa } from '../../pruebas/montarSpa';
import { cerrarSesion, iniciarSesion } from '../../sesion/almacen';
import { CARRERAS } from '../carreras/datosDePrueba';
import { RESUMENES } from '../docentes/datosDePrueba';
import { PERIODOS } from '../periodos/datosDePrueba';
import { literal } from './consultas';
import { ACTIVIDADES, CARRERA, POSIBLES, SIN_ACTIVIDADES, SIN_DISTRIBUTIVO } from './datosDePrueba';

const COORDINADOR = [
  'OFERTA:LECTURA:PROPIO',
  'OFERTA:ESCRITURA:PROPIO',
  'DISTRIBUTIVO:LECTURA:PROPIO',
  'DOCENTES:LECTURA',
];
const VICERRECTOR = ['DISTRIBUTIVO:LECTURA', 'DISTRIBUTIVO:ESCRITURA'];
const DOCENTE = '0109990010';
const RUTA = '/api/generacion/periodos/2026-2/actividades';
const RUTA_POSIBLES = `/api/generacion/periodos/2026-2/docentes/${DOCENTE}/actividades-posibles`;
const RUTA_ACTIVIDAD = (actividad: string) =>
  `/api/generacion/periodos/2026-2/parciales/${CARRERA}/docentes/${DOCENTE}/actividades/${actividad}`;
const GUARDADA = {
  actividad: { actividad: '140-b', nombre: 'Planificar', componente: 'OTRAS_DOCENCIA', horas: 4 },
  horasSuficientes: true,
};

function sesionCon(simulada: ApiSimulada, permisos: string[]) {
  simulada.responder(
    'GET',
    '/api/usuarios/sesion',
    json(200, { usuario: 'coordinacion.demo', nombre: 'Coordinación', roles: [], permisos }),
  );
}

async function cuerpoDe(simulada: ApiSimulada, metodo: string, ruta: string): Promise<unknown> {
  const peticion = simulada.peticiones.find(
    (candidata) => candidata.method === metodo && new URL(candidata.url).pathname === ruta,
  );
  if (!peticion) {
    throw new Error(`No hubo ${metodo} ${ruta}`);
  }
  return (await peticion.json()) as unknown;
}

describe('Actividades no lectivas', () => {
  let simulada: ApiSimulada;

  beforeEach(() => {
    simulada = instalarApiSimulada();
    simulada.responder('GET', '/api/academico/periodos', json(200, PERIODOS));
    simulada.responder('GET', '/api/academico/carreras', json(200, CARRERAS));
    simulada.responder('GET', '/api/docentes/docentes', json(200, RESUMENES));
    simulada.responder('GET', RUTA, json(200, SIN_ACTIVIDADES));
    simulada.responder('GET', RUTA_POSIBLES, json(200, POSIBLES));
    iniciarSesion('cabecera.cuerpo.firma', new Date(Date.now() + 60_000));
  });

  afterEach(() => {
    cerrarSesion();
  });

  async function abrirFormulario() {
    sesionCon(simulada, COORDINADOR);
    montarSpa('/actividades');
    await userEvent.click(await screen.findByRole('button', { name: 'Asignar actividad' }));
    const dialogo = await screen.findByRole('dialog', { name: 'Asignar actividad' });
    await within(dialogo).findByRole('option', { name: 'PERALTA MALDONADO JUAN ANDRES' });
    await userEvent.selectOptions(within(dialogo).getByLabelText('Docente'), DOCENTE);
    await waitFor(() =>
      expect(within(dialogo).getByRole('combobox', { name: 'Actividad' })).toBeEnabled(),
    );
    return dialogo;
  }

  async function abrirLaLista(dialogo: HTMLElement, etiqueta: string) {
    await userEvent.click(within(dialogo).getByRole('combobox', { name: etiqueta }));
    return screen.findByRole('listbox', { name: etiqueta });
  }

  async function elegir(dialogo: HTMLElement, actividad: string) {
    const lista = await abrirLaLista(dialogo, 'Actividad');
    await userEvent.click(
      within(lista).getByRole('option', {
        name: new RegExp(`^${literal(actividad).replace(')', '\\)')}`),
      }),
    );
  }

  it('está en el menú de quien lee la oferta, y no para los demás', async () => {
    sesionCon(simulada, COORDINADOR);
    montarSpa('/actividades');

    expect(
      await screen.findByRole('heading', { level: 1, name: 'Actividades no lectivas' }),
    ).toBeVisible();
    expect(screen.getByRole('link', { name: 'Actividades no lectivas' })).toBeInTheDocument();
  });

  it('sin permiso no está en el menú y la ruta no la muestra', async () => {
    sesionCon(simulada, ['USUARIOS_ROLES:LECTURA']);
    montarSpa('/actividades');

    await screen.findByRole('navigation');
    expect(screen.queryByRole('link', { name: 'Actividades no lectivas' })).not.toBeInTheDocument();
    expect(
      screen.queryByRole('heading', { level: 1, name: 'Actividades no lectivas' }),
    ).not.toBeInTheDocument();
  });

  it('el formulario agrupa por componente y no ofrece comisiones ni clases', async () => {
    const dialogo = await abrirFormulario();

    const lista = await abrirLaLista(dialogo, 'Actividad');
    expect(
      within(lista).getByRole('group', { name: 'Otras actividades de docencia' }),
    ).toBeInTheDocument();
    expect(within(lista).getByRole('group', { name: 'Gestión' })).toBeInTheDocument();
    expect(within(lista).queryByRole('option', { name: /comisi/i })).not.toBeInTheDocument();
    expect(within(lista).queryByRole('option', { name: /Impartir clases/ })).toBeNull();
  });

  it('busca la actividad por lo que escribes, sin importar las tildes', async () => {
    const dialogo = await abrirFormulario();

    await userEvent.type(
      within(dialogo).getByRole('combobox', { name: 'Actividad' }),
      'vinculacion',
    );

    const lista = await screen.findByRole('listbox', { name: 'Actividad' });
    expect(within(lista).getAllByRole('option')).toHaveLength(1);
    expect(within(lista).getByRole('option', { name: /vinculación/ })).toBeInTheDocument();
  });

  it('sin coincidencias lo dice', async () => {
    const dialogo = await abrirFormulario();

    await userEvent.type(within(dialogo).getByRole('combobox', { name: 'Actividad' }), 'zzz');

    expect(await screen.findByText('Ninguna coincide con lo que escribiste.')).toBeVisible();
  });

  it('muestra el texto completo de la actividad elegida', async () => {
    const dialogo = await abrirFormulario();

    await elegir(dialogo, '140-b');

    expect(
      within(dialogo).getByText('140 b) Planificar y actualizar contenidos de clases'),
    ).toBeVisible();
  });

  it('asigna con el docente, la actividad y las horas, cierra y recarga', async () => {
    simulada.responder('PUT', RUTA_ACTIVIDAD('140-b'), json(200, GUARDADA));
    const dialogo = await abrirFormulario();

    await elegir(dialogo, '140-b');
    await userEvent.type(within(dialogo).getByLabelText('Horas semanales'), '4,25');
    simulada.responder('GET', RUTA, json(200, ACTIVIDADES));
    await userEvent.click(within(dialogo).getByRole('button', { name: 'Asignar' }));

    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
    expect(await cuerpoDe(simulada, 'PUT', RUTA_ACTIVIDAD('140-b'))).toEqual({ horas: 4.25 });
    expect(await screen.findByText('PERALTA MALDONADO JUAN ANDRES')).toBeVisible();
  });

  it.each(['', '0', '41', '40,01', '4,125', 'abc'])(
    'con horas «%s» no se guarda y lo dice',
    async (horas) => {
      const dialogo = await abrirFormulario();

      await elegir(dialogo, '140-b');
      if (horas) {
        await userEvent.type(within(dialogo).getByLabelText('Horas semanales'), horas);
      }
      await userEvent.click(within(dialogo).getByRole('button', { name: 'Asignar' }));

      expect(await within(dialogo).findByText(/mayores que 0 y hasta 40/)).toBeVisible();
      expect(simulada.peticiones.some((peticion) => peticion.method === 'PUT')).toBe(false);
    },
  );

  it('sin actividad no se guarda y dice qué falta', async () => {
    const dialogo = await abrirFormulario();

    await userEvent.type(within(dialogo).getByLabelText('Horas semanales'), '4');
    await userEvent.click(within(dialogo).getByRole('button', { name: 'Asignar' }));

    expect(await within(dialogo).findByText(/Falta: actividad/)).toBeVisible();
    expect(simulada.peticiones.some((peticion) => peticion.method === 'PUT')).toBe(false);
  });

  it('avisa si al docente no le alcanzan las horas', async () => {
    simulada.responder(
      'PUT',
      RUTA_ACTIVIDAD('140-b'),
      json(200, { ...GUARDADA, horasSuficientes: false }),
    );
    const dialogo = await abrirFormulario();

    await elegir(dialogo, '140-b');
    await userEvent.type(within(dialogo).getByLabelText('Horas semanales'), '4');
    await userEvent.click(within(dialogo).getByRole('button', { name: 'Asignar' }));

    expect(await screen.findByText(/no le alcanzan las horas/)).toBeVisible();
  });

  it('un error del catálogo no cierra el diálogo', async () => {
    simulada.responder(
      'PUT',
      RUTA_ACTIVIDAD('140-b'),
      json(409, {
        titulo: 'Parcial cerrado',
        descripcion: 'Este parcial ya no admite cambios.',
        codigo: 'GENERACION_PARCIAL_NO_EDITABLE',
      }),
    );
    const dialogo = await abrirFormulario();

    await elegir(dialogo, '140-b');
    await userEvent.type(within(dialogo).getByLabelText('Horas semanales'), '4');
    await userEvent.click(within(dialogo).getByRole('button', { name: 'Asignar' }));

    expect(await within(dialogo).findByText('Parcial cerrado')).toBeVisible();
  });

  it('un 400 se muestra junto a las horas', async () => {
    simulada.responder(
      'PUT',
      RUTA_ACTIVIDAD('140-b'),
      json(400, {
        titulo: 'Solicitud inválida',
        descripcion: 'Revisa los campos marcados.',
        codigo: 'COMUN_SOLICITUD_INVALIDA',
        detalles: [{ campo: 'horas', descripcion: 'Motivo del servidor' }],
      }),
    );
    const dialogo = await abrirFormulario();

    await elegir(dialogo, '140-b');
    await userEvent.type(within(dialogo).getByLabelText('Horas semanales'), '4');
    await userEvent.click(within(dialogo).getByRole('button', { name: 'Asignar' }));

    expect(await within(dialogo).findByText(/Motivo del servidor/)).toBeVisible();
    expect(within(dialogo).getByLabelText('Horas semanales')).toHaveAttribute(
      'aria-invalid',
      'true',
    );
  });

  it('quita con confirmación, mandando el proyecto', async () => {
    simulada.responder('GET', RUTA, json(200, ACTIVIDADES));
    simulada.responder('DELETE', RUTA_ACTIVIDAD('141-a'), json(204));
    sesionCon(simulada, COORDINADOR);
    montarSpa('/actividades');

    await userEvent.click(
      await screen.findByRole('button', { name: 'Quitar 141-a de PERALTA MALDONADO JUAN ANDRES' }),
    );
    const confirmar = await screen.findByRole('dialog', { name: /^Quitar 141-a/ });
    simulada.responder('GET', RUTA, json(200, SIN_ACTIVIDADES));
    await userEvent.click(within(confirmar).getByRole('button', { name: 'Quitar' }));

    await screen.findByText('Todavía no hay actividades en este parcial.');
    const borrado = simulada.peticiones.find((peticion) => peticion.method === 'DELETE');
    expect(borrado && new URL(borrado.url).search).toBe('?proyecto=INV-2025-01');
  });

  it('un error al quitar se muestra', async () => {
    simulada.responder('GET', RUTA, json(200, ACTIVIDADES));
    simulada.responder(
      'DELETE',
      RUTA_ACTIVIDAD('140-b'),
      json(409, {
        titulo: 'Periodo cerrado',
        descripcion: 'El periodo está cerrado.',
        codigo: 'GENERACION_PERIODO_CERRADO',
      }),
    );
    sesionCon(simulada, COORDINADOR);
    montarSpa('/actividades');

    await userEvent.click(
      await screen.findByRole('button', { name: 'Quitar 140-b de PERALTA MALDONADO JUAN ANDRES' }),
    );
    await userEvent.click(
      within(await screen.findByRole('dialog')).getByRole('button', { name: 'Quitar' }),
    );

    expect(await screen.findByText('Periodo cerrado')).toBeVisible();
  });

  it('sin distributivo lo dice y no ofrece asignar', async () => {
    simulada.responder('GET', RUTA, json(200, SIN_DISTRIBUTIVO));
    sesionCon(simulada, COORDINADOR);
    montarSpa('/actividades');

    expect(await screen.findByText(/todavía no tiene distributivo/)).toBeVisible();
    expect(screen.queryByRole('button', { name: 'Asignar actividad' })).not.toBeInTheDocument();
  });

  it('quien no escribe ve la lista sin acciones', async () => {
    simulada.responder('GET', RUTA, json(200, ACTIVIDADES));
    sesionCon(simulada, VICERRECTOR);
    montarSpa('/actividades');

    await screen.findByText('PERALTA MALDONADO JUAN ANDRES');
    expect(screen.queryByRole('button', { name: 'Asignar actividad' })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /^Quitar/ })).not.toBeInTheDocument();
  });

  it('con el periodo cerrado no ofrece asignar ni quitar', async () => {
    simulada.responder('GET', RUTA, json(200, ACTIVIDADES));
    simulada.responder(
      'GET',
      '/api/academico/periodos',
      json(
        200,
        PERIODOS.map((periodo) =>
          periodo.codigo === '2026-2' ? { ...periodo, estado: 'CERRADO' } : periodo,
        ),
      ),
    );
    sesionCon(simulada, COORDINADOR);
    montarSpa('/actividades');

    await userEvent.selectOptions(await screen.findByLabelText('Periodo'), '2026-2');
    await screen.findByText('PERALTA MALDONADO JUAN ANDRES');
    expect(screen.queryByRole('button', { name: 'Asignar actividad' })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /^Quitar/ })).not.toBeInTheDocument();
  });

  it('si Generación falla, muestra el error sin una lista falsa', async () => {
    simulada.responder(
      'GET',
      RUTA,
      json(503, {
        titulo: 'Servicio no disponible',
        descripcion: 'Intenta de nuevo en unos minutos.',
        codigo: 'COMUN_SERVICIO_NO_DISPONIBLE',
      }),
    );
    sesionCon(simulada, COORDINADOR);
    montarSpa('/actividades');

    expect(await screen.findByText('Servicio no disponible')).toBeVisible();
    expect(screen.queryByRole('button', { name: 'Asignar actividad' })).not.toBeInTheDocument();
  });

  describe('proyecto', () => {
    it('una actividad con proyectos pide elegir uno y lo manda', async () => {
      simulada.responder('PUT', RUTA_ACTIVIDAD('141-a'), json(200, GUARDADA));
      const dialogo = await abrirFormulario();

      await elegir(dialogo, '141-a');
      await within(dialogo).findByRole('combobox', { name: 'Proyecto' });
      const lista = await abrirLaLista(dialogo, 'Proyecto');
      await userEvent.click(within(lista).getByRole('option', { name: /^INV-2025-01/ }));
      await userEvent.type(within(dialogo).getByLabelText('Horas semanales'), '6');
      await userEvent.click(within(dialogo).getByRole('button', { name: 'Asignar' }));

      await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
      expect(await cuerpoDe(simulada, 'PUT', RUTA_ACTIVIDAD('141-a'))).toEqual({
        horas: 6,
        proyecto: 'INV-2025-01',
      });
    });

    it('sin elegir el proyecto no se guarda', async () => {
      const dialogo = await abrirFormulario();

      await elegir(dialogo, '141-a');
      await userEvent.type(within(dialogo).getByLabelText('Horas semanales'), '6');
      await userEvent.click(within(dialogo).getByRole('button', { name: 'Asignar' }));

      expect(await within(dialogo).findByText(/Falta: proyecto/)).toBeVisible();
      expect(simulada.peticiones.some((peticion) => peticion.method === 'PUT')).toBe(false);
    });

    it('sin proyectos dice por qué y no deja guardar', async () => {
      const dialogo = await abrirFormulario();

      await elegir(dialogo, '142-a');
      await userEvent.type(within(dialogo).getByLabelText('Horas semanales'), '6');

      expect(await within(dialogo).findByText(/no tiene proyectos asignables/)).toBeVisible();
      expect(within(dialogo).getByRole('button', { name: 'Asignar' })).toBeDisabled();
      expect(within(dialogo).queryByRole('combobox', { name: 'Proyecto' })).toBeNull();
    });

    it('una actividad sin exigencias no pide proyecto', async () => {
      const dialogo = await abrirFormulario();

      await elegir(dialogo, '140-b');

      expect(within(dialogo).queryByRole('combobox', { name: 'Proyecto' })).toBeNull();
    });
  });

  describe('gestión', () => {
    it('una actividad de cargo no disponible muestra su motivo y no deja guardar', async () => {
      const dialogo = await abrirFormulario();

      await elegir(dialogo, '143-d');

      expect(await within(dialogo).findByText(/no tiene vigente el cargo/)).toBeVisible();
      expect(within(dialogo).getByRole('button', { name: 'Asignar' })).toBeDisabled();
    });

    it('una actividad de gestión sin cargo se puede guardar', async () => {
      const dialogo = await abrirFormulario();

      await elegir(dialogo, '143-k');

      expect(within(dialogo).getByRole('button', { name: 'Asignar' })).toBeEnabled();
    });
  });

  describe('la lista', () => {
    it('cada docente con sus actividades, sus horas, su proyecto y la suma por componente', async () => {
      simulada.responder('GET', RUTA, json(200, ACTIVIDADES));
      sesionCon(simulada, COORDINADOR);
      montarSpa('/actividades');

      const tabla = await screen.findByRole('table', {
        name: 'Actividades de DESARROLLO DE SOFTWARE',
      });
      const fila = within(tabla).getByText('PERALTA MALDONADO JUAN ANDRES').closest('tr');
      expect(fila).toHaveTextContent(
        '140 b) Planificar y actualizar contenidos de clases · 4,25 h',
      );
      expect(fila).toHaveTextContent('141 a) Diseñar y ejecutar proyectos de investigación · 6 h');
      expect(fila).toHaveTextContent('Proyecto: Monitoreo de hatos');
      expect(fila).toHaveTextContent('Otras actividades de docencia: 4,25 h');
      expect(fila).toHaveTextContent('Investigación: 6 h');
      expect(fila).not.toHaveTextContent('Gestión');
    });

    it('un parcial sin actividades lo dice', async () => {
      sesionCon(simulada, COORDINADOR);
      montarSpa('/actividades');

      expect(await screen.findByText('Todavía no hay actividades en este parcial.')).toBeVisible();
    });

    it('el alcance total ve varias carreras', async () => {
      simulada.responder(
        'GET',
        RUTA,
        json(200, {
          creado: true,
          carreras: [...ACTIVIDADES.carreras, { carrera: 'OTRA-CARRERA', docentes: [] }],
        }),
      );
      sesionCon(simulada, VICERRECTOR);
      montarSpa('/actividades');

      expect(
        await screen.findByRole('heading', { level: 2, name: 'DESARROLLO DE SOFTWARE' }),
      ).toBeVisible();
      expect(screen.getByRole('heading', { level: 2, name: 'OTRA-CARRERA' })).toBeVisible();
    });
  });
});

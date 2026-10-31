import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { instalarApiSimulada, json, type ApiSimulada } from '../../pruebas/apiSimulada';
import { montarSpa } from '../../pruebas/montarSpa';
import { cerrarSesion, iniciarSesion } from '../../sesion/almacen';
import type { Carrera } from '../carreras/consultas';
import { CARRERAS } from '../carreras/datosDePrueba';
import {
  ADMINISTRADOR,
  ADMINISTRADOR_SESION,
  AUDITOR,
  FUNCIONALIDADES,
  REVISOR_ASIGNADO,
  ROLES_DE_USUARIOS,
  SOLO_LECTURA_SESION,
  USUARIOS,
} from './datosDePrueba';

async function cuerpoDe(simulada: ApiSimulada, metodo: string, ruta: string): Promise<unknown> {
  const peticion = simulada.peticiones.find(
    (candidata) => candidata.method === metodo && new URL(candidata.url).pathname === ruta,
  );
  if (!peticion) {
    throw new Error(`No hubo ${metodo} ${ruta}`);
  }
  return (await peticion.json()) as unknown;
}

function carreraQueCumple(condicion: (carrera: Carrera) => boolean): Carrera {
  const carrera = CARRERAS.find(condicion);
  if (!carrera) {
    throw new Error('Los datos de prueba no tienen esa carrera');
  }
  return carrera;
}

const VIGENTE = carreraQueCumple((carrera) => carrera.estado === 'VIGENTE');
const NO_VIGENTE = carreraQueCumple((carrera) => carrera.estado !== 'VIGENTE');

describe('Roles', () => {
  let simulada: ApiSimulada;

  beforeEach(() => {
    simulada = instalarApiSimulada();
    simulada.responder('GET', '/api/roles/funcionalidades', json(200, FUNCIONALIDADES));
    simulada.responder('GET', '/api/roles', json(200, [ADMINISTRADOR, AUDITOR, REVISOR_ASIGNADO]));
    simulada.responder('GET', '/api/roles/usuarios', json(200, ROLES_DE_USUARIOS));
    simulada.responder('GET', '/api/usuarios/usuarios', json(200, USUARIOS));
    simulada.responder('GET', '/api/academico/carreras', json(200, CARRERAS));
    iniciarSesion('cabecera.cuerpo.firma', new Date(Date.now() + 60_000));
  });

  afterEach(() => {
    cerrarSesion();
  });

  describe('con el administrador', () => {
    beforeEach(() => {
      simulada.responder('GET', '/api/usuarios/sesion', json(200, ADMINISTRADOR_SESION));
    });

    it('lista los roles y marca los de la matriz D-16', async () => {
      montarSpa('/roles');

      const tabla = await screen.findByRole('table', { name: 'Roles' });
      const filas = within(tabla).getAllByRole('row');
      expect(filas).toHaveLength(4);
      expect(filas[1]).toHaveTextContent('Administrador del sistema');
      expect(filas[1]).toHaveTextContent('Matriz D-16');
      expect(filas[2]).toHaveTextContent('Auditor de cargas');
      expect(filas[2]).not.toHaveTextContent('Matriz D-16');
      expect(document.title).toBe('Roles · MDD');
      expect(screen.getByRole('link', { name: 'Roles' })).toHaveAttribute('aria-current', 'page');
    });

    it('el detalle de un rol de D-16 muestra su matriz en solo lectura', async () => {
      const usuario = userEvent.setup();
      montarSpa('/roles');

      await usuario.click(
        await screen.findByRole('button', { name: 'Ver Administrador del sistema' }),
      );

      const dialogo = await screen.findByRole('dialog', { name: 'Administrador del sistema' });
      expect(dialogo).toHaveTextContent(
        'Los roles de la matriz D-16 no se pueden cambiar ni borrar.',
      );
      expect(
        within(dialogo).getByRole('cell', { name: 'Usuarios y roles: escritura' }),
      ).toHaveTextContent('Todo');
      expect(within(dialogo).queryByRole('combobox')).not.toBeInTheDocument();
      expect(within(dialogo).queryByRole('button', { name: 'Guardar' })).not.toBeInTheDocument();
      expect(within(dialogo).queryByRole('button', { name: 'Borrar rol' })).not.toBeInTheDocument();
    });

    it('crea un rol con un selector por cada tipo que admite cada funcionalidad', async () => {
      const usuario = userEvent.setup();
      simulada.responder(
        'POST',
        '/api/roles',
        json(201, { ...AUDITOR, identificador: 'AUDITOR_DE_ROLES', nombre: 'Auditor de roles' }),
      );
      montarSpa('/roles');

      await usuario.click(await screen.findByRole('button', { name: 'Crear rol' }));
      const dialogo = await screen.findByRole('dialog', { name: 'Rol nuevo' });
      // Votación del OCS solo admite aprobación: no tiene selector de lectura.
      expect(within(dialogo).getAllByRole('combobox')).toHaveLength(7);
      expect(
        within(dialogo).queryByRole('combobox', { name: 'Votación del OCS: lectura' }),
      ).not.toBeInTheDocument();
      await usuario.type(within(dialogo).getByLabelText('Nombre'), 'Auditor de roles');
      await usuario.type(within(dialogo).getByLabelText('Descripción'), 'Ve los roles.');
      await usuario.selectOptions(
        within(dialogo).getByRole('combobox', { name: 'Usuarios y roles: lectura' }),
        'TODO',
      );
      await usuario.selectOptions(
        within(dialogo).getByRole('combobox', { name: 'Docentes: lectura' }),
        'PROPIO',
      );
      await usuario.click(within(dialogo).getByRole('button', { name: 'Guardar' }));

      await waitFor(() =>
        expect(screen.queryByRole('dialog', { name: 'Rol nuevo' })).not.toBeInTheDocument(),
      );
      expect(await cuerpoDe(simulada, 'POST', '/api/roles')).toEqual({
        nombre: 'Auditor de roles',
        descripcion: 'Ve los roles.',
        permisos: [
          { funcionalidad: 'USUARIOS_ROLES', tipo: 'LECTURA', alcance: 'TODO' },
          { funcionalidad: 'DOCENTES', tipo: 'LECTURA', alcance: 'PROPIO' },
        ],
      });
    });

    it('sin ningún permiso marcado, no deja guardar y lo dice', async () => {
      const usuario = userEvent.setup();
      montarSpa('/roles');

      await usuario.click(await screen.findByRole('button', { name: 'Crear rol' }));
      const dialogo = await screen.findByRole('dialog', { name: 'Rol nuevo' });
      await usuario.type(within(dialogo).getByLabelText('Nombre'), 'Vacío');
      await usuario.type(within(dialogo).getByLabelText('Descripción'), 'Sin nada.');
      await usuario.click(within(dialogo).getByRole('button', { name: 'Guardar' }));

      expect(within(dialogo).getByText('El rol necesita al menos un permiso.')).toBeInTheDocument();
      expect(simulada.peticiones.some((peticion) => peticion.method === 'POST')).toBe(false);
    });

    it('muestra el 409 y el 400 del servidor con su título y su detalle, sin el código', async () => {
      const usuario = userEvent.setup();
      let intento = 0;
      simulada.responder('POST', '/api/roles', () => {
        intento += 1;
        return intento === 1
          ? json(409, {
              titulo: 'Nombre repetido',
              descripcion:
                'Ya existe un rol con ese nombre, o con uno que solo cambia en mayúsculas, tildes o signos. Elige otro.',
              codigo: 'ROLES_NOMBRE_REPETIDO',
            })
          : json(400, {
              titulo: 'Solicitud inválida',
              descripcion: 'Revisa los campos marcados y vuelve a intentarlo.',
              codigo: 'COMUN_SOLICITUD_INVALIDA',
              detalles: [
                {
                  campo: 'permisos',
                  descripcion: '«Cargas de datos» no admite el permiso de firma.',
                },
              ],
            });
      });
      montarSpa('/roles');

      await usuario.click(await screen.findByRole('button', { name: 'Crear rol' }));
      const dialogo = await screen.findByRole('dialog', { name: 'Rol nuevo' });
      await usuario.type(within(dialogo).getByLabelText('Nombre'), 'Administrador');
      await usuario.type(within(dialogo).getByLabelText('Descripción'), 'Otro.');
      await usuario.selectOptions(
        within(dialogo).getByRole('combobox', { name: 'Cargas de datos: lectura' }),
        'TODO',
      );
      await usuario.click(within(dialogo).getByRole('button', { name: 'Guardar' }));
      expect(await within(dialogo).findByRole('alert')).toHaveTextContent('Nombre repetido');

      await usuario.click(within(dialogo).getByRole('button', { name: 'Guardar' }));
      expect(
        await within(dialogo).findByText('«Cargas de datos» no admite el permiso de firma.'),
      ).toBeInTheDocument();
      expect(document.body).not.toHaveTextContent('ROLES_');
      expect(document.body).not.toHaveTextContent('COMUN_');
    });

    it('edita un rol propio y lo borra si no está asignado', async () => {
      const usuario = userEvent.setup();
      simulada.responder('PUT', '/api/roles/AUDITOR_DE_CARGAS', json(200, AUDITOR));
      simulada.responder(
        'DELETE',
        '/api/roles/AUDITOR_DE_CARGAS',
        new Response(null, { status: 204 }),
      );
      montarSpa('/roles');

      await usuario.click(await screen.findByRole('button', { name: 'Ver Auditor de cargas' }));
      let dialogo = await screen.findByRole('dialog', { name: 'Auditor de cargas' });
      expect(
        within(dialogo).getByRole('combobox', { name: 'Cargas de datos: lectura' }),
      ).toHaveValue('TODO');
      await usuario.selectOptions(
        within(dialogo).getByRole('combobox', { name: 'Docentes: lectura' }),
        'TODO',
      );
      await usuario.click(within(dialogo).getByRole('button', { name: 'Guardar' }));
      await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
      const editado = (await cuerpoDe(simulada, 'PUT', '/api/roles/AUDITOR_DE_CARGAS')) as {
        permisos: unknown[];
      };
      expect(editado.permisos).toHaveLength(2);

      await usuario.click(screen.getByRole('button', { name: 'Ver Auditor de cargas' }));
      dialogo = await screen.findByRole('dialog', { name: 'Auditor de cargas' });
      await usuario.click(within(dialogo).getByRole('button', { name: 'Borrar rol' }));
      await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
      expect(
        simulada.peticiones.some(
          (peticion) =>
            peticion.method === 'DELETE' &&
            new URL(peticion.url).pathname === '/api/roles/AUDITOR_DE_CARGAS',
        ),
      ).toBe(true);
    });

    it('un rol propio asignado dice a cuántos usuarios y no deja borrarlo', async () => {
      const usuario = userEvent.setup();
      montarSpa('/roles');

      await usuario.click(await screen.findByRole('button', { name: 'Ver Revisor' }));
      const dialogo = await screen.findByRole('dialog', { name: 'Revisor' });

      expect(dialogo).toHaveTextContent('Asignado a 2 usuarios. Quítaselo antes de borrarlo.');
      expect(within(dialogo).getByRole('button', { name: 'Borrar rol' })).toBeDisabled();
    });

    it('la vista de usuarios muestra los roles de cada uno y los cambia', async () => {
      const usuario = userEvent.setup();
      simulada.responder(
        'PUT',
        '/api/roles/usuarios/docente.demo',
        json(200, { usuario: 'docente.demo', roles: ['AUDITOR_DE_CARGAS', 'REVISOR'] }),
      );
      montarSpa('/roles');

      await usuario.click(await screen.findByRole('tab', { name: 'Usuarios' }));
      const tabla = await screen.findByRole('table', { name: 'Usuarios y sus roles' });
      const docente = within(tabla).getByRole('row', { name: /docente\.demo/ });
      expect(docente).toHaveTextContent('Auditor de cargas');
      await usuario.click(
        within(docente).getByRole('button', { name: 'Cambiar roles de docente.demo' }),
      );

      const dialogo = await screen.findByRole('dialog', { name: 'Roles de docente.demo' });
      expect(within(dialogo).getByRole('checkbox', { name: 'Auditor de cargas' })).toBeChecked();
      await usuario.click(within(dialogo).getByRole('checkbox', { name: 'Revisor' }));
      await usuario.click(within(dialogo).getByRole('button', { name: 'Guardar' }));

      await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
      expect(await cuerpoDe(simulada, 'PUT', '/api/roles/usuarios/docente.demo')).toEqual({
        roles: ['AUDITOR_DE_CARGAS', 'REVISOR'],
      });
    });

    describe('la carrera del coordinador', () => {
      const CON_CARRERA = [
        ROLES_DE_USUARIOS[0],
        ROLES_DE_USUARIOS[1],
        { ...ROLES_DE_USUARIOS[2], carrera: VIGENTE.codigo },
      ];

      async function tablaDeUsuarios(usuario: ReturnType<typeof userEvent.setup>) {
        montarSpa('/roles');
        await usuario.click(await screen.findByRole('tab', { name: 'Usuarios' }));
        return screen.findByRole('table', { name: 'Usuarios y sus roles' });
      }

      it('se ve junto a los roles', async () => {
        simulada.responder('GET', '/api/roles/usuarios', json(200, CON_CARRERA));
        const tabla = await tablaDeUsuarios(userEvent.setup());

        expect(within(tabla).getByRole('row', { name: /coordinacion\.demo/ })).toHaveTextContent(
          VIGENTE.nombre,
        );
      });

      it('sin carrera, la fila del coordinador dice que falta', async () => {
        const tabla = await tablaDeUsuarios(userEvent.setup());

        expect(within(tabla).getByRole('row', { name: /coordinacion\.demo/ })).toHaveTextContent(
          'Sin carrera',
        );
      });

      it('solo los coordinadores ofrecen indicar la carrera', async () => {
        const tabla = await tablaDeUsuarios(userEvent.setup());

        expect(
          within(tabla).getByRole('button', { name: 'Indicar la carrera de coordinacion.demo' }),
        ).toBeInTheDocument();
        expect(
          within(tabla).queryByRole('button', { name: /carrera de docente\.demo/ }),
        ).not.toBeInTheDocument();
      });

      it('la lista trae solo las carreras vigentes, y dice cuándo vale el cambio', async () => {
        const usuario = userEvent.setup();
        const tabla = await tablaDeUsuarios(usuario);
        await usuario.click(
          within(tabla).getByRole('button', { name: 'Indicar la carrera de coordinacion.demo' }),
        );

        const dialogo = await screen.findByRole('dialog', {
          name: 'Carrera de coordinacion.demo',
        });
        expect(within(dialogo).getByRole('option', { name: VIGENTE.nombre })).toBeInTheDocument();
        expect(
          within(dialogo).queryByRole('option', { name: NO_VIGENTE.nombre }),
        ).not.toBeInTheDocument();
        expect(within(dialogo).getByText(/próxima renovación de su sesión/)).toBeInTheDocument();
      });

      it('la indica y recarga la lista', async () => {
        simulada.responder(
          'PUT',
          '/api/roles/usuarios/coordinacion.demo/carrera',
          json(200, CON_CARRERA[2]),
        );
        const usuario = userEvent.setup();
        const tabla = await tablaDeUsuarios(usuario);
        await usuario.click(
          within(tabla).getByRole('button', { name: 'Indicar la carrera de coordinacion.demo' }),
        );
        const dialogo = await screen.findByRole('dialog', {
          name: 'Carrera de coordinacion.demo',
        });

        await usuario.selectOptions(
          within(dialogo).getByLabelText('Carrera'),
          within(dialogo).getByRole('option', { name: VIGENTE.nombre }),
        );
        await usuario.click(within(dialogo).getByRole('button', { name: 'Guardar' }));

        await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
        expect(
          await cuerpoDe(simulada, 'PUT', '/api/roles/usuarios/coordinacion.demo/carrera'),
        ).toEqual({ carrera: VIGENTE.codigo });
      });

      it('sin elegir una carrera no deja guardar', async () => {
        const usuario = userEvent.setup();
        const tabla = await tablaDeUsuarios(usuario);
        await usuario.click(
          within(tabla).getByRole('button', { name: 'Indicar la carrera de coordinacion.demo' }),
        );

        const dialogo = await screen.findByRole('dialog', {
          name: 'Carrera de coordinacion.demo',
        });
        expect(within(dialogo).getByRole('button', { name: 'Guardar' })).toBeDisabled();
        expect(
          within(dialogo).queryByRole('button', { name: 'Quitar carrera' }),
        ).not.toBeInTheDocument();
      });

      it('la quita', async () => {
        simulada.responder('GET', '/api/roles/usuarios', json(200, CON_CARRERA));
        simulada.responder(
          'DELETE',
          '/api/roles/usuarios/coordinacion.demo/carrera',
          json(200, ROLES_DE_USUARIOS[2]),
        );
        const usuario = userEvent.setup();
        const tabla = await tablaDeUsuarios(usuario);
        await usuario.click(
          within(tabla).getByRole('button', { name: 'Indicar la carrera de coordinacion.demo' }),
        );
        const dialogo = await screen.findByRole('dialog', {
          name: 'Carrera de coordinacion.demo',
        });

        await usuario.click(within(dialogo).getByRole('button', { name: 'Quitar carrera' }));

        await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
        expect(simulada.peticiones.some((peticion) => peticion.method === 'DELETE')).toBe(true);
      });

      it('un 409 muestra el aviso del catálogo y no cierra', async () => {
        simulada.responder(
          'PUT',
          '/api/roles/usuarios/coordinacion.demo/carrera',
          json(409, {
            titulo: 'Usuario sin rol de coordinador',
            descripcion:
              'Solo un usuario con el rol Coordinador de carrera puede tener carrera. Dale ese rol primero.',
            codigo: 'ROLES_CARRERA_SIN_ROL',
          }),
        );
        const usuario = userEvent.setup();
        const tabla = await tablaDeUsuarios(usuario);
        await usuario.click(
          within(tabla).getByRole('button', { name: 'Indicar la carrera de coordinacion.demo' }),
        );
        const dialogo = await screen.findByRole('dialog', {
          name: 'Carrera de coordinacion.demo',
        });

        await usuario.selectOptions(
          within(dialogo).getByLabelText('Carrera'),
          within(dialogo).getByRole('option', { name: VIGENTE.nombre }),
        );
        await usuario.click(within(dialogo).getByRole('button', { name: 'Guardar' }));

        expect(
          await within(dialogo).findByText('Usuario sin rol de coordinador'),
        ).toBeInTheDocument();
        expect(screen.getByRole('dialog')).toBeInTheDocument();
      });
    });

    it('si el cambio dejaría al sistema sin administrador, lo dice y no cierra', async () => {
      const usuario = userEvent.setup();
      simulada.responder(
        'PUT',
        '/api/roles/usuarios/admin.demo',
        json(409, {
          titulo: 'Sin administrador',
          descripcion: 'El cambio dejaría al sistema sin nadie que administre los roles.',
          codigo: 'ROLES_SIN_ADMINISTRADOR',
        }),
      );
      montarSpa('/roles');

      await usuario.click(await screen.findByRole('tab', { name: 'Usuarios' }));
      await usuario.click(
        await screen.findByRole('button', { name: 'Cambiar roles de admin.demo' }),
      );
      const dialogo = await screen.findByRole('dialog', { name: 'Roles de admin.demo' });
      await usuario.click(
        within(dialogo).getByRole('checkbox', { name: 'Administrador del sistema' }),
      );
      await usuario.click(within(dialogo).getByRole('button', { name: 'Guardar' }));

      expect(await within(dialogo).findByRole('alert')).toHaveTextContent('Sin administrador');
    });
  });

  describe('con solo lectura de «Usuarios y roles»', () => {
    beforeEach(() => {
      simulada.responder('GET', '/api/usuarios/sesion', json(200, SOLO_LECTURA_SESION));
    });

    it('ve la carrera, pero no la indica', async () => {
      const usuario = userEvent.setup();
      montarSpa('/roles');

      await usuario.click(await screen.findByRole('tab', { name: 'Usuarios' }));
      const tabla = await screen.findByRole('table', { name: 'Usuarios y sus roles' });
      expect(within(tabla).getByRole('row', { name: /coordinacion\.demo/ })).toHaveTextContent(
        'Sin carrera',
      );
      expect(screen.queryByRole('button', { name: /^Indicar la carrera/ })).not.toBeInTheDocument();
    });

    it('ve los roles y los usuarios, pero no crear, editar, borrar ni asignar', async () => {
      const usuario = userEvent.setup();
      montarSpa('/roles');

      await screen.findByRole('table', { name: 'Roles' });
      expect(screen.queryByRole('button', { name: 'Crear rol' })).not.toBeInTheDocument();
      await usuario.click(screen.getByRole('button', { name: 'Ver Auditor de cargas' }));
      const dialogo = await screen.findByRole('dialog', { name: 'Auditor de cargas' });
      expect(within(dialogo).queryByRole('combobox')).not.toBeInTheDocument();
      expect(within(dialogo).queryByRole('button', { name: 'Guardar' })).not.toBeInTheDocument();
      expect(within(dialogo).queryByRole('button', { name: 'Borrar rol' })).not.toBeInTheDocument();
      await usuario.click(within(dialogo).getByRole('button', { name: 'Cerrar' }));

      await usuario.click(screen.getByRole('tab', { name: 'Usuarios' }));
      await screen.findByRole('table', { name: 'Usuarios y sus roles' });
      expect(screen.queryByRole('button', { name: /Cambiar roles/ })).not.toBeInTheDocument();
    });

    it('un 403 de la API se muestra con su título y su descripción, sin el código', async () => {
      simulada.responder(
        'GET',
        '/api/roles',
        json(403, {
          titulo: 'Acceso no autorizado',
          descripcion:
            'No tienes permiso para esta acción. Si crees que deberías tenerlo, pide al administrador que revise tus roles.',
          codigo: 'SEGURIDAD_ACCESO_NO_AUTORIZADO',
        }),
      );
      montarSpa('/roles');

      expect(await screen.findByRole('alert')).toHaveTextContent('Acceso no autorizado');
      expect(document.body).not.toHaveTextContent('SEGURIDAD_');
      expect(document.body).not.toHaveTextContent('403');
    });
  });

  describe('sin ningún permiso de «Usuarios y roles»', () => {
    beforeEach(() => {
      simulada.responder(
        'GET',
        '/api/usuarios/sesion',
        json(200, {
          usuario: 'docente.demo',
          roles: ['DOCENTE'],
          permisos: ['MI_DISTRIBUTIVO:ESCRITURA:PROPIO', 'MI_DISTRIBUTIVO:LECTURA:PROPIO'],
        }),
      );
    });

    it('/roles muestra «Acceso no autorizado» sin pedir los roles a la API', async () => {
      montarSpa('/roles');

      expect(await screen.findByRole('alert')).toHaveTextContent('Acceso no autorizado');
      expect(screen.getByRole('alert')).toHaveTextContent(
        'No tienes permiso para esta acción. Si crees que deberías tenerlo, pide al administrador que revise tus roles.',
      );
      expect(screen.queryByRole('link', { name: 'Roles' })).not.toBeInTheDocument();
      expect(
        simulada.peticiones.some((peticion) =>
          new URL(peticion.url).pathname.startsWith('/api/roles'),
        ),
      ).toBe(false);
    });
  });
});

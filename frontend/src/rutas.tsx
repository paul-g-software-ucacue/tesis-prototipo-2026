// Las rutas de la SPA (ADR-019): el inicio de sesión es público; lo demás va dentro del marco y
// protegido (D-50). Cada historia agrega sus rutas como hijas del marco.
import type { RouteObject } from 'react-router';
import { Marco } from './navegacion/Marco';
import { IniciarSesion } from './pantallas/IniciarSesion';
import { Inicio } from './pantallas/Inicio';
import { NoEncontrada } from './pantallas/NoEncontrada';
import { Normativa } from './pantallas/normativa/Normativa';
import { Roles } from './pantallas/roles/Roles';
import { RutaConPermiso } from './sesion/RutaConPermiso';
import { RutaProtegida } from './sesion/RutaProtegida';

export const rutasDeLaSpa: RouteObject[] = [
  { path: '/iniciar-sesion', element: <IniciarSesion /> },
  {
    element: (
      <RutaProtegida>
        <Marco />
      </RutaProtegida>
    ),
    children: [
      { index: true, element: <Inicio /> },
      {
        path: 'roles',
        element: (
          <RutaConPermiso funcionalidad="USUARIOS_ROLES" tipo="LECTURA">
            <Roles />
          </RutaConPermiso>
        ),
      },
      {
        path: 'normativa',
        element: (
          <RutaConPermiso funcionalidad="PARAMETROS" tipo="LECTURA">
            <Normativa />
          </RutaConPermiso>
        ),
      },
    ],
  },
  { path: '*', element: <NoEncontrada /> },
];

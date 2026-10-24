// Las rutas de la SPA (ADR-019): el inicio de sesión es público; lo demás va dentro del marco y
// protegido (D-50). Cada historia agrega sus rutas como hijas del marco.
import type { RouteObject } from 'react-router';
import { Marco } from './navegacion/Marco';
import { IniciarSesion } from './pantallas/IniciarSesion';
import { Inicio } from './pantallas/Inicio';
import { NoEncontrada } from './pantallas/NoEncontrada';
import { Normativa } from './pantallas/normativa/Normativa';
import { Carreras } from './pantallas/carreras/Carreras';
import { Estructura } from './pantallas/estructura/Estructura';
import { Oferta } from './pantallas/oferta/Oferta';
import { Plantillas } from './pantallas/plantillas/Plantillas';
import { Periodos } from './pantallas/periodos/Periodos';
import { Roles } from './pantallas/roles/Roles';
import { RutaConPermiso } from './sesion/RutaConPermiso';
import { RutaProtegida } from './sesion/RutaProtegida';
import {
  DESCARGA_DE_PLANTILLAS,
  LECTURA_DE_LA_ESTRUCTURA,
  LECTURA_DE_LA_OFERTA,
} from './sesion/funcionalidades';

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
      {
        path: 'periodos',
        element: (
          <RutaConPermiso funcionalidad="DISTRIBUTIVO" tipo="LECTURA">
            <Periodos />
          </RutaConPermiso>
        ),
      },
      {
        path: 'estructura',
        element: (
          <RutaConPermiso cualquieraDe={LECTURA_DE_LA_ESTRUCTURA}>
            <Estructura />
          </RutaConPermiso>
        ),
      },
      {
        path: 'carreras',
        element: (
          <RutaConPermiso cualquieraDe={LECTURA_DE_LA_ESTRUCTURA}>
            <Carreras />
          </RutaConPermiso>
        ),
      },
      {
        path: 'oferta',
        element: (
          <RutaConPermiso cualquieraDe={LECTURA_DE_LA_OFERTA}>
            <Oferta />
          </RutaConPermiso>
        ),
      },
      {
        path: 'plantillas',
        element: (
          <RutaConPermiso cualquieraDe={DESCARGA_DE_PLANTILLAS}>
            <Plantillas />
          </RutaConPermiso>
        ),
      },
    ],
  },
  { path: '*', element: <NoEncontrada /> },
];

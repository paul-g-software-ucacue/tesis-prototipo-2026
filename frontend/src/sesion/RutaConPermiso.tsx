// Una pantalla que exige un permiso, o uno de varios alternativos (ADR-021, D-70): sin él,
// «Acceso no autorizado» en lugar de la pantalla, sin pedir sus datos a la API. Solo decide qué se muestra; el servidor responde 403 a
// cualquier operación sin el permiso.
import type { ReactNode } from 'react';
import { AvisoDeError } from '../errores/AvisoDeError';
import { MENSAJE_ACCESO_NO_AUTORIZADO } from '../errores/mensajes';
import { traducirError } from '../errores/traducir';
import { useTitulo } from '../navegacion/useTitulo';
import type { TipoDePermiso } from './funcionalidades';
import { tieneAlguno, type PermisoRequerido } from './permisos';
import { useSesionActual } from './useSesionActual';

type Propiedades = (
  | { funcionalidad: string; tipo: TipoDePermiso; cualquieraDe?: never }
  | { cualquieraDe: readonly PermisoRequerido[]; funcionalidad?: never; tipo?: never }
) & { children: ReactNode };

export function RutaConPermiso({ funcionalidad, tipo, cualquieraDe, children }: Propiedades) {
  const sesion = useSesionActual();
  if (sesion.isError) {
    return <AvisoDeError mensaje={traducirError(sesion.error)} />;
  }
  if (!sesion.data) {
    return null;
  }
  const requeridos = cualquieraDe ?? [{ funcionalidad, tipo }];
  if (!tieneAlguno(sesion.data.permisos, requeridos)) {
    return <AccesoNoAutorizado />;
  }
  return children;
}

function AccesoNoAutorizado() {
  useTitulo(MENSAJE_ACCESO_NO_AUTORIZADO.titulo);
  return <AvisoDeError mensaje={MENSAJE_ACCESO_NO_AUTORIZADO} />;
}

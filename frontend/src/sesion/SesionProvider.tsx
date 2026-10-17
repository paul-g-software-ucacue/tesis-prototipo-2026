// Expone la sesión en memoria a los componentes (ADR-019) y borra la caché de las consultas cuando
// se cierra, para que ningún dato del usuario quede en pantalla después de cerrar la sesión.
// Mientras hay sesión, anota la actividad del usuario (teclado, ratón y navegación) y vigila la
// renovación del token (ADR-020).
import type { QueryClient } from '@tanstack/react-query';
import { useEffect, useSyncExternalStore, type ReactNode } from 'react';
import { estadoDeSesion, marcarActividad, suscribir } from './almacen';
import { ContextoDeSesion } from './contexto';
import { activarRenovacion } from './renovacion';

const EVENTOS_DE_ACTIVIDAD = ['keydown', 'pointerdown', 'wheel', 'popstate'] as const;

interface Propiedades {
  clienteDeConsultas: QueryClient;
  children: ReactNode;
}

export function SesionProvider({ clienteDeConsultas, children }: Propiedades) {
  const estado = useSyncExternalStore(suscribir, estadoDeSesion);

  useEffect(() => {
    if (!estado.activa) {
      clienteDeConsultas.clear();
    }
  }, [estado, clienteDeConsultas]);

  useEffect(() => activarRenovacion(), []);

  useEffect(() => {
    if (!estado.activa) {
      return;
    }
    EVENTOS_DE_ACTIVIDAD.forEach((evento) =>
      window.addEventListener(evento, marcarActividad, { passive: true }),
    );
    return () => {
      EVENTOS_DE_ACTIVIDAD.forEach((evento) => window.removeEventListener(evento, marcarActividad));
    };
  }, [estado.activa]);

  return <ContextoDeSesion value={estado}>{children}</ContextoDeSesion>;
}

// Expone la sesión en memoria a los componentes (ADR-019) y borra la caché de las consultas cuando
// se cierra, para que ningún dato del usuario quede en pantalla después de cerrar la sesión.
import type { QueryClient } from '@tanstack/react-query';
import { useEffect, useSyncExternalStore, type ReactNode } from 'react';
import { estadoDeSesion, suscribir } from './almacen';
import { ContextoDeSesion } from './contexto';

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

  return <ContextoDeSesion value={estado}>{children}</ContextoDeSesion>;
}

// Las rutas internas solo se ven con sesión (D-50): sin ella, se va al inicio de sesión con la ruta
// pedida como destino y, si la sesión terminó por un 401 o por vencimiento, con ese motivo.
import type { ReactNode } from 'react';
import { Navigate, useLocation } from 'react-router';
import { useSesion } from './useSesion';

export function RutaProtegida({ children }: { children: ReactNode }) {
  const sesion = useSesion();
  const ubicacion = useLocation();
  if (sesion.activa) {
    return children;
  }
  const parametros = new URLSearchParams({ destino: `${ubicacion.pathname}${ubicacion.search}` });
  if (sesion.motivo) {
    parametros.set('motivo', sesion.motivo);
  }
  return <Navigate to={`/iniciar-sesion?${parametros.toString()}`} replace />;
}

// El estado de la sesión para los componentes (ADR-019).
import { useContext } from 'react';
import { ContextoDeSesion } from './contexto';

export function useSesion() {
  return useContext(ContextoDeSesion);
}

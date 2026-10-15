// El contexto de React con el estado de la sesión en memoria (ADR-019).
import { createContext } from 'react';
import type { EstadoDeSesion } from './almacen';

export const ContextoDeSesion = createContext<EstadoDeSesion>({ activa: false });

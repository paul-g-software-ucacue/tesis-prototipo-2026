// El título del documento de cada pantalla, como «Inicio · MDD» (WCAG 2.4.2).
import { useEffect } from 'react';

export function useTitulo(titulo: string) {
  useEffect(() => {
    document.title = `${titulo} · MDD`;
  }, [titulo]);
}

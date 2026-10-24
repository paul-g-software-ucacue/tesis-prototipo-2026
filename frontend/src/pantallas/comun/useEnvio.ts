// Enviar un formulario de los datos maestros y mostrar lo que responde el servidor (ADR-019).
import { useState } from 'react';
import { traducirError, type MensajeDeError } from '../../errores/traducir';

/** Envía, muestra el error del servidor si lo hay y avisa cuando terminó bien. */
export function useEnvio(alGuardar: () => Promise<void>) {
  const [aviso, setAviso] = useState<MensajeDeError | null>(null);
  const [enviando, setEnviando] = useState(false);
  const enviar = (operacion: () => Promise<unknown>) => {
    setAviso(null);
    setEnviando(true);
    void (async () => {
      try {
        await operacion();
        await alGuardar();
      } catch (fallo) {
        setAviso(traducirError(fallo));
      } finally {
        setEnviando(false);
      }
    })();
  };
  return { aviso, enviando, enviar };
}

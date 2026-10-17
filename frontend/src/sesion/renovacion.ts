// Renovación del token mientras el usuario trabaja (RNF-SEG-07, ADR-020). El token dura 30 minutos:
// si hubo actividad del usuario desde la última renovación y le quedan menos de 20 minutos, se pide
// otro a POST /api/usuarios/sesion/renovacion. Sin actividad no se renueva, el token vence y
// almacen.ts cierra la sesión como terminada. La petición de renovación va marcada como
// automática, así que no cuenta como actividad: una sesión inactiva no se renueva sola.
import { ENCABEZADO_AUTOMATICA, api, llamar } from '../api/cliente';
import {
  huboActividadDesdeLaRenovacion,
  momentoDeLaRenovacion,
  obtenerToken,
  renovarToken,
  suscribir,
  suscribirActividad,
  vencimientoDelToken,
} from './almacen';

/** Se renueva cuando al token le queda menos que esto. */
const UMBRAL = 20 * 60_000;
/** Nunca dos renovaciones más seguidas que esto, aunque el token dure poco. */
const INTERVALO_MINIMO = 60_000;
// setTimeout desborda por encima de 2^31 - 1 ms: una espera más larga se hace en tramos.
const ESPERA_MAXIMA = 2_147_483_647;

let enCurso = false;

async function intentarRenovar() {
  const vence = vencimientoDelToken();
  if (
    enCurso ||
    !obtenerToken() ||
    !vence ||
    !huboActividadDesdeLaRenovacion() ||
    vence.getTime() - Date.now() > UMBRAL ||
    Date.now() - momentoDeLaRenovacion() < INTERVALO_MINIMO
  ) {
    return;
  }
  enCurso = true;
  try {
    const nuevo = await llamar(
      api.POST('/api/usuarios/sesion/renovacion', {
        headers: { [ENCABEZADO_AUTOMATICA]: '1' },
      }),
    );
    renovarToken(nuevo.token, new Date(Date.parse(nuevo.venceEn)));
  } catch {
    // Un 401 ya cerró la sesión en el cliente de la API (D-50). Otro fallo deja el token como
    // está: si la red vuelve antes del vencimiento, la próxima actividad lo intenta de nuevo.
  } finally {
    enCurso = false;
  }
}

/**
 * Empieza a vigilar la sesión: renueva con cada actividad dentro de los últimos 20 minutos y, si
 * hubo actividad antes, al entrar en ellos. Devuelve la función para dejar de vigilar.
 */
export function activarRenovacion(): () => void {
  let programada: ReturnType<typeof setTimeout> | undefined;

  function programar() {
    clearTimeout(programada);
    const vence = vencimientoDelToken();
    if (!vence) {
      return;
    }
    const espera = vence.getTime() - UMBRAL - Date.now();
    if (espera <= 0) {
      // Ya en los últimos 20 minutos: desde aquí renueva la próxima actividad.
      return;
    }
    programada = setTimeout(
      () => {
        void intentarRenovar().then(programar);
      },
      Math.min(espera, ESPERA_MAXIMA),
    );
  }

  const dejarDeEscuchar = suscribir(programar);
  const dejarDeEscucharActividad = suscribirActividad(() => {
    void intentarRenovar().then(programar);
  });
  programar();
  return () => {
    clearTimeout(programada);
    dejarDeEscuchar();
    dejarDeEscucharActividad();
  };
}

// La sesión vive solo en memoria (D-51, ADR-019): el token y su vencimiento están en este módulo y
// nunca en localStorage, sessionStorage, cookies ni la URL. Al recargar la página, se pierden. Los
// componentes se suscriben con SesionProvider; el cliente de la API lee el token de aquí.

/**
 * Por qué se cerró la sesión: «sesion-terminada» (un 401 o el vencimiento) lo explica el inicio de
 * sesión; «cierre» es el usuario que pidió salir, y se vuelve al inicio de sesión sin aviso.
 */
export type MotivoDeCierre = 'sesion-terminada' | 'cierre';

export interface EstadoDeSesion {
  activa: boolean;
  motivo?: MotivoDeCierre;
}

let token: string | null = null;
let estado: EstadoDeSesion = { activa: false };
let vencimiento: ReturnType<typeof setTimeout> | undefined;
const oyentes = new Set<() => void>();

function avisar() {
  oyentes.forEach((oyente) => oyente());
}

// setTimeout desborda por encima de 2^31 - 1 ms (unos 24,8 días): el cierre se programa en tramos.
const ESPERA_MAXIMA = 2_147_483_647;

function programarVencimiento(venceEn: Date) {
  const restante = venceEn.getTime() - Date.now();
  if (restante <= 0) {
    cerrarSesion('sesion-terminada');
    return;
  }
  vencimiento = setTimeout(() => programarVencimiento(venceEn), Math.min(restante, ESPERA_MAXIMA));
}

/** Guarda el token en memoria y programa el cierre al vencer (data-model «Sesión»). */
export function iniciarSesion(nuevoToken: string, venceEn: Date) {
  clearTimeout(vencimiento);
  if (venceEn.getTime() <= Date.now()) {
    cerrarSesion('sesion-terminada');
    return;
  }
  token = nuevoToken;
  estado = { activa: true };
  programarVencimiento(venceEn);
  avisar();
}

/**
 * Borra el token. Con un motivo, el inicio de sesión lo explica; sin motivo, es un cierre pedido
 * por el usuario.
 */
export function cerrarSesion(motivo?: MotivoDeCierre) {
  clearTimeout(vencimiento);
  token = null;
  estado = motivo ? { activa: false, motivo } : { activa: false };
  avisar();
}

/** El token vigente, o null sin sesión. */
export function obtenerToken(): string | null {
  return token;
}

/** El estado actual; el mismo objeto mientras no cambie, para useSyncExternalStore. */
export function estadoDeSesion(): EstadoDeSesion {
  return estado;
}

/** Avisa cada cambio de la sesión; devuelve la función para dejar de escuchar. */
export function suscribir(oyente: () => void): () => void {
  oyentes.add(oyente);
  return () => oyentes.delete(oyente);
}

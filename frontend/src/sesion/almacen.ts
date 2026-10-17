// La sesión vive solo en memoria (D-51, ADR-019): el token y su vencimiento están en este módulo y
// nunca en localStorage, sessionStorage, cookies ni la URL. Al recargar la página, se pierden. Los
// componentes se suscriben con SesionProvider; el cliente de la API lee el token de aquí.
// También guarda cuándo hubo actividad del usuario y cuándo se renovó el token, para que la
// renovación (renovacion.ts) solo ocurra mientras el usuario trabaja (ADR-020).

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
let venceEn: Date | null = null;
let ultimaActividad = 0;
let ultimaRenovacion = 0;
let estado: EstadoDeSesion = { activa: false };
let vencimiento: ReturnType<typeof setTimeout> | undefined;
const oyentes = new Set<() => void>();
const oyentesDeActividad = new Set<() => void>();

function avisar() {
  oyentes.forEach((oyente) => oyente());
}

// setTimeout desborda por encima de 2^31 - 1 ms (unos 24,8 días): el cierre se programa en tramos.
const ESPERA_MAXIMA = 2_147_483_647;

function programarVencimiento(vence: Date) {
  const restante = vence.getTime() - Date.now();
  if (restante <= 0) {
    cerrarSesion('sesion-terminada');
    return;
  }
  vencimiento = setTimeout(() => programarVencimiento(vence), Math.min(restante, ESPERA_MAXIMA));
}

/** Guarda el token en memoria y programa el cierre al vencer (data-model «Sesión»). */
export function iniciarSesion(nuevoToken: string, vence: Date) {
  clearTimeout(vencimiento);
  if (vence.getTime() <= Date.now()) {
    cerrarSesion('sesion-terminada');
    return;
  }
  token = nuevoToken;
  venceEn = vence;
  ultimaActividad = ultimaRenovacion = Date.now();
  estado = { activa: true };
  programarVencimiento(vence);
  avisar();
}

/**
 * Reemplaza el token por el renovado y reprograma el cierre, sin cambiar el estado de la sesión:
 * los componentes no se vuelven a dibujar ni se borra la caché de consultas.
 */
export function renovarToken(nuevoToken: string, vence: Date) {
  if (!token || vence.getTime() <= Date.now()) {
    return;
  }
  clearTimeout(vencimiento);
  token = nuevoToken;
  venceEn = vence;
  ultimaRenovacion = Date.now();
  programarVencimiento(vence);
}

/**
 * Borra el token. Con un motivo, el inicio de sesión lo explica; sin motivo, es un cierre pedido
 * por el usuario.
 */
export function cerrarSesion(motivo?: MotivoDeCierre) {
  clearTimeout(vencimiento);
  token = null;
  venceEn = null;
  estado = motivo ? { activa: false, motivo } : { activa: false };
  avisar();
}

/** Anota una acción del usuario (navegar, teclear, hacer clic o pedir algo a la API). */
export function marcarActividad() {
  if (!token) {
    return;
  }
  ultimaActividad = Date.now();
  oyentesDeActividad.forEach((oyente) => oyente());
}

/** Si el usuario hizo algo desde la última renovación o desde el inicio de sesión. */
export function huboActividadDesdeLaRenovacion(): boolean {
  return ultimaActividad > ultimaRenovacion;
}

/** Cuándo fue la última renovación o el inicio de sesión (milisegundos desde la época). */
export function momentoDeLaRenovacion(): number {
  return ultimaRenovacion;
}

/** El token vigente, o null sin sesión. */
export function obtenerToken(): string | null {
  return token;
}

/** Cuándo vence el token vigente, o null sin sesión. */
export function vencimientoDelToken(): Date | null {
  return venceEn;
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

/** Avisa cada actividad del usuario; devuelve la función para dejar de escuchar. */
export function suscribirActividad(oyente: () => void): () => void {
  oyentesDeActividad.add(oyente);
  return () => oyentesDeActividad.delete(oyente);
}

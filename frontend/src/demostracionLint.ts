// Demostración del escenario 4 de B00-H06 (T022): este error de ESLint debe bloquear la fusión.
// Se revierte en el commit siguiente.
export function guardarToken(token: string) {
  localStorage.setItem('token', token);
}

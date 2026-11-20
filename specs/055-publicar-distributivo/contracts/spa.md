# Contrato: publicar en la SPA

F18-H01 (#71).

## «Resolución del OCS» (`pantallas/resolucion/Resolucion.tsx`, la tarjeta `EstadoDelFlujo`)

- **En APROBADO, con la acción `PUBLICAR`:** un botón «Publicar el distributivo» debajo de la constancia de la resolución.
- **El diálogo de confirmación** (`DialogoDePublicacion`):
  - título «Publicar el distributivo»;
  - el texto: «La versión {v} de {periodo} quedará publicada y bloqueada: no se podrá cambiar sin una reforma. Se notificará a los docentes, a Talento Humano y a las coordinaciones.»;
  - «Cancelar» y «Publicar»; mientras envía, «Publicar» se desactiva.
- **Al publicar:** se recarga el estado del flujo. La tarjeta muestra la constancia de la publicación y ya no ofrece la acción.
- **Un error** (409 o 403): el mensaje del catálogo en el diálogo, que queda abierto.

## La constancia de la publicación (`EstadoDelFlujo`)

Con `publicacion` en el estado del flujo, para todo el que lo lee: «Versión {v} publicada por {publicadaPor} el {dd/mm/aa hh:mm}», en `America/Guayaquil`, con el mismo estilo que la constancia de la congelación.

## El aviso de la versión

En «Consolidado del periodo» y en el panel de inconsistencias, con el flujo en PUBLICADO: «La versión {v} está publicada: ya no cambia.», en lugar de «La versión {v} está congelada: ya no cambia.».

## La notificación

La campana de F16-H01 no cambia. La notificación de la publicación lleva a las coordinaciones a «Oferta y propuesta de docentes» (`/oferta`), con la constancia en la tarjeta del flujo, y a los docentes y Talento Humano al inicio (`/`), hasta F19.

## Pruebas

- **Vitest:**
  - `EstadoDelFlujo.test.tsx`: el botón solo con la acción, el diálogo y su texto, el `POST` sin cuerpo, el error en el diálogo y la constancia;
  - `Consolidado.test.tsx`: el aviso «publicada»;
  - `enlaces.test.ts`: la publicación lleva a COORDINADOR_CARRERA a `/oferta` y a DOCENTE y OPERADOR_DATOS a `/`, y cada uno puede abrir su enlace.
- **Playwright (`e2e/resolucion.spec.ts`):** la Secretaría publica con el teclado, confirma y ve la constancia; axe-core (WCAG 2.1 AA) sin fallas con el diálogo abierto y en PUBLICADO (SC-006).

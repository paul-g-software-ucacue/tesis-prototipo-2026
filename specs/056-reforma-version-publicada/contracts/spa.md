# Contrato: la reforma en la SPA

F18-H02 (#72).

## «Consolidado del periodo» (`pantallas/consolidado/Consolidado.tsx`, la tarjeta `EstadoDelFlujo`)

- **En PUBLICADO, con la acción `INICIAR_REFORMA`:** un botón «Iniciar una reforma», debajo de la constancia de la publicación.
- **El diálogo** (`DialogoDeReforma`):
  - título «Iniciar una reforma»;
  - el texto: «Se creará la versión {v+1}, una reforma de la versión {v}, y el distributivo volverá a consolidación con el ciclo completo de aprobación y firmas. La versión {v} sigue vigente hasta que se publique la reforma. Se notificará a quien consolida, a los docentes, a Talento Humano y a las coordinaciones.»;
  - el campo «Motivo de la reforma», obligatorio, con su contador (de 10 a 500 caracteres) y su error junto al campo;
  - «Cancelar» y «Iniciar la reforma»; el foco empieza en «Cancelar»; mientras envía, el botón se desactiva.
- **Al iniciar:** se recarga el estado del flujo. La tarjeta muestra la versión nueva, la reforma en curso y la publicación anterior.
- **Un error** (409, 403 o 400): el mensaje del catálogo en el diálogo, que queda abierto.

## La reforma en curso (`EstadoDelFlujo`)

Con `reforma` en el estado del flujo, para todo el que lo lee:
- «Reforma de la versión {versionPublicada} en curso: versión {version}, iniciada por {iniciadaPor} el {dd/mm/aa hh:mm}», en `America/Guayaquil`;
- el motivo, en su propia línea;
- la constancia de la publicación anterior suma «Vigente hasta que se publique la reforma.».

## La notificación

La campana de F16-H01 no cambia. La notificación de la reforma lleva a quien consolida a `/consolidado`, a las coordinaciones a `/oferta` y a los docentes y Talento Humano al inicio.

## Pruebas

- **Vitest:**
  - `EstadoDelFlujo.test.tsx`: el botón solo con la acción; el diálogo, su texto y el foco; el motivo corto que no se envía; el `POST` con el motivo; el error en el diálogo; la reforma en curso y la publicación vigente;
  - `enlaces.test.ts`: los enlaces de la reforma, cada uno contra los permisos de su rol.
- **Playwright,** en `e2e/consolidado.spec.ts`: el Vicerrector inicia la reforma con el teclado y ve la reforma en curso; axe-core (WCAG 2.1 AA) sin fallas con el diálogo abierto y después.

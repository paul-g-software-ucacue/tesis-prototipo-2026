# Contrato: la pantalla «Exportación a SIGA»

F19-H01 (#266). Es una pantalla nueva en `frontend/src/pantallas/siga/` (research §12). Los tipos salen del cliente generado del contrato de Reporting (`frontend/src/api/generado/reporting.d.ts`). «Reportes» no cambia.

## La ruta y el menú

- **La ruta:** `/exportacion-siga`, con el título «Exportación a SIGA» (`useTitulo`).
- **El menú:** una entrada en `Marco.tsx`, después de «Reportes», con el permiso `{ funcionalidad: 'PUBLICACION', tipo: 'APROBACION' }`. Solo la ve la Secretaría general académica.
- **Las pruebas del menú:** `enlaces.test.ts` y `Marco.test.tsx` suman el caso. `SECRETARIA_GENERAL`, con `PUBLICACION:APROBACION`, la ve; Rector, Vicerrector, Administrador y coordinación no.
- **Sin el permiso:** entrar por la dirección muestra la página de «sin acceso» común.

## Los estados

| Estado | Qué se ve |
|---|---|
| Cargando | «Cargando…», con `aria-live="polite"` |
| Sin publicaciones | En «CSV para SIGA»: «Ningún periodo tiene un distributivo publicado. El CSV para SIGA sale de la versión publicada.» El botón no aparece, y la sección del layout se ve igual |
| Con publicaciones | Las dos secciones |
| Error | El `titulo` y la `descripcion` del catálogo, y el `idIncidente` si viene |

## Sección «CSV para SIGA» (US1 y US2)

- **«Periodo»:** una lista de `listarPublicaciones`, propuesta en la publicación más reciente, igual que en «Reportes».
- **El texto de la versión:** «Versión N, publicada el aaaa-mm-dd por {usuario}». Si la publicación está en reforma, agrega «Hay una reforma en curso: el CSV sale de esta versión publicada.» (D-113).
- **El texto del layout:**
  - «Se generará con el layout por defecto.», si es el número 0;
  - si no, «Se generará con la versión N del layout de exportación, cargada el aaaa-mm-dd por {usuario}.».
- **El botón «Generar CSV para SIGA»:**
  - pide `generarCsvSiga` y descarga el archivo con el nombre de `Content-Disposition`;
  - mientras genera, está deshabilitado y dice «Generando…»;
  - al terminar, «Se generó siga-2026-I-v1.csv.», en una región `aria-live="polite"`;
  - si falla, el mensaje del catálogo en un `Alert` con `role="alert"`. Un 409 dice «Sin distributivo publicado».

## Sección «Layout de exportación» (US3)

- **«Layout vigente»:**
  - una tabla con `caption`, con las columnas «Posición», «Nombre», «Campo» y «Traducción»;
  - la traducción se muestra como «CLASE → 01, …», o «—» si no hay;
  - arriba, el mismo texto del layout de la sección anterior.
- **«Descargar plantilla»:** un botón que pide `descargarPlantillaLayoutSiga`.
- **«Cargar archivo de mapeo»:** un botón que abre el selector de archivos (un `input type="file"` oculto, con su `aria-label`, que acepta `.yaml` y `.yml`), y carga el archivo al elegirlo, como en «Flujo y firmas» (F12-H01). **Ajuste al implementar:** no hay un segundo botón «Cargar»; por eso tampoco existe el caso «sin archivo».
  - **antes de enviar** (principio XIV): un archivo de más de 256 KB no se envía y se muestra «Archivo demasiado grande»;
  - **al cargar bien:** «Se cargó la versión N del layout de exportación. El próximo CSV saldrá con sus columnas.», la tabla se actualiza y se invalidan las consultas del layout;
  - **al rechazarse:** «El archivo tiene N problemas. El layout vigente no cambió.» y una lista con cada `detalle` («columnas[3] (HORAS): …»), en un `Alert` con `role="alert"`. El foco va al título de la lista.
- **«Versiones anteriores»:**
  - una tabla con «Versión», «Cargada por», «Cargada el» y «Columnas»;
  - cada fila se puede expandir con un botón con `aria-expanded`, para ver sus columnas;
  - si no hay, «No hay versiones anteriores.».

## Accesibilidad (FR-025, SC-010)

- Cada sección es una `section` con su `h2`.
- Los botones tienen texto propio.
- Los mensajes de estado van en regiones `aria-live`.
- Los errores van en `role="alert"`.
- Las tablas tienen `caption` y `th` con `scope`.

## Las pruebas

**Vitest y Testing Library (`Siga.test.tsx`):**
- los estados;
- el texto del layout, por defecto y cargado;
- la descarga del CSV y su mensaje;
- el 409;
- la carga válida y la rechazada, con la lista de problemas y el foco;
- la validación antes de enviar (sin archivo y de más de 256 KB);
- las versiones anteriores, expandidas y vacías.

**Playwright (`frontend/e2e/siga.spec.ts`):**
- `secretaria.demo` genera el CSV y carga un layout;
- axe-core corre en los tres estados (con publicaciones, con el layout rechazado y con versiones anteriores), sin fallas WCAG 2.1 AA.

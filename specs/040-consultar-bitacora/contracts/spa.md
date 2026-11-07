# Contrato: pantalla «Bitácora» (`frontend/src/pantallas/bitacora/`)

## Acceso

- Ruta `/bitacora`, protegida como las demás. En `Marco.tsx`, la opción «Bitácora» con `permiso: { funcionalidad: 'BITACORA', tipo: 'LECTURA' }`: solo la ven el Vicerrector académico y el Administrador del sistema (FR-009).
- Un usuario sin el permiso que entra por la URL ve el aviso del 403 del catálogo, como en las demás pantallas protegidas (`errores/`).

## Filtros

| Campo | Control | Mientras se escribe | Antes de enviar (motivo) |
|---|---|---|---|
| Desde | fecha | Solo una fecha | — |
| Hasta | fecha | Solo una fecha | «La fecha hasta no puede ser anterior a la fecha desde.» |
| Usuario | texto | Hasta 64 caracteres | — |
| Servicio | lista: Generación, Académico, Docentes, Normativa, Roles, Validación OCS | — | — |
| Acción | lista de `listarAccionesDeBitacora` del servicio elegido; deshabilitada sin servicio | — | — |
| Identificador | texto | Solo letras sin tilde, dígitos, `.`, `_`, `-` y `/`, hasta 64 | — |
| Periodo | texto | Solo letras, dígitos, `.`, `_` y `-`, hasta 20 | — |

- Las reglas son las del contrato (`contracts/usuarios.md`), en un esquema `zod` con sus pruebas de casos válidos e inválidos en los límites (principio XIV).
- «Consultar» envía los filtros y vuelve a la página 1, sin `corte`. «Limpiar» quita los filtros.
- Los filtros, la página y el `corte` viven en la URL (`useSearchParams`): recargar o compartir la dirección da la misma consulta.
- Si el backend responde `COMUN_SOLICITUD_INVALIDA`, cada detalle marca su campo, como en las demás pantallas.

## Resultado

- Encabezado: «N entradas» con el `total`, y el paginador (50 por página por defecto; 25, 50 o 100). Al cambiar de página se envía el `corte` de la primera respuesta.
- Tabla, de la más reciente a la más antigua: Momento (`es-EC`, `timeZone: 'America/Guayaquil'`), Usuario, Servicio (con su nombre), Acción, Periodo y Objeto (los pares clave–valor del objeto en una línea).
- Cada fila se expande y muestra el objeto, el valor anterior y el nuevo como pares campo–valor. Si falta el anterior: «Sin valor anterior (se creó)». Si falta el nuevo: «Sin valor nuevo (se quitó)». Un valor `null`: «sin valor». Una lista o un objeto anidado se muestran con sangría, nunca como JSON crudo.
- Sin entradas: «No hay entradas con estos filtros.», sin error (US1-5).
- La pantalla no tiene ningún botón que cambie una entrada (FR-006).

## Pruebas

- **Vitest** (`Bitacora.test.tsx`, `validacionDeBitacora.test.ts`): el menú con y sin el permiso, los filtros en la URL, cada regla en sus límites, la paginación con el `corte`, los textos de creación y eliminación, la lista vacía y los detalles de un 400 en sus campos.
- **Playwright** (`e2e/bitacora.spec.ts`): con `vicerrectorado.demo`, filtrar por `coordinacion.demo` y el día de hoy, y ver solo sus entradas (US1-1); con `coordinacion.demo`, la opción no aparece y la URL muestra el aviso del 403 (US2). La prueba de accesibilidad (`accesibilidad.spec.ts`) suma la pantalla.

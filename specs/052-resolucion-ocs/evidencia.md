# Evidencia: Registrar la resolución del OCS

F15-H01 (#66). Cada sección dice qué se verificó, cómo y con qué resultado. Las comprobaciones son del 2026-11-15, sobre la rama `052-resolucion-ocs`, antes del pull request.

## Pruebas y comprobaciones (T016 a T019)

| Comprobación | Resultado | Cómo |
|---|---|---|
| Pruebas unitarias del backend | 3 239 pruebas, 0 fallas | `./gradlew build integrationTest jacocoTestReport jacocoTestCoverageVerification --rerun-tasks --continue` |
| `integrationTest` completo | 332 pruebas, 0 fallas. <br>• Validación OCS: V7 con sus reglas (`ValidacionocsEsquemaIntegrationTest`), la resolución guardada y leída con su transición, y dos registros a la vez, de los que queda uno (`RepositorioProcesoIntegrationTest`). <br>• Roles: V9 en la matriz D-16 (`MatrizD16IntegrationTest`). | El mismo |
| Fallas en la corrida completa | 2: `ClienteDeEventosLocalTest` y la medición de tiempo de `ConfirmarLasDiezCargasIntegrationTest`, las intermitentes de siempre, en módulos que esta rama no toca. Las dos pasan con sus módulos completos | Cada módulo otra vez |
| Cobertura por módulo (60 % de líneas, D-32) | Pasa en todos: Validación OCS 83,3 %; la más baja, Roles, 77,2 % | `jacocoTestReport` |
| Spotless, SpotBugs y arquitectura | En verde: ArchUnit y Spring Modulith. Validación OCS tiene su propia comprobación del PDF y su almacén, sin clases de Generación ni de Firma (principio V) | `build`, `ArquitecturaTest`, `ModulosTest` |
| El dominio | <br>• `ResolucionOcsTest` (3) y `ArchivoDeResolucionTest` (3): el número y el PDF. <br>• `ProcesoAprobacionTest` (+7): registrar en EN_OCS, la fecha en sus bordes y en la noche de Guayaquil, otro rol, otra etapa, el avance genérico desde EN_OCS y la acción. <br>• `TransicionTest` (+3). | `./gradlew :servicios:validacionocs:test` |
| La aplicación | <br>• `RegistrarResolucionServicioTest` (5): el archivo fuera de la transacción y después del agregado sin cambios; el paso a APROBADO y la bitácora en la transacción; ningún rechazo deja el archivo. <br>• `AlmacenDeResolucionesEnDiscoTest` (3). | El mismo |
| Contrato y catálogo | <br>• `RegistrarResolucionContratoTest` (4): el 200, cada 400, el 413, los 409 y los 403. <br>• `FlujoContratoTest` (+5): el 409 del avance, la acción, la resolución y su descarga para quien puede leerla, y nada para quien no; el 404 y el 500. <br>• `ErroresValidacionocsTest`, con los ocho códigos. <br>• `PermisosValidacionocsTest`, con diez operaciones. | Las mismas |
| SPA: tipos, ESLint y Prettier | En verde | En `node:24-alpine` |
| SPA: Vitest | 589 pruebas, 0 fallas. <br>• `EstadoDelFlujo.test.tsx` (+4): el formulario, el cuerpo multipart, el 400 en el formulario y la constancia con la descarga. <br>• `Resolucion.test.tsx` (2): la pantalla, su ruta y su entrada en el menú. | `vitest run` |
| SPA: Playwright | 130 pruebas, 0 fallas. `e2e/resolucion.spec.ts`: la Secretaría registra con el teclado, ve la constancia y descarga el PDF; axe-core (WCAG 2.1 AA) sin fallas en EN_OCS, con la confirmación abierta y en APROBADO | `mcr.microsoft.com/playwright:v1.64.0-noble` |
| Scripts de `perfil-consolidado` (T017, SC-004) | Los 36 del workflow pasan, en su orden, sobre un perfil construido desde la rama; `probar_documento_firmable.sh` en 76 s | Uno tras otro, como el workflow |
| shellcheck | 0 hallazgos en `probar_documento_firmable.sh` | `koalaman/shellcheck:v0.11.0` |

### `probar_documento_firmable.sh` (contracts/demostracion.md)

16/16 en la primera corrida, en 90 s, sobre un perfil construido desde la rama; dentro del workflow, otra vez 16/16.

| Paso | Resultado |
|---|---|
| 0 a 12 | OK, como en F14-H03: las tres firmas y EN_OCS por el sistema |
| 13. Sin resolución (SC-001) | OK. <br>• El avance genérico desde EN_OCS: 409 `VALIDACIONOCS_REQUIERE_RESOLUCION`. <br>• `vicerrectorado.demo`, `rectorado.demo` y `consolidacion.demo` reciben 403 al registrar, sin fila. |
| 14. Los registros inválidos (SC-001) | OK, cada uno con su código y sin fila: <br>• número de espacios; <br>• fecha de mañana y fecha del día anterior a EN_OCS (en `America/Guayaquil`); <br>• un archivo que no es PDF; <br>• un PDF de más de 10 MB, dentro del límite de 11 MB del proceso, que da 413. |
| 15. El registro (SC-002) | OK. <br>• `secretaria.demo` registra con el número entre espacios: 200, APROBADO, y la resolución en el flujo con el número recortado. <br>• La transición por `secretaria.demo`, con su resolución y la huella. <br>• El archivo en `resoluciones/<sha256>.pdf`. <br>• `vicerrectorado.demo` lo descarga idéntico y `coordinacion.demo` recibe 403. <br>• `RESOLUCION_REGISTRADA` en la bitácora. |

El PDF de prueba de la resolución es mínimo y ficticio. `MDD_CONSERVAR=EN_OCS` (nuevo) se detiene después del paso 12, para el recorrido a mano.

## Recorrido a mano (T017, SC-003)

Hecho por el autor el 2026-11-15, con `quickstart.md` §3, sobre un perfil construido desde la rama, después de `MDD_CONSERVAR=EN_OCS deploy/probar_documento_firmable.sh`.

| Comprobación | Resultado |
|---|---|
| El registro (SC-003) | `secretaria.demo`, desde «Resolución del OCS», registró el número, la fecha y un PDF propio en 45 s |
| El resultado | «Aprobado · versión 1» con la constancia: número, fecha, quién, cuándo y la huella del PDF |
| La descarga | El autor comprobó que el PDF descargado es el mismo que subió |
| El Vicerrector | En «Consolidado del periodo», la misma constancia con la descarga, sin el formulario |
| Limpieza | El perfil se bajó con `down -v` |

**Hallazgo del recorrido, corregido en la rama.** En APROBADO, el aviso de Generación seguía diciendo «La versión 1 está congelada para la firma: ya no cambia.». Generación no sabe en qué etapa está el flujo. Ahora dice «La versión 1 está congelada: ya no cambia.», en «Consolidado del periodo» y en el panel de inconsistencias. El autor no lo volvió a ver a mano; lo comprueban Vitest y Playwright.

## Desviaciones del plan

| Plan | Lo hecho | Por qué |
|---|---|---|
| El formulario en «Consolidado del periodo» | Una pantalla propia, «Resolución del OCS» (`/resolucion-ocs`), para quien lee o escribe la resolución; la constancia sale también en «Consolidado del periodo» | La Secretaría general no abre «Consolidado del periodo»: no lee el consolidado (D-16). Lo decidió el autor durante T015; quedó en la spec, el contrato de la SPA y D-107 |
| Leer la resolución con el permiso del flujo | Con `RESOLUCION_OCS:LECTURA`, que Roles V9 suma al Vicerrector, al Rector y a los miembros del OCS | Análisis S1: la matriz D-16 solo se lo daba a la Secretaría y al Administrador. Lo decidió el autor |
| El agregado decide con el proceso bloqueado | También antes, sin cambiar nada (`validarResolucion`) | Para que un rechazo no deje ni el archivo en el volumen |
| La clasificación «Técnico» de `VALIDACIONOCS_RESOLUCION_ILEGIBLE` | «Sistema» | «Técnico» no es una de las admitidas; es la de `GENERACION_INFORME_ILEGIBLE` |
| El evento `ResolucionRegistrada` | No se publica todavía | Nadie lo escucha (research §8); lo agregará F18-H01 o F16 |
| Playwright en `consolidado.spec.ts` | En `e2e/resolucion.spec.ts` | Por la pantalla propia |
| Ver fallar cada prueba antes del código | Así en la mayoría. No se vieron fallar: <br>• los registros a la vez (T011), porque el bloqueo ya estaba; <br>• el 409 del avance en `FlujoContratoTest` (T010), por T006; <br>• Playwright (T015), escrito con la pantalla hecha; <br>• los casos de «no hace nada» de varios grupos, contra esqueletos que no hacían nada. | Se cuenta tal como pasó |

**Limitación conocida.** Las dos fallas intermitentes de la corrida completa son de módulos que esta rama no toca y dependen de la carga del equipo.

## Criterios de terminado (constitución §6)

| Criterio | Dónde |
|---|---|
| Escenarios de aceptación | `probar_documento_firmable.sh` (pasos 13 a 15).<br>• Backend: `ResolucionOcsTest`, `ArchivoDeResolucionTest`, `ProcesoAprobacionTest`, `TransicionTest`, `RegistrarResolucionServicioTest`, `AlmacenDeResolucionesEnDiscoTest`, `RegistrarResolucionContratoTest`, `FlujoContratoTest`, `RepositorioProcesoIntegrationTest`, `ValidacionocsEsquemaIntegrationTest` y `MatrizD16IntegrationTest`.<br>• SPA: `EstadoDelFlujo.test.tsx`, `Resolucion.test.tsx` y `e2e/resolucion.spec.ts`. |
| Contrato publicado y verificado | `registrarResolucion`, `descargarResolucion`, `ResolucionDelOcs`, `REGISTRAR_RESOLUCION` y el 409 del avance en `contratos/validacionocs/openapi.yaml`; con sus pruebas de contrato y `verificar:ruptura` |
| Prueba de arquitectura | ArchUnit y Spring Modulith en verde |
| Errores en el catálogo | Ocho códigos nuevos en `contratos/comun/errores.md` y `ErroresValidacionocs`, verificados por sus pruebas |
| JavaDoc | Registrar es aprobar, la fecha en días de la institución, D-19, el archivo por su huella antes de la transacción y el 409 del avance (T019) |
| Seguridad | El permiso y el rol, en el servidor. El PDF comprobado por su contenido y su tamaño, el nombre sin ruta y la huella al leer. Leer la resolución exige `RESOLUCION_OCS:LECTURA` (D-16, V9). La resolución no se reescribe |
| Decisión registrada | D-107, antes de implementar, puesta al día con la pantalla propia y el permiso de lectura |

## Equipo de demostración (T021)

| Comprobación | Resultado |
|---|---|
| Fusión | PR #239 (`6507996`), con las 12 comprobaciones en verde en la primera corrida |
| Despliegue | `cd` exitoso; `mdd-actualizar` tomó `6507996` y dice «Sano en 52 s»; los 8 contenedores sanos con las imágenes `*-6507996953ac` |
| Las migraciones | En la base, `validacionocs` en la V7 (`resolucion ocs`) y `roles` en la V9 (`resolucion para quien aprueba`) |
| El permiso | Leen la resolución ADMINISTRADOR, MIEMBRO_OCS, RECTOR, SECRETARIA_GENERAL y VICERRECTOR |
| Nada registrado | `validacionocs.resolucion_ocs` vacía: 2026-I sigue en EN_CONSOLIDACION |
| Memoria | `distributivo` usa 230 de sus 512 MiB; `identidad`, 470 (#224) |

En la demo no se llega a EN_OCS: 2026-I está en consolidación. La resolución la cubren el script en la CI y el recorrido del autor en el perfil local.

# Evidencia: F03-H01

Registro de las comprobaciones de la historia (issue #35): mantener la estructura orgánica en Académico, con su jerarquía, solo las 21 gestiones autorizadas y las comisiones como dato informativo (RF-12, RN-10, C-12, V-02, D-70). Cada fila dice el comando y la salida resumida, o enlaza al run de GitHub Actions. Los datos son ficticios; las contraseñas de local y de la CI también.

## Entorno

| Pieza | Versión | Notas |
|---|---|---|
| Sistema | Fedora Linux 44 (SELinux en modo *enforcing*) | |
| Java | OpenJDK 25.0.4.1 | Gradle con el *wrapper* del repositorio. |
| Node | 24 (`~/.local/node-24`) | Vitest y Playwright. |
| PostgreSQL | `postgres:18.6-alpine` | Testcontainers y el perfil consolidado. |
| Docker Engine | 29.8.2 (Fedora) | Con `sg docker -c '…'`. |

## Base: permisos alternativos, contrato, dominio, migración y repositorio

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| T002 · `VerificadorDePermisos` con permisos alternativos (D-70) | ✅ 3 pruebas nuevas; las 2 de fallo fallaron antes del cambio. Los `Permisos*Test` de Académico, Normativa, Roles y Usuarios siguen en verde | `VerificadorDePermisosTest`, `permisos-alternativos*.yaml` | 2026-10-21 |
| T003 · Contrato: 6 operaciones, 6 esquemas y 3 códigos | ✅ Generados `EstructuraApi` y los modelos; `generar:api` y `verificar:ruptura` en verde | `contratos/academico/openapi.yaml`, `contratos/comun/errores.md` | 2026-10-21 |
| T004–T005 · Dominio, con las pruebas primero | ✅ 29 pruebas nuevas, sin dobles; las 24 unidades de la carga 01 de prueba entran en orden | `UnidadOrganicaTest` (14), `EstructuraOrganicaTest` (15), `EstructuraDePrueba` | 2026-10-21 |
| Regla de la gestión autorizada frente a los datos de prueba | ⚠️→✅ La carga 01 tiene `RECT` (Gobierno, G01) y `VRA` (Gobierno, G02): FR-004 prohibía el código fuera de una gestión. El autor eligió seguir el diccionario (obligatoria en gestión, prohibida en comisión, opcional en los demás) | commit `b85041c`, aclaración en spec.md | 2026-10-21 |
| T006–T007 · V3: `gestion_autorizada` con las 21, `unidad_organica`, índice de la sede matriz, 4 acciones | ✅ Fallaron antes de la migración; 13 combinaciones inválidas rechazadas por la base | `AcademicoEsquemaIntegrationTest` | 2026-10-21 |
| T008 · `RepositorioEstructuraJdbc` | ✅ Campo por campo, repetido, guardar, catálogo y el candado (una segunda transacción espera) | `RepositorioEstructuraJdbcIntegrationTest` | 2026-10-21 |

## Mantener las unidades (US1)

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| T009 · Servicios con Mockito | ✅ 15 pruebas: bloquear → validar → guardar → registrar; repetido, sede matriz, dependientes; lo repetido no registra | `MantenerEstructuraServicioTest`, `ConsultarEstructuraServicioTest` | 2026-10-21 |
| T010 · Contrato de la estructura | ✅ 13 pruebas validadas contra el contrato (201, 400 con su campo, 404, 409 de los 3 códigos) | `EstructuraContratoTest`, `EstructuraEnMemoria` | 2026-10-21 |
| T011 · Permisos de las 12 operaciones | ✅ Lecturas con `[DISTRIBUTIVO:LECTURA, CATALOGOS:LECTURA]`, cada uno por separado; escrituras con `CATALOGOS:ESCRITURA` | `PermisosAcademicoTest`, `ErroresAcademicoTest` | 2026-10-21 |
| `check` e `integrationTest` de Académico y del proceso `datos` | ✅ Cobertura de líneas de Académico 77,9 % | `./gradlew :servicios:academico:check :apps:datos:check …` | 2026-10-21 |

## Gestión fuera del catálogo (US2)

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| T012 · G99, G00 y G22 rechazadas con 400 en `gestionAutorizada`, también con `CATALOGOS:ESCRITURA`; sin código, 400 con su motivo; no hay operación para ampliar el catálogo | ✅ Pasó a la primera. Mutación: sin la regla «una gestión exige su código», la prueba falla; restaurada | `EstructuraContratoTest` | 2026-10-21 |

## Comisión informativa (US3)

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| T013 · Comisión con «permanente» 201; sin él o con gestión, 400 con su motivo | ✅ Pasó a la primera. Mutación: sin la regla «una comisión dice si es permanente», la prueba falla; restaurada | `EstructuraContratoTest` | 2026-10-21 |
| La estructura no tiene horas | ✅ Ninguna unidad tiene un campo de horas (contrato y `unidad_organica`); F07 no las toma de las comisiones (C-12) | data-model.md | 2026-10-21 |

## Pantalla «Estructura orgánica» (US4)

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| T014 · Permisos alternativos en la ruta y el menú | ✅ Fallaron antes del cambio; `tieneAlguno` y `RutaConPermiso cualquieraDe`; las rutas de un solo permiso siguen igual | `permisos.test.ts`, `RutaConPermiso.test.tsx` | 2026-10-21 |
| T015 · La pantalla, con Vitest | ✅ 12 pruebas, que fallaron antes de la pantalla: el árbol con sangría y «Depende de», tipos en español, gestión con su nombre, comisión «Informativa, sin horas»; registrar (padre de las activas, gestión de la lista, «¿Es permanente?» solo en comisión), corregir sin código ni tipo, inactivar y reactivar con confirmación; el 400 marca su campo y el 409 muestra su título; sin escritura, sin acciones; menú con cualquiera de los dos permisos | `Estructura.test.tsx` | 2026-10-21 |
| SPA completa | ✅ 18 archivos, 133 pruebas; cobertura de líneas 95,3 % (la carpeta `estructura`, 94,1 %); lint, formato, tipos y `verificar:ruptura` en verde | `npm test -- --run --coverage` | 2026-10-21 |
| T016 · Playwright y axe | ✅ 5 pruebas nuevas: registrar la sede matriz y una gestión debajo, inactivarla; sin fallas WCAG 2.1 AA en el árbol y en los diálogos de registrar, corregir e inactivar. Toda la suite: 62 pasan | `e2e/estructura.spec.ts` | 2026-10-21 |

## Cierre

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| Perfil reconstruido con el código de la rama | ✅ Académico en V3 con las 21 gestiones. Tras el reinicio del PC, BuildKit del daemon tenía una instantánea rota (`snapshots/4782`) y `node:24-alpine` no construía; se construyó con un builder `docker-container` limpio (`--builder mdd-limpio`) sin tocar las imágenes del equipo | `docker compose build --builder mdd-limpio` | 2026-10-22 |
| T017 · `probar_estructura.sh`, dos corridas seguidas | ✅ 14 de 14 pasos en cada una: las 24 unidades con su padre, G99 y comisión sin «permanente» 400, ciclo 400, segunda sede matriz 409, dependientes 409, hoja inactivada y reactivada, Vicerrector lee y recibe 403 al escribir, docente 403, registro 24/1/1. La limpieza deja 0 unidades y 0 registros | salida del script | 2026-10-22 |
| T017 · Con una sede matriz previa en la base | ✅ 23 de 23 unidades debajo de la matriz existente; todos los pasos pasan | matriz temporal `TMP-MATRIZ`, borrada después | 2026-10-22 |
| `shellcheck` del script | ✅ Sin observaciones | `koalaman/shellcheck:stable` | 2026-10-22 |

## Equipo de demostración

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|

## Notas

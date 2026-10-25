# Evidencia: F03-H04

Registro de las comprobaciones de la historia (issue #38): mantener los docentes con su ficha académica en el servicio de Docentes, con las referencias de Académico y los permisos de D-70 (RF-16, RF-17, RN-01). Cada fila dice el comando y la salida resumida, o enlaza al run de GitHub Actions. Los datos son ficticios; las contraseñas de local y de la CI también.

## Entorno

| Pieza | Versión | Notas |
|---|---|---|
| Sistema | Fedora Linux 44 (SELinux en modo *enforcing*) | |
| Java | OpenJDK 25.0.4.1 | Gradle con el *wrapper* del repositorio. |
| Node | 24 (`~/.local/node-24`) | Vitest y Playwright. |
| Docker Engine | 29.8.2 (Fedora) | Con `sg docker -c '…'`. |

## Base: contrato, referencias de Académico, dominio, migración y repositorios

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| Datos de prueba de las cargas 02, 04 y 06 en Docentes | `02_carreras.csv`, `04_asignaturas_malla.csv` y `06_docentes.csv` (47 docentes ficticios, RNF-SEG-06), copiados de `backend/servicios/cargas/src/test/resources/cargas/datos-de-prueba/` a `backend/servicios/docentes/src/test/resources/docentes/datos-de-prueba/` | SHA-256 `a4ee44cea177…`, `eb1417c75800…`, `b06fe68e66fd…` | 2026-10-24 |
| Los contratos: Docentes con 6 operaciones y 2 códigos 409; Académico con `consultarReferencias` | Los YAML cargan; los modelos de Java y los tipos de la SPA se generan; `verificar:ruptura` pasa. `DocenteNuevo` y `Docente` llevan los campos al mismo nivel que `DatosDeDocente`, para que un 400 diga el mismo campo al registrar y al corregir | `contratos/docentes/openapi.yaml`, `contratos/academico/openapi.yaml`, `contratos/comun/errores.md` | 2026-10-24 |
| Académico publica sus referencias: `ConsultarReferencias` y la operación `consultarReferencias`, que la lee también «Docentes: lectura» | `ConsultarReferenciasServicioTest` (Mockito), `CarrerasContratoTest` (200 contra el contrato) y `PermisosAcademicoTest` con 31 operaciones pasan. La operación quedó en el tag `Carreras`, así que la implementa `CarrerasControlador` en lugar de un `ReferenciasControlador` aparte | `./gradlew :servicios:academico:test` | 2026-10-24 |
| Las reglas del docente y de su identificación, sin dobles | 19 de 19 pasan: cédula (10 dígitos, provincia, tercer dígito, módulo 10) y pasaporte; nombres en mayúsculas; correo y teléfono; categoría según el tipo de personal; horas de 1 a 40 (`TP` con 40 se acepta: la coherencia es de la normativa); fin de contrato; corrección con sus cambios; estado. Ningún mensaje repite la cédula, el correo ni el teléfono | `IdentificacionTest` (10), `DocenteTest` (9) | 2026-10-24 |
| Las reglas de la ficha académica, sin dobles | 6 de 6 pasan: tercer nivel y su campo, años desde 1950 y hasta el actual, horas del curso de 1 a 2000, años de experiencia hasta 60, 20 elementos por lista, afines sin repetir; el campo dice la posición (`ficha.cursos[1].horas`) | `FichaAcademicaTest` | 2026-10-24 |
| Referencias, búsqueda y valores permitidos, con los 47 docentes de la carga 06 | 12 de 12 pasan: los 47 son válidos sobre las referencias de las cargas 02 y 04 y cada uno se encuentra por su cédula (SC-002); «peralta», «PERALTA» y «Peraltá» encuentran lo mismo; las 7 enumeraciones son las de la carga 6 del diccionario | `ReferenciasTest` (5), `BusquedaDeDocentesTest` (4), `ValoresPermitidosTest` (3) | 2026-10-24 |
| Las pruebas del dominio fallan si la regla se rompe | Las clases se escribieron junto con sus pruebas; para comprobarlo, el dígito verificador devolviendo siempre `true` hace fallar `IdentificacionTest` (mutación revertida) | `./gradlew :servicios:docentes:test` | 2026-10-24 |
| V2: `docente` y sus `check`, el correo único sin mayúsculas y las 3 acciones del registro | Pasa; falló sin la migración (2 de 2). Cada caso inválido va con su propia identificación y su propio correo, para que solo falle por el valor que prueba: tipo, personal, categoría, dedicación, condición, estado, horas 0 y 41, fin igual al inicio, específico de otro amplio, teléfono, `cursos` que no es lista, correo repetido con otras mayúsculas e identificación repetida | `DocentesEsquemaIntegrationTest`, con Testcontainers (postgres:18.6-alpine) | 2026-10-24 |
| Los repositorios guardan y leen igual, con la ficha completa y la mínima | 7 de 7 pasan: orden por apellidos, corrección y estado, correo sin mayúsculas que excluye al propio docente, candado y registro de cambios | `RepositorioDocentesJdbcIntegrationTest` | 2026-10-24 |
| Docentes usa Académico solo por `puertos.entrada` | `AcademicoDesdePuertosTest` (doble escrito a mano, sin Mockito) y `ModulosTest` del proceso `datos` pasan. Con un campo de tipo `ec.mdd.academico.dominio.CampoAmplio` en el adaptador, `ModulosTest` falla con «non-exposed type … within module 'academico'» (mutación revertida) | `./gradlew :servicios:docentes:test :apps:datos:test` | 2026-10-24 |

## Registrar (US1)

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| El servicio bloquea, decide la identificación y el correo repetidos, guarda y registra; las referencias se verifican antes de la transacción | 9 de 9 pasan (también corregir y cambiar de estado, US3 y US4); los mensajes de los 409 no repiten la cédula ni el correo | `MantenerDocentesServicioTest` (Mockito) | 2026-10-24 |
| Registrar 201; 400 en `horasContratadas`, `identificacion`, `carreraBase` y `ficha.cursos[0].horas`; 409 `DOCENTES_DOCENTE_REPETIDO` y `DOCENTES_CORREO_REPETIDO` | Pasan, y cada respuesta cumple el contrato | `DocentesContratoTest`, con los servicios reales sobre dobles en memoria | 2026-10-24 |
| **Escenario del issue «Edición inválida»:** una dedicación «XX» no se guarda y el 400 dice «no es un valor permitido: usa TC, MT o TP» en `dedicacion` | Pasa; el docente queda como estaba. Antes, un valor fuera de una enumeración respondía `COMUN_CUERPO_ILEGIBLE` sin campo: `ManejadorDeErrores` (plataforma) ahora responde `COMUN_SOLICITUD_INVALIDA` con el campo, su posición en las listas y los valores permitidos, sin repetir el valor recibido. Sirve a los 4 procesos | `DocentesContratoTest`; `ManejadorDeErroresTest` (16 de 16, uno nuevo con un valor arriba y otro dentro de una lista) | 2026-10-24 |
| Las 6 operaciones exigen sus permisos alternativos (D-70) | Pasa; sin `DOCENTES:ESCRITURA` en la escritura, falla con «con DOCENTES:ESCRITURA respondió 403» (mutación revertida) | `PermisosDocentesTest` con `VerificadorDePermisos`; `ErroresDocentesTest` (2) | 2026-10-24 |
| Docentes, Académico, la plataforma de errores y el proceso `datos` completos | `./gradlew :servicios:docentes:check :servicios:docentes:integrationTest :servicios:academico:check :plataforma:errores:check :apps:datos:check` en verde: Docentes 77 pruebas (80,8 % de líneas en el informe de las unitarias; los adaptadores de salida los cubren las de integración), Académico 242 (77,9 %), errores 21 (77,2 %), `datos` 6 | JaCoCo (mínimo 60 %) | 2026-10-24 |

## Consultar y buscar (US2)

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| **Escenarios del issue «Consulta» y «Ficha académica»:** «peraltá» y la cédula encuentran al docente; la ficha trae el campo amplio y específico de cada título, las horas de cada curso, los nombres de los campos amplios y de las asignaturas afines | Pasan, y cada respuesta cumple el contrato; las referencias traen las carreras, las asignaturas con su carrera y los 11 campos amplios; 404 con un docente que no existe | `DocentesContratoTest`; `ConsultarDocentesServicioTest` (3, con los 47 de prueba) | 2026-10-24 |

## Corregir (US3)

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| Corregir la dedicación, agregar un curso y quitar la experiencia; el correo de otro docente da 409 y el propio no; una asignatura afín que no existe da 400 en `ficha.asignaturasAfines[1]` | Pasan; el registro tiene `DOCENTE_CORREGIDO` con cada cambio; sin cambios no registra | `DocentesContratoTest`, `MantenerDocentesServicioTest` | 2026-10-24 |

## Estado laboral (US4)

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| Activo → licencia → activo, con su registro; el mismo estado no registra; «JUBILADO» da 400 con «usa ACTIVO, LICENCIA o INACTIVO» | Pasan | `DocentesContratoTest`, `MantenerDocentesServicioTest` | 2026-10-24 |

## Pantalla (US5)

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| «Docentes»: la lista en español, la búsqueda y los filtros (con sus parámetros), la ficha con los campos y las horas, solo lectura sin escritura, registrar con la categoría que sigue al tipo de personal y «Falta: …», corregir agregando un curso y quitando la experiencia, el 400 junto a su campo y a su fila, el 409 y el estado; el menú con las tres lecturas | 10 de 10 pasan. La prueba del registro llenaba ~15 campos letra por letra y pasaba de los 15 s con la cobertura activa: ahora pega cada texto de una vez y tarda 2,9 s | `frontend/src/pantallas/docentes/Docentes.test.tsx` | 2026-10-24 |
| La SPA completa | 179 pruebas en 22 archivos; 93,4 % de líneas; `lint`, `tipos` y `formato:verificar` en verde | `npx vitest run --coverage`, `npm run lint`, `npm run tipos` | 2026-10-24 |
| En el navegador: Talento Humano registra un docente con un curso, lo busca por «peralta» y su ficha dice «PROGRAMACIÓN EN JAVA · 40 h (2023)»; axe-core sin fallas WCAG 2.1 AA en la lista, la ficha y los diálogos de registrar y de estado | 5 de 5 pasan. axe encontró `scrollable-region-focusable` en la ficha: su contenido se desplaza y no tenía nada que recibiera el foco; ahora la región recibe el foco y se desplaza con el teclado | `frontend/e2e/docentes.spec.ts`; la suite completa de Playwright, 83 de 83 | 2026-10-24 |

## Cierre

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| `deploy/probar_docentes.sh` sobre el perfil reconstruido, dos veces seguidas | Todos los pasos pasan las dos veces, en 9 s y 6 s: 9 de 9 datos maestros; 47 de 47 docentes registrados y cada uno encontrado por su cédula (SC-002); «PERALTÁ» encuentra a los dos PERALTA; la carrera 550613A01-P-0301 trae sus 6; la ficha de 0109990010 con 04/041, los nombres de los campos, las horas de los cursos y su maestría; corregir a MT con 20 h y una asignatura afín; «XX» da 400 con «usa TC, MT o TP»; la afín inexistente, 400 en `ficha.asignaturasAfines[0]`; los dos 409; licencia y vuelta; consolidación lee y recibe 403 al escribir; docente.demo, 403; el registro con 47 + 1 + 2 acciones. La segunda corrida no encontró nada de la primera: la limpieza borra lo creado | Salida del script | 2026-10-24 |
| El tiempo de la búsqueda (meta del plan) | 12 ms y 9 ms (el mejor de tres), con la meta en 300 ms y la falla desde 500 ms | `probar_docentes.sh`, paso 8 | 2026-10-24 |
| Ninguna cédula en los logs (principio IX, análisis U1) | Después de las corridas, `docker compose logs gateway datos` no contiene 0109990010, 0109990028 ni 0109999904 | `grep -c` sobre los logs | 2026-10-24 |
| `shellcheck` de `probar_docentes.sh` | Sin observaciones | `koalaman/shellcheck:stable`, sobre una copia en el scratchpad | 2026-10-24 |
| Los 12 scripts de punta a punta sobre el perfil | 12 de 12 terminan con 0 | `probar_autenticacion`, `errores`, `eventos`, `spa`, `normativa`, `periodos`, `estructura`, `plantillas`, `subida`, `carreras`, `oferta` y `docentes` | 2026-10-24 |
| `deploy/equipo/probar_equipo.sh` con un constructor limpio (`BUILDX_BUILDER=mdd-limpio`) | Todos los pasos OK; memoria del equipo después de iniciar sesión: 1402,9 MiB | Salida del script | 2026-10-24 |
| Todo el backend: `./gradlew build integrationTest` | 1012 pruebas, 0 fallas, en 4 min 3 s | Gradle | 2026-10-24 |
| Gitleaks v8.30.1 sobre un clon en el scratchpad (montado `:ro,Z`) | Sin fugas, 59 commits | `gitleaks git --redact` | 2026-10-24 |

## Equipo de demostración

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|

## Notas

- Que las horas contratadas cuadren con la dedicación (RN-01), que el ocasional sea solo TC o MT (POL 5.1), que el contrato cubra el periodo y las horas que cambia la condición especial no se revisan aquí: son de la normativa del periodo (análisis C2).

# Evidencia: F03-H05

Registro de las comprobaciones de la historia (issue #39): registrar los cargos y los proyectos aprobados en el servicio de Docentes, con las referencias de Académico y los permisos de D-70 (RF-18, RN-12, RN-20, RN-25; D-72, D-73). Cada fila dice el comando y la salida resumida, o enlaza al run de GitHub Actions. Los datos son ficticios; las contraseñas de local y de la CI también.

## Entorno

| Pieza | Versión | Notas |
|---|---|---|
| Sistema | Fedora Linux 44 (SELinux en modo *enforcing*) | |
| Java | OpenJDK 25.0.4.1 | Gradle con el *wrapper* del repositorio. |
| Node | 24 (`~/.local/node-24`) | Vitest y Playwright. |
| Docker Engine | 29.8.2 (Fedora) | Con `sg docker -c '…'`. |

## Base: contrato, catálogo, referencias de Académico, dominio, migración y repositorios

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| Datos de prueba de las cargas 01, 03, 08 y 09 en Docentes | `01_estructura_organica.csv`, `03_ciclos_academicos.csv`, `08_cargos_autoridades.csv` (21 cargos) y `09_proyectos_practicas.csv` (11 proyectos), ficticios (RNF-SEG-06), copiados de `backend/servicios/cargas/src/test/resources/cargas/datos-de-prueba/` a `backend/servicios/docentes/src/test/resources/docentes/datos-de-prueba/`, junto a las 02, 04 y 06 | SHA-256 `314995c68138…`, `5217bb27074d…`, `4784c87fcbac…`, `fd955f518167…` | 2026-10-25 |
| Los contratos: Docentes con 7 operaciones más (tags `Cargos` y `Proyectos`), Académico con unidades y periodos en `consultarReferencias`, y los 4 códigos `DOCENTES_*` con sus 8 columnas | Los YAML cargan; los modelos de Java y los tipos de la SPA se generan; `verificar:ruptura` pasa (el cambio de Académico solo agrega propiedades). Las lecturas son `[DOCENTES, CATALOGOS, DISTRIBUTIVO]:LECTURA`, sin `PROYECTOS:LECTURA` (D-64, aclaración 8) | `contratos/docentes/openapi.yaml`, `contratos/academico/openapi.yaml`, `contratos/comun/errores.md`, `npm run verificar:ruptura` | 2026-10-25 |
| El diccionario lista los 16 cargos de gestor (D-73) | El catálogo `CARGOS` tiene 22 valores, sin la fila del rango; la plantilla y el diccionario de la carga 8 salen de él. `DiccionarioYamlTest` comprueba los 22; Cargas, 96 pruebas sin fallas | `./gradlew :servicios:cargas:test` | 2026-10-25 |
| Académico publica unidades y periodos en sus referencias | `ConsultarReferenciasServicioTest` (todas las unidades, activas e inactivas, con su tipo y su gestión; todos los periodos con sus fechas, por código) y `CarrerasContratoTest` (la respuesta valida contra el contrato). Académico, 218 pruebas sin fallas | `./gradlew :servicios:academico:test` | 2026-10-25 |
| Las reglas del cargo, sin dobles | `CargoTest` (17): coordinador con y sin carrera, unidad que falta, no existe o está inactiva, gestor con la unidad de otra gestión, miembro de comisión fuera de una comisión, el fin de funciones en sus límites (igual, un día antes, un día después, exactamente 2 años y 2 años y un día), el documento de 60 y 61, el docente en licencia, inactivo, de medio tiempo o de tiempo parcial (`DocenteNoHabilitado`, sin la cédula en el mensaje), la corrección con sus cambios. **Los 21 cargos de la carga 08 son válidos**; los 7 coordinadores, de carreras distintas. Encontró que los 3 miembros de comisión de prueba tienen su fin de funciones el 2026-08-31: hoy aparecen con `FECHA_FIN_VENCIDA`, como se espera | `./gradlew :servicios:docentes:test` | 2026-10-25 |
| Las reglas del proyecto, sin dobles | `ProyectoTest`: el código con minúsculas, espacio o `_`, de 30 y de 31; el nombre de 250 y 251; carreras vacías, repetidas, de 20 y de 21, y una que no existe con su posición; la resolución de 60 y 61; la vigencia; el director y un participante no registrados (sin la cédula en el mensaje); participantes repetidos o sin rol; las horas `0.01`, `2.55`, `2.50`, `99.99`, `999.9` y `24` pasan, y `0`, `-1`, `2.555`, `100.25`, `1000` y `99999` no; la suma exacta pasa y una décima más no. **Los 11 proyectos de la carga 09 son válidos** | `./gradlew :servicios:docentes:test` | 2026-10-25 |
| Referencias y valores permitidos | `ReferenciasTest` (las 24 unidades y los periodos de las cargas 01 y 03; un periodo que no existe da `DatoInvalido` en `periodo`), `ValoresPermitidosTest` (los 22 cargos, los 2 estados del cargo, los 4 tipos y los 4 estados del proyecto y los 4 roles son los del diccionario) y `AcademicoDesdePuertosTest` (traduce las unidades y los periodos). El dominio y el adaptador: 89 pruebas sin fallas | `./gradlew :servicios:docentes:test --tests '*dominio*'` | 2026-10-25 |
| Las pruebas del dominio fallan si la regla se rompe | Desactivar la comprobación de la suma de horas hace fallar `ProyectoTest` (mutación revertida) | `./gradlew :servicios:docentes:test --tests '*ProyectoTest*'` | 2026-10-25 |

## Cargos (US1)

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|

## Requisitos del cargo (US2)

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|

## Proyectos (US3)

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|

## Asignables (US4)

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|

## Corregir y finalizar (US5)

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|

## Pantallas (US6)

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|

## Cierre

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|

## Equipo de demostración

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|

## Notas

- El número de gestores por tipo de IST (RN-10), el aviso del coordinador académico institucional (RN-09), las horas de gestión por cargo y el máximo de 16 h de docencia con proyecto son del motor (F06).

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

## Registrar (US1)

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|

## Consultar y buscar (US2)

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|

## Corregir (US3)

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|

## Estado laboral (US4)

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|

## Pantalla (US5)

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|

## Cierre

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|

## Equipo de demostración

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|

## Notas

- Que las horas contratadas cuadren con la dedicación (RN-01), que el ocasional sea solo TC o MT (POL 5.1), que el contrato cubra el periodo y las horas que cambia la condición especial no se revisan aquí: son de la normativa del periodo (análisis C2).

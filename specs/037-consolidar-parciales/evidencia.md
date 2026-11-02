# Evidencia: F10-H02

Registro de las comprobaciones de la historia (issue #54): consolidar los parciales y adjuntar el informe sustentado.

## Entorno

| Pieza | Versión | Notas |
|---|---|---|
| Sistema | Fedora Linux 44 (SELinux en modo *enforcing*) | |
| Java | OpenJDK 25 | Gradle con el *wrapper* del repositorio. |
| Node | 24 (`~/.local/node-24`) | Vitest y Playwright. |
| Docker Engine | Fedora | Con `sg docker -c '…'`. |

## Consolidar

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| Suma aparte (SC-001) | Las 7 carreras con sus paralelos, igual a una suma desde las tablas de Académico; las abiertas, iguales al panel | `probar_consolidado.sh`, paso 2 | 2026-11-02 |
| Quién y cuándo | Se guarda y va al registro; se repite y vale la última; admite abiertas | `ConsolidarServicioTest`, `probar_consolidado.sh`, paso 4 | 2026-11-02 |
| No bloquea | El estado de los parciales no cambia, y las carreras siguen editando | `ConsolidadoDelPeriodoTest`, `probar_consolidado.sh`, paso 5 | 2026-11-02 |
| Horas | Por carrera y totales, sin contar retirados | `ConsolidadoDelPeriodoTest`, `probar_consolidado.sh`, paso 5 | 2026-11-02 |
| Desactualizado | Una propuesta lo deja desactualizado y repetir lo arregla; justificar y adjuntar no | `ConsolidadoDelPeriodoTest`, `RepositorioConsolidacionIntegrationTest`, `probar_consolidado.sh`, pasos 5 y 6 | 2026-11-02 |
| A la vez | Dos consolidaciones dejan las dos filas, y una vigente | `RepositorioConsolidacionIntegrationTest` | 2026-11-02 |

## El informe sustentado

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| 10 MB exactos | Se adjunta por el API Gateway y por nginx, y se descarga idéntico | `probar_consolidado.sh`, paso 6 | 2026-11-02 |
| Lo que no es un PDF (SC-005) | Texto, imagen, vacío y un PDF cortado, con nombre `.pdf`, se rechazan sin guardar | `ArchivoPdfTest`, `probar_consolidado.sh`, paso 7 | 2026-11-02 |
| Tamaño | 1 byte más de 10 MB: `GENERACION_INFORME_DEMASIADO_GRANDE`; 12 MB: `COMUN_CONTENIDO_DEMASIADO_GRANDE` | `probar_consolidado.sh`, paso 7 | 2026-11-02 |
| El nombre | Sin ruta ni caracteres de control; nunca decide dónde se guarda; la descarga lleva uno fijo | `ArchivoPdfTest`, `AlmacenEnDiscoIntegrationTest`, `ConsolidadoContratoTest` | 2026-11-02 |
| Reemplazar | El nuevo es el vigente, y el anterior se conserva | `RepositorioConsolidacionIntegrationTest`, `probar_consolidado.sh`, paso 8 | 2026-11-02 |
| Un archivo dañado o que falta | `GENERACION_INFORME_ILEGIBLE`, nunca un archivo dañado | `InformeSustentadoServicioTest`, `probar_consolidado.sh`, paso 9 | 2026-11-02 |
| Falla el almacén | No queda fila | `InformeSustentadoServicioTest` | 2026-11-02 |
| Sobrevive | Recrear el contenedor no pierde el informe | `probar_consolidado.sh`, paso 10 | 2026-11-02 |
| Volumen | `distributivo` escribe en él, con la imagen y el volumen de verdad | `probar_equipo.sh`, paso 12 | 2026-11-02 |

## Permisos, rendimiento y fallos

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| Permisos | Consolidación escribe; el Vicerrectorado y el Administrador leen y descargan; un coordinador recibe 403 en las cuatro | `ConsolidadoContratoTest`, `probar_consolidado.sh`, paso 11 | 2026-11-02 |
| Abrir (SC-002) | 0,52 s con los datos de prueba (meta: menos de 5 s) | `probar_consolidado.sh`, paso 2 | 2026-11-02 |
| Adjuntar 10 MB (SC-003) | 0,45 s por el API Gateway y 0,42 s por nginx (meta: menos de 10 s) | `probar_consolidado.sh`, paso 6 | 2026-11-02 |
| Sin el proceso `datos` (SC-006) | 504 en 5,0 s (meta: menos de 10 s), sin presentarlo como vacío, y vuelve | `probar_consolidado.sh`, paso 12 | 2026-11-02 |
| Registros | El contenido del PDF no aparece; 0 posibles secretos en 1222 líneas | `probar_consolidado.sh`, paso 13, y registros del perfil | 2026-11-02 |

## La verificación de cierre

| Comprobación | Resultado | Fecha |
|---|---|---|
| Backend, `check jacocoTestReport` | 2146 pruebas, 0 fallas, 4 omitidas. Spotless y SpotBugs incluidos | 2026-11-02 |
| `integrationTest` completo | 224 pruebas, 0 fallas | 2026-11-02 |
| Cobertura por módulo | Generación 89,9 %, Normativa 77,5 %, Docentes 80,7 %, Académico 82,5 % | 2026-11-02 |
| Cobertura de la historia | `MotorReglas`, `ConsolidadoDelPeriodo` y los servicios, 100 % de líneas; `ArchivoPdf` 92,6 %, porque no se llega a su rama sin SHA-256 | 2026-11-02 |
| SPA | Vitest 441, Playwright 111; 94,7 % de sentencias; ESLint, `npm run tipos` y Prettier | 2026-11-02 |
| Scripts de `deploy/` | 27 de 27, sin avisos, con la base limpia | 2026-11-02 |
| `deploy/equipo/probar_equipo.sh` | Despliega, vuelve atrás e informa a GitHub, con el volumen escribible | 2026-11-02 |
| shellcheck | 0 hallazgos, con una copia nueva en el scratchpad | 2026-11-02 |
| Gitleaks | Sin filtraciones, con un clon nuevo en el scratchpad y `.gitleaks.toml`, en modo `git` como CI | 2026-11-02 |

JaCoCo cuenta solo las pruebas unitarias, así que `AlmacenEnDisco` y `RepositorioConsolidacionJdbc` salen con 0 %. Los cubren sus pruebas de integración.

## Desviaciones

- **Pruebas primero.** Se vio fallar cada grupo antes de implementar, en un comando aparte.
- **T007.** El último cambio de asignaciones va en `RepositorioConsolidacion`, no en `RegistroDeCambios`, que es de escritura.
- **El botón de adjuntar.** Primero fue una etiqueta, que no sirve con teclado. Se cambió por el patrón de «Plantillas de carga» antes de cerrar.
- **Estado de los parciales.** Hoy todos están en elaboración, porque enviarlos llega con F12-H02.

## La comprobación del autor

Pendiente, después del despliegue (SC-004, con cronómetro).

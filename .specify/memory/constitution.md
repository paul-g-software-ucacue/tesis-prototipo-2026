# Constitución del desarrollo del MDD

- **Versión:** 1.0.1, enmienda de parche del 11/10/2026 (D-28): en la sección 3, «JUnit 5» pasa a «JUnit Jupiter 6», la versión que fija el BOM de Spring Boot (6.0.3 con Spring Boot 4.1.1). Se detectó al planificar B00-H01 y no cambia ningún principio.
- **Versión 1.0:** ratificada el 11/10/2026. Cambios desde la 0.5: se cierran los pendientes de ratificación; el repositorio privado y el proyecto de GitHub Projects ya existen (pendiente 6), y las comprobaciones técnicas que quedaban pasan a ser criterios de las historias de la ola 0 (sección 8).
- **Fecha:** 11/10/2026
- **Autor:** el autor de la tesis
- **Uso:** al iniciar el desarrollo con SDD (desarrollo guiado por especificaciones), este texto se copia como constitución del proyecto (por ejemplo, `.specify/memory/constitution.md` si se usa Spec Kit) y se ratifica según la sección 8.
- **Fuentes:** ERS v2.1 (`ERS_MDD_Requerimientos.md`), anexo de firmas (`Diseno_firmas_cadena_aprobacion.md`), ADR-001 a ADR-011 (`lean_inception/adr/`), matriz de decisiones D-01 a D-27, estándares de ingeniería backend del autor (`sdd/fuentes/estandares_backend_autor.md`), modelo de dominio (`sdd/modelo_dominio.md`), metodología de desarrollo (`sdd/metodologia_desarrollo.md`), pipeline de CI/CD (`sdd/pipeline_cicd.md`) y paquete de entrada al Lean Inception (`paquete_lean_inception/`).

> **Vigente.** Esta constitución prevalece sobre especificaciones y planes. Para cambiarla se registra primero una decisión nueva (D-28 en adelante) o un ADR (ADR-012 en adelante) y se sube la versión según la sección 7.

## 1. Qué se construye

El MDD es un prototipo web que carga los datos del periodo, construye el distributivo docente, lo valida contra la política SENESCYT 2024, recoge en orden tres firmas electrónicas con FirmaEC, registra la resolución del OCS, lo publica y exporta el CSV para SIGA. El alcance es el de la ERS v2.1: 42 RF Must más RF-37 (resolución) en cuatro olas. Lo hace una persona en unas 585 h (ADR-007).

## 2. Principios

Cada principio dice qué se debe hacer (**DEBE**), qué no (**NO DEBE**) y por qué.

### I. Arquitectura limpia con puertos y adaptadores

- Cada servicio se organiza en cinco capas. Las dependencias del código apuntan hacia el dominio, nunca desde el dominio hacia los detalles (ADR-009).

  | Capa | Contiene | NO DEBE |
  |---|---|---|
  | Dominio | Entidades, agregados, objetos de valor, servicios de dominio y errores de negocio. | Importar el framework web, el ORM, librerías de PDF ni de HTTP, ni usar códigos HTTP. |
  | Aplicación | Casos de uso: orquestan el dominio y los puertos, y fijan los límites de cada transacción. | Depender de implementaciones concretas de infraestructura ni dejar pasar detalles técnicos al consumidor. |
  | Puertos | Contratos de entrada (lo que ofrece el servicio) y de salida (persistencia, firma, otros servicios). | Exponer detalles de infraestructura. |
  | Adaptadores | Controladores REST, repositorios, clientes HTTP y oyentes de eventos. Traducen entre los modelos externos y los internos. | Dejar que el protocolo contamine el dominio. |
  | Infraestructura | Configuración de Spring y de la inyección de dependencias, base de datos, registros, métricas y mecanismos técnicos de fallos. | Contener reglas de negocio. |

- Los puertos de entrada son obligatorios en los servicios de los subdominios centrales. En los de soporte y genéricos, el controlador puede llamar directamente al servicio de aplicación (principio II).
- Todo sistema externo y todo efecto lateral se usa a través de un puerto:

  | Puerto | Adaptador del prototipo | Adaptador futuro | Fuente |
  |---|---|---|---|
  | Fuente de datos maestros | Archivo CSV o XLSX (10 cargas) | API de Moodle (RF-57, C) | RF-05 a RF-10, RF-55, RNF-SEG-05 |
  | Firma electrónica | Validador PAdES para FirmaEC: descargar, firmar y subir | Firma integrada | RF-38, RF-39, RNF-SEG-08, ADR-007 |
  | Exportación a SIGA | CSV con mapeo configurable | API de SIGA, si llega a existir | RF-52, RF-53, ADR-002 |
  | Almacenamiento de archivos | Sistema de archivos, separado de la base de datos | Almacenamiento de objetos | RNF-POR-01 |
  | Notificaciones | Bandeja interna | Correo | RF-41, V-09 |
  | Configuración del flujo | Archivo versionado por periodo | — | RF-34, RF-58, ADR-007 |
  | Reloj | Reloj del sistema | — | Plazos y vigencia de certificados; permite probar fechas |

- **Por qué:** RNF-MAN-01 exige sustituir un sistema externo sin tocar el núcleo, y SIGA, Moodle y Quipux no tienen API hoy.

### II. DDD con profundidad según el subdominio

- Los subdominios **centrales** usan DDD táctico completo: agregados con sus invariantes, objetos de valor, eventos de dominio y repositorios.
- Los subdominios **de soporte y genéricos** usan un CRUD simple detrás de sus puertos, sin agregados elaborados.

  | Tipo | Módulos (ERS 8.5) | Por qué |
  |---|---|---|
  | Central | 5 Generación (construcción, validación y consolidación), 12 Normativa, 6 Validación OCS (flujo de estados), 10 Firma | Aquí están las reglas de la política, las transiciones del flujo y la cadena de firmas: lo que diferencia al MDD. |
  | Soporte | 3 Cargas, 4 Docentes, 11 Académico, 8 Reporting, 9 Dashboard | Necesarios, pero con poca lógica propia. |
  | Genérico | 1 Usuarios, 2 Roles y Permisos, 7 Notificaciones | Problemas resueltos. |

- **Por qué:** con unas 78 h de holgura, DDD táctico en todos los módulos no cabe. El esfuerzo se pone donde está el riesgo de construir mal el producto.

### III. Lenguaje ubicuo

- El glosario (sección 7 de `sdd/modelo_dominio.md`) es la fuente única de los términos. Las especificaciones, los contratos de API, el código del dominio, las pruebas y la interfaz usan las mismas palabras con el mismo significado.
- Un término nuevo entra primero al glosario y después al código. NO DEBE haber sinónimos para un mismo concepto: «distributivo» no se llama también «carga» ni «plan».
- Las especificaciones de feature se escriben solo en lenguaje del negocio, sin nombres de tablas, clases ni endpoints.
- **Identificadores (ADR-008):** el dominio en español sin tildes y con los términos del glosario (`Distributivo`, `VersionCongelada`, `congelar()`); la infraestructura técnica en inglés (`Repository`, `Controller`, `Adapter`). La interfaz y los mensajes, en español.
- **Por qué:** el Vicerrectorado, los coordinadores y el código deben entender lo mismo por cada término. Además, la tesis se defiende ante un tribunal que lee las dos cosas.

### IV. API primero (API-first)

- Cada módulo publica un contrato **OpenAPI 3.0.3** en `contratos/`, que se escribe y revisa **antes** de implementarlo. El contrato es parte de la especificación de la feature. Se usa la 3.0.3 porque openapi-generator la soporta por completo (ADR-008).
- openapi-generator genera las interfaces de los controladores Spring y openapi-typescript genera los tipos del frontend: si la implementación o la SPA no cumplen el contrato, no compilan.
- Entre procesos, los módulos se llaman por HTTP según ese contrato. Dentro de un proceso, por la interfaz de aplicación que refleja el mismo contrato (ADR-006).
- Los errores siguen el modelo común del principio XIII: un esquema `Error` compartido en `contratos/` y un catálogo de códigos que se escribe junto con el contrato (ADR-009).
- Las pruebas de contrato (swagger-request-validator) verifican que las respuestas cumplen el OpenAPI. Un cambio incompatible exige una versión nueva de la API.
- **Por qué:** fijar el contrato primero reduce el riesgo de construir un módulo que no encaja, y le da al asistente de IA un objetivo verificable.

### V. Microservicios y límites de servicio

- El backend es una arquitectura de microservicios. Cada uno de los **12 servicios** de la ERS 8.5 es un microservicio: tiene una capacidad de negocio, su contrato OpenAPI, su esquema en PostgreSQL y sus reglas, y se puede desplegar solo sin cambiar código (ADR-009).
- En el prototipo se despliegan agrupados en 4 procesos, más el API Gateway: identidad (1, 2, 7), datos (3, 4, 11, 12), distributivo (5, 6, 10) y salida (8, 9). Ver ADR-006. No es una excepción a la regla: 12 JVM no caben en la VM de 4 GB (R-19), y distribuir sin una razón técnica es justo lo que este principio evita.
- Un servicio NO DEBE leer ni escribir tablas de otro, ni depender de sus clases internas, entidades de dominio o repositorios. Solo usa los DTO de su contrato, generados del OpenAPI. Los dos servicios de un mismo contexto delimitado (por ejemplo, Académico y Docentes) comparten los términos del glosario, no las clases.
- Dentro de un proceso, los servicios se llaman por la interfaz de aplicación que refleja el contrato; entre procesos, por HTTP según ese contrato (ADR-006).
- **Fallos entre procesos:**
  - Toda llamada HTTP entre procesos tiene un plazo de espera.
  - Lo que no puede perderse, como la bitácora y las notificaciones, se publica como evento de dominio dentro de la transacción del servicio. El registro persistente de publicaciones de Spring Modulith lo guarda, y un oyente del mismo proceso hace la llamada. Si la llamada falla, la publicación queda pendiente y una tarea programada la vuelve a enviar.
  - El receptor es idempotente por el identificador del evento. No hay colas ni broker (principio X).
- **Transacciones:** cada transacción queda dentro de un servicio. Una carga de datos la aplica el servicio dueño de la entidad, entera o nada (RF-10), y el hash del archivo es la clave de idempotencia.
- Una prueba automática de arquitectura falla si se cruza un límite: Spring Modulith (`ApplicationModules.verify()`) para las dependencias entre servicios de un proceso, y ArchUnit para el sentido de las cinco capas.
- Cada servicio es un subproyecto de Gradle y cada proceso es una aplicación Spring Boot que ensambla sus servicios (ADR-008, ADR-011).
- **Por qué:** así cada servicio se puede separar en su propio despliegue más adelante sin reescribirlo, que es lo que la tesis demuestra en lugar del despliegue distribuido. Y la bitácora queda completa aunque un proceso se caiga (objetivo 2).

### VI. Las reglas son datos, no código

- Las cifras de la política (horas, rangos, tipos de IST, ventanas de jornada) son parámetros versionados por periodo (RN-01 a RN-26, RF-19). NO DEBEN quedar fijas en el código.
- El flujo y el árbol de firmas se configuran en un archivo versionado. El esqueleto fijo BORRADOR_CARRERA → CONGELADO_EN_FIRMA → EN_OCS → APROBADO → PUBLICADO no se puede romper desde la configuración, y no hay editor de flujo ni motor BPM (ADR-007).
- **Por qué:** la política cambia (E-JS, E-FL) y las fuentes no coinciden en el orden de aprobación (C-04).

### VII. Inmutabilidad y trazabilidad

- Las cargas y los cambios de estado son transaccionales (RNF-INT-01).
- Una versión congelada es inmutable: se identifica por su hash y cualquier cambio crea una versión nueva que anula las firmas anteriores (RF-59).
- Toda acción relevante deja su registro en la bitácora (RF-44, RNF-INT-02). Cada asignación guarda su justificación: afinidad, disponibilidad o excepción (RNF-EQU-01).
- **Por qué:** la firma solo vale si se puede probar sobre qué versión exacta se firmó.

### VIII. Primero las pruebas en el dominio

- Los criterios de aceptación de la ERS se escriben como escenarios Dado–Cuando–Entonces antes de implementar la feature.
- El dominio central se desarrolla guiado por pruebas, con al menos un 80 % de cobertura de líneas en cada paquete `dominio` de los servicios 5, 6, 10 y 12 (RNF-MAN-02).
- Todo módulo tiene al menos un 60 % de cobertura de líneas. JaCoCo verifica los dos umbrales en cada pull request y el pull request no se fusiona si alguno falla (ADR-011).
- Hay cuatro niveles de prueba:
  1. Unitarias del dominio.
  2. De contrato por cada API.
  3. De integración de los adaptadores.
  4. Una de punta a punta del ciclo completo por ola.
- Las pruebas de contrato también cubren los errores: cada error del catálogo llega con su código y su estado HTTP, y una respuesta 500 no contiene trazas, nombres de clases, SQL ni mensajes de una excepción técnica (principio XIII).
- Las reglas de negocio se prueban sin infraestructura y sin dobles: el dominio es Java puro.
- Mockito se usa solo en la capa de aplicación, para reemplazar los puertos de salida. Los adaptadores se prueban con Testcontainers y PostgreSQL real, no simulando el repositorio, porque una simulación no prueba el SQL (ADR-011).
- Las pruebas usan solo los datos ficticios de `cargas/datos_prueba/` y sus escenarios de nómina de ±15 % (D-06).
- **Por qué:** con SDD el código lo genera un asistente. Las pruebas son la forma de comprobar que hace lo especificado.

### IX. Seguridad y datos personales

- HTTPS, contraseñas con bcrypt o Argon2, mínimo privilegio validado en el backend según la matriz de permisos (D-16), y protección OWASP Top 10, incluida la inyección de fórmulas en los CSV (RNF-SEG-01 a 04).
- Solo se usan datos ficticios o agregados. NO DEBEN entrar al repositorio nombres ni cédulas reales, ni datos de la matriz de matriculados (RNF-SEG-06, LOPDP).
- Los certificados de la demo los emite una CA de prueba, que solo se acepta en el entorno de demo (S-10). El certificado real del autor NO DEBE entrar al repositorio; Gitleaks rechaza los `.p12`, `.pfx`, `.pem` y similares fuera de la carpeta de la CA de prueba.
- **Ciclo de desarrollo seguro (SSDLC).** La seguridad se aplica en cada fase, no solo al final (ADR-011, `sdd/pipeline_cicd.md`):
  - *Requisitos:* RNF-SEG-01 a 08.
  - *Diseño:* mínimo privilegio (D-16), errores que no filtran información (principio XIII), firma validada en el servidor (D-23) y límites entre servicios (principio V).
  - *Implementación:* Spotless y SpotBugs con FindSecBugs en la etapa de lint, y Gitleaks en cada commit (pre-commit) y en cada pull request.
  - *Verificación:* pruebas con cobertura mínima, de arquitectura y de contrato en cada pull request, y OWASP ZAP sin hallazgos altos al cerrar cada ola (RNF-SEG-04).
  - *Entrega:* un SBOM CycloneDX por módulo y por aplicación, y Grype sin vulnerabilidades altas o críticas con versión corregida, en las dependencias y en las imágenes.
  - *Operación:* despliegue automático en el VPS de demostración, con acceso restringido, *healthchecks* y vuelta atrás.
- **El propio pipeline es parte de la superficie de ataque.** Las acciones de GitHub se fijan por SHA, los permisos son mínimos y los secretos del despliegue solo existen en su entorno (ADR-011).

### X. Simplicidad y alcance

- Solo se construye lo que la ERS v2.1 marca como Must para el prototipo. Los RF W no se construyen. Los Should (RF-20, RF-60, RN-26, la votación de RF-37 y la interfaz de RF-57) solo entran si sobra holgura al cerrar la ola 4.
- No se agrega infraestructura que el alcance no pida: nada de colas, caché distribuida ni microservicios desplegados por separado.
- Toda funcionalidad nueva requiere una decisión registrada y su estimación.
- **Por qué:** la capacidad es de 663 h y la estimación es de 585 h.

### XI. Diseño pragmático

- SOLID, interfaces y patrones se usan cuando resuelven un problema concreto, no por regla:
  - Una interfaz se define cuando marca un límite de arquitectura (un puerto), permite cambiar la implementación o aísla una dependencia en las pruebas. NO DEBE haber una interfaz por cada clase.
  - La composición se prefiere a la herencia. La herencia solo se usa para una especialización auténtica que respeta el contrato del tipo base, nunca solo para reutilizar código.
  - NO DEBE haber fábricas, jerarquías, capas ni abstracciones especulativas.
- Antes de agregar una abstracción o un patrón se responden cinco preguntas. Si la respuesta no muestra un beneficio concreto, se elige la solución más sencilla.
  1. ¿Resuelve un problema real?
  2. ¿Reduce el acoplamiento o separa mejor las responsabilidades?
  3. ¿Facilita las pruebas o el mantenimiento?
  4. ¿Hay una alternativa más sencilla?
  5. ¿El beneficio justifica lo que cuesta construirla y mantenerla?
- Cada cambio es el más pequeño que cumple la historia. No se mezclan refactorizaciones ajenas a la historia ni se agregan dependencias sin justificación, y se preserva el comportamiento existente salvo que la especificación diga lo contrario.
- **Por qué:** con SDD el asistente de IA tiende a generar capas y patrones de más. Estas reglas le dan al autor un criterio para rechazarlos al revisar cada pull request.

### XII. Documentación con JavaDoc

- El código Java se documenta con JavaDoc en español, también en las clases técnicas con nombre en inglés (ADR-009).
- El JavaDoc explica el contrato: qué hace el componente, qué condiciones exige, qué garantiza, qué efectos tiene y el porqué de una decisión que no es evidente. NO DEBE repetir lo que ya dicen el nombre y la firma, ni narrar la implementación línea por línea, ni inventar reglas, garantías o excepciones.
- En la práctica, siempre llevan JavaDoc los puertos (su contrato vale para cualquier implementación) y las clases públicas del dominio de los servicios centrales (sus invariantes). Un método cuyo nombre y firma lo dicen todo no lo lleva.
- `@param`, `@return` y `@throws` solo cuando aportan; nunca `@return` en un método `void`; `@throws` solo para las excepciones que de verdad ocurren; `@deprecated` con su alternativa. No se usa `@author`, y `@since` solo si se versiona la API.
- La plantilla y las reglas completas están en la sección 6 de `sdd/fuentes/estandares_backend_autor.md`.

### XIII. Errores

- **Clasificación.** Cada error es de uno de cuatro tipos, que se mantienen separados aunque compartan un estado HTTP:

  | Tipo | Qué representa | Dónde se define | Ejemplo en el MDD |
  |---|---|---|---|
  | Dominio | Una regla o invariante del negocio que no se cumple. | Dominio, como tipo propio sin HTTP. | Asignar 42 h a un docente TC (RN-02, RF-28). |
  | Aplicación | Una situación del caso de uso: recurso inexistente o conflicto de operación. | Aplicación. | Avanzar un distributivo que ya cambió de etapa. |
  | Infraestructura | Un fallo técnico: base de datos, red o proveedor externo. | Adaptadores. | El proceso de identidad no responde. |
  | Entrada | Una solicitud mal formada o un dato que no pasa la validación. | Adaptador de entrada. | Una devolución sin observación (RF-36). |

- **Traducción centralizada.** El dominio y la aplicación no conocen HTTP. Cada aplicación Spring Boot tiene un solo `@RestControllerAdvice` que traduce los errores reconocidos a su código público y a su estado HTTP, según un catálogo. Ningún controlador ni caso de uso repite esa traducción.
- **Modelo público.** Todas las respuestas de error usan el esquema `Error` de `contratos/`:

  ```java
  /**
   * Error público que devuelve la API.
   *
   * <p>Solo contiene información segura y estable: el consumidor
   * identifica el error por {@code codigo}, nunca por el texto.</p>
   *
   * @param titulo categoría breve del error
   * @param descripcion qué ocurrió y qué puede hacer el usuario, en español
   * @param codigo identificador estable del catálogo de errores
   * @param detalles campos que no pasaron la validación; solo en errores de entrada
   * @param idIncidente identificador para buscar el incidente en los registros;
   *                    solo en errores técnicos
   */
  public record ErrorResponse(
          String titulo,
          String descripcion,
          String codigo,
          List<DetalleError> detalles,
          String idIncidente
  ) {
  }
  ```

  `titulo`, `descripcion` y `codigo` son obligatorios; `detalles` (una lista de `DetalleError` con `campo` y `descripcion`) e `idIncidente` son opcionales. `descripcion` dice la acción correctiva (RNF-USA-01). El record vive en el adaptador de entrada; el dominio y la aplicación no dependen de él.
- **Catálogo de códigos.** Cada código es estable, lleva el prefijo de su servicio y no depende del texto ni del mensaje de una excepción (por ejemplo, `GENERACION_HORAS_EXCEDIDAS`). El catálogo se escribe junto con el contrato de cada servicio, antes de implementar (principio IV).
- **Estados HTTP:**

  | Estado | Cuándo | Ejemplo en el MDD |
  |---|---|---|
  | 400 | Solicitud mal formada o dato inválido | Devolución sin observación (RF-36) |
  | 401 | Sin sesión o token inválido | Acceso sin iniciar sesión (RF-01) |
  | 403 | El rol no tiene el permiso | Avanzar sin ser el rol de la etapa (RF-34) |
  | 404 | Recurso inexistente | Una versión del distributivo que no existe |
  | 409 | Choca con el estado actual o con una regla dura | Horas laborales excedidas (RF-28); editar una versión congelada (RF-40) |
  | 422 | Solicitud bien formada cuyo contenido no se puede procesar | PDF firmado que no pasa la validación básica (RF-39, D-23) |
  | 500 | Fallo inesperado | Error no reconocido |
  | 502, 503, 504 | Falla, no está disponible o no responde a tiempo otro proceso | El proceso de datos no responde al construir el distributivo |

- **Lo que no es un error:** una inconsistencia blanda se devuelve en una respuesta correcta, con su advertencia, porque exige justificación pero no bloquea (RF-28, MD-07). Un archivo de carga con errores responde con el informe por fila (RF-09), no con un error HTTP.
- **Nunca se expone al consumidor:** trazas, nombres de clases, paquetes o métodos, SQL o detalles del esquema, el mensaje original de una excepción del framework, del controlador JDBC o de un proveedor, credenciales, tokens, direcciones internas ni datos personales que no hagan falta.
- **Error técnico inesperado:** se registra con su contexto técnico y su causa original, sin contraseñas, tokens ni datos personales (RNF-SEG-06). Se responde con un código genérico, una descripción segura, el estado que corresponda y el `idIncidente` para buscarlo en los registros.
- **Errores de otro servicio:** el servicio que recibe el error lo traduce a su propio código y contexto. No reenvía el cuerpo del error remoto.
- **Por qué:** los mensajes deben decirle al usuario qué hacer (RNF-USA-01) sin dar pistas a un atacante (RNF-SEG-04, OWASP).

## 3. Restricciones técnicas

| Tema | Restricción | Fuente |
|---|---|---|
| Infraestructura | Docker Compose sobre Ubuntu Server, compatible con aaPanel, en una VM de 2 vCPU y 4 GB. Los 4 procesos, el gateway y PostgreSQL deben caber con límites de memoria. La demostración corre en un VPS del autor del mismo tamaño; el pipeline no despliega en la VM del IST. | RNF-POR-01, RNF-POR-02, ADR-006, ADR-011 |
| Base de datos | PostgreSQL, un esquema por módulo. Los archivos (PDF y evidencias) van fuera de la base. | ERS 8.5, RNF-POR-01 |
| Frontend | SPA en español. Funciona en las dos últimas versiones de Chrome, Firefox y Edge, desde 1366 px de ancho, y cumple WCAG 2.1 AA en las pantallas principales. | RNF-COM-01, RNF-USA-01 |
| Archivos de intercambio | CSV según RFC 4180, en UTF-8, con encabezado y fechas ISO 8601. También se aceptan XLSX en las cargas. | RNF-COM-02 |
| Firma | PDF/A con hash. PAdES con FirmaEC, en el modo descargar, firmar y subir. Se validan el prefijo de bytes, que haya una sola firma nueva, el orden, el rol y la vigencia al subir; la cadena se valida contra raíces en un archivo y la identidad por el CN (D-23). No se valida sello de tiempo, LTV ni DocMDP. Solo firman F1, F2 y F3. | RNF-SEG-08, ADR-004, ADR-007, D-23 |
| Rendimiento | Se mide con k6 sobre el perfil consolidado y la meta se fija con lo medido. | D-15, RNF-REN-01, RNF-REN-02 |
| Licencias | Software libre y estándares abiertos. | RNF-LEG-01 |
| Backend | Java 25 y Spring Boot 4.1.1, multiproyecto de Gradle con Groovy DSL (12 servicios y 4 aplicaciones) y plugins de convención, Spring Modulith 2.1, Spring Data JPA en los adaptadores, Flyway por módulo, Spring Security con JWT RS256 y Argon2id, Spring Cloud Gateway (2025.1.2 o posterior), registro persistente de publicaciones de eventos de Spring Modulith, openapi-generator 7.16 o posterior con `useSpringBoot4` y `useJackson3`. | D-20, ADR-008, ADR-009, ADR-011 |
| Frontend | React con TypeScript y Vite, React Router, TanStack Query, React Hook Form con Zod, MUI en español, cliente generado desde OpenAPI; servido por Nginx. | D-20, ADR-008 |
| Documentos y firma | OpenHTMLtoPDF con plantillas Thymeleaf para el PDF/A (verificado con veraPDF); Apache PDFBox y Bouncy Castle para validar PAdES; Apache Commons CSV y Apache POI para las cargas. | ADR-007, ADR-008 |
| Pruebas | JUnit Jupiter 6 (la versión del BOM de Spring Boot: 6.0.3 con Boot 4.1.1), AssertJ, Mockito, Testcontainers, ArchUnit, Spring Modulith, swagger-request-validator, JaCoCo (60 % por módulo y 80 % en el dominio central); Vitest, Testing Library, axe-core y Playwright; k6 y OWASP ZAP al cerrar cada ola. | ADR-008, ADR-011, D-28 |
| Memoria | Límite explícito por JVM y por contenedor: cinco JVM y PostgreSQL deben caber en 4 GB (R-19), lo que se mide en la ola 0. | ADR-008 |
| Repositorio y CI/CD | Un repositorio privado de GitHub (monorepo) con GitHub Actions; la rama principal solo recibe pull requests con los checks en verde. Pipeline de seis etapas: lint, pruebas, secretos, SCA, publicación en Docker Hub (repositorio privado) y despliegue en el VPS (`sdd/pipeline_cicd.md`). | D-24, ADR-011 |
| Seguridad en el pipeline | Spotless, SpotBugs con FindSecBugs, Gitleaks, CycloneDX y Grype; acciones fijadas por SHA. | ADR-011 |

## 4. Mapa de contextos

El mapa está en la sección 3 de `sdd/modelo_dominio.md` (v1.0, aprobado el 11/10/2026), con el diagrama `diagramas/05_mapa_contextos.png`. Hay 9 contextos para los 12 módulos, y cada uno cae entero en un proceso de ADR-006:

| Contexto delimitado | Módulos | Tipo | Proceso |
|---|---|---|---|
| Identidad y acceso (incluye la bitácora) | 1, 2 | Genérico | identidad |
| Notificaciones | 7 | Genérico | identidad |
| Ingesta de datos | 3 | Soporte | datos |
| Datos maestros | 11, 4 | Soporte | datos |
| Normativa | 12 | Central | datos |
| Distributivo | 5 | Central | distributivo |
| Aprobación | 6 | Central | distributivo |
| Firma | 10 | Central | distributivo |
| Publicación y consulta | 8, 9 | Soporte | salida |

Las relaciones entre contextos, el lenguaje publicado, los agregados de los contextos centrales y los eventos de dominio están en las secciones 3 a 6 de ese documento. Si se cambia un contexto o una relación, se cambia allí y se revisa esta tabla.

## 5. Lenguaje ubicuo

El glosario está en la sección 7 de `sdd/modelo_dominio.md`, y los términos prohibidos en la sección 8. Reemplaza a la semilla de 17 términos de la versión 0.2. Un término nuevo se agrega allí antes de usarse en una especificación o en el código (principio III).

## 6. Flujo de trabajo con SDD

1. **Constitución:** este documento, en su versión vigente.
2. **Especificar** cada feature con el glosario. La especificación cita sus RF y RN de la ERS y trae los criterios de aceptación como escenarios.
3. **Aclarar** las dudas de la especificación antes de planificar. Si una duda cambia el alcance, se registra como decisión.
4. **Planificar** en lenguaje técnico: agregados, puertos, adaptadores, eventos y endpoints. Si una decisión afecta a más de una feature, se registra como ADR.
5. **Contrato:** se escribe o actualiza el OpenAPI y se revisa antes de escribir código.
6. **Tareas:** se parte el trabajo en tareas chicas, cada una con su prueba.
7. **Implementar** con la prueba primero. El autor revisa cada cambio generado.
8. **Verificar:** las pruebas de contrato, de aceptación y de arquitectura pasan; la feature queda trazada a sus RF.

**Una feature está terminada cuando:**
- sus escenarios de aceptación pasan;
- su contrato está publicado y verificado;
- la prueba de arquitectura pasa;
- sus errores están en el catálogo y las pruebas de contrato los cubren;
- el JavaDoc de sus puertos y de su dominio está al día;
- no hay hallazgos altos de seguridad: SpotBugs, Gitleaks y Grype están en verde en su pull request;
- el glosario está al día.

**Orden de construcción:** las olas 0 a 4 del plan de 22 semanas (`planificacion/Estimacion_desarrollo_MDD.xlsx`), con las pruebas transversales al cierre de cada ola.

**Metodología (D-22):** Kanban personal con prácticas XP y SDD, con cadencia semanal y las olas como milestones de GitHub (D-24). Cada feature es un issue épico y cada historia de usuario es un sub-issue que pasa por las columnas Lista, Especificar, Planificar y contrato, Implementar (límite de 1), Verificar y Hecho del tablero de GitHub Projects, y se cierra con un pull request. El detalle está en `sdd/metodologia_desarrollo.md`.

## 7. Gobernanza

- La constitución prevalece sobre cualquier especificación o plan. Si una especificación la contradice, se corrige la especificación o se enmienda la constitución, nunca se ignora.
- Toda enmienda sube la versión: mayor si cambia o elimina un principio, menor si agrega uno y parche si solo aclara. Las enmiendas de arquitectura van acompañadas de un ADR.
- Cada revisión de un plan comprueba que respeta los principios I a XIII.

## 8. Comprobaciones de la ola 0

La constitución se ratificó el 11/10/2026. Los pendientes 1 a 4, 6 y 8 de la versión 0.5 quedaron resueltos. Lo que todavía hay que comprobar con código pasa a los criterios de aceptación de las historias de la ola 0. Si alguna comprobación falla, se registra una decisión nueva y se enmienda esta constitución.

| Comprobación | Historia |
|---|---|
| PDFBox y Bouncy Castle validan un PDF/A firmado con FirmaEC (antes pendiente 5) | B00-S01 |
| Cinco JVM y PostgreSQL caben en 4 GB (R-19, antes pendiente 5) | B00-H02 |
| palantir-java-format y SpotBugs funcionan con Java 25, y Spring Modulith 2.1 es la línea para Spring Boot 4.1 (ADR-011, antes pendiente 7) | B00-H01 |
| El esquema `Error` común y el formato del catálogo de códigos están escritos en `contratos/` (principio XIII, antes pendiente 9) | B00-H04 |
| Las acciones de GitHub están fijadas por SHA con pinact (antes pendiente 6) | B00-H01 |

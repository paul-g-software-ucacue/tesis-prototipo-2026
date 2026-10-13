# Investigación (fase 0): B00-H04 · Modelo de errores común y catálogo de códigos

**Fecha:** 2026-10-13 · **Especificación:** [spec.md](spec.md) · **Plan:** [plan.md](plan.md)

Fuentes leídas:
- constitución 2.1.0 (principios I, IV, VIII, IX y XIII) y `docs/decisiones.md` (D-38, D-40 a D-42, ADR-014 y ADR-015);
- lo que dejó B00-H03: `contratos/comun/`, `plataforma:seguridad`, `plataforma:pruebas`, `mdd.contrato`, el controlador de Usuarios y el API Gateway;
- el comportamiento de Spring MVC 7 (`ResponseEntityExceptionHandler`, `ErrorController`) y de Spring WebFlux (`ErrorWebExceptionHandler`) en Spring Boot 4.1.1.

Las decisiones que afectan a todas las features están en **ADR-016**, aprobado (§12).

---

## 1. Dónde vive la traducción

- **Decisión:** un módulo nuevo, `backend/plataforma/errores` (servlet), con:
  - `ManejadorDeErrores`, el único `@RestControllerAdvice` de cada proceso;
  - `ControladorDeErrores`, que reemplaza la página `/error` de Spring Boot;
  - el registro del catálogo y el modelo de respuesta `RespuestaError`.

  Se aplica como autoconfiguración y los 4 procesos lo agregan en su `build.gradle`, igual que `plataforma:seguridad`. La plantilla de proceso lo trae.
- **Por qué:** «un solo `@RestControllerAdvice` por aplicación» (principio XIII). Los 4 procesos comparten la misma traducción, y copiarla haría 4 versiones que se desalinean (el mismo argumento que `plataforma:seguridad`, research §6 de B00-H03).
- **Alternativas consideradas:**
  - un advice por servicio: varios advice en un mismo proceso, que choca con la regla de uno por aplicación;
  - meterlo en `plataforma:seguridad`: mezcla dos responsabilidades distintas.

## 2. Cómo se registra un error en el catálogo

- **Decisión:** cada servicio declara sus errores en una clase de sus adaptadores de entrada que implementa `CatalogoDeErrores`. Cada entrada dice la clase de la excepción, el código, el estado HTTP, el título y la descripción:

  ```java
  // servicios/usuarios/.../adaptadores/entrada/ErroresUsuarios.java
  @Component
  class ErroresUsuarios implements CatalogoDeErrores {
      public List<EntradaCatalogo> entradas() {
          return List.of(EntradaCatalogo.de(CredencialesInvalidas.class,
                  "USUARIOS_CREDENCIALES_INVALIDAS", 401,
                  "Credenciales inválidas", "El usuario o la contraseña no son correctos."));
      }
  }
  ```

  `ManejadorDeErrores` reúne los catálogos de todos los servicios del proceso al arrancar y traduce por la clase de la excepción, o por la superclase más cercana que esté registrada.
- **El dominio y la aplicación no cambian:** sus excepciones son clases Java comunes, sin HTTP ni una superclase de la plataforma (principio I, `DOMINIO_SIN_DETALLES`). La relación entre la excepción y su código vive en los adaptadores.
- **Validación al arrancar:** si dos servicios registran el mismo código o la misma excepción, el proceso no arranca y dice cuál se repite.
- **Por qué:** cumple «el dominio y la aplicación no conocen HTTP» y «el catálogo se escribe junto con el contrato de cada servicio». Cada servicio es dueño de sus códigos (principio V).
- **Alternativas consideradas:**
  - una superclase `ErrorDeDominio` con el código: el dominio dependería de la plataforma;
  - anotaciones en las excepciones: el dominio dependería de una anotación de la plataforma;
  - un archivo YAML leído al arrancar: habría que empaquetar `contratos/` en cada imagen.

## 3. El catálogo documentado y el catálogo del código no se desalinean

- **Decisión:** `contratos/comun/errores.md` sigue siendo el catálogo documentado y normativo. Una prueba compara cada entrada registrada en el código con su fila de la tabla: el código existe, y el estado, el título y la descripción son iguales.
  - **Dónde:** el verificador, `VerificadorDeCatalogo`, vive en `plataforma:pruebas`. Lo usan los servicios con catálogo (hoy Usuarios), `plataforma:errores` (códigos comunes) y `plataforma:seguridad` (el cuerpo constante de `SEGURIDAD_NO_AUTENTICADO`).
- **Por qué:** el catálogo es parte del contrato (principio IV) y lo leerá la SPA. Si el texto de la tabla y el del código difieren, una de las dos pruebas falla.

## 4. Códigos comunes nuevos

Con el prefijo `COMUN_`, porque no son de un solo servicio (como `SEGURIDAD_`):

| Código | Estado | Cuándo |
|---|---|---|
| `COMUN_ERROR_INTERNO` | 500 | Excepción técnica no reconocida, o de dominio o aplicación sin registrar. Siempre con `idIncidente`. |
| `COMUN_SOLICITUD_INVALIDA` | 400 | Uno o más campos no pasan la validación del contrato. Siempre con `detalles`. |
| `COMUN_CUERPO_ILEGIBLE` | 400 | El cuerpo no es JSON válido o no tiene la forma esperada. |
| `COMUN_RECURSO_NO_ENCONTRADO` | 404 | La ruta no existe (con un token válido; sin token, el 401 llega antes). |
| `COMUN_METODO_NO_PERMITIDO` | 405 | La ruta existe, pero no con ese método. |
| `COMUN_FORMATO_NO_ACEPTADO` | 406 | El cliente pide un formato que la API no produce. |
| `COMUN_TIPO_NO_SOPORTADO` | 415 | El cuerpo viene en un tipo de contenido que la operación no acepta. |
| `COMUN_SERVICIO_NO_DISPONIBLE` | 503 | El API Gateway no puede conectar con el proceso de la ruta (D-40). |

Los títulos y las descripciones van en `errores.md`. Cada descripción dice qué puede hacer el usuario (RNF-USA-01): por ejemplo, «Revisa los campos marcados y vuelve a intentarlo» o «Intenta de nuevo en unos minutos; si el problema sigue, informa el identificador del incidente». El catálogo no lleva marcadores variables: el `idIncidente` va en su propio campo.

## 5. Errores del framework y la página `/error`

- **Decisión:**
  - `ManejadorDeErrores` extiende `ResponseEntityExceptionHandler`. Así recibe todas las excepciones estándar de Spring MVC, como `NoResourceFoundException`, `HttpRequestMethodNotSupportedException`, `HttpMediaTypeNotSupportedException` o `HttpMessageNotReadableException`. Su `handleExceptionInternal` reemplaza el cuerpo `ProblemDetail` por `RespuestaError`, con el código común según el estado (§4).
  - `ControladorDeErrores` implementa `ErrorController` en `/error`, para los errores que no pasan por el advice, como los lanzados en un filtro. Responde con el modelo común según el estado; con un 5xx, registra el incidente.
  - En `plataforma:seguridad`, las peticiones con *dispatch* `ERROR` pasan sin autenticación, para que la página de error se pueda escribir. Una petición directa a `/error` es de tipo `REQUEST` y sigue pidiendo token.
- **Excepciones de seguridad:** el advice no captura `AuthenticationException` ni `AccessDeniedException`; las vuelve a lanzar, para que las atiendan el 401 de ADR-014 y el futuro 403 de F05-H02. Si no, se convertirían en un 500.
- **Por qué:** cumple «todos los errores con el mismo formato» y FR-010, sin reescribir uno por uno los manejadores de Spring.

## 6. Errores técnicos e `idIncidente`

- **Decisión:**
  - Una excepción no reconocida responde 500 con `COMUN_ERROR_INTERNO`, la descripción del catálogo y un `idIncidente`, que es un UUID aleatorio (versión 4) e impredecible.
  - El registro, en nivel ERROR, guarda: `idIncidente`, método, ruta, la clase de la excepción y la traza completa con sus causas.
  - **Datos sensibles en el registro:** antes de registrar, el texto de la traza se sanea:
    - los JWT (`eyJ…​.…​.…`) y lo que sigue a `Bearer ` se reemplazan por `[oculto]`;
    - también los valores de `clave=`, `password=` y `contraseña=`.

    Así un token o una contraseña que se hayan colado en el mensaje de una excepción no quedan en el registro (FR-008, edge case de datos sensibles).
- **Excepción de dominio o aplicación sin registrar:** se trata como técnica, y el registro agrega `sin código en el catálogo: <clase>`. Se reconoce porque su paquete contiene `.dominio.` o `.aplicacion.`, y así un error nuevo sin registrar no pasa desapercibido.
- **Por qué:** cumple el escenario «Error técnico inesperado» y el principio XIII («se registra con su contexto técnico y su causa original, sin contraseñas, tokens ni datos personales»).
- **Alternativas consideradas:** registrar la excepción tal cual, que dejaría pasar un token del mensaje; un identificador correlativo, que sería adivinable.

## 7. Validación de entrada (D-42)

- **Decisión:**
  - `mdd.contrato` pasa a `useBeanValidation=true`: las interfaces generadas validan el cuerpo con `@Valid`, y los modelos llevan las restricciones del contrato (`@NotNull`, `@Size`, `@Pattern`…). Se agrega `spring-boot-starter-validation` (Hibernate Validator, del BOM).
  - **Contrato de Usuarios:** `usuario` y `clave` de `CredencialesSesion` llevan `minLength: 1`, para que una cadena vacía no pase como si tuviera valor. El inicio de sesión agrega la respuesta 400.
  - **Respuesta:** un `MethodArgumentNotValidException` (o un `HandlerMethodValidationException`) responde 400 con `COMUN_SOLICITUD_INVALIDA` y un `DetalleError` por campo, con estas descripciones en español:
    - valor nulo o cadena vacía → «es obligatorio»;
    - `@Size` → «debe tener entre {min} y {max} caracteres»;
    - `@Pattern` → «no tiene el formato esperado»;
    - otro → «no es válido».

    No se usan los mensajes por defecto del validador, que salen en inglés según el idioma del servidor. Los campos se nombran como en el JSON del contrato.
- **Por qué:** cumple el escenario «Error de entrada» con descripciones accionables (RNF-USA-01) y estables.

## 8. Credenciales inválidas pasan al mecanismo común (D-38, D-41)

- **Decisión:** se borra el `@ExceptionHandler` de `SesionControlador`, y `ErroresUsuarios` registra `CredencialesInvalidas` con el mismo código, estado y textos.
- **Efecto:** la interfaz generada sigue devolviendo `ResponseEntity<TokenSesion>`, y la excepción la traduce el advice. La respuesta no cambia (SC-005).

## 9. El error de dominio de prueba (D-41)

- **Decisión:** las pruebas de `plataforma:errores` tienen una aplicación mínima con:
  - una excepción de dominio de prueba (`…fixtures.dominio.HorasExcedidas`, Java puro);
  - un catálogo de prueba que la registra como `PRUEBA_HORAS_EXCEDIDAS` con 409;
  - un «caso de uso» de prueba que la lanza;
  - otra excepción de dominio que no está registrada.

  Comprueba el escenario «Error reconocido» con el mecanismo real, y el caso límite del error sin registrar.

## 10. API Gateway (D-40)

- **Decisión:** un `ErrorWebExceptionHandler` propio, `ManejadorDeErroresGateway`, con `@Order(-2)` para que vaya antes del de Spring Boot. Traduce así:
  - **`ResponseStatusException`:**
    - 404 (ruta sin destino) → `COMUN_RECURSO_NO_ENCONTRADO`;
    - 405 → `COMUN_METODO_NO_PERMITIDO`;
    - otros 4xx → `COMUN_SOLICITUD_INVALIDA`, sin `detalles`.
  - **Fallas de conexión con el proceso** (`ConnectException`, `UnknownHostException` y sus envolturas de Netty y de Spring Cloud Gateway) → 503 `COMUN_SERVICIO_NO_DISPONIBLE`, sin la dirección interna.
  - **Lo demás** → 500 `COMUN_ERROR_INTERNO`, con `idIncidente` y el registro saneado de §6.

  El 401 por token sigue saliendo de `RespuestaNoAutenticadoReactiva` (ADR-014).
- **Código compartido:** el API Gateway no usa `plataforma:errores`, que es servlet. Lo común vive en un módulo nuevo sin dependencias de servlet ni de WebFlux, `plataforma:errores-nucleo` (paquete `ec.mdd.plataforma.errores.nucleo`):
  - `RespuestaError` y `DetalleRespuesta`, el modelo público;
  - `CatalogoDeErrores` y `EntradaCatalogo`, para que los servicios registren sus errores sin depender de Spring MVC;
  - `SaneadorDeTrazas` y la generación del `idIncidente`;
  - los códigos comunes.

  Lo usan `plataforma:errores`, el API Gateway y los adaptadores de los servicios.
- **Por qué:** sin esto, el API Gateway responde con el formato por defecto de Spring Cloud Gateway (`timestamp`, `path`, `error`, `requestId`), distinto del resto.
- **Alternativas consideradas:** duplicar el saneo en el API Gateway: dos copias de una regla de seguridad.

## 11. Contratos y pruebas de contrato

- **`contratos/comun/errores.yaml`:** agrega las respuestas reutilizables `SolicitudInvalida` (400), `ErrorInterno` (500) y `ServicioNoDisponible` (503), todas con el esquema `Error`.
- **Contratos de los servicios:**
  - cada operación declara `500 ErrorInterno` y `503 ServicioNoDisponible`, que puede dar el API Gateway;
  - las que reciben cuerpo declaran también `400 SolicitudInvalida`, hoy solo el inicio de sesión;
  - el inicio de sesión agrega `minLength: 1`.
- **Pruebas de contrato de Usuarios:** suman el 400 con `detalles` (usuario vacío y clave ausente, que dan 2 detalles) y el 500 (un doble del caso de uso que lanza una excepción técnica).
- **Validación de los contratos:** con el CLI de openapi-generator 7.25.0, como en B00-H03.

## 12. ADR-016 (aprobado)

**ADR-016, «Traducción de errores con catálogo por servicio»:**
- un solo `@RestControllerAdvice` por proceso, en `plataforma:errores`, más la página `/error` propia;
- cada servicio registra sus errores en un `CatalogoDeErrores` de sus adaptadores, y el dominio no conoce HTTP;
- `contratos/comun/errores.md` es el catálogo normativo, verificado por pruebas;
- códigos comunes con el prefijo `COMUN_`;
- `idIncidente` aleatorio y registro saneado;
- validación de entrada activada en la generación, con descripciones propias en español;
- el manejador reactivo del API Gateway y `plataforma:errores-nucleo`.

Afecta a todas las historias con errores de negocio, a la SPA (B00-H06) y a F05-H02 (403).

## 13. Lo que queda fuera

- El 403 y la matriz de permisos (F05-H02): esta historia solo evita que una `AccessDeniedException` se convierta en 500.
- Los errores entre procesos 502 y 504 por llamadas HTTP entre servicios, que llegan con la primera llamada.
- La traducción en la SPA (B00-H06).

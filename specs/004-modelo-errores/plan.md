# Implementation Plan: Modelo de errores común y catálogo de códigos

**Branch**: `004-modelo-errores` | **Date**: 2026-10-13 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/004-modelo-errores/spec.md` (issue #21, B00-H04)

## Summary

Cada proceso tiene un solo `@RestControllerAdvice`, en el módulo nuevo `plataforma:errores`, que traduce todos los errores al esquema `Error` del principio XIII:
- **errores registrados:** cada servicio declara sus errores en un catálogo de sus adaptadores, así que el dominio y la aplicación no conocen HTTP;
- **errores de entrada:** 400 con `detalles` en español, ahora que la generación valida los contratos;
- **errores técnicos:** 500 con un `idIncidente` aleatorio, y la traza saneada en el registro;
- **errores del framework:** 404, 405, 406, 415 y cuerpo ilegible.

Otras piezas:
- una página `/error` propia cubre los errores que no pasan por el advice;
- el API Gateway responde los suyos con un manejador reactivo (D-40);
- `contratos/comun/errores.md` sigue siendo el catálogo normativo, y una prueba compara cada entrada registrada en el código con su fila;
- las credenciales inválidas pasan al mecanismo común (D-38, D-41), y el inicio de sesión con datos vacíos responde 400 (D-42).

Los detalles y sus alternativas están en [research.md](research.md). La decisión que afecta a todas las features está en ADR-016, aprobado por el autor el 2026-10-13.

## Technical Context

**Language/Version**: Java 25 · YAML (OpenAPI 3.0.3) · Markdown (catálogo de códigos)

**Primary Dependencies**:
- Spring MVC 7 (`ResponseEntityExceptionHandler`, `ErrorController`) y Spring WebFlux (`ErrorWebExceptionHandler`), de Spring Boot 4.1.1;
- `spring-boot-starter-validation` (Hibernate Validator, del BOM);
- openapi-generator 7.25.0 con `useBeanValidation=true`.

No se agregan dependencias fuera del BOM.

**Storage**: no cambia.

**Testing**:
- JUnit Jupiter 6, AssertJ y MockMvc;
- `OutputCaptureExtension`, para verificar el registro del incidente;
- Testcontainers, para el fallo de la base de datos;
- `WebTestClient` o `HttpClient` para el API Gateway;
- pruebas de contrato con `ValidadorDeContrato` y la verificación del catálogo contra `errores.md`.

**Target Platform**: el perfil consolidado de B00-H02 y B00-H03.

**Project Type**: monorepo web: multiproyecto Gradle en `backend/` y contratos en `contratos/`.

**Performance Goals**: la traducción no agrega llamadas de red. El catálogo se arma una vez al arrancar.

**Constraints**:
- Ninguna respuesta de error lleva trazas, clases, SQL ni mensajes originales.
- El registro no guarda tokens ni contraseñas.
- El 401 por token y el de credenciales inválidas no cambian.

**Scale/Scope**:
- 2 módulos de plataforma nuevos (`errores` y `errores-nucleo`);
- 8 códigos comunes y 1 catálogo de servicio (Usuarios);
- el manejador del API Gateway;
- los contratos de 4 servicios con las respuestas 400, 500 y 503.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principio | Cómo lo respeta el plan | Estado |
|---|---|---|
| I. Cinco capas | El dominio y la aplicación siguen sin HTTP. La relación entre la excepción y su código vive en los adaptadores de entrada (`CatalogoDeErrores`), y la traducción, en la plataforma. | ✅ |
| II. DDD según el subdominio | No hay modelo nuevo. El error de dominio de prueba solo existe en las pruebas (D-41). | ✅ N/A |
| III. Lenguaje ubicuo | Los textos de los errores están en español y usan los términos del glosario. No hay términos de negocio nuevos. | ✅ |
| IV. API primero | `errores.yaml` y los contratos declaran las respuestas 400, 500 y 503 antes del código, y el catálogo es parte del contrato. | ✅ |
| V. Límites de servicio | Cada servicio es dueño de sus códigos, con el prefijo del servicio. Los errores de otro servicio se traducen al propio cuando exista la primera llamada. | ✅ |
| VI y VII | Sin reglas ni datos de negocio. | ✅ N/A |
| VIII. Pruebas | Pruebas primero: el mecanismo con el error de dominio de prueba, el error técnico con la base detenida, la validación, la página `/error`, el API Gateway, las pruebas de contrato de 400 y 500, y la verificación del catálogo. Sin Mockito fuera de la aplicación. 60 % por módulo. | ✅ |
| IX. Seguridad | Sin detalles técnicos en las respuestas, `idIncidente` impredecible y registro saneado (RNF-SEG-04, RNF-SEG-06). Las excepciones de seguridad no se convierten en 500. | ✅ |
| X. Simplicidad | Sin internacionalización ni catálogo en tiempo de ejecución desde archivos. Los errores entre procesos quedan para cuando existan. | ✅ |
| XI. Diseño pragmático | `plataforma:errores` existe porque la regla es un advice por proceso, y hay 4 procesos. `errores-nucleo` existe porque el API Gateway (WebFlux) y los servicios no pueden depender de Spring MVC. | ✅ |
| XII. JavaDoc | El mecanismo, el catálogo y el saneo, en español. | ✅ |
| XIII. Errores | Es la historia que lo implementa: clasificación, traducción centralizada, modelo público, catálogo, estados, lo que nunca se expone y el registro del error técnico. | ✅ |
| §6.4 ADR | ADR-016, aprobado por el autor el 2026-10-13. | ✅ |

**Resultado del gate antes de la fase 0:** pasa. ADR-016 quedó aprobado el 2026-10-13.

**Revisión después de la fase 1:** pasa. [data-model.md](data-model.md) y [contracts/](contracts/) solo agregan lo listado.

## Project Structure

### Documentation (this feature)

```text
specs/004-modelo-errores/
├── spec.md
├── plan.md                  # este archivo
├── research.md              # fase 0
├── data-model.md            # fase 1: error, detalle, entrada del catálogo e incidente
├── quickstart.md            # fase 1
├── contracts/
│   ├── traduccion.md            # qué responde cada tipo de error en los procesos y en el API Gateway
│   └── catalogo.md              # cómo se registra un error y cómo se verifica el catálogo
├── checklists/
│   └── requirements.md
├── evidencia.md             # se crea al implementar
└── tasks.md                 # /speckit-tasks
```

### Source Code (repository root)

```text
contratos/comun/
├── errores.yaml               # cambia: + respuestas SolicitudInvalida, ErrorInterno y ServicioNoDisponible
└── errores.md                 # cambia: + 8 códigos COMUN_
contratos/{usuarios,cargas,generacion,dashboard}/openapi.yaml   # cambia: + 400/500; minLength en el inicio de sesión
backend/
├── settings.gradle                           # cambia: + plataforma:errores y plataforma:errores-nucleo
├── buildSrc/src/main/groovy/mdd.contrato.gradle   # cambia: useBeanValidation, validación y errores-nucleo
├── plataforma/
│   ├── errores-nucleo/      # nuevo: RespuestaError, CatalogoDeErrores, EntradaCatalogo, SaneadorDeTrazas y códigos comunes
│   ├── errores/             # nuevo: ManejadorDeErrores (advice), ControladorDeErrores (/error) y autoconfiguración
│   ├── seguridad/           # cambia: el dispatch ERROR pasa sin autenticación
│   └── pruebas/             # cambia: + VerificadorDeCatalogo
├── plantillas/aplicacion/   # cambia: + plataforma:errores
├── apps/{identidad,datos,distributivo,salida}/   # cambia: + plataforma:errores
├── gateway/                 # cambia: ManejadorDeErroresGateway
└── servicios/usuarios/      # cambia: ErroresUsuarios; sin @ExceptionHandler en el controlador
```

**Structure Decision**: se mantienen las rutas de ADR-012 y ADR-015. Los módulos nuevos van en `plataforma/`, como `seguridad` y `pruebas`.

## Fases de implementación (guía para /speckit-tasks)

1. **ADR-016:** aprobado el 2026-10-13 (`docs/decisiones.md`).
2. **Contratos primero:** códigos `COMUN_` en `errores.md`, respuestas comunes en `errores.yaml`, y 400 y 500 en los contratos. Validación con el CLI de openapi-generator.
3. **`plataforma:errores-nucleo`**, con sus pruebas primero: modelo, catálogo, saneo e `idIncidente`.
4. **`plataforma:errores`**, con sus pruebas primero, incluido el error de dominio de prueba: el advice, la validación y `/error`. También el dispatch `ERROR` en `plataforma:seguridad` y `VerificadorDeCatalogo`.
5. **Generación con validación** (`mdd.contrato`) y Usuarios: `ErroresUsuarios`, sin `@ExceptionHandler`, con las pruebas de contrato de 400 y 500 y la verificación del catálogo.
6. **Procesos:** `plataforma:errores` en los 4 procesos y en la plantilla; prueba de integración del error técnico con la base detenida en identidad.
7. **API Gateway:** `ManejadorDeErroresGateway`, con sus pruebas (404, 503 con un proceso detenido y 500).
8. **De punta a punta:** el perfil levantado y `probar_autenticacion.sh` sigue pasando. Comprobación manual de 404, 400, 503 y 500 con `idIncidente` en el registro.
9. **Cierre:** `evidencia.md`, pull request con la CI en verde y fusión con la aprobación del autor.

## Complexity Tracking

| Pieza | Por qué hace falta | Alternativa más simple descartada porque |
|---|---|---|
| `plataforma:errores-nucleo` (un módulo más) | El API Gateway (WebFlux) y los servicios (librerías) necesitan el modelo, el catálogo y el saneo sin depender de Spring MVC | Duplicar el saneo en el API Gateway repetiría una regla de seguridad. Poner el catálogo en `plataforma:errores` haría que cada servicio arrastre Spring MVC. |

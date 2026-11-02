# Implementation Plan: Consolidar los parciales con el informe sustentado

**Branch**: `037-consolidar-parciales` | **Date**: 2026-11-02 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/037-consolidar-parciales/spec.md`

## Summary

Generación guarda quién y cuándo consolidó, y calcula el consolidado al abrirlo: carreras, totales e inconsistencias abiertas, reutilizando el panel. El informe es un PDF en un volumen del proceso `distributivo`, con su huella en la base. La SPA agrega la pantalla «Consolidado del periodo».

## Technical Context

**Language/Version**: Java 25 (Spring Boot 4.1.1) · TypeScript 5.9 (React 19) · Bash

**Primary Dependencies**: ninguna nueva.

**Storage**:
- Generación, `V6`: `consolidacion` e `informe_sustentado`, solo de inserción, y 2 acciones del registro.
- Un volumen con nombre, `archivos`, solo en `distributivo`. Es el primero del sistema que guarda archivos de negocio.

**Testing**:
- **Dominio:** sin dobles: la validación del PDF, el consolidado y el aviso de desactualizado.
- **Aplicación:** Mockito.
- **Almacén y persistencia:** integración, con una carpeta temporal y la base real.
- **Contrato y permisos:** `ValidadorDeContrato` y `VerificadorDePermisos`.
- **SPA:** Vitest, con `npm run tipos`, y Playwright con axe.
- **Punta a punta:** el script, con 10 MB reales, y `probar_equipo.sh`, porque cambia el despliegue.

**Target Platform**: el perfil consolidado y el equipo de demostración.

**Project Type**: monorepo web: 1 servicio, el contrato, la SPA, el despliegue y un script.

**Performance Goals**: abrir en menos de 5 s (SC-002) y adjuntar 10 MB en menos de 10 s (SC-003), con holgura.

**Constraints**:
- `nginx.conf` y Spring admiten 10 MB en total: con la envoltura del formulario, un PDF de exactamente 10 MB no llegaría. Suben a 11 MB, y el límite real lo pone el dominio;
- el nombre recibido no toca el disco;
- nada de `:z` ni carpetas del anfitrión: un volumen con nombre;
- el proceso corre como 10001: la imagen crea la carpeta con ese dueño;
- `spring.servlet.multipart` hoy solo está en `apps/datos`.

**Scale/Scope**:
- 4 operaciones públicas;
- 1 migración y 1 volumen;
- 5 códigos de error nuevos;
- 1 pantalla nueva, 1 script nuevo;
- 1 decisión (D-88) y 1 término en el glosario.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principio | Cómo lo respeta el plan | Estado |
|---|---|---|
| I. Capas | El disco va detrás de un puerto; validar el PDF es dominio. | ✅ |
| II. DDD según el subdominio | `InformeSustentado` es parte de la versión, como dice el modelo táctico. | ✅ |
| III. Lenguaje ubicuo | «Consolidado desactualizado» entra al glosario antes del código. | ✅ con T001 |
| IV. API primero | El contrato antes del código. | ✅ |
| V. Límites | El panel se reutiliza por su puerto, dentro de Generación. | ✅ |
| VI. Las reglas son datos | El límite de 10 MB no es una regla de la normativa: es del sistema. | ✅ |
| VII. Inmutabilidad y trazabilidad | Consolidaciones e informes solo se insertan, y van al registro. | ✅ |
| VIII. Pruebas | Primero el dominio, con esqueleto, viéndolo fallar. | ✅ |
| IX. Seguridad y datos personales | El nombre nunca es ruta; se comprueba la huella; el contenido no va a los registros. | ✅ |
| X. Simplicidad | Sin foto del consolidado, sin servicio de archivos ni dependencias. | ✅ |
| XI y XII | Sin módulos nuevos. JavaDoc en español. | ✅ |
| XIII. Catálogo de errores | 5 códigos nuevos con sus 8 columnas. | ✅ |
| XIV. Validación de entradas | Tipo y tamaño, en la interfaz y en el servidor. | ✅ |

## Project Structure

### Documentation (this feature)

```text
specs/037-consolidar-parciales/
├── spec.md
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── generacion.md
└── tasks.md
```

### Source Code (repository root)

```text
contratos/generacion/openapi.yaml, contratos/comun/errores.md

backend/servicios/generacion/
├── dominio/                    ConsolidadoDelPeriodo, InformeSustentado, ArchivoPdf
├── aplicacion/                 Consolidar, consultar y adjuntar
├── puertos/                    los casos de uso y AlmacenDeArchivos
└── adaptadores/                ConsolidadoControlador, AlmacenEnDisco, repositorio, V6

backend/apps/distributivo/      el límite del formulario y la raíz del almacén
frontend/src/pantallas/consolidado/, frontend/nginx.conf

deploy/docker/java.Dockerfile   la carpeta con dueño 10001
compose.yaml, deploy/equipo/compose.demo.yml   el volumen
deploy/probar_consolidado.sh    punta a punta
```

**Structure Decision**: no hay módulos nuevos. El almacén queda en Generación detrás de un puerto; cuando llegue Firma se decide si sube a la plataforma.

## Complexity Tracking

Sin violaciones que justificar.

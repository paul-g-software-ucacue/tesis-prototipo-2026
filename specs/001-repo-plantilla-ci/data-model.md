# Modelo de datos (fase 1): B00-H01

Esta historia no persiste datos. El «modelo» describe los elementos de la base técnica que nombra la especificación (Key Entities): qué los identifica, cómo se relacionan y qué reglas los validan. Sirve para derivar las tareas y las pruebas.

## Servicio

Uno de los 12 servicios de la ERS 8.5, creado desde la plantilla.

| Atributo | Valor o regla |
|---|---|
| Nombre | El de su etiqueta `modulo:NN-nombre`, sin número ni guiones: `usuarios`, `roles`, `cargas`, `docentes`, `generacion`, `validacionocs`, `notificaciones`, `reporting`, `dashboard`, `firma`, `academico` y `normativa`. |
| Subproyecto Gradle | `:servicios:<nombre>`, en `backend/servicios/<nombre>` |
| Paquete raíz | `ec.mdd.<nombre>`. Es el módulo de Spring Modulith. |
| Tipo | `central` (5, 6, 10 y 12) o `soporte/genérico` (el resto). Lo determina el plugin que aplica: `mdd.servicio-central` o `mdd.servicio`. |
| Capas | Los subpaquetes `dominio`, `aplicacion`, `puertos` (`entrada` y `salida`), `adaptadores` e `infraestructura`. Todas son opcionales mientras no tengan clases. |
| API publicada | `ec.mdd.<nombre>.puertos.entrada`, marcado como `@NamedInterface("api")` |

**Validaciones:**

- Las reglas de capas de ArchUnit pasan (ver «Regla de arquitectura» más abajo).
- La cobertura de líneas del módulo es de al menos 60 %.
- Si el servicio es central, cada paquete `*.dominio` o `*.dominio.*` tiene al menos 80 %.
- Spotless no encuentra diferencias de formato.
- SpotBugs no encuentra hallazgos de confianza alta en el código de `main`.

**Relaciones:**

- Pertenece a un solo Proceso.
- Usa de otro servicio del mismo proceso solo su API publicada.
- Usa de un servicio de otro proceso solo su contrato HTTP (principio V, fuera de esta historia).

## Proceso

Aplicación Spring Boot que ensambla servicios (ADR-006).

| Atributo | Valor o regla |
|---|---|
| Nombre | `identidad` (1, 2 y 7), `datos` (3, 4, 11 y 12), `distributivo` (5, 6 y 10) o `salida` (8 y 9) |
| Subproyecto Gradle | `:apps:<nombre>`, en `backend/apps/<nombre>`, con el plugin `mdd.aplicacion` |
| Clase de arranque | `ec.mdd.<Nombre>Aplicacion`. Está excluida de JaCoCo. |
| Módulos | Los `ec.mdd.<servicio>` de los servicios que declara como dependencia |

**Validación:** `ApplicationModules.of(<Nombre>Aplicacion.class).verify()` pasa (prueba `ModulosTest`).

## Capa

| Capa | Paquete | Puede depender de | No puede depender de |
|---|---|---|---|
| Dominio | `..dominio..` | Java estándar | Ninguna otra capa; Spring, JPA y ORM, HTTP y PDF |
| Puertos | `..puertos..` | Dominio | Aplicación, Adaptadores e Infraestructura |
| Aplicación | `..aplicacion..` | Dominio y Puertos | Adaptadores e Infraestructura |
| Adaptadores | `..adaptadores..` | Dominio, Puertos y Aplicación | Infraestructura |
| Infraestructura | `..infraestructura..` | Todas | — (ninguna capa depende de ella) |

## Regla de arquitectura

Vive en `plataforma:arquitectura` y se aplica a cada servicio con la prueba `ArquitecturaTest` del esqueleto.

| Regla | Falla cuando | Escenario o FR |
|---|---|---|
| Sentido de las cinco capas | Una capa usa otra que no le corresponde según la tabla de «Capa» | FR-005 |
| Dominio sin framework web, ORM ni HTTP | Una clase de `..dominio..` depende de `org.springframework..`, `jakarta.persistence..`, `org.hibernate..`, `jakarta.servlet..`, `java.net.http..`, `org.apache.hc..` u `okhttp3..` | Escenario 1, FR-004 |
| Dominio sin PDF | Una clase de `..dominio..` depende de `org.apache.pdfbox..` o `com.openhtmltopdf..` | Caso límite, FR-004 |
| Límites entre servicios (Spring Modulith) | Un servicio usa una clase de otro que no está en su `puertos.entrada` | Escenario 2, FR-006 |

**Propiedades:**

- Una capa ausente no hace fallar la regla (capas opcionales).
- Un servicio sin clases tampoco la hace fallar (se permite un `should` vacío).

## Pull request

| Atributo | Regla |
|---|---|
| Rama destino | `main` |
| Disparadores de la CI | `opened`, `synchronize`, `reopened` y `ready_for_review` |
| Borrador | `test-integracion`, `sbom-backend` y `sbom-frontend` no corren y quedan como omitidos. Al pasar a listo para revisión, corren. |
| Se puede fusionar si | Los 10 checks obligatorios están en éxito u omitidos, la rama está al día con `main` y la fusión es *squash* |

**Estados de la fusión:**

```text
abierto ──(checks corriendo)──▶ bloqueado
bloqueado ──(algún check en falla)──▶ bloqueado
bloqueado ──(todos en éxito u omitidos y al día con main)──▶ fusionable
fusionable ──(nuevo push o main avanza)──▶ bloqueado
fusionable ──(squash)──▶ fusionado
```

## Check

| Atributo | Regla |
|---|---|
| Nombre | El `name:` del job: `<etapa> · <job>`. La lista exacta está en [contracts/ci-y-regla-main.md](contracts/ci-y-regla-main.md). |
| Fuente | GitHub Actions (`integration_id` 15368) |
| Resultado | `success`, `failure`, `cancelled` o `skipped`. Para la regla, `skipped` cuenta como aprobado. |

## Regla de la rama principal

Es el ruleset `main` (id 24901935) de `paul-g-software-ucacue/tesis-prototipo-2026`.

| Atributo | Antes | Después de B00-H01 |
|---|---|---|
| Objetivo | `~DEFAULT_BRANCH` | Igual |
| Borrado y *force-push* | Prohibidos | Igual |
| Pull request obligatorio | Sí, con 0 aprobaciones | Igual |
| Métodos de fusión | `merge`, `squash` y `rebase` | Solo `squash` |
| Checks obligatorios | Ninguno | Los 10 checks, con la política estricta (al día con `main`) |

## Comprobación de la ola 0

Registro en `evidencia.md` (FR-018).

| Atributo | Valor |
|---|---|
| Comprobación | `palantir-java25`, `spotbugs-java25`, `modulith21-boot41` o `pinact` |
| Resultado | `confirmada` o `fallida` |
| Evidencia | Comando, versión y salida resumida, o el enlace al run de CI |
| Si falla | Una decisión D-28 o posterior y la enmienda de la sección 8 de la constitución |

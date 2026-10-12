# Contrato: servicio y proceso creados desde la plantilla

Decisión de arquitectura: ADR-012, en `docs/decisiones.md`.

Este documento dice lo que garantiza un servicio o un proceso creado copiando `backend/plantillas/servicio/` o `backend/plantillas/aplicacion/`. Es lo que verá la primera historia que cree un servicio (F04-H01, Normativa) y la que cree los procesos (B00-H02).

## Crear un servicio

1. Copiar `backend/plantillas/servicio/` a `backend/servicios/<servicio>/`.
2. En la copia, cambiar el marcador `servicio` del paquete por el nombre real, en las rutas y en los `package` (`ec.mdd.<servicio>`).
3. El `build.gradle` ya trae `id 'mdd.servicio'`; vale para todos los servicios, centrales o no (D-32).
4. Agregar `include 'servicios:<servicio>'` en `backend/settings.gradle`.
5. Agregar el servicio como dependencia de su proceso: `implementation project(':servicios:<servicio>')` en `apps/<proceso>/build.gradle`.

No hace falta ninguna otra configuración (SC-006).

### Estructura que entrega

```text
servicios/<servicio>/
├── build.gradle                         # plugins { id 'mdd.servicio' }
└── src/
    ├── main/java/ec/mdd/<servicio>/
    │   ├── dominio/package-info.java          # contrato de la capa (JavaDoc)
    │   ├── aplicacion/package-info.java
    │   ├── puertos/entrada/package-info.java  # @NamedInterface("api"): API publicada en el proceso
    │   ├── puertos/salida/package-info.java
    │   ├── adaptadores/package-info.java
    │   └── infraestructura/package-info.java
    └── test/java/ec/mdd/<servicio>/
        └── ArquitecturaTest.java              # aplica las reglas de plataforma:arquitectura
```

### Garantías

| Garantía | Cómo se comprueba | Escenario o FR |
|---|---|---|
| Si una clase de `dominio` importa Spring, JPA, HTTP o PDF, `ArquitecturaTest` falla y el check `2 · test-backend` queda en rojo | Regla «dominio sin detalles» | Escenario 1, FR-004 |
| Las dependencias entre capas respetan su sentido | Regla «sentido de las cinco capas» | FR-005 |
| Los demás servicios del proceso solo pueden usar `puertos.entrada` | `ModulosTest` del proceso | Escenario 2, FR-006 |
| El módulo exige 60 % de líneas cubiertas, sea central o no | `jacocoTestCoverageVerification` | Escenario 4, FR-012 y FR-013 (D-32) |
| El formato es el de google-java-format en estilo AOSP (D-33) | `spotlessCheck` | FR-015 |
| SpotBugs con FindSecBugs no encuentra hallazgos de confianza alta en `main` | `spotbugsMain` | Escenario 5, FR-014 |
| Un servicio recién creado, sin clases o sin alguna capa, pasa las reglas de arquitectura | Capas opcionales y `should` vacío permitido | research §11 |

### Lo que la plantilla no hace

- No crea agregados, entidades ni casos de uso de ejemplo.
- No trae dependencias de Spring Web, JPA ni Flyway. Cada historia agrega las que usa su servicio, en su `build.gradle` y solo en las capas permitidas.
- No define el contrato OpenAPI. Este se escribe en `contratos/` antes de implementar (principio IV).

## Crear un proceso

1. Copiar `backend/plantillas/aplicacion/` a `backend/apps/<proceso>/`.
2. Renombrar la clase de arranque a `ec.mdd.<Proceso>Aplicacion` y actualizar `ModulosTest`.
3. Agregar `include 'apps:<proceso>'` en `backend/settings.gradle`.

### Estructura que entrega

```text
apps/<proceso>/
├── build.gradle                                   # plugins { id 'mdd.aplicacion' } + dependencias de sus servicios
└── src/
    ├── main/java/ec/mdd/<Proceso>Aplicacion.java  # @SpringBootApplication; excluida de JaCoCo
    └── test/java/ec/mdd/ModulosTest.java          # ApplicationModules.of(<Proceso>Aplicacion.class).verify()
```

### Garantías

| Garantía | Cómo se comprueba | Escenario o FR |
|---|---|---|
| Si un servicio usa una clase de otro servicio del mismo proceso que no está en su API publicada, `ModulosTest` falla y el check queda en rojo | `ApplicationModules.verify()` | Escenario 2, FR-006 |
| Los módulos del proceso son exactamente los `ec.mdd.<servicio>` que declara como dependencia | La clase de arranque está en `ec.mdd` | research §12 |
| La clase de arranque no baja la cobertura del proceso | Exclusión en `mdd.aplicacion` | research §14 |

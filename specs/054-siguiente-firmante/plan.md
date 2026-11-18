# Implementation Plan: Notificar al siguiente firmante y ver la cadena de firmas

**Branch**: `054-siguiente-firmante` | **Date**: 2026-11-18 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/054-siguiente-firmante/spec.md`

## Summary

**Quién publica** (research §1, §5):
- `SubirFirmaServicio`, en la transacción que registra una firma aceptada, llama al puerto nuevo `NotificacionDeFirmaPendiente` si la cadena tiene un paso siguiente distinto de F1.
- El adaptador publica `firma.NotificacionesSolicitadas` hacia `notificaciones` (ADR-018), con el contenido de F16-H01 (`SolicitudDeNotificaciones`):
  - el autor es quien subió la firma;
  - el momento, cuando el MDD la aceptó;
  - el título, «Tienes una firma pendiente: «{rótulo}»»;
  - el enlace, `/firmas-pendientes`.

**A quién** (§2 a §4):
- El destino de un mensaje admite, además del rol, una persona: `{usuario}` o `{identificacion}`. Firma manda el firmante esperado del paso siguiente tal como lo guardó al generar (D-102).
- Notificaciones lo resuelve en el proceso `identidad`:
  - `{usuario}`, con `ListarUsuarios`;
  - `{identificacion}`, con un caso de uso nuevo de Usuarios, `ConsultarCuentaPorIdentificacion`.
  
  Solo cuentas activas. Si no hay cuenta, nadie recibe la notificación.

**La cadena** (§6, §7):
- `consultarDocumentoFirmable` suma `cadena`: cada paso con su rótulo, el nombre impreso, el estado (`FIRMADO`, `EN_TURNO`, `PENDIENTE` o `RECHAZADO`) y `firmadoEn`.
- La regla va en el dominio, `FirmasDelDocumento.cadenaConEstado()`, con la misma regla del turno que «Pendientes de firma».
- La SPA la muestra en la tarjeta del documento firmable de «Consolidado del periodo».

**Los scripts** (§8):
- `probar_documento_firmable.sh` revisa la cadena y las dos notificaciones nuevas, con una caída de `identidad` durante F1, y suma `MDD_CONSERVAR=F2`.
- `probar_rechazo_en_firma.sh` revisa la cadena de la versión nueva.

## Technical Context

**Language/Version**: Java 25 (Spring Boot 4.1.1) · TypeScript (React 19) · Bash

**Primary Dependencies**: las que ya están, sin dependencias nuevas ni módulos nuevos entre servicios.

**Storage**: PostgreSQL, sin migraciones. Firma ya guarda la cadena, los firmantes esperados con su nombre, las firmas aceptadas con `subidoEn` y el rechazo. Notificaciones guarda la notificación de F16-H01, con el usuario destinatario.

**Testing**:
- **Dominio de Firma:**
  - `cadenaConEstado()` recién generado, con F1 y F2 firmadas, completo, rechazado en F2, anulado y sin firmantes esperados;
  - `siguienteTrasFirmar(orden)` en F1, F2 y F3.
- **Aplicación de Firma (Mockito):** `SubirFirmaServicio`:
  - notifica a F2 tras F1 y a F3 tras F2;
  - no notifica tras F3, con una firma rechazada por la validación, ni con una ganada por otra subida;
  - la notificación va en la misma transacción.
- **Adaptador de Firma:** `PublicadorDeFirmaPendiente` arma el evento con el tipo, el origen, el destino, el contenido y el destino por persona.
- **Contrato de Firma:** `DocumentoControlador` con la cadena en los casos de [contracts/firma.md](contracts/firma.md).
- **Dominio de Notificaciones:** `SolicitudDeNotificaciones` con los tres destinos, dos claves a la vez, `carrera` sin `rol`, los patrones y el error sin el valor recibido.
- **Aplicación de Notificaciones:** `RecibirNotificacionesServicio`:
  - con el origen `firma`;
  - con un usuario activo, uno inactivo y uno inexistente;
  - con una identificación con cuenta y sin cuenta;
  - sin el autor.
- **Usuarios:** `ConsultarCuentaPorIdentificacion`, con y sin cuenta, contra PostgreSQL.
- **Integración:**
  - `distributivo`: la firma aceptada guarda la publicación en el registro, en la misma transacción;
  - `identidad`: la solicitud de Firma llega a la bandeja del Vicerrector con Usuarios real.
- **SPA:**
  - Vitest de `DocumentoFirmable` y de `enlaces.test.ts`;
  - Playwright con axe-core.
- **Punta a punta:** los dos scripts ([contracts/demostracion.md](contracts/demostracion.md)).

**Target Platform**: el perfil consolidado y el equipo de demostración.

**Project Type**: monorepo web:
- Firma (proceso `distributivo`);
- Notificaciones y Usuarios (proceso `identidad`);
- la SPA y dos scripts.

**Performance Goals**:
- SC-002: menos de 2 minutos después de la vuelta de `identidad`, con el reenvío cada 30 s.
- Publicar suma una inserción a la transacción de la firma.
- La cadena se calcula con lo que la consulta del documento ya lee, sin consultas nuevas.

**Constraints**:
- **ADR-018:** el mecanismo común de eventos.
- **Principio V:** Notificaciones usa los puertos de entrada de Usuarios, en su proceso. Firma no consulta a Usuarios dentro de la transacción de la firma.
- **MD-05:** Firma no conoce el flujo ni a Notificaciones: publica un hecho con su contenido.
- **D-102:** el firmante esperado fijado al generar.
- **FR-016:** la identificación nunca se muestra ni se guarda en la notificación.

**Scale/Scope**:
- Firma: 1 puerto, 1 adaptador, 2 consultas de dominio, 1 campo de la respuesta.
- Notificaciones: 2 tipos de destino nuevos y 1 origen.
- Usuarios: 1 caso de uso.
- SPA: 1 componente.
- Scripts: 1 paso nuevo, revisiones en pasos existentes y `MDD_CONSERVAR=F2`.
- La decisión D-110.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principio | Cómo lo respeta el plan | Estado |
|---|---|---|
| I. Capas | Las reglas van en el dominio: el estado de cada paso, el paso siguiente y la forma del destino. La publicación sale por un puerto de salida, y Usuarios entra por su puerto de entrada. | ✅ |
| II. DDD | Firma decide quién sigue, porque es dueño de la cadena. Notificaciones sigue siendo genérico: resuelve un destino y guarda. | ✅ |
| III. Lenguaje ubicuo | Firmante esperado, paso, cadena de firmas, firma pendiente y `Notificacion` («notificación», MD-09). | ✅ |
| IV. API primero | `contratos/firma/openapi.yaml` (`cadena`) y `contratos/notificaciones/openapi.yaml` (los destinos por persona), antes del código. | ✅ |
| V. Límites | Entre procesos, el evento persistente. Dentro de `identidad`, el puerto de entrada de Usuarios. No hay consultas síncronas entre procesos en la transacción de la firma. | ✅ |
| VI. Las reglas son datos | Los rótulos y los firmantes salen de la cadena y de los firmantes fijados al generar, no del código. | ✅ |
| VII. Inmutabilidad | No cambia ninguna tabla. La notificación sigue sin poder editarse ni borrarse. | ✅ |
| VIII. Pruebas | Dominio y aplicación, viéndolos fallar; PostgreSQL real; los dos procesos; los scripts con la caída. | ✅ |
| IX. Seguridad | La cadena, con el permiso de hoy. El destino por persona solo lo crea un proceso con su token. La identificación no se muestra, no se guarda en la notificación y no se repite en los errores. | ✅ |
| X. Simplicidad | Sin tablas, sin permisos y sin rutas nuevas: un campo en una respuesta y dos formas de destino. | ✅ |
| XI. Diseño pragmático | Los pasos van en los scripts que ya firman. | ✅ |
| XII. JavaDoc | Por qué publica Firma, por qué el destino es la persona y por qué se resuelve en `identidad`. | ✅ |
| XIII. Errores | Sin códigos nuevos: el destino inválido es `NOTIFICACIONES_NOTIFICACION_INVALIDA`. | ✅ |
| XIV. Validación | Las formas del destino en el contrato y en el dominio. | ✅ |

**Post-diseño:** sin cambios. D-110 queda registrada antes de implementar, con research §1 a §8.

## Project Structure

### Documentation (this feature)

```text
specs/054-siguiente-firmante/
├── spec.md, plan.md, research.md, data-model.md, quickstart.md
├── contracts/ (eventos.md, firma.md, spa.md, demostracion.md)
├── checklists/requirements.md
└── tasks.md            (/speckit-tasks)
```

### Source Code (repository root)

```text
contratos/firma/openapi.yaml               EstadoDelDocumento.cadena, PasoConEstado
contratos/notificaciones/openapi.yaml      DestinoDelMensaje: oneOf rol, usuario, identificación

backend/servicios/firma/
├── dominio/            FirmasDelDocumento.cadenaConEstado(), siguienteTrasFirmar(); PasoConEstado
├── puertos/entrada/    EstadoDelDocumento (cadena)
├── puertos/salida/     NotificacionDeFirmaPendiente, FirmaPendienteSolicitada
├── aplicacion/         SubirFirmaServicio (la llamada), DocumentoFirmableServicio (la cadena)
├── adaptadores/        PublicadorDeFirmaPendiente; DocumentoControlador (la cadena)
└── infraestructura/    ConfiguracionFirma (el bean)

backend/servicios/notificaciones/
├── dominio/            DestinoDelMensaje (por rol, por usuario, por identificación),
│                       SolicitudDeNotificaciones
├── aplicacion/         RecibirNotificacionesServicio (ORIGENES + firma)
└── adaptadores/salida/ DestinatariosDesdeIdentidad (las personas)

backend/servicios/usuarios/
├── puertos/entrada/    ConsultarCuentaPorIdentificacion
└── aplicacion/         su servicio, sobre el repositorio de usuarios

frontend/src/pantallas/comun/DocumentoFirmable.tsx (+ prueba), navegacion/enlaces.test.ts
frontend/e2e/ (la cadena con axe-core)
deploy/probar_documento_firmable.sh, deploy/probar_rechazo_en_firma.sh
.github/workflows/perfil-consolidado.yml   (el comentario)
docs/decisiones.md                         D-110
```

**Structure Decision**:
- Quién sigue y el estado de cada paso son de Firma, dueño de la cadena.
- Resolver una persona a su cuenta es de Usuarios, en el proceso de Notificaciones.
- Guardar y mostrar sigue siendo de Notificaciones.

## Riesgos

| Riesgo | Mitigación |
|---|---|
| La identificación de una persona viaja en el evento y queda en el registro de publicaciones. | Ya está en Firma desde D-102, y viaja por la red interna con el token de proceso. No se guarda en la notificación ni se repite en un error (research §4). Se registra en D-110. |
| La cadena y «Pendientes de firma» se contradicen. | Las dos usan la regla del turno de `FirmasDelDocumento`, y una prueba de dominio las compara en cada caso. |
| El firmante esperado no tiene cuenta, y la cadena se detiene sin que nadie se entere. | Es lo que decidió el autor (FR-002 d). La cadena muestra quién está en turno. |
| El script se alarga con la segunda caída de `identidad`. | Alrededor de un minuto. `MDD_CONSERVAR` la salta. |
| `identidad` recibe más trabajo, y #224 recién se resolvió. | Una notificación más por firma, resuelta con una consulta por identificación. Se mide la memoria después del despliegue. |
| Un cambio del contrato de Notificaciones rompe lo que publica Validación OCS. | El destino por rol no cambia, y su prueba de contrato sigue. La prueba de contrato de Validación OCS sigue publicando el destino por rol, y la de `DocumentoControlador` sigue validando la respuesta de antes. `verificar:ruptura` solo compara el contrato de Usuarios. |

## Complexity Tracking

Sin violaciones que justificar.

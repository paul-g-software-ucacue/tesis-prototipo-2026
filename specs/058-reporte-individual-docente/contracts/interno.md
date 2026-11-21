# Contrato: las rutas internas de Aprobación y de Generación

F20-H01 (#268). Son las primeras rutas internas del proceso `distributivo`:
- solo se llaman entre procesos, con el token de proceso (ADR-023);
- la configuración de `plataforma:seguridad` ya exige el rol `PROCESO` en `/interno/**`;
- el API Gateway no las enruta.

## Aprobación: `consultarPublicacionesVigentes` (nueva)

`GET /interno/validacionocs/publicaciones`, en `contratos/validacionocs/openapi.yaml`.

- **Descripción:** «Solo entre procesos, con el token de proceso: lo usa Reporting (F20-H01, D-115). Para cada periodo con distributivo publicado, la publicación vigente: la última transición a PUBLICADO. Durante una reforma, la anterior (D-113).»
- **200:** un arreglo de `PublicacionVigente`, vacío si no hay ninguna.

| Campo | Tipo | Regla |
|---|---|---|
| `periodo` | string | El código del periodo |
| `version` | integer, ≥ 1 | La versión publicada |
| `hash` | string, 64 hex | El hash registrado en la transición |
| `publicadaPor` | string | Quién publicó |
| `publicadaEn` | string, date-time | Cuándo |
| `enReforma` | boolean | Si hay una reforma en curso |

`additionalProperties: false`.
- **Errores:** 401, 403 y 500, los comunes.
- **Por qué una lista y no una ruta por periodo:** hay un distributivo por periodo y pocos periodos, y Reporting necesita la lista para la pantalla.

**Pruebas:**
- `InternoValidacionocsContratoTest`: sin publicaciones, con una y durante una reforma;
- el 403 con un token de usuario.

## Generación: `consultarCargasCongeladas` y `consultarCargaCongelada` (nuevas)

En `contratos/generacion/openapi.yaml`.

### `GET /interno/generacion/periodos/{periodo}/versiones/{numero}/cargas`

- **Descripción:** «Solo entre procesos: el contenido congelado de una versión, guardado al congelarla (F20-H01, D-115). Si falta, se calcula y se guarda en ese momento.»
- **200:** `CargasCongeladas`:
  - `hash`, el hash de la versión;
  - `docentes`, un arreglo de `{identificacion, nombre}` por nombre.
- **404:** `NoEncontrada`: el periodo no tiene esa versión, o no está CONGELADA.

### `GET /interno/generacion/periodos/{periodo}/versiones/{numero}/cargas/{identificacion}`

- **200:** `CargaCongeladaDelDocente`:
  - `hash`;
  - `docente`: `identificacion`, `nombre`, `dedicacion` (opcional) y `horasContratadas` (opcional);
  - `asignaturas`: `codigo`, `nombre`, `paralelo`, `carrera`, `nombreCarrera` y `horas`;
  - `actividades`: `codigo`, `nombre`, `componente`, `carrera`, `proyecto` (opcional), `nombreProyecto` (opcional) y `horas`;
  - `horas`: los cinco componentes.
- **404:** `NoEncontrada`: la versión no existe o no está congelada, o el docente no está en ella.
  - El cliente de Reporting distingue los dos casos porque primero pide la lista: si la versión está, un 404 aquí es el docente.

**Errores:** 400 (periodo, número o identificación inválidos), 401, 403 y 500, los comunes.

**Pruebas:**
- `InternoGeneracionContratoTest`: guardado, tardío, versión no congelada y docente ausente.
- `ContenidoCongeladoIntegrationTest`, en PostgreSQL real:
  - guardar dos veces deja una fila;
  - el contenido guardado no cambia al corregir las horas de un paralelo después;
  - la versión 1 sigue disponible con la 2 vigente durante la reforma.

## Los clientes de Reporting

`PublicacionesDesdeAprobacion` y `ContenidoDesdeGeneracion`, en `reporting/adaptadores/salida`, usan `ConsultasEntreProcesos`:
- **Configuración:** `mdd.procesos.distributivo` en `apps/salida/application.yaml`, con `MDD_URI_DISTRIBUTIVO`.
- **Respuestas:** un 404 es un resultado vacío; un error o un plazo vencido, el 502 o el 504 común.

## Identidad y Usuarios

- **El token de proceso de `salida`:** Identidad suma `mdd.usuarios.procesos.salida.clave: ${MDD_CLAVE_PROCESO_SALIDA:}`. `compose.yaml` y el despliegue pasan la clave a `identidad` y a `salida`.
  - Sin la clave, `salida` no consulta ni registra, y la descarga responde 502. Por eso el despliegue falla si falta (D-55).
- **`recibirEvento`** (`/interno/usuarios/eventos`) admite `reporting.EntradaBitacora`. Su descripción pasa a nombrar siete servicios.
- **Pruebas:**
  - `RegistrarEventoRecibidoServicioTest`, con el origen `reporting`;
  - `EmitirTokenDeProcesoServicioTest`, con `salida` habilitado.

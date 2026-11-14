# Research: Ver mis firmas pendientes y descargar el PDF vigente

## §1. Quién es el firmante esperado y cuándo se fija

**Decisión.** El firmante esperado de cada paso se fija **al generar el documento**, en la misma transacción que lo registra, y se guarda en una tabla nueva de Firma, `firmante_esperado`. Es la persona cuyo nombre imprime el PDF (D-101):
- **Responsable de consolidación:** el **usuario** de la designación del periodo. La ruta interna de Roles ya lo devuelve con su nombre (`/interno/roles/periodos/{periodo}/responsable` → `{usuario, nombre}`).
- **Vicerrector y Rector:** la **identificación** del docente del cargo vigente. `cargos-vigentes` de Docentes ya la trae (`Cargo.docente`), y Firma ya la usa para el nombre.

El puerto `FirmantesDelPeriodo` devuelve, por rol, el nombre y la persona: un usuario o una identificación. El nombre se sigue imprimiendo como en F13-H02.

**Por qué al generar.** El firmante es quien figura en el PDF. Si cambia la designación o el cargo después, el turno no se mueve a alguien cuyo nombre no está impreso. Rehacer el documento es de F12-H04.

**Los documentos generados antes de esta historia** no tienen firmante esperado y no aparecen como pendientes de nadie. En la demo no hay ninguno, porque 2026-I está en EN_CONSOLIDACION. En un perfil local con uno viejo, basta con levantar la base otra vez. Se ajusta el supuesto de la spec: no se calcula después.

**Alternativas.**
- *Calcularlo en cada consulta:* sigue al cargo vigente, aunque no sea el nombre impreso. Además pide Datos e Identidad en cada apertura de la pantalla.
- *Calcularlo una vez para los documentos viejos:* es un relleno perezoso con escritura en una lectura, para documentos que en la demo no existen.

## §2. Cómo se reconoce al usuario de un cargo: la identificación de la cuenta

**Decisión.** La cuenta de usuario guarda un `identificacion` opcional y único, con el formato de Docentes (`^[0-9A-Z]{1,13}$`; migración V7 de Usuarios).

El usuario está en turno para un paso de autoridad si su identificación es la del firmante esperado. Firma la pide a Usuarios por una ruta interna nueva, solo con token de proceso: `/interno/usuarios/{usuario}/identificacion` → `{identificacion}`, o 404 si no tiene. La pide solo cuando algún documento tiene en turno un paso por identificación.

`usuarios-demo.yaml` agrega `identificacion` a:
- `rectorado.demo`: `0109990010`, el RECTOR vigente en `08_cargos_autoridades.csv`;
- `vicerrectorado.demo`: `0109990028`, el VICERRECTOR vigente.

`CargaUsuariosDemo` la pone y la corrige igual que el nombre y los roles. Sigue siendo idempotente: si nada cambió, no escribe. El equipo de demostración carga los mismos datos de prueba (`poblar_demo.sh`).

**Por qué una ruta interna y no el token.** La identificación es un dato personal (FR-013, principio IX): no viaja en cada petición, ni la ve el navegador. La consulta es una por apertura de la pantalla o por descarga.

**Alternativas.**
- *En el JWT:* la expone al navegador y a todos los servicios.
- *Resolver la identificación a un usuario al generar:* si la cuenta recibe la identificación después, el paso quedaría sin nadie para siempre.
- *Una entidad que enlace usuario y docente en Docentes:* F03-H04 traerá la entidad docente; hoy basta el dato en la cuenta.

## §3. El turno y el archivo vigente, sin firmas todavía

**Decisión.**
- **Paso en turno:** el primer paso de la cadena (`generacion_documento.cadena`, en su orden) que no tiene firma. Sin la tabla de firmas (F14-H02), es el de orden 1. El dominio lo escribe como regla sobre una lista de órdenes firmados, hoy vacía, para que F14-H03 solo la alimente.
- **Archivo vigente:** el resultante de la última firma, o el original sin firmas. Hoy es `documento_firmable.archivo` con `sha256_original`. F14-H02 agregará el archivo resultante de cada firma, que nunca sobrescribe al anterior.

## §4. Las operaciones HTTP

**Decisión.** Dos operaciones en `contratos/firma/openapi.yaml`, ambas con `x-mdd-permiso: DISTRIBUTIVO:FIRMA`. La matriz D-16 ya da ese permiso a RESPONSABLE_CONSOLIDACION, VICERRECTOR y RECTOR, y la designación del docente le da los permisos del Responsable.

- **`GET /api/firma/pendientes`** (`listarFirmasPendientes`) → `[{documento, periodo, version, orden, rotulo, generadoEn}]`, ordenado por periodo. Sin documentos en turno, `[]`.
- **`GET /api/firma/documentos/{documento}/archivo`** (`descargarArchivoVigente`) → `application/pdf`:
  - `Content-Disposition: attachment; filename="distributivo-<periodo>-v<version>.pdf"`;
  - `ETag` con el SHA-256 entre comillas.
  
  Antes de entregar, Firma recalcula el SHA-256 del archivo leído. Si el archivo falta o no coincide, responde `FIRMA_ARCHIVO_NO_DISPONIBLE` (500), sin entregar nada y con un incidente en el registro.

**El 403 común** sale para cualquiera que no sea el firmante en turno, y también para un documento que no existe o que no está generado. Así no se revela si existe ni de quién es el turno (FR-010). El identificador mal formado da 400.

**Las rutas siguen el diseño de firmas §9:** `/firmas/pendientes` y `/documentos/{id}/archivo`, bajo el prefijo del servicio.

## §5. La pantalla

**Decisión.** Ruta `/firmas-pendientes`, con la entrada «Pendientes de firma» en el menú bajo el permiso `DISTRIBUTIVO:FIRMA`, después de «Flujo y firmas». La pantalla tiene una tabla accesible con:
- el periodo, la versión, el paso y la fecha de generación;
- «Descargar el PDF» por fila.

Sin pendientes, dice «No tienes firmas pendientes.».

La descarga reusa el patrón de `plantillas/consultas.ts` (`parseAs: 'blob'` y un enlace temporal), con el nombre que da el servidor. Un error se muestra con `AvisoDeError`, como en las demás pantallas.

## §6. La prueba de punta a punta

**Decisión.** Se extiende `deploy/probar_documento_firmable.sh` con dos pasos después del 6, porque ese script ya deja el documento generado:
- **7:** `docente.demo`, el designado, ve una fila con «Elaborado por» y descarga un archivo con el SHA-256 registrado;
- **8:** no lo ven ni lo descargan (lista vacía y 403):
  - `consolidacion.demo`, con el rol pero sin la designación;
  - `vicerrectorado.demo` y `rectorado.demo`, fuera de su turno;
  - `coordinacion.demo`, sin el permiso.

`probar_autenticacion.sh` no cambia: los roles y los permisos de los usuarios de la demo son los mismos.

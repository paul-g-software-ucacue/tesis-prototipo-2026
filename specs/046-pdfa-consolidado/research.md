# Research: Generar el PDF/A del consolidado con su hash

## §1. Cómo se genera el PDF/A: el camino del spike B00-S01

**Decisión.** OpenHTMLtoPDF 1.1.87 sobre PDFBox 3.0.8, como `PdfaDePruebaTest` de B00-S01, que veraPDF dio conforme a PDF/A-2b (`specs/005-spike-firmaec/evidencia.md`):
- conformidad `PDFA_2_B`, modo rápido;
- DejaVu Sans incrustada, que pasa de los recursos de prueba a los de producción, con su licencia;
- el perfil sRGB de la JVM como *output intent*;
- XMP con `pdfaid:part=2` y `pdfaid:conformance=B`, que pone OpenHTMLtoPDF.

El XHTML se arma en Java, con cada texto escapado, desde un registro inmutable del contenido. Sin motor de plantillas: son tres tablas y un bloque fijo.

Las dos librerías pasan del classpath de pruebas al de producción del servicio `firma`. Bouncy Castle y veraPDF siguen solo en pruebas (F14 decide Bouncy Castle).

**Alternativas.**
- *JasperReports:* una dependencia grande y un diseñador aparte para tres tablas.
- *PDFBox solo:* armar tablas a mano, con saltos de página y cortes de fila.
- *OpenPDF:* no lo probó el spike, y su PDF/A no está verificado.

## §2. Los campos de firma vacíos

OpenHTMLtoPDF no crea campos de firma. **Decisión:**
- El bloque de firmas va solo en la **última página** (`page-break-before: always`), con posiciones fijas en milímetros (CSS de página, no de flujo). Así cada rótulo cae en un rectángulo conocido.
- Después de renderizar, PDFBox agrega un `AcroForm` con un `PDSignatureField` por firmante, llamado `firma-<orden>-<rol>` (por ejemplo, `firma-1-RESPONSABLE_CONSOLIDACION`). El widget va en el rectángulo del rótulo, con el indicador *Print* y una apariencia normal vacía, que PDF/A-2b exige a los widgets visibles.
- Se guarda en una sola pasada, sin actualización incremental: es el documento original.

veraPDF, en las pruebas, verifica la conformidad **con** los campos.

**Por qué.** El diseño de firmas §4.3 pide campos con nombre. Si FirmaEC no los rellena, la firma se agrega igual de forma incremental: eso es de F14.

## §3. Dónde se verifica el PDF/A

**Decisión.** En las pruebas, no en el proceso:
- veraPDF necesita JAXB 2.3 (`javax.xml.bind`), y Spring Boot 4.1 trae JAXB 4 (D-46, hallazgo 5). Meterlo en el proceso obliga a fijar JAXB 2.3 en el classpath de producción de `distributivo`, con riesgo para el resto.
- La VM de 4 GB (R-19) no tiene margen para validar 165 paralelos en cada generación.
- La conformidad se garantiza por construcción (§1). Se verifica:
  - en `test`, con un contenido pequeño;
  - en `integrationTest`, con el contenido completo de la prueba (los 165 paralelos, armado con datos sintéticos de tamaño real);
  - en el script de punta a punta: el PDF del volumen pasa por la imagen `verapdf/cli:v1.30.2`, la misma versión de la librería, con el perfil 2b.

La spec quedó así en FR-002.

**Alternativa.** *veraPDF en el proceso, en un classloader aparte:* complejidad sin necesidad (principio X).

## §4. Qué dispara la generación: Aprobación le entrega la cadena a Firma (MD-05)

MD-05 fija la dirección: **Aprobación le entrega la cadena a Firma al prepararse el documento, y Firma informa por eventos**, para que Firma no conozca el flujo y no haya ciclo. Esto importa en F14: Firma publicará `CadenaDeFirmasCompleta` y `RechazoEnFirma`, que Aprobación escucha. Si Firma dependiera de Validación OCS, habría un ciclo.

**Decisión.**
- **En la transacción del paso** hacia CONGELADO_EN_FIRMA, Validación OCS publica su evento interno `DistributivoEnFirma`, con:
  - el periodo, la versión y el hash;
  - la cadena de la copia de la configuración (orden, etiqueta y rol de cada paso del documento CONSOLIDADO);
  - el rol de la etapa CONGELADO_EN_FIRMA;
  - el momento.

  Lo guarda el registro de Spring Modulith de `distributivo` (ADR-018). Vale para los cuatro caminos (aprobar, envío a revisión sin la revisión, último parcial y avance genérico), porque todos pasan por `paso()` hacia CONGELADO_EN_FIRMA.
- **Un oyente de Validación OCS** (`PreparacionDelDocumento`, adaptador de salida, `@ApplicationModuleListener`) lo recibe después de confirmar, en otro hilo, y llama a la API publicada de Firma: `PrepararDocumentoFirmable.preparar(SolicitudDeDocumento)`. La aprobación no espera al PDF.
- **Firma** registra la solicitud (con la cadena y el rol de la etapa) y genera. Con lo guardado decide el reintento y las acciones sin preguntar a Aprobación.
- **Un fallo de generación no se relanza:** Firma lo registra (FALLIDA con el motivo) y `preparar` termina normal, así la publicación queda completada. Si se relanzara, `ReenvioDePublicaciones` regeneraría el PDF cada 30 s sin fin ante un dato que falta. El reintento es el botón (FR-012). Solo una caída del proceso antes de registrar deja la publicación incompleta, y el reenvío la retoma: preparar es idempotente (§8).

Dependencias: Validación OCS → Firma (la API publicada `preparacion`), Firma → Generación (el contenido). Firma no depende de Validación OCS; Validación OCS ya depende de Generación. Sin ciclos.

**Alternativas.**
- *Que Firma escuche un evento de Validación OCS y le pregunte la etapa y el árbol* (el primer plan): Firma conocería el flujo y, con los eventos de F14, habría un ciclo. Contra MD-05.
- *Que Generación publique `VersionCongelada` al congelar* (el nombre del modelo de dominio): la versión se congela antes de que la etapa avance; si la segunda transacción de la aprobación falla, habría PDF sin etapa en firma.
- *Llamar a Firma dentro de la transacción de la aprobación:* ata la aprobación a la generación, contra la decisión del autor (4).

## §5. El contenido: Generación lo da por su API publicada

**Decisión.** Una interfaz publicada nueva de Generación, `ContenidoCongelado`, con `contenido(periodo)`:
- exige la versión vigente CONGELADA (si no, `VersionNoCongelada`);
- devuelve el número y el hash de la versión (Firma comprueba que son los del evento);
- el resumen por carrera (nombre, estado del parcial, paralelos, con propuesta y horas por componente) y los totales institucionales, los mismos del consolidado (`ConsultarConsolidado`);
- por docente, ordenado por apellidos: identificación, nombre, sus asignaturas-paralelo (código, nombre, paralelo, carrera y horas), sus actividades no lectivas (nombre, componente y horas) y sus horas por componente, como la carga horaria (`ConsultarCargaHoraria`, D-82).

Generación ya tiene todo eso; la interfaz lo junta sin cálculos nuevos. Llama a Datos (D-83): se usa fuera de una transacción.

**Alternativa.** *Que Firma lea las tablas de Generación:* contra el principio V.

## §6. Los firmantes

**Decisión.**
- **El árbol de firmas** sale de la copia de la configuración del distributivo, la misma de las etapas (FR-003 de F12-H01): la carga que regía al crearse el distributivo, o la plantilla por defecto. Aprobación la lee al publicar el evento (`EtapasAlCrearse` lee también `firmas`) y la entrega en la solicitud (§4, MD-05).
- **Rector y Vicerrector académico:** sus cargos vigentes (`/interno/docentes/cargos-vigentes`, tipos `RECTOR` y `VICERRECTOR`) y el nombre de ese docente (`/interno/docentes/activos`), en el proceso `datos`. Firma tiene su propio puerto, `FirmantesDelPeriodo`, y un adaptador entre procesos, como los de Generación (D-83).
- **Responsable de consolidación:** el usuario designado para el periodo (`roles.designacion_responsable`, ADR-003) y su nombre para mostrar (`usuarios.usuario.nombre_para_mostrar`), en el proceso `identidad`. Ruta interna nueva en Roles, `/interno/roles/periodos/{periodo}/responsable` → `{usuario, nombre}`. Roles pide el nombre a Usuarios por una interfaz publicada nueva (`NombreDeUsuario`), en el mismo proceso.
- **El cargo** que se muestra sale del rol: RESPONSABLE_CONSOLIDACION → «Responsable de consolidación», VICERRECTOR → «Vicerrector académico», RECTOR → «Rector».
- **Si falta alguno** (sin cargo vigente, sin designación o sin nombre): `FirmanteSinNombre` con el cargo, y la generación falla con el motivo (FR-008).

**Supuesto.** Los cargos vigentes son los de hoy, no los del periodo: la carga de autoridades no tiene historia por periodo. Para un periodo en curso es lo mismo.

## §7. El archivo: un almacén propio de Firma en el mismo volumen

D-88 dejó para Firma decidir si el puerto de archivos sube a la plataforma. **Decisión:** no sube.
- Firma tiene su puerto `AlmacenDeDocumentos`, con su adaptador sobre el volumen `archivos`, en la carpeta `documentos/`.
- El nombre del archivo es el SHA-256 del PDF más `.pdf`.
- Se escribe con `CREATE_NEW`. Si ya existe un archivo con ese nombre, su contenido es el mismo por construcción: se reutiliza sin escribir (FR-009). Nunca se sobrescribe ni se borra.

El de Generación guarda archivos subidos con una clave que genera; este guarda documentos generados nombrados por su contenido. Compartir el puerto obligaría a uno de los dos a cambiar sus reglas.

**Alternativa.** *`:plataforma:archivos`:* un módulo para dos clases con reglas distintas (principio X). Si F14 o F15 lo piden, se sube entonces.

## §8. Una sola generación por versión

**Decisión.**
- El esquema `firma` (V2) tiene `generacion_documento` (una fila por periodo y versión: estado EN_CURSO, FALLIDA o GENERADA, motivo, quién y cuándo) y `documento_firmable` (único por periodo, versión y tipo).
- Generar toma la fila de la generación con `FOR UPDATE` (la crea si no existe):
  - si ya está GENERADA, devuelve el documento sin hacer nada;
  - si no, la marca EN_CURSO y confirma; genera fuera de la transacción (el contenido llama a Datos, D-83); al final, en otra transacción con la fila bloqueada, guarda el archivo y registra el documento, la entrada de la bitácora y la generación GENERADA.
- Dos generaciones a la vez: la segunda espera el bloqueo de la última transacción y, al ver GENERADA, no registra nada (SC-005). Si las dos generaron el mismo PDF, el archivo es el mismo nombre: la segunda no escribe.
- Una generación EN_CURSO vieja (una caída a mitad) no bloquea: el reintento la retoma.

**Bitácora.** Una entrada `DOCUMENTO_FIRMABLE_GENERADO` del servicio `firma`, con el periodo, la versión, el hash de contenido y el SHA-256 del PDF. Hace falta:
- agregar `firma` a los servicios de `usuarios.bitacora` (migración de Usuarios);
- agregarlo a la enumeración `servicio` de `contratos/usuarios`;
- y `firma` publica por el mismo camino de ADR-018.

Un fallo no deja entrada (FR-013).

**El PDF no es reproducible byte a byte:** OpenHTMLtoPDF pone la fecha de creación. Por eso hay un solo documento por versión: el primero que se registra.

## §9. La operación y la pantalla

**Decisión.**
- **`GET /api/firma/periodos/{periodo}/documento`** (`consultarDocumentoFirmable`): el estado del documento de la versión vigente:
  - `estado`: SIN_GENERAR, EN_CURSO, FALLIDA o GENERADO;
  - la versión;
  - el motivo, si falló;
  - el documento (SHA-256, hash de contenido, quién y cuándo), si existe;
  - `acciones`: `[GENERAR]` para el rol de la etapa que llegó en la solicitud, con la generación FALLIDA.
  - Sin solicitud de Aprobación responde 200 SIN_GENERAR, sin versión: Firma no conoce el flujo. Lo leen quienes leen el flujo.
- **`POST /api/firma/periodos/{periodo}/documento`** (`generarDocumentoFirmable`), sin cuerpo:
  - permiso `CONSOLIDACION:APROBACION` (el Responsable lo tiene) y, en el dominio, el rol de la etapa que llegó en la solicitud de Aprobación (§4);
  - genera en la misma petición y responde el estado: 200 GENERADO;
  - si la generación falla, 409 `FIRMA_GENERACION_FALLIDA` con el motivo en los detalles;
  - 409 `FIRMA_ETAPA_NO_ADMITE_GENERACION` si Aprobación no pidió el documento de la versión vigente (el distributivo no entró en firma);
  - 403 sin el rol.
- Contrato nuevo: `contratos/firma/openapi.yaml`. El API Gateway ya enruta `/api/firma/**` a `distributivo`.
- **SPA:** en CONGELADO_EN_FIRMA, la tarjeta del flujo consulta el documento y muestra «Documento firmable generado el <fecha>» con la huella abreviada y «Copiar la huella» (el mismo componente de la constancia), «Generando el documento firmable…», o «El PDF no se pudo generar» con el motivo. Con la acción, el botón «Generar el PDF». Mientras el estado sea EN_CURSO o SIN_GENERAR, vuelve a consultar cada 3 s, hasta 1 minuto.

## §10. La prueba de punta a punta

`deploy/probar_documento_firmable.sh` parte de `probar_aprobacion.sh`:
1. Arma el consolidado, lo envía a revisión y `vicerrectorado.demo` aprueba.
2. Espera el documento GENERADO (hasta 30 s, SC-003).
3. Comprueba en el volumen que el archivo existe, se llama como su SHA-256 y recalcularlo da el registrado (SC-004).
4. Lo verifica con veraPDF (SC-001).
5. Comprueba el hash de contenido igual al de la congelación (SC-002).
6. Comprueba una sola entrada en la bitácora.
7. Comprueba que otro pedido devuelve el mismo documento y que un 403 o 409 no cambia nada.

**El camino de fallo** (un firmante sin nombre) se prueba en la aplicación y en la integración. En el script es difícil quitar el Rector a mitad sin romper otras cargas.

Su paso va en `perfil-consolidado`, después de «Aprobar y congelar».

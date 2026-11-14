# Research: Validar que el PDF subido es el mismo documento con una firma íntegra

## §1. Una sola tabla para las validaciones; la firma es la validación aceptada

**Decisión:** una tabla nueva, `validacion_firma`, con una fila por cada subida del firmante en turno, aceptada o rechazada. La firma de un paso es su validación aceptada. Un índice único parcial sobre `(periodo, version, tipo, orden) where aceptada` impide dos firmas en el mismo paso.

**Por qué:**
- El informe y la firma tienen los mismos datos: quién, cuándo, el SHA-256, las comprobaciones y el certificado. Separarlos duplicaría datos (principio X).
- Una tabla con `revoke update` no puede ser destino de una clave foránea: PostgreSQL bloquea la fila apuntada con `FOR KEY SHARE` como dueño, y eso exige UPDATE (hallazgo de T023 de F13-H02, data-model de F14-H01). Con una `firma` aparte que apunte a su informe, el problema se repetiría.
- El índice único parcial es la garantía de FR-014 en la base, además del bloqueo (§4).

**Alternativas:**
- `firma` e `informe_validacion` separadas, como el diseño de firmas §3. Descartada por la clave foránea entre dos tablas inmutables y la duplicación.
- Guardar las firmas en jsonb dentro de `generacion_documento`, que sí se actualiza. Descartada: la firma debe ser inmutable (principio VII).

## §2. El archivo vigente y el turno salen de las firmas

**Decisión:** `FirmasDelDocumento` (F14-H01) recibe las firmas registradas.
- `pasoEnTurno` usa sus órdenes, en lugar del conjunto vacío de hoy.
- `archivoVigente()` es el archivo de la firma de mayor orden; sin firmas, el original.

Como la firma solo se acepta si el archivo empieza con los bytes del vigente, la de mayor orden es siempre la última de la cadena.

**Por qué:** F14-H01 ya escribió la regla del turno para la cadena completa (research §3 de F14-H01). La lista de pendientes y la descarga siguen solas al nuevo firmante y al nuevo archivo.

**Alternativa:** guardar el «archivo vigente» en una columna que se actualiza. Descartada: es un dato derivado y obligaría a una tabla mutable.

## §3. Las comprobaciones: dónde y cómo

**Decisión:**
- **Mismo documento**, en el dominio: el archivo subido empieza con los bytes del vigente y es más largo. Es una comparación de bytes, sin bibliotecas.
- **Lo que necesita leer el PDF**, detrás de un puerto de salida nuevo, `ValidadorDeFirmas`, con un adaptador sobre PDFBox y Bouncy Castle (`ValidadorPades`), que parte del validador del spike (D-44):
  - **una sola firma nueva:** la cantidad de diccionarios de firma del subido es la del vigente más uno. Los campos vacíos del PDF/A de F13-H02 no cuentan, porque no tienen diccionario de firma. Así vale si el firmante llena el campo de su paso o agrega uno nuevo;
  - **integridad:**
    - la firma nueva es la que termina en el final del archivo: su ByteRange es `[0, a, b, c]` con `b + c` igual al largo del archivo, y el hueco es exactamente su `/Contents`;
    - el CMS se lee como un solo objeto ASN.1, descartando el relleno de ceros (D-46, hallazgo 3);
    - se verifica con la llave pública del certificado, sin la vigencia (D-46, hallazgo 4);
  - **confianza:** PKIX desde el certificado del firmante, con los intermedios que trae el CMS (D-46, hallazgo 2), hasta una raíz de la lista. Sin revocación (ADR-007). La fecha de la comprobación es el `signingTime` firmado del CMS, como en el spike: la vigencia al subir es otra comprobación, de F14-H03, y el informe debe decir cuál de las dos falla.
- **El orden es fijo** (FR-008): mismo documento, una sola firma nueva, integridad y confianza. Se evalúan todas las que se pueden; una que depende de otra que falló queda «no evaluada»:
  - si falla el mismo documento, las demás no se evalúan: el archivo no es el documento;
  - si no hay exactamente una firma nueva, la integridad y la confianza no se evalúan: no hay una sola firma que revisar;
  - si falla la integridad, la confianza tampoco: un CMS ilegible no tiene certificado.
- **Los textos del informe son del sistema**, fijos por resultado. Nunca llevan el mensaje de una excepción de las bibliotecas (RNF-SEG-04): ese va al registro del proceso.

**Por qué:** el dominio decide qué se acepta y en qué orden; el adaptador solo dice qué encontró en el PDF. El validador del spike ya se probó con las firmas reales de FirmaEC (D-46).

**Alternativas:**
- EU DSS: informes más completos, pero es una dependencia grande y queda como trabajo futuro (ADR-007).
- Validar con la fecha de la subida: mezclaría la vigencia (F14-H03) con la confianza.

## §4. Validar fuera de la transacción y volver a comprobar con el bloqueo

**Decisión:**
1. **Fuera de transacciones:**
   - el turno (con la identificación, pedida a Usuarios; D-83);
   - leer el archivo vigente y verificar su SHA-256;
   - validar.
2. **Si se acepta:**
   1. se guarda el archivo por su SHA-256 (`CREATE_NEW`; si ya existe con ese nombre, es el mismo contenido);
   2. en una transacción, se bloquea la fila de `generacion_documento` (`FOR UPDATE`);
   3. se vuelve a leer las firmas. Si cambiaron, otra subida ganó: el informe pasa a rechazado por «mismo documento»;
   4. se inserta la validación y se publica el cambio para la bitácora.
3. **Si se rechaza:** en una transacción, la validación rechazada y su cambio para la bitácora.

**Por qué:**
- La validación criptográfica tarda milisegundos, pero la consulta entre procesos no puede ir dentro de una transacción (D-83).
- El bloqueo y el índice único aseguran FR-014.
- Si la transacción falla después de escribir el archivo, queda un archivo sin registro, nombrado por su hash. No es un archivo vigente ni se entrega: es el mismo caso de F13-H02 (FR-013 se cumple en la base).

**Alternativa:** todo dentro de la transacción. Descartada por D-83.

## §5. La lista de confianza

**Decisión:**
- **Formato:** archivos PEM en una carpeta de recursos de Firma, `backend/servicios/firma/src/main/resources/firma/raices/`, leída al arrancar.
- **Gitleaks:** su regla `certificado-o-llave` y el hook de pre-commit rechazan cualquier `.crt` fuera de `src/test/resources/ca-prueba/`, y el principio IX lo dice así. Se agrega una excepción solo para esa carpeta y solo para `.crt`, y una prueba comprueba que cada archivo es un certificado sin llave privada. El principio IX se enmienda en parche (2.2.1, D-103): las raíces públicas de confianza viven en su carpeta, y el certificado real del autor y cualquier llave siguen prohibidos. La propiedad `mdd.firma.raices` dice qué archivos se cargan. Sin raíces, o con un archivo ilegible, el proceso no arranca (FR-021 y el caso borde).
- **La imagen del proceso** trae `ca-prueba.crt` (la CA de prueba del spike) y `bce-raiz.crt` (la raíz pública del BCE), y la configuración por defecto carga las dos. Así el perfil local, la CI de `perfil-consolidado` y la demo confían en ambas sin cambiar el `compose` de la demo.
- **Las pruebas automáticas** (unitarias y de integración) cargan solo la CA de prueba. La CI no tiene ningún certificado real.
- **La CA de prueba solo en demo** (principio IX, S-10): la configuración por defecto de la imagen es la de demostración. Una instalación de producción cambia `mdd.firma.raices` por las raíces de las entidades acreditadas; una prueba comprueba que la propiedad reemplaza la lista. Queda en D-103.
- **La raíz del BCE la entrega el autor**, la misma que usó en el spike (D-46), descargada del sitio oficial. Su huella SHA-256 se anota en D-103 y en una prueba que lee el archivo. Ningún certificado de una persona entra al repositorio (principio IX).

**Por qué:**
- Lo que cambia por entorno es un dato, sin interruptores (ADR-017).
- La raíz es pública.
- En la CI, `perfil-consolidado` firma con la CA de prueba y no necesita otra cosa.

**Alternativas:**
- Montar las raíces con `compose`: obligaría a cambiar `/opt/mdd` en la demo, que no se toca.
- Un archivo local fuera del repositorio: descartado por el autor.
- Guardar la raíz del BCE en `src/test/resources/ca-prueba/` y copiarla a la imagen al construirla: evita la enmienda, pero mezcla una raíz real con la CA de prueba.

## §6. Cómo firma el script de punta a punta

**Decisión:** un programa Java de un solo archivo, `deploy/herramientas/FirmarDePrueba.java`. Corre en `eclipse-temurin:25-jdk`, la imagen con la que ya se compilan los procesos, con PDFBox y Bouncy Castle copiados del contenedor `distributivo` (`docker compose cp`), las mismas versiones del proceso. Modos:
- `firmar <entrada> <salida> <p12> <campo>`: una firma incremental en el campo vacío del paso, con un certificado ficticio de `ca-prueba/` (contraseña ficticia de `generar.sh`);
- `--ca-ajena`: firma con una CA creada en memoria, que no está en la lista;
- `--alterar`: firma y después cambia un byte dentro de lo firmado.

Las dos firmas nuevas se hacen firmando dos veces. Sin firma nueva, se sube el vigente tal cual.

**Por qué:**
- Sin dependencias nuevas, con las mismas bibliotecas de Firma.
- La imagen ya está en la CI.
- FirmaEC no se puede automatizar (ADR-017).

**Alternativas:**
- pyHanko en un contenedor: una dependencia nueva de terceros solo para pruebas.
- Una tarea de Gradle: el job de `perfil-consolidado` no tiene JDK; solo Docker.

## §7. El campo de la firma de FirmaEC

**Hallazgo pendiente:** el PDF/A de F13-H02 trae tres campos vacíos (`firma-<orden>-<rol>`). El spike firmó un PDF sin campos, así que no se sabe si FirmaEC llena el campo vacío o agrega uno nuevo.

**Decisión:** la validación no depende de eso (§3). El firmador de prueba llena el campo del paso. El autor lo averigua en su recorrido a mano con FirmaEC (`quickstart.md` §3), y se anota en la evidencia. Exigir el campo del paso es de F14-H03.

## §8. La respuesta: 201 con el informe, o 422 con las comprobaciones en los detalles

**Decisión:**
- **Aceptada:** `201`, con el informe de validación (`InformeDeValidacion`).
- **Rechazada:** `422` con el código de la primera comprobación que falló. Los `detalles` del error llevan una entrada por comprobación: `campo` es la comprobación, y `descripcion`, su resultado con el texto del sistema.

**Códigos nuevos:**

| Código | Estado |
|---|---|
| `FIRMA_NO_ES_LA_VERSION_VIGENTE` | 422 |
| `FIRMA_SIN_UNA_FIRMA_NUEVA` (ninguna o más de una) | 422 |
| `FIRMA_NO_INTEGRA` | 422 |
| `FIRMA_SIN_CONFIANZA` | 422 |
| `FIRMA_ARCHIVO_DEMASIADO_GRANDE` (más de 10 MB) | 413 |
| `FIRMA_ARCHIVO_VACIO` | 400 |

Un PDF dañado después del prefijo no pasa «una sola firma nueva», porque no se puede leer: es `FIRMA_SIN_UNA_FIRMA_NUEVA`, con ese motivo en el informe.

**Por qué:** el esquema `Error` común ya tiene `detalles` como una lista de `{campo, descripcion}`. No hace falta cambiar el contrato común, y la pantalla muestra lo mismo en los dos casos.

**Alternativa:** un `200` con `aceptada: false`. Descartada: el issue pide un 422.

## §9. La bitácora

**Decisión:** dos acciones nuevas del servicio `firma`, en la misma transacción que la validación:
- `FIRMA_ACEPTADA`;
- `FIRMA_RECHAZADA`.

El detalle lleva:
- el periodo, la versión y el orden;
- el SHA-256 del archivo subido;
- el resultado de cada comprobación;
- con firma nueva, el CN y el número de serie del certificado.

En `nuevo`, para la aceptada, el SHA-256 del archivo vigente.

**Por qué:** RF-44 registra cada cambio, y la subida rechazada también es evidencia (FR-017). La acción es texto libre en la bitácora (V5 de Usuarios), así que no hace falta otra migración allí.

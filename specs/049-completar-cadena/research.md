# Research: Validar firmante, turno y vigencia, y completar la cadena

## §1. Tres comprobaciones más, en el dominio

**Decisión:** el enum `Comprobacion` de F14-H02 suma `VIGENCIA`, `ORDEN` y `FIRMANTE`, en ese orden, después de `CONFIANZA`. Un resultado lleva las siete. La regla de F14-H02 no cambia: después de una no aprobada, las siguientes quedan «no evaluadas».

Las tres se deciden en el dominio, en `ComprobacionDelFirmante`, con lo que ya existe:
- **el certificado de la firma nueva** (`CertificadoDeLaFirma`, de F14-H02): su CN y su vigencia;
- **el momento de la subida**;
- **los firmantes esperados del documento**, con el nombre impreso de cada paso (§2).

Las reglas:
- **Vigencia:** `validoDesde ≤ subida ≤ validoHasta`. Si no, rechazada. Es la vigencia al subir que F14-H02 dejó fuera de la confianza (D-103).
- **Orden:** si el CN normalizado es el nombre impreso de un paso posterior al paso en turno, y no el del paso en turno, rechazada por fuera de orden.
- **Firmante:** el CN normalizado tiene que ser el nombre impreso del paso en turno. Si no, rechazada.

**La normalización (D-23):**
- mayúsculas;
- sin tildes ni diéresis (descomposición NFD, quitando las marcas), salvo la «Ñ», que se conserva;
- espacios internos unificados y sin espacios al inicio ni al final.

**Por qué:**
- no hace falta leer otra vez el PDF: el validador ya entrega el certificado;
- el dominio decide qué se acepta, como en F14-H02;
- con la misma persona en dos pasos (la instalación del autor), su CN es también el del paso en turno: «orden» solo rechaza si **no** es el del paso en turno.

**Alternativa:** comparar en el adaptador. Descartada: el adaptador no conoce los pasos.

## §2. El nombre impreso se guarda con el firmante esperado

**Decisión:** migración V5 de Firma: `alter table firmante_esperado add column nombre varchar(200)`, nula.
- Se llena al generar, con el mismo nombre que escribe el PDF (`Firmante.nombre`).
- Los firmantes de documentos anteriores quedan sin nombre. Un paso en turno sin nombre no acepta firmas: la subida da `FIRMA_PASO_SIN_NOMBRE` (409), antes de validar.

**Por qué:**
- el firmante esperado ya se fija al generar (D-102) y es inmutable;
- el nombre es parte de la misma decisión;
- `alter table … add column` lo hace el dueño de la tabla, al que V3 solo le quitó UPDATE, DELETE y TRUNCATE.

**Alternativas:**
- releer el nombre del PDF: frágil;
- volver a pedirlo a Datos: seguiría al cargo vigente, no al nombre impreso (D-102).

## §3. Firma avisa a Aprobación con un evento propio

**Decisión:** con la última firma aceptada, Firma publica `CadenaDeFirmasCompleta(periodo, version, momento)` en la misma transacción que registra la firma, por un puerto de salida (`AvisoDeCadenaCompleta`) con el publicador de Spring.
- El tipo vive en la API publicada de Firma (`puertos.entrada`), que Aprobación ya usa.
- En Aprobación, un oyente con `@ApplicationModuleListener` (`CierreDeLaFirma`, adaptador de entrada) llama a su caso de uso `CompletarFirmas`.

**Por qué:**
- MD-05: Firma no conoce el flujo, solo dice que la cadena está completa.
- La dependencia sigue siendo de Aprobación hacia Firma, sin ciclo (`ModulosTest`).
- `@ApplicationModuleListener` corre después de confirmar, en una transacción nueva, y el registro de publicaciones reintenta si falla (ADR-018), como el paso a firma de F13-H02 en sentido contrario.

**Alternativas:**
- que Firma llame a un puerto de Aprobación: crea el ciclo que MD-05 evita;
- que Aprobación consulte a Firma periódicamente: más lento y sin necesidad.

## §4. Aprobación pasa a EN_OCS como el sistema

**Decisión:** `ProcesoAprobacion.completarFirmas(version, ahora)`:
- **en CONGELADO_EN_FIRMA y con esa versión:** una transición `PASO_DE_ETAPA` a la siguiente etapa activa, EN_OCS (del esqueleto fijo), por `sistema`, con el hash de la versión congelada;
- **ya en EN_OCS con esa versión:** no hace nada; el aviso repetido es idempotente;
- **en otra etapa o con otra versión:** no hace nada y lo registra en el log, sin lanzar, para que el aviso no se reintente para siempre (FR-012).

`CompletarFirmasServicio` bloquea el proceso, como los demás pasos, guarda la transición y la registra en la bitácora con el mismo `cambio` de los otros pasos.

**Por qué:** el agregado ya sabe pasar de etapa y registrar quién. El «sistema» es el usuario de las transiciones automáticas (F13-H02).

## §5. La CA de prueba regenerada

**Decisión:** `generar.sh` emite una CA nueva y tres firmantes con los nombres que el PDF imprime en los datos de prueba:

| Paso | CN (ficticio) | De dónde sale |
|---|---|---|
| F1 | `Docente (demo)` | el nombre de `docente.demo` en `usuarios-demo.yaml`, el designado |
| F2 | `CABRERA VAZQUEZ ANA GABRIELA` | el docente 0109990028, del cargo de Vicerrector en `08_cargos_autoridades.csv` |
| F3 | `PERALTA MALDONADO JUAN ANDRES` | el docente 0109990010, del cargo de Rector |

Además, `firmante-vencido.p12`: el mismo CN de F1 con un certificado que ya venció. Lo usan las pruebas y el script para el escenario de vigencia. Como la llave de la CA no se guarda, se emite en la misma corrida.

Cambia la huella de `ca-prueba.crt` en D-103, en `ListaConfianzaTest` y en `firma/raices/`. El `pdfa-prueba.pdf` del spike no depende de la CA.

**Por qué:** las pruebas de punta a punta deben firmar con el CN del paso.

**Alternativa:** una CA en memoria en cada prueba. Descartada por el autor: el perfil de la CI tendría que cambiar su lista de confianza.

## §6. El nombre local del autor

**Decisión:** la variable `MDD_FIRMA_NOMBRE_LOCAL`, en el `.env` del autor (Git lo ignora; `compose.yaml` lo carga con `env_file`).
- Si tiene valor, `FirmantesDelPeriodo` devuelve ese nombre para todos los pasos: el PDF lo imprime y es el nombre esperado. La persona en turno, el usuario o la identificación, no cambia.
- Sin valor, los nombres de siempre.
- La demo y la CI no la definen.

**Por qué:**
- es el «archivo local fuera del repositorio» de ADR-017;
- no agrega montajes;
- solo cambia un dato, sin interruptores en la validación.

**Alternativa:** un archivo YAML montado en el contenedor: un montaje más y un archivo que no hace falta.

## §7. Los códigos nuevos

| Código | Estado | Cuándo |
|---|---|---|
| `FIRMA_CERTIFICADO_NO_VIGENTE` | 422 | `VIGENCIA` rechazada |
| `FIRMA_FUERA_DE_ORDEN` | 422 | `ORDEN` rechazada |
| `FIRMA_FIRMANTE_EQUIVOCADO` | 422 | `FIRMANTE` rechazada |
| `FIRMA_PASO_SIN_NOMBRE` | 409 | el paso en turno no tiene nombre impreso guardado (§2) |

`ArchivoFirmadoRechazado` suma tres subclases. Los detalles del 422 llevan las siete comprobaciones.

## §8. Las filas viejas de `validacion_firma`

Las validaciones de F14-H02 tienen cuatro comprobaciones. En la demo no hay ninguna (D-103), y los perfiles locales empiezan sin datos. Leerlas con `validaciones()`, que solo usan las pruebas, completaría las tres nuevas como «no evaluadas» con el motivo «Anterior a F14-H03». No se reescriben (principio VII).

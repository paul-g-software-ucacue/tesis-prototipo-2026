# Modelo de datos (fase 1): B00-S01

No hay datos persistidos. El modelo describe los artefactos del *spike* (Key Entities) y sus reglas.

## CA de prueba

| Atributo | Regla |
|---|---|
| Nombre | `CN=CA de Prueba MDD Demo, O=MDD Demo, C=EC` |
| Llave | RSA de 3072 bits. **No se guarda**: el script la borra después de emitir. |
| Vigencia | 10 años |
| Extensiones | `basicConstraints=critical,CA:TRUE`, `keyUsage=critical,keyCertSign,cRLSign` |
| En el repositorio | Solo `ca-prueba.crt` (pública) |

## Certificado de firmante

| Atributo | Regla |
|---|---|
| Nombres | `CN=Firmante Uno Prueba (F1)`, `CN=Firmante Dos Prueba (F2)` y `CN=Firmante Tres Prueba (F3)`, con `O=MDD Demo, C=EC`. Ficticios. |
| Llave | RSA de 2048 bits |
| Vigencia | 3 años desde su emisión |
| Extensiones | `keyUsage=critical,digitalSignature,nonRepudiation` |
| Formato | `firmante-N.p12`, con la contraseña ficticia `cambiar-en-local-firma` |
| Ubicación | Solo `backend/servicios/firma/src/test/resources/ca-prueba/` (Gitleaks) |

## PDF/A de prueba

| Atributo | Regla |
|---|---|
| Conformidad | PDF/A-2b, comprobada con veraPDF antes de firmar |
| Contenido | Ficticio: «Distributivo de prueba, periodo 2026-I», sin datos personales |
| Fuente | DejaVu Sans, incrustada |
| Perfil de color | sRGB |

## Firma (PAdES de FirmaEC)

| Atributo | Regla |
|---|---|
| ByteRange | `[0, a, b, c]`: cubre el archivo menos `/Contents`, hasta el final de su revisión |
| `/Contents` | CMS (PKCS #7) separado, con el certificado del firmante |
| Momento | `signingTime` del CMS o, si falta, `/M` del diccionario |
| Revisión | La segunda firma se agrega como actualización incremental: el archivo con dos firmas empieza con los bytes exactos del archivo con una |

**Estados de cada comprobación:** válida o no válida, con el motivo.

## Resultado de la validación (por firma)

| Campo | Contenido |
|---|---|
| `indice` | Número de la firma (1, 2…) |
| `firmante` | CN del certificado |
| `integra` | sí o no (motivo) |
| `cadenaValida` | sí o no (motivo), contra la raíz configurada |
| `vigente` | sí o no, en el momento de la firma |
| `momento` | Fecha y hora de la firma |

## Resultado del *spike*

| Escenario | Estado | Evidencia |
|---|---|---|
| 1. Certificados de la demo | funcionó, no funcionó o pendiente | Mensaje de FirmaEC y versión |
| 2. Firma incremental | ídem | Tamaños y comparación de bytes |
| 3. Validación | ídem | Salida de la prueba por firma, y los 3 casos negativos |
| 4. Resultado | ídem | Comentario en el issue #26, y la constitución o el ADR |

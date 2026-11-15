# Data Model: Validar firmante, turno y vigencia, y completar la cadena

## Firma (servicio 10)

### Migración V5 del esquema `firma`

```sql
alter table firmante_esperado add column nombre varchar(200);
```

- Nula: los firmantes de documentos anteriores no la tienen.
- Se llena al generar con el nombre impreso del paso (research §2).
- Sigue sin UPDATE para el rol del proceso.

### Dominio

| Valor | Cambio |
|---|---|
| `Comprobacion` | Suma `VIGENCIA`, `ORDEN` y `FIRMANTE`, en ese orden, después de `CONFIANZA` |
| `ResultadoValidacion` | Las siete comprobaciones; la regla de «no evaluada» no cambia |
| `FirmanteEsperado` | Suma `nombre`, opcional |
| `NombreDeFirmante` (nuevo) | `normalizar(String)`: mayúsculas, sin marcas diacríticas salvo la Ñ, espacios unificados (research §1) |
| `ComprobacionDelFirmante` (nuevo) | `evaluar(CertificadoDeLaFirma, Instant subida, FirmasDelDocumento)` → las tres comprobaciones en orden, hasta la primera no aprobada |
| `ArchivoFirmadoRechazado` | Suma `NoVigente`, `FueraDeOrden` y `FirmanteEquivocado` |
| `PasoSinNombre` (nuevo) | El paso en turno sin nombre impreso (409) |

### Puertos

- **`AvisoDeCadenaCompleta`** (salida, nuevo): `avisar(CadenaDeFirmasCompleta)`, en la transacción en curso.
- **`CadenaDeFirmasCompleta`** (en `puertos.entrada`, la API publicada): `periodo`, `version`, `momento`.
- **`FirmantesDelPeriodo`:** sin cambios. Un decorador, `FirmantesConNombreLocal`, reemplaza el nombre si `mdd.firma.nombre-local` tiene valor (research §6).

### `SubirFirmaServicio`

1. Antes de validar, con el turno: si el paso no tiene nombre, `PasoSinNombre`.
2. Después del validador, si `CONFIANZA` se aprobó, `ComprobacionDelFirmante.evaluar`.
3. Si se acepta y era el último paso de la cadena, en la misma transacción, `AvisoDeCadenaCompleta.avisar`.

## Aprobación (servicio 6, `validacionocs`)

| Elemento | Cambio |
|---|---|
| `ProcesoAprobacion.completarFirmas(int version, Instant ahora)` | Devuelve la transición a EN_OCS, o vacío si ya está o no corresponde (research §4) |
| `CompletarFirmas` (puerto de entrada, nuevo) | `completar(String periodo, int version, Instant momento)` |
| `CompletarFirmasServicio` | Bloquea el proceso, guarda la transición y registra el cambio en la bitácora |
| `CierreDeLaFirma` (adaptador de entrada, nuevo) | `@ApplicationModuleListener` de `CadenaDeFirmasCompleta` |

Sin migraciones: la etapa EN_OCS y la transición `PASO_DE_ETAPA` ya existen (V3 y V4 de `validacionocs`).

## CA de prueba

`ca-prueba/generar.sh`, regenerado:
- `ca-prueba.crt`;
- `firmante-1.p12` a `firmante-3.p12`, con los CN de research §5, y sus variantes `-legacy`;
- `firmante-vencido.p12`.

Contraseña ficticia: la misma. Huella nueva en D-103.

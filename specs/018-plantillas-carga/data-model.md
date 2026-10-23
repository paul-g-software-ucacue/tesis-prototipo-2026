# Data Model: Descargar las plantillas de carga (F01-H01)

No hay tablas nuevas: la definición de las cargas es un archivo versionado que se lee al arrancar ([research.md](research.md) §2). Tampoco hay registro de cambios, porque descargar no cambia nada (§7).

## `diccionario-de-datos.yaml`

En `backend/servicios/cargas/src/main/resources/cargas/`. Transcrito de `build_diccionario.py` (ERS v2.1).

```yaml
fuente: ERS v2.1, sección 8.3 y anexo cargas/
convenciones:            # 13, iguales para todas las cargas
  - tema: Separador de columnas CSV
    regla: Coma (,) según RFC 4180. …
catalogos:               # 5
  GESTIONES_AUTORIZADAS:
    nombre: Gestiones autorizadas (POL 6)
    valores:
      - { codigo: G01, descripcion: Rector }
      # …
cargas:                  # 10, en el orden de la ERS §8.3
  - numero: 1
    codigo: estructura_organica
    nombre: Estructura orgánica
    archivo: 01_estructura_organica.csv
    dependeDe: []
    obligatoriedad: OBLIGATORIA
    columnas:
      - campo: codigo_gestion_autorizada
        descripcion: Gestión autorizada por SENESCYT que representa
        tipo: Catálogo
        longitud: 3
        obligatorio: Sí si tipo_unidad = GESTION
        formato: G01–G21
        clave: ''
        validacion: Solo las 21 gestiones de POL 6
        ejemplo: G07
        catalogo: GESTIONES_AUTORIZADAS
```

## Dominio (`ec.mdd.cargas.dominio`)

| Tipo | Campos | Reglas |
|---|---|---|
| `Obligatoriedad` (enum) | `OBLIGATORIA`, `OBLIGATORIA_SALVO_SIN_PROYECTOS`, `RECOMENDADA` | cargas 1–8, 9 y 10 (ERS §8.3, RF-18) |
| `ValorDeCatalogo` (record) | `codigo`, `descripcion` | no vacíos |
| `Catalogo` (record) | `codigo`, `nombre`, `valores` | al menos un valor; códigos sin repetir |
| `Convencion` (record) | `tema`, `regla` | no vacíos |
| `ColumnaDeCarga` (record) | `campo`, `descripcion`, `tipo`, `longitud` (Integer, opcional), `obligatorio`, `formato`, `clave`, `validacion`, `ejemplo`, `catalogo` (opcional) | `campo` `^[a-z0-9_]+$`; `descripcion`, `tipo`, `obligatorio` y `ejemplo` no vacíos; ningún texto empieza con `=`, `+`, `-`, `@`, tabulador ni retorno de carro |
| `Carga` (record) | `numero`, `codigo`, `nombre`, `archivo`, `dependeDe` (números), `obligatoriedad`, `columnas` | `codigo` `^[a-z_]{1,40}$`; al menos una columna; campos sin repetir; `plantilla()` devuelve su `PlantillaCarga` |
| `PlantillaCarga` (record) | `archivo`, `encabezados` | los campos de la carga, en su orden |
| `DiccionarioDeDatos` (valor) | `cargas`, `convenciones`, `catalogos` | 10 cargas numeradas 1 a 10; códigos y archivos únicos; cada `dependeDe` apunta a una carga anterior; cada `catalogo` existe; `carga(codigo)` u `Optional.empty()`; `columnas()` = 115 con la ERS v2.1 |

Una definición que no cumple estas reglas lanza `IllegalArgumentException` con la carga y la columna: el proceso `datos` no arranca.

## Puertos

| Puerto | Tipo | Qué hace |
|---|---|---|
| `ConsultarPlantillas` | entrada | `diccionario()`; `plantilla(codigo)`, que lanza `CargaNoEncontrada` |
| `FuenteDelDiccionario` | salida | `leer()`: el `DiccionarioDeDatos` de la definición versionada |

`DiccionarioYaml` (adaptador de salida) implementa `FuenteDelDiccionario` con SnakeYAML (`SafeConstructor`). El servicio lo lee una vez al construirse.

## Los dos CSV (adaptador de entrada)

`EscritorCsv`: UTF-8 con BOM, separador coma, fin de línea CRLF; comillas dobles si el valor tiene coma, comillas o salto de línea; apóstrofo delante de un valor que empiece como fórmula.

| Archivo | Contenido |
|---|---|
| `NN_<codigo>.csv` | una fila: los encabezados |
| `NN_<codigo>_diccionario.csv` | `numero,campo,descripcion,tipo,longitud_maxima,obligatorio,formato_o_valores,clave,validacion,ejemplo`; una fila por columna; con catálogo, `formato_o_valores` lista «código descripción» separados por «; »; luego una fila vacía, `tema,regla` y las 13 convenciones |

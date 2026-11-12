# Data Model: Generar el PDF/A del consolidado con su hash

## Firma (servicio 10, módulo `firma` del proceso `distributivo`)

### Migración V2 del esquema `firma`

```sql
create table generacion_documento (
    periodo        varchar(20) not null,
    version        integer     not null check (version >= 1),
    tipo           varchar(20) not null check (tipo in ('CONSOLIDADO')),
    estado         varchar(20) not null check (estado in ('EN_CURSO', 'FALLIDA', 'GENERADA')),
    motivo         varchar(200),             -- solo con FALLIDA
    pedida_por     varchar(64) not null,     -- 'sistema' o el usuario del reintento
    actualizada_en timestamptz not null,
    primary key (periodo, version, tipo),
    check ((estado = 'FALLIDA') = (motivo is not null))
);

create table documento_firmable (
    id              uuid primary key,
    periodo         varchar(20) not null,
    version         integer     not null,
    tipo            varchar(20) not null,
    hash_contenido  char(64)    not null check (hash_contenido ~ '^[0-9a-f]{64}$'),
    sha256_original char(64)    not null check (sha256_original ~ '^[0-9a-f]{64}$'),
    archivo         varchar(80) not null,    -- documentos/<sha256>.pdf
    tamano          integer     not null check (tamano > 0),
    generado_por    varchar(64) not null,
    generado_en     timestamptz not null,
    unique (periodo, version, tipo),
    foreign key (periodo, version, tipo) references generacion_documento
);
```

Sin borrado ni actualización de `documento_firmable` por el rol del proceso: solo `insert` y `select` (principio VII).

### DocumentoFirmable (agregado nuevo, dominio)

| Campo | Regla |
|---|---|
| `periodo`, `version`, `tipo` | Uno por versión y tipo (FR-005). Tipo `CONSOLIDADO` en el prototipo |
| `hashContenido` | El de la congelación (D-100), el del pie (FR-003) |
| `sha256Original` | El del archivo generado; también es su nombre (FR-009) |
| `generadoPor`, `generadoEn` | `sistema` en la generación automática; el usuario en el reintento |

### GeneracionDeDocumento (agregado nuevo, dominio)

Estados: `EN_CURSO` → `GENERADA` | `FALLIDA`; `FALLIDA` → `EN_CURSO` (reintento). `GENERADA` no cambia.

- **`iniciar(quien, ahora)`**:
  - con GENERADA no hace nada y dice que ya está;
  - con FALLIDA, EN_CURSO o sin fila, pasa a EN_CURSO.
- **`fallar(motivo, ahora)`**: pasa a FALLIDA con el motivo, de hasta 200 caracteres, sin datos personales más allá del cargo.
- **`completar(ahora)`**: pasa a GENERADA.

### ContenidoDelDocumento (valor, dominio)

Lo que se imprime, inmutable:
- periodo, versión y hash de contenido;
- carreras: nombre, estado, paralelos, con propuesta y horas por componente;
- totales institucionales;
- docentes, por apellidos: identificación, nombre, asignaturas (código, nombre, paralelo, carrera y horas), actividades (nombre, componente y horas) y horas por componente;
- firmantes: orden, rótulo («Elaborado por»…), cargo, nombre y el nombre del campo de firma (`firma-<orden>-<rol>`).

### Excepciones nuevas

| Excepción | Código | HTTP |
|---|---|---|
| `EtapaNoAdmiteGeneracion` | `FIRMA_ETAPA_NO_ADMITE_GENERACION` | 409 |
| `GeneracionFallida` (con el motivo en detalles) | `FIRMA_GENERACION_FALLIDA` | 409 |
| `SinDistributivo` | `FIRMA_SIN_DISTRIBUTIVO` | 404 |
| `SinRolDeFirma` | — el controlador la traduce a 403 con el cuerpo común, sin decir qué falta | 403 |
| `FirmanteSinNombre` (con el cargo) | — interna: la generación la convierte en motivo de FALLIDA | — |

### Puertos

- **Entrada:**
  - `ConsultarDocumentoFirmable.estado(periodo, quien)` → `EstadoDelDocumento` (estado, versión, motivo, documento, acciones);
  - `GenerarDocumentoFirmable.generar(periodo, quien)`;
  - `AlEntrarEnFirma.generar(DistributivoEnFirma)`, la reacción al evento.
- **Salida:**
  - `RepositorioDocumentos` (generación bloqueada, documento);
  - `AlmacenDeDocumentos.guardar(byte[]) → nombre` (`CREATE_NEW`, nombre = SHA-256);
  - `ContenidoDelDistributivo` (adaptador sobre `ContenidoCongelado` de Generación);
  - `EtapaYFirmas` (adaptador sobre `ArbolDeFirmas` y el estado del flujo de Validación OCS);
  - `FirmantesDelPeriodo` (adaptador entre procesos: Datos e Identidad);
  - `GeneradorDePdf.generar(ContenidoDelDocumento) → byte[]` (OpenHTMLtoPDF + PDFBox);
  - `RegistroDeCambios` (bitácora, ADR-018);
  - `Transaccion`.

## Validación OCS

- **Evento `DistributivoEnFirma(periodo, version, hash, momento)`** en la interfaz nombrada `eventos`. Se publica en la transacción que guarda un `PASO_DE_ETAPA` hacia CONGELADO_EN_FIRMA, en los cuatro caminos de F13-H01.
- **`ArbolDeFirmas.firmas(periodo)`** → los `PasoFirma` del documento CONSOLIDADO en la copia de la configuración, en su orden.
- **`EtapaDelDistributivo.etapa(periodo)`** → la etapa actual y la versión, y si un `Solicitante` tiene el rol de la etapa CONGELADO_EN_FIRMA.
- La copia de la configuración (`EtapasAlCrearse`) ya lee el YAML completo: `firmas` sale de la misma carga.

## Generación

**`ContenidoCongelado.contenido(periodo)`** (interfaz publicada nueva) → `ContenidoDeLaVersion`:
- versión y hash (`hashDelDistributivo`);
- carreras y totales (del consolidado);
- docentes con sus asignaturas, actividades y horas (de la carga horaria).

Lanza `VersionNoCongelada` si la vigente no está CONGELADA, y `DistributivoNoEncontrado` sin distributivo.

## Roles (proceso `identidad`)

**`/interno/roles/periodos/{periodo}/responsable`** → `{usuario, nombre}`, 404 sin designación. El nombre sale de Usuarios por una interfaz publicada nueva, `NombreDeUsuario.nombre(usuario)` (`nombre_para_mostrar`).

## Usuarios

`bitacora.servicio` admite `firma` (migración nueva, con el `check` ampliado) y la enumeración `servicio` de `contratos/usuarios/openapi.yaml` también.

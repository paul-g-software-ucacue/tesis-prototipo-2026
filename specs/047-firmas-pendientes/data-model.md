# Data Model: Ver mis firmas pendientes y descargar el PDF vigente

## Firma (servicio 10, módulo `firma` del proceso `distributivo`)

### Migración V3 del esquema `firma`

```sql
create table firmante_esperado (
    periodo        varchar(20) not null,
    version        integer     not null,
    tipo           varchar(20) not null,
    orden          integer     not null check (orden >= 1),
    rol            varchar(40) not null,
    usuario        varchar(64),                                    -- el Responsable designado
    identificacion varchar(13) check (identificacion ~ '^[0-9A-Z]{1,13}$'),  -- el docente del cargo
    primary key (periodo, version, tipo, orden),
    foreign key (periodo, version, tipo) references generacion_documento (periodo, version, tipo),
    check ((usuario is null) <> (identificacion is null))
);
```

Como `documento_firmable`, el rol del proceso solo puede insertar y leer: sin `update` ni `delete` (principio VII). Las filas se insertan en la misma transacción que el documento, una por paso de la cadena.

La clave foránea apunta a `generacion_documento`, no a `documento_firmable`. PostgreSQL verifica una clave foránea bloqueando la fila apuntada con `FOR KEY SHARE`, como dueño de esa tabla, y eso exige UPDATE. V2 se lo quitó al dueño de `documento_firmable`: con la clave hacia esa tabla, insertar los firmantes fallaría con «permission denied» (el hallazgo de T023 de F13-H02). La generación tiene la misma clave, y se registra antes.

### FirmanteEsperado (valor nuevo, dominio)

| Campo | Regla |
|---|---|
| `orden`, `rol` | Los del paso de la cadena (MD-05) |
| `usuario` o `identificacion` | Exactamente uno. `usuario` para RESPONSABLE_CONSOLIDACION; `identificacion` para VICERRECTOR y RECTOR (research §1) |

- **`reconoce(Solicitante, Optional<String> identificacionDelSolicitante)`**: verdadero solo si el usuario coincide o la identificación coincide. Tener el rol no basta (FR-007).

### DocumentoFirmable (agregado de F13-H02, ampliado)

- **`firmantes`**: la lista de `FirmanteEsperado`, uno por paso.
- **`pasoEnTurno(Set<Integer> ordenesFirmados)`**: el primer paso sin firma, o vacío si todos firmaron. Hoy recibe un conjunto vacío (research §3).
- **`archivoVigente()`**: el nombre y el SHA-256 del archivo vigente. Hoy, `archivo` y `sha256Original`.

### Firmante (valor de F13-H02, ampliado)

Al cargo y al nombre impresos se suma la persona (`usuario` o `identificacion`). Sale de `FirmantesDelPeriodo`, que pasa de `Map<rol, nombre>` a `Map<rol, FirmanteDelPeriodo(nombre, usuario, identificacion)>`. Un rol sin nombre sigue fallando con `FirmanteSinNombre`. Un rol sin persona falla igual, con el mismo motivo.

### Puertos

- **Entrada (nuevos):**
  - `ConsultarFirmasPendientes.pendientes(Solicitante)` → `List<FirmaPendiente(documento, periodo, version, orden, rotulo, generadoEn)>`;
  - `DescargarArchivoVigente.archivo(UUID documento, Solicitante)` → `ArchivoVigente(nombre, sha256, byte[])`. Lanza `SinTurnoDeFirma`, que el controlador traduce al 403 común, y `ArchivoNoDisponible`.
- **Salida:**
  - `RepositorioDocumentos`: registra los firmantes con el documento; lista los documentos generados con sus firmantes y su cadena; lee uno por id;
  - `AlmacenDeDocumentos.leer(nombre)` → `byte[]`, nuevo;
  - `IdentificacionDeUsuarios.de(usuario)` → `Optional<String>`, nuevo: el adaptador entre procesos sobre la ruta interna de Usuarios;
  - `FirmantesDelPeriodo`, ampliado (arriba).

### Excepciones nuevas

| Excepción | Código | HTTP |
|---|---|---|
| `SinTurnoDeFirma` | — el controlador la traduce al 403 común, sin decir por qué | 403 |
| `ArchivoNoDisponible` | `FIRMA_ARCHIVO_NO_DISPONIBLE` | 500 |

## Usuarios (proceso `identidad`)

### Migración V7 del esquema `usuarios`

```sql
alter table usuario add column identificacion varchar(13)
    check (identificacion ~ '^[0-9A-Z]{1,13}$');
create unique index usuario_identificacion on usuario (identificacion) where identificacion is not null;
```

- **`Usuario`** (dominio): `identificacion` opcional.
- **`CargaUsuariosDemo`**: lee `identificacion` de `usuarios-demo.yaml` y la pone o la corrige, junto con el nombre y los roles. Una identificación repetida en el YAML falla al arrancar.
- **`/interno/usuarios/{usuario}/identificacion`** → `{identificacion}`; 404 `USUARIOS_SIN_IDENTIFICACION` si la cuenta no existe o no tiene. Solo con token de proceso, como `/interno/usuarios/eventos`.
- La identificación **no** se agrega a `UsuarioResumen` ni al token (FR-013).

### Excepciones nuevas

| Excepción | Código | HTTP |
|---|---|---|
| `SinIdentificacion` | `USUARIOS_SIN_IDENTIFICACION` | 404 |

# Contrato: Docentes (F03-H04)

`contratos/docentes/openapi.yaml`, nuevo, antes del código (principio IV), según `backend/plantillas/README.md` («Agregar un contrato a un servicio»). El porqué está en [research.md](../research.md) §3 a §7.

## Esquemas

| Esquema | Campos |
|---|---|
| `NumeroDeIdentificacion` | `^[0-9A-Z]{1,13}$`, ejemplo `0109990010` (ficticio) |
| `TipoDeIdentificacion` | `CEDULA`, `PASAPORTE` |
| `TipoPersonal` | `TITULAR`, `OCASIONAL`, `INVITADO`, `HONORARIO` |
| `CategoriaDeEscalafon` | `AUXILIAR_1`, `AUXILIAR_2`, `AGREGADO_1`, `AGREGADO_2`, `AGREGADO_3`, `PRINCIPAL_1`, `PRINCIPAL_2`, `PRINCIPAL_3`, `NO_APLICA` |
| `Dedicacion` | `TC`, `MT`, `TP` |
| `CondicionEspecial` | `CUIDADOR_SUSTITUTO`, `MATERNIDAD_LACTANCIA`, `APOYO_SIES`, `NINGUNA` |
| `EstadoLaboral` | `ACTIVO`, `LICENCIA`, `INACTIVO` |
| `TipoDeTituloDeCuartoNivel` | `MAESTRIA`, `ESPECIALIZACION`, `DOCTORADO` |
| `CampoAmplio` | `codigo`, `nombre` |
| `TituloDeTercerNivel` | `titulo` (1–150), `registroSenescyt` (opcional, 1–30), `campoAmplio` (`^[0-9]{2}$`), `campoEspecifico` (`^[0-9]{3}$`) |
| `TituloDeCuartoNivel` | `titulo` (1–150), `tipo`, `campoEspecifico`, `anio` (1950–2100; el año actual lo revisa el dominio) |
| `Curso` | `nombre` (1–150), `campoEspecifico`, `horas` (1–2000), `anio` |
| `ExperienciaLaboral` | `cargo`, `institucion` (1–150), `campoEspecifico`, `anios` (> 0, ≤ 60) |
| `FichaAcademica` | `tercerNivel`, `cuartoNivel`, `cursos`, `experiencia` (listas de hasta 20), `asignaturasAfines` (hasta 20 códigos, únicos) |
| `DatosDeDocente` | `tipoIdentificacion`, `primerApellido`, `segundoApellido`, `primerNombre`, `segundoNombre`, `correoInstitucional`, `telefono`, `tipoPersonal`, `categoriaEscalafon`, `dedicacion`, `horasContratadas` (opcional), `fechaIngreso`, `inicioContrato`, `finContrato`, `condicionEspecial` (por omisión `NINGUNA`), `carreraBase`, `ficha` |
| `DocenteNuevo` | `identificacion` + `DatosDeDocente` |
| `Docente` | lo de `DocenteNuevo` con `nombreCompleto`, `nombreDeCarreraBase`, `horasContratadas` (las efectivas), `estado`; la ficha con el `CampoAmplio` (con nombre) de cada título y el nombre de cada asignatura afín |
| `ResumenDeDocente` | `identificacion`, `nombreCompleto`, `carreraBase`, `nombreDeCarreraBase`, `tipoPersonal`, `dedicacion`, `horasContratadas`, `condicionEspecial`, `estado` |
| `EstadoLaboralNuevo` | `estado` |
| `ReferenciasDeDocentes` | `carreras` (`codigo`, `nombre`, `siglas`), `asignaturas` (`codigo`, `nombre`, `carrera`), `camposAmplios` (`CampoAmplio`) |

## Operaciones (tag `Docentes`)

| Operación | Ruta | `x-mdd-permiso` | Respuestas |
|---|---|---|---|
| `buscarDocentes` | `GET /api/docentes/docentes?texto=&carrera=&estado=` | lectura | 200 (`ResumenDeDocente[]`) · 400 |
| `registrarDocente` | `POST /api/docentes/docentes` | escritura | 201 · 400 · 409 `DOCENTES_DOCENTE_REPETIDO` o `DOCENTES_CORREO_REPETIDO` |
| `consultarDocente` | `GET /api/docentes/docentes/{identificacion}` | lectura | 200 · 404 |
| `corregirDocente` | `PUT /api/docentes/docentes/{identificacion}` | escritura | 200 · 400 · 404 · 409 `DOCENTES_CORREO_REPETIDO` |
| `cambiarEstadoDocente` | `PUT /api/docentes/docentes/{identificacion}/estado` | escritura | 200 · 400 · 404 |
| `consultarReferenciasDeDocentes` | `GET /api/docentes/referencias` | lectura | 200 |

- **Lectura:** `[DOCENTES:LECTURA, DISTRIBUTIVO:LECTURA, CATALOGOS:LECTURA]`. **Escritura:** `[DOCENTES:ESCRITURA, CATALOGOS:ESCRITURA, DISTRIBUTIVO:ESCRITURA]` (D-70).
- Todas responden también 401 y 403 comunes (`../comun/seguridad.yaml`) y 500 (`../comun/errores.yaml`).
- Un 400 trae `COMUN_SOLICITUD_INVALIDA` con un detalle por campo; en las listas, con su posición (`ficha.cursos[1].horas`). Un valor fuera de un enum dice los valores permitidos.

## Códigos nuevos (`contratos/comun/errores.md`)

| Código | Estado | Título | Descripción | Cuándo |
|---|---|---|---|---|
| `DOCENTES_DOCENTE_REPETIDO` | 409 | Docente repetido | Ya existe un docente con esa identificación. Búscalo y corrígelo. | Registrar un docente con la identificación de otro (F03-H04). |
| `DOCENTES_CORREO_REPETIDO` | 409 | Correo repetido | Otro docente ya usa ese correo institucional. Revisa el correo. | Registrar o corregir un docente con el correo de otro, sin distinguir mayúsculas. |

## SPA

- `frontend/package.json`: `generar:api` agrega `contratos/docentes/openapi.yaml` → `src/api/generado/docentes.d.ts`.
- `frontend/src/api/cliente.ts`: `apiDocentes`.

# Contratos: Asignar actividades no lectivas

Permisos entre paréntesis (`x-mdd-permiso`).

## Generación

| Operación | Ruta | Permiso | Respuesta |
|---|---|---|---|
| `consultarActividades` | `GET /api/generacion/periodos/{periodo}/actividades` | `OFERTA:LECTURA`, `DISTRIBUTIVO:LECTURA` o `CATALOGOS:LECTURA` | `ActividadesDelPeriodo{creado, carreras[]}`. Cada carrera con sus docentes, sus actividades y su suma por componente |
| `consultarActividadesPosibles` | `GET /api/generacion/periodos/{periodo}/docentes/{docente}/actividades-posibles` | `OFERTA:ESCRITURA` | Cada actividad con `disponible`, `motivo` y `proyectos` |
| `asignarActividad` | `PUT /api/generacion/periodos/{periodo}/parciales/{carrera}/docentes/{docente}/actividades/{actividad}` | `OFERTA:ESCRITURA` | `{horas, proyecto?}` → `ActividadAsignada`, con `horasSuficientes` |
| `quitarActividad` | `DELETE` la misma ruta, con `?proyecto=` | `OFERTA:ESCRITURA` | 204 |

- `horas`: número mayor que 0, hasta 40 (aprobado por el autor), con dos decimales como mucho.
- `actividad`: `^14[0-3]-[a-p]$`.
- `proyecto`: `^[0-9A-Z-]{1,30}$`.

## Internas (ADR-023)

| Operación | Ruta | Respuesta |
|---|---|---|
| `consultarActividadesDelPeriodo`, Normativa | `GET /interno/normativa/periodos/{periodo}/actividades` | La versión y sus actividades. 404 sin versión |
| `consultarProyectosYCargosDelDocente`, Docentes | `GET /interno/docentes/{identificacion}/proyectos-y-cargos?periodo=` | `{proyectos[], cargos[]}`. 404 si el docente o el periodo no existen |

## Errores nuevos (`contratos/comun/errores.md`)

| Código | Estado | Cuándo |
|---|---|---|
| `GENERACION_ACTIVIDAD_FUERA_DEL_CATALOGO` | 404 | La actividad no está en el catálogo (RN-22). |
| `GENERACION_PROYECTO_NO_VALIDO` | 409 | El proyecto falta, sobra, no es del docente, no es del tipo o no es asignable (RN-25). |
| `GENERACION_CARGO_NO_VIGENTE` | 409 | El docente no tiene el cargo que exige la actividad. |
| `GENERACION_ACTIVIDAD_NO_ENCONTRADA` | 404 | Quitar una actividad que no existe. |

`quitarPropuesta` suma el 409 `GENERACION_PERIODO_CERRADO`, que corrige F06-H03.

Se reutilizan `GENERACION_SIN_DISTRIBUTIVO`, `GENERACION_PERIODO_CERRADO`, `GENERACION_PERIODO_SIN_ABRIR`, `GENERACION_DOCENTE_NO_ACTIVO`, `GENERACION_PARCIAL_NO_EDITABLE` y `GENERACION_PERIODO_NO_ENCONTRADO`.

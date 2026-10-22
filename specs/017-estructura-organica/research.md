# Research: Mantener la estructura orgánica (F03-H01)

Decisiones técnicas del plan. Cada sección tiene la decisión, por qué y las alternativas. Parte de lo que ya existe en `main` (`bec1497`):
- **Académico** (proceso `datos`): los periodos académicos (F02-H01) con su contrato, su registro de cambios (`academico.registro_cambio`), la pantalla «Periodos» y los avisos a Roles (F05-H03).
- **Permisos** (ADR-021): cada operación declara un solo `x-mdd-permiso`, y `VerificadorDePermisos` comprueba que sin él responde 403 y con él no.
- La carga 01 de los datos de prueba (24 unidades) y el catálogo de las 21 gestiones de POL 6 están fuera del repositorio, en `MDD_artefactos_desarrollo/requerimientos_mdd/cargas`.

## 1. Profundidad del modelo (principio II)

- **Decisión:** Académico es de soporte. El dominio tiene lo justo para probar las reglas sin dobles:
  - **`TipoDeUnidad`** (enum): `SEDE_MATRIZ`, `SEDE`, `EXTENSION`, `CAMPUS`, `GOBIERNO`, `UNIDAD_ACADEMICA`, `GESTION`, `COMISION`, con `esSede()`.
  - **`UnidadOrganica`** (record): código, nombre, tipo, padre, gestión autorizada, comisión permanente y si está activa. El constructor valida las reglas de su tipo (§3).
  - **`EstructuraOrganica`** (valor con todas las unidades): las reglas que miran a varias, como el padre que existe y está activo, el ciclo, la sede matriz única y los dependientes al inactivar.
- **Por qué:** el ciclo y la sede matriz única necesitan ver toda la estructura, que es chica (decenas de unidades); cargarla entera y validar en el dominio es más simple que consultas recursivas.
- **Alternativa:** validar el ciclo con una consulta recursiva en la base. Es más difícil de probar y deja la regla fuera del dominio.

## 2. El catálogo de las 21 gestiones autorizadas (aclaración 2, principio VI)

- **Decisión:** una tabla `gestion_autorizada` (código `G01`…`G21`, nombre), precargada por la migración con los 21 de POL 6, como la política 2024 en Normativa. Ninguna operación la cambia; si la norma cambia, se amplía con una migración nueva.
- La interpretación de RF-12 («sin permiso de administrador») queda escrita en la migración y en D-70: nadie registra una gestión fuera del catálogo, tampoco el Administrador (V-02, D-07, RN-10).
- **Alternativa:** un enum en el código: cambiar la norma exigiría cambiar el código (principio VI).

## 3. Las reglas de cada unidad

| Regla | Dónde |
|---|---|
| Código: mayúsculas, dígitos, `-` o `_`, hasta 20 (diccionario de la carga 01); único; no cambia | dominio y clave primaria |
| Nombre: no vacío, hasta 150 | dominio y `check` |
| La sede matriz no tiene padre; todas las demás sí; hay una sola | dominio (`EstructuraOrganica`) e índice único parcial |
| El padre existe y está activo; no se forma un ciclo | dominio (`EstructuraOrganica`) |
| Una gestión lleva un código de `gestion_autorizada`; ningún otro tipo lo lleva | dominio, `check` y clave foránea |
| Una comisión lleva «permanente» (S o N); ningún otro tipo lo lleva | dominio y `check` |
| El tipo no cambia | dominio (`corregida`) |

- **Inactivar con dependientes activos** (caso límite que la spec dejó al plan): **se rechaza** con 409 `ACADEMICO_UNIDAD_CON_DEPENDIENTES`. Así ninguna unidad activa queda debajo de una inactiva, y el padre activo de FR-003 se cumple siempre. Primero se inactivan las de abajo.
- **Dos unidades con la misma gestión autorizada** (caso límite): **se permite**. RN-10 cuenta docentes con cargo, no unidades, y nada en la ERS lo prohíbe; la carga de prueba usa una por gestión.
- **Reactivar** exige que el padre esté activo.

## 4. Quién lee: D-70 (aceptada)

- **Decisión (D-70, aceptada por el autor el 2026-10-21):** una operación puede declarar **varios permisos alternativos** en `x-mdd-permiso` (una lista); basta con tener uno. Las consultas de la estructura declaran `[DISTRIBUTIVO:LECTURA, CATALOGOS:LECTURA]`: las leen el Administrador y todos los que leen el distributivo (el Vicerrector, el Responsable de consolidación, el Rector y los demás de D-65). La escritura sigue siendo un solo permiso, `CATALOGOS:ESCRITURA`.
- `VerificadorDePermisos` se amplía: con varios permisos, sin ninguno responde 403 y con **cada uno** por separado no; con uno solo, como hoy. El `@PreAuthorize` usa `hasAnyAuthority`. La SPA acepta varios permisos en una entrada del menú y en `RutaConPermiso`.
- **Por qué:** la aclaración 1 pide que la lean quienes la usan, sin cambiar la matriz ni dar «Catálogos y datos maestros: lectura» a siete roles más (que también les daría los catálogos que lleguen después).
- **Alternativas:** dar `CATALOGOS:LECTURA` a los roles que leen el distributivo (cambia D-65 para siete roles); leer con `DISTRIBUTIVO:LECTURA` solamente (el Administrador también lo tiene, pero mezcla la funcionalidad).

## 5. Errores (ADR-016)

| Código | Estado | Cuándo |
|---|---|---|
| `ACADEMICO_UNIDAD_REPETIDA` | 409 | Registrar una unidad con el código de otra. |
| `ACADEMICO_SEDE_MATRIZ_EXISTENTE` | 409 | Registrar una segunda sede matriz. |
| `ACADEMICO_UNIDAD_CON_DEPENDIENTES` | 409 | Inactivar una unidad que tiene unidades activas debajo. |

- **Datos inválidos** (código, nombre, padre inexistente o inactivo, ciclo, gestión fuera del catálogo o en otro tipo, comisión sin «permanente»): 400 `COMUN_SOLICITUD_INVALIDA` con un detalle por campo (`codigo`, `nombre`, `padre`, `gestionAutorizada`, `comisionPermanente`), en español.
- **Unidad que no existe** (en la ruta): 404 común.

## 6. Registro (principio VII)

- `academico.registro_cambio` gana `UNIDAD_REGISTRADA`, `UNIDAD_CORREGIDA` (cada cambio con su anterior y su nuevo), `UNIDAD_INACTIVADA` y `UNIDAD_REACTIVADA`, con el usuario de la sesión. Una corrección sin cambios no se registra.

## 7. Contrato (`contratos/academico/openapi.yaml`)

| Operación | Ruta | Permiso | Respuestas |
|---|---|---|---|
| `listarGestionesAutorizadas` | `GET /api/academico/gestiones-autorizadas` | `[DISTRIBUTIVO:LECTURA, CATALOGOS:LECTURA]` | 200 |
| `listarUnidades` | `GET /api/academico/unidades` | igual | 200 (por código, con su padre) |
| `consultarUnidad` | `GET /api/academico/unidades/{codigo}` | igual | 200 · 404 |
| `registrarUnidad` | `POST /api/academico/unidades` | `CATALOGOS:ESCRITURA` | 201 · 400 · 409 |
| `corregirUnidad` | `PUT /api/academico/unidades/{codigo}` | `CATALOGOS:ESCRITURA` | 200 · 400 · 404 |
| `cambiarEstadoUnidad` | `PUT /api/academico/unidades/{codigo}/estado` | `CATALOGOS:ESCRITURA` | 200 · 400 · 404 · 409 |

- La lista viene plana, cada unidad con el código de su padre; la pantalla arma la jerarquía.
- `cambiarEstadoUnidad` recibe `{activa: true|false}`; pedir el estado que ya tiene no cambia nada.

## 8. Pantalla «Estructura orgánica»

- Entrada del menú con `[DISTRIBUTIVO:LECTURA, CATALOGOS:LECTURA]`.
- Una tabla en árbol: cada unidad con sangría bajo su padre, con código, nombre, tipo en español, estado y, si es gestión, su gestión autorizada; las comisiones con la marca «Informativa, sin horas».
- Con `CATALOGOS:ESCRITURA`: «Registrar unidad» (código, nombre, tipo, padre de una lista de las activas, gestión autorizada de la lista de 21 si es gestión, permanente si es comisión), «Corregir» e «Inactivar» o «Reactivar», que pide confirmación.

## 9. Glosario (principio III)

- «Sede», «Unidad», «Gestión autorizada» y «Comisión» ya están en §7.3. En el código: `UnidadOrganica` para «Unidad» (para no confundirla con otras «unidades»), `TipoDeUnidad`, `GestionAutorizada`.
- **Decisión, aprobada por el autor el 2026-10-21 y ya aplicada (copia `modelo_dominio.md.antes-F03-H01`):** en «Unidad» se agregó «Depende de otra unidad, salvo la sede matriz; se inactiva, nunca se borra».

## 10. Pruebas

- **Dominio, sin dobles:** cada regla de §3, con la estructura de la carga 01 como caso grande (las 24 unidades son válidas).
- **Aplicación, con Mockito:** registrar, corregir, inactivar y reactivar, con su registro; lo repetido no registra.
- **Adaptadores, con Testcontainers:** la migración (21 gestiones, `check`, índice de la sede matriz); el repositorio.
- **Contrato:** las 6 operaciones; `PermisosAcademicoTest` con los permisos alternativos; `VerificadorDePermisosTest` con una operación de dos permisos; los 3 códigos.
- **SPA:** Vitest (árbol, registrar, corregir, inactivar con confirmación, el 400 y el 409, solo lectura, menú con cualquiera de los dos permisos) y Playwright con axe.
- **Punta a punta:** `deploy/probar_estructura.sh`, nuevo: `admin.demo` registra las 24 unidades de la carga 01 de prueba (en el script, ficticias); una gestión `G99` y una comisión sin «permanente» dan 400; un ciclo da 400; inactivar una unidad con dependientes da 409; el Vicerrector la lee y recibe 403 al escribir; `docente.demo` recibe 403 al leer; el registro tiene las acciones. Limpia lo que crea.

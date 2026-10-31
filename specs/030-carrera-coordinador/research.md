# Research: Indicar la carrera del coordinador

## 1. Dónde vive el dato

- **Decisión:** en Roles, en una tabla propia `carrera_de_coordinador`, con el nombre de usuario como llave.
- **Por qué:** los alcances son de Roles (D-64), y la regla «solo con el rol Coordinador de carrera» necesita las asignaciones de rol.
- **Alternativas:**
  - En Académico: es el dueño de las carreras, pero no sabe qué roles tiene un usuario, y Usuarios tendría que consultarlo entre procesos para emitir el token.
  - Ligar el usuario con el docente y con su cargo: exige un dato nuevo en Usuarios y decisiones sobre cargos vencidos; la spec lo deja fuera.
- **Transacción:** `AsignarRolesServicio` no tenía: se le agrega, porque ahora cambia dos tablas.

## 2. Validar la carrera (cambia FR-004 de la spec aprobada)

- **Decisión (D-76):** Roles valida el formato del código, pero no consulta a Académico. La pantalla solo ofrece las carreras vigentes.
- **El problema:** Roles está en el proceso `identidad`, que no tiene `:plataforma:eventos`, ni clave de proceso, ni la URL de `datos`. Hoy solo `datos` y `distributivo` llaman a otros procesos.
- **Por qué es seguro:** el filtro falla cerrado. Un código que no es de ninguna carrera no coincide con nada, y el coordinador no ve datos. Solo se llega ahí llamando a la API a mano, con permiso de Administrador.
- **Alternativas:**
  - Dar a `identidad` una consulta a `datos`: suma `:plataforma:eventos` con su esquema de eventos, un secreto `MDD_CLAVE_PROCESO_IDENTIDAD` y correr `preparar.sh` en tu equipo, para validar un dato que la pantalla ya ofrece bien.
  - Que Académico avise a Roles de sus carreras por eventos, como con los periodos: las carreras que ya existen no tienen evento, así que haría falta una carga inicial.
- **Para más adelante:** si se agrega una gestión real de usuarios, esta validación se puede revisar.

## 3. La carrera en el token

- **Decisión:** `Autorizacion` suma `carrera` opcional, y `EmisorJwt` agrega el reclamo `carrera` solo si existe.
- **Por qué:** cada servicio decide con el token, sin llamadas. Y vale desde la próxima renovación, como pide D-62.
- **Principio IX:** es un código de carrera, no un dato personal.

## 4. Quién ve qué: `AlcanceDeCarrera`

- **Decisión:** dos piezas. `AutoridadesDelToken`, que corre en todos los procesos, agrega `ALCANCE_PROPIO_<FUNCIONALIDAD>:<TIPO>` por cada permiso con `:PROPIO`, y `CARRERA_<código>` si el token trae carrera. `AlcanceDeCarrera`, en `:plataforma:errores-nucleo`, es una función pura sobre los nombres de las autoridades. Los servicios la llaman con las autoridades de `Authentication`, y no conocen el token. Mira los tres permisos que dan acceso a la oferta (`OFERTA:LECTURA`, `DISTRIBUTIVO:LECTURA`, `CATALOGOS:LECTURA`):
  - alguno sin `:PROPIO`: ve todo;
  - todos con `:PROPIO`: solo su carrera, y sin carrera lanza `CarreraSinIndicar`;
  - ninguno: ve todo.
- **Por qué los tres, también en la sugerencia y la afinidad:** esas operaciones se abren con `DOCENTES:LECTURA`, que el coordinador tiene con alcance total. Mirar ese permiso dejaría al coordinador sin filtro. El filtro es sobre la oferta, no sobre los docentes.
- **Por qué «ninguno» ve todo:** Talento Humano lee la afinidad sin tener permisos sobre la oferta. Hoy la ve, y no es un coordinador (SC-003).
- **Quién queda filtrado:** hoy solo el Coordinador de carrera tiene esos permisos con `:PROPIO`. Lo comprobé en la matriz.
- **Alternativas:** hacer que cada servicio dependa de `:plataforma:seguridad`: mete el servidor de recursos en 12 módulos para leer un dato.

## 5. El filtro en Académico

- **Decisión:** `OfertaDelPeriodo.deCarrera(carrera)` devuelve la misma oferta con solo los paralelos de esa carrera. `consultarOferta` la usa, y todos los totales salen solos, también las horas a cubrir.
- **`consultarAsignaturaParalelo`:** con otra carrera, 403.
- **Las rutas internas no cambian de reglas:** las usa Generación con el token de proceso, y `AlcanceDeCarrera` solo se usa en `/api/`.
- **Las operaciones de escritura no cambian:** el coordinador no mantiene la oferta (spec 021).

## 6. El filtro en Generación

- **Decisión:** los puertos reciben `Optional<String> carrera`: vacío significa todas. Se calcula en el controlador.
- **Sugerencia:** si el paralelo es de otra carrera, `OtraCarrera`, que el controlador responde como 403, antes de pedir pesos y docentes.
- **Afinidad:** `consultarAsignaturasOfertadas` suma el parámetro opcional `carrera`, para que Académico filtre y nunca viaje de más (principio IX).

## 7. Los errores

- **`COMUN_CARRERA_SIN_INDICAR` (403):** común, porque lo responden Académico y Generación. Lo traduce un catálogo de `:plataforma:errores`, como `ErroresEntreProcesos`.
- **`ROLES_CARRERA_SIN_ROL` (409):** indicar una carrera a quien no tiene el rol.
- **Otra carrera:** `SEGURIDAD_ACCESO_NO_AUTORIZADO`, el 403 de siempre, sin revelar datos.
- **`ROLES_USUARIO_SIN_CARRERA` (404):** quitar la carrera a quien no tiene.
- **Formato inválido:** no hace falta código nuevo: es `COMUN_SOLICITUD_INVALIDA`.

## 8. El glosario y D-76

Antes del código, y con la aprobación del autor (principio III), en `sdd/modelo_dominio.md` §7:

- **«Carrera del coordinador», nuevo:** «La carrera con la que trabaja un usuario con el rol Coordinador de carrera. La indica el Administrador; hay una por coordinador y decide qué es “lo propio” de su alcance.» · `carrera` · Identidad y acceso · D-64, D-76.

## 9. La prueba punta a punta

- **Decisión:** `deploy/probar_carrera_coordinador.sh`, con los datos de prueba, que tienen 7 carreras.
- **Cubre:** el coordinador sin carrera; indicar la carrera con `admin.demo`; la oferta, la sugerencia y la afinidad de su carrera y de otra; el cambio tras la renovación; un usuario con alcance total; quitar el rol.
- **Los scripts existentes:** `probar_afinidad.sh` y `probar_sugerencia.sh` usan `coordinacion.demo` para leer toda la oferta. Pasan a `vicerrectorado.demo`, con alcance total. Los demás scripts no leen oferta con ese usuario.

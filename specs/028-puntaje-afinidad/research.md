# Research: Calcular el puntaje de afinidad con su desglose

Fase 0 del plan de F06-H01 (issue #46). Cada sección responde una pregunta del diseño con la decisión, el porqué y lo que se descartó.

## §1 · ¿Cómo pide Generación los datos a otro proceso? (ADR-023 propuesto)

Es la primera consulta por HTTP entre procesos: Generación corre en `distributivo`; Docentes, Académico y Normativa, en `datos`.

**Decisión**: rutas internas con el token de proceso. ADR-018 las fija solo para los eventos, y ADR-023 las extiende a las consultas:
- Cada dueño publica una consulta en `/interno/<servicio>/…`. El API Gateway no las enruta, y `plataforma:seguridad` solo admite ahí el rol `PROCESO`.
- Generación las llama con el token de proceso que ya existe (D-49), con un plazo de espera (ADR-009).
- Cada ruta devuelve solo lo que el cálculo necesita (principio IX), con un modelo propio, no el del dominio del dueño.

**Por qué no reenviar el token del usuario**: leer Normativa exige `PARAMETROS:LECTURA`, y un Coordinador de carrera no lo tiene. Con su token, el cálculo fallaría para el actor de la historia. Darle ese permiso cambiaría la matriz D-65 para resolver un detalle técnico.

**Descartado**:
- **Reenviar el token del usuario:** lo de arriba, y además ata la consulta a los permisos de cada dueño.
- **Copiar los datos a Generación por eventos:** evita la llamada, pero suma tablas de réplica y datos viejos para un cálculo que debe reflejar la ficha de ese momento (FR-017).
- **Mover el cálculo a `datos`:** contradice ADR-010.

**Requiere ADR-023**, que apruebas tú antes del código (§6.4 de la constitución). Hace tres cosas:
- extiende ADR-018 de eventos a consultas;
- precisa ADR-010, que dice que la ficha se pide «por la API de Docentes»: se pide por una ruta interna de esa API, no por la pública;
- fija los errores 502 y 504 que quedaron pendientes para la primera llamada entre procesos.

## §2 · Las rutas internas

**Decisión**: tres consultas, una por dueño, en este orden. Cada una corta si falla, sin puntajes parciales.

| Orden | Ruta | Dueño | Devuelve | 404 significa |
|---|---|---|---|---|
| 1 | `GET /interno/academico/periodos/{codigo}/asignaturas-ofertadas` | Académico | Cada asignatura `OFERTADA` del periodo, una vez: código, nombre, campo amplio y específico | El periodo no existe |
| 2 | `GET /interno/normativa/periodos/{periodo}/pesos-de-afinidad` | Normativa | La versión que rige el periodo y sus pesos | El periodo no tiene versión: no está abierto |
| 3 | `GET /interno/docentes/{identificacion}/formacion` | Docentes | Títulos, cursos y experiencia, con sus campos. Sin datos personales de contacto ni contrato, ni asignaturas afines | El docente no existe |

**Contrato (principio IV):** van en el `openapi.yaml` de cada dueño, con la etiqueta `Interno`, sin `x-mdd-permiso`: la seguridad es por ruta y rol, no por permiso de usuario. `/interno/usuarios/token-proceso`, en el contrato de Usuarios, ya está declarada así. `ValidadorDeContrato` las valida como a las demás.

## §3 · El cliente entre procesos

**Decisión**: `ConsultasEntreProcesos`, en `:plataforma:eventos`, junto a `ClienteDeEventos`, con los mismos `Clientes.http(plazo)` y `ProveedorDeTokenDeProceso`:
- Un GET con el token de proceso y, ante un 401, otro token y un reintento (como `ClienteDeEventos`).
- 404 se devuelve como un resultado vacío, para que cada adaptador diga qué significa.
- Se agota el plazo: `ProcesoSinRespuesta`. No se puede conectar, o responde 5xx: `ProcesoConError`.
- Usuarios no emite el token (`EntregaFallida` de `ProveedorDeTokenDeProceso`): también `ProcesoConError`, para que el usuario vea el mensaje del catálogo y no un error sin traducir.

El plazo es el de `mdd.eventos.plazo` (5 s). Las tres consultas van al mismo proceso y la primera que falla corta, así que el usuario espera como máximo un plazo si `datos` está caído (SC-004: menos de 10 s, con holgura).

**Descartado**: un módulo de plataforma nuevo. El token y el cliente ya están en `:plataforma:eventos`, y `:servicios:generacion` ya depende de él (principio X).

## §4 · Errores nuevos

| Código | Estado | Cuándo |
|---|---|---|
| `GENERACION_PERIODO_NO_ENCONTRADO` | 404 | El periodo no existe en Académico. |
| `GENERACION_PERIODO_SIN_ABRIR` | 409 | El periodo no tiene su versión de normativa. |
| `GENERACION_DOCENTE_NO_ENCONTRADO` | 404 | El docente no existe. |
| `COMUN_PROCESO_CON_ERROR` | 502 | Otro proceso falló o no se pudo conectar con él. |
| `COMUN_PROCESO_SIN_RESPUESTA` | 504 | Otro proceso no respondió dentro del plazo. |

Cada uno con sus 8 columnas (D-72) y en «tú» (D-74). Los dos `COMUN_` son los 502 y 504 que `docs/decisiones.md` dejó pendientes para la primera llamada entre procesos.

## §5 · El dominio de Generación (DDD táctico, principio II)

**Decisión**: en `generacion.dominio`:
- **`CalculadoraAfinidad`** (servicio de dominio, MD-02): función pura de la formación, la asignatura y los pesos.
- **`PuntajeAfinidad`** (objeto de valor): la asignatura, el total y su desglose.
- **`AporteDeAfinidad`** (objeto de valor): el tipo (título de tercer nivel, de cuarto nivel, experiencia o curso), el nombre, si coincidió el campo específico o el amplio, y el aporte.
- **`PesosAfinidad`, `FormacionDelDocente` y `AsignaturaOfertada`**: los valores que Generación recibe, propios de su contexto (capa anticorrupción); no comparte tipos con los dueños.

**Las reglas (FR-001 a FR-004):**
- **Título:** el campo específico igual da el peso «campo específico» de su nivel; si no, el campo amplio igual da el peso «campo amplio»; si no, nada. El campo amplio de un título de cuarto nivel son los dos primeros dígitos de su campo específico.
- **Experiencia y curso:** solo el campo específico igual.
- **Tope:** los cursos suman hasta `topeCursos`; el exceso se descarta y el puntaje marca `topeAplicado`. En el desglose, cada curso aparece con su aporte, y el último que entra lleva lo que cabe.

**Por qué propios y no reusar los de Normativa:** Generación es otro proceso y otro contexto, y principio V.

## §6 · La operación pública

**Decisión**: `consultarAfinidadDelDocente` (`GET /api/generacion/docentes/{identificacion}/afinidad?periodo=…`), con `[DOCENTES:LECTURA, DISTRIBUTIVO:LECTURA, CATALOGOS:LECTURA]`, los permisos de `consultarDocente` (FR-014, D-70). El API Gateway ya enruta `/api/generacion/**` a `distributivo`.

La identificación y el periodo usan las reglas de sus dueños (principio XIV, la lección de F02-H02): `^[0-9A-Z]{1,13}$` (`NumeroDeIdentificacion`, Docentes) y `^[A-Za-z0-9._-]{1,20}$` (Académico).

## §7 · La pantalla

**Decisión**: en el diálogo de la ficha de «Docentes», una sección «Afinidad por asignatura»:
- un selector de periodo con los de las referencias que la pantalla ya lee, porque quien ve Docentes no siempre puede leer los periodos de Académico;
- una tabla con la asignatura y el puntaje, del mayor al menor, y el desglose desplegable;
- los mensajes del catálogo si el periodo no está abierto;
- «Este periodo todavía no tiene oferta» si la lista viene vacía.

## §8 · Pruebas

- **Dominio:** `CalculadoraAfinidadTest`, sin dobles, con los 8 escenarios de la spec y pesos que no son los de por defecto.
- **Rutas internas:** una prueba de contrato en cada dueño y la de seguridad: un token de usuario recibe 403, y uno de proceso, 200.
- **Cliente:** `ConsultasEntreProcesosTest` con `ReceptorSimulado`: 200, 404, 401 con reintento, 500 y el plazo agotado.
- **Generación:** Mockito en la aplicación; `AfinidadContratoTest` y sus permisos.
- **Dos procesos reales:** `deploy/probar_afinidad.sh` en el perfil consolidado, incluida una versión con otros pesos y un proceso `datos` detenido.
- **SPA:** Vitest, y Playwright con axe.

## §9 · Rendimiento

Tres consultas y un cálculo en memoria por asignatura. La meta es menos de 3 s (SC-003, con holgura); se mide en el script.

## §10 · Glosario

`PuntajeAfinidad` y `CalculadoraAfinidad` están en el modelo táctico, y «Pesos de afinidad», «Afinidad», «Puntaje de afinidad» y «Ficha académica», en el glosario. No hay términos nuevos. El autor aprobó el 2026-10-30 ampliar la definición de «Puntaje de afinidad» con el periodo, el origen de los pesos y que no se guarda; ya está aplicada en `sdd/modelo_dominio.md` §7.

# Research: Seguir los plazos de cada etapa

F12-H03 (#58). Cada sección resuelve una pregunta del plan: la decisión, el porqué y lo que se descartó.

## §1. De dónde sale el plazo de cada etapa del proceso

- **Decision:** el plazo de cada etapa se **reconstruye** con `EtapasAlCrearse`, a partir de la configuración que regía cuando se creó el distributivo. No se guarda en la copia del proceso.
  - `EtapasAlCrearse` suma `plazos(periodo, creadoEn)`, que devuelve un `PlazosDeEtapas`: los días de cada etapa activa, sin PUBLICADO.
  - `EtapaDelProceso` y el `jsonb` `proceso_aprobacion.etapas` no cambian.
- **Rationale:**
  - La copia que guarda el proceso (`EtapaDelProceso`) **no trae el plazo**: `EtapaDelProceso.de(Etapa)` lo descarta desde F12-H02.
  - Las cargas son solo de inserción, así que la configuración de entonces se reconstruye exacta. Es el mismo camino que ya usa la cadena de firmas del consolidado (`cadenaDelConsolidado`, F13-H02, MD-05).
  - Guardarlo en la copia exigiría completar los procesos anteriores. Una migración SQL no puede leer el YAML guardado: tendría que poner los valores por defecto, como V4 hizo con `admiteDevolucion`. Eso daría un plazo falso para un periodo cargado con otros plazos (`probar_flujo.sh` carga uno con 20 días en BORRADOR_CARRERA).
  - Así se cumple «un cambio de configuración no afecta a un distributivo en curso» (RF-34): la reconstrucción usa la carga anterior a la creación, nunca una posterior.
- **Alternatives considered:**
  - **Sumar `plazoDias` a la copia y completar con una migración SQL con los valores por defecto:** imprecisa, por lo de arriba.
  - **Completar la copia en Java al arrancar:** son dos caminos para el mismo dato, y la vista quedaría sin plazos hasta que termine.
  - **Leer la configuración vigente hoy:** rompería RF-34, porque un periodo puede cargar otra mientras está en Planificación.

## §2. La llegada a la etapa y la fecha límite

- **Decision:** la regla va en el dominio.
  - `ProcesoAprobacion.llegadaALaEtapa()` es el momento de la última transición que **cambia de etapa** (`desde ≠ hacia`) y termina en la etapa actual. Si no hay ninguna, es `distributivoCreadoEn`: el distributivo empieza en BORRADOR_CARRERA.
  - `Plazo.de(llegada, dias)`, un objeto de valor nuevo (`Plazo` del glosario), calcula la fecha límite: la fecha de la llegada en `America/Guayaquil` (`ProcesoAprobacion.ZONA`) más los días corridos.
  - `Plazo.en(hoy)` da el estado: `A_TIEMPO` si `hoy < fechaLimite`, `VENCE_HOY` si son iguales y `VENCIDO` si `hoy > fechaLimite`. También da los días que faltan, que son negativos si venció.
- **Qué reinicia el plazo:** una devolución (vuelve a BORRADOR_CARRERA), un rechazo en firma (vuelve a la etapa activa anterior), una reforma (a EN_CONSOLIDACION) y cada paso de etapa.
  - El envío de un parcial que no es el último no cambia de etapa, así que no lo reinicia.
  - La `VERSION_NUEVA` tampoco: va de la etapa a la misma, y llega segundos después del rechazo, que ya lo reinició. Es el caso «versión nueva» de FR-002.
- **Rationale:** D-89 dice que el plazo son días corridos desde que el distributivo llega a la etapa, y que la etapa vence cuando termina el día límite en hora de Ecuador (spec, Edge Cases). La regla depende solo de las transiciones, que ya existen, sin datos nuevos.
- **Alternatives considered:**
  - **Contar en horas desde la llegada:** el plazo es en días y la revisión es diaria; las horas darían vencimientos a mitad del día.
  - **Excluir fines de semana y feriados:** D-89 dice días corridos.

## §3. La revisión diaria

- **Decision:** un caso de uso nuevo, `RevisarPlazosServicio` (puerto de entrada `RevisarPlazos`), y un adaptador de entrada, `RevisionDiariaDePlazos`, que lo llama de dos formas:
  - con `@Scheduled(cron = "${mdd.validacionocs.plazos.revision:0 0 7 * * *}", zone = "America/Guayaquil")`, cada día a las 07:00;
  - con `@EventListener(ApplicationReadyEvent.class)`, al arrancar (FR-004, escenario 9).

  La programación ya está habilitada en el proceso (`EventosAutoConfiguracion` tiene `@EnableScheduling`).
- **Qué hace**, para cada proceso que no está en PUBLICADO (`RepositorioProceso.enCurso()`, una consulta nueva):
  1. calcula el plazo de la etapa actual con el reloj (§1, §2); si no está `VENCIDO`, sigue con el siguiente;
  2. pregunta a Académico el estado del periodo; si está cerrado, sigue con el siguiente. Si Académico no responde, sigue como con el periodo abierto, con un `WARN`. *Cambio al implementar:* el plan decía dejarlo para la próxima revisión, pero sin `identidad` no hay token de proceso para preguntar a Académico, y el aviso habría esperado al día siguiente (lo mostró el paso 11 de `probar_avance.sh`; escenario 8 y SC-004);
  3. en una transacción y **con el proceso bloqueado** (el `bloquear` que ya usan las transiciones), vuelve a calcular el plazo. Si la etapa cambió entre medio, no hace nada;
  4. inserta el vencimiento en `plazo_vencido` con `on conflict do nothing` (§5);
  5. solo si se insertó, en la misma transacción, publica la notificación (§4) y la entrada de la bitácora `PLAZO_VENCIDO`.
- Un fallo con un proceso no detiene la revisión de los demás: se registra y se sigue.
- **Rationale:**
  - El principio X no admite colas ni infraestructura nueva, y el proceso ya programa el reenvío de eventos.
  - La clave única hace idempotente la revisión, aunque corra varias veces el mismo día, al arrancar y por el `cron` (SC-002).
  - El bloqueo evita avisar una etapa que se acaba de dejar.
  - Con el reloj inyectado (`Clock`), las pruebas fijan «hoy».
- **Alternatives considered:**
  - **Revisar al consultar el estado del flujo:** el aviso dependería de que alguien abra la pantalla.
  - **Un `fixedDelay` cada hora:** hace más trabajo para un plazo en días. El arranque ya cubre los días en que el proceso estuvo detenido.
  - **ShedLock u otro bloqueo distribuido:** hay un solo proceso `distributivo` (ADR-006), y la clave única basta.

## §4. A quién se avisa y qué dice

- **Decision:** el dominio decide los destinos, con `ProcesoAprobacion.destinosDelVencimiento()`:
  - **El rol de la etapa** en la copia de la configuración:
    - en BORRADOR_CARRERA, un destino por carrera con el parcial `PENDIENTE` o `DEVUELTO`: `COORDINADOR_CARRERA` con su carrera, como en la devolución;
    - en las demás etapas, el rol sin carrera; RESPONSABLE_CONSOLIDACION se resuelve como en D-108: quienes tienen el rol y el designado del periodo.
  - **El Vicerrector:** el rol de la etapa EN_REVISION_VRA de la copia, que por defecto es `VICERRECTOR`. Es la misma regla que eligió D-113 para «el Vicerrector académico» en la reforma (principio VI).
  - `distinct()` y la clave de Notificaciones dejan una notificación por usuario. Por ejemplo, en EN_REVISION_VRA el rol de la etapa y el Vicerrector son el mismo rol.
  - El `autor` del evento es `sistema` (`ProcesoAprobacion.SISTEMA`): no hay nadie a quien excluir.
- **El mensaje** lo arma `MensajesDePlazo`, en el dominio, como `MensajesDeTransicion`:
  - título: «Venció el plazo de <nombre de la etapa>»;
  - detalle: «El distributivo de <periodo> está en <nombre de la etapa> desde el <fecha de llegada>. Su plazo de <N> días venció el <fecha límite>; lleva <D> días de atraso.»;
  - enlace: el de la etapa (`MensajesDeTransicion.enlaceDe`) para el rol de la etapa, y `/consolidado` para el Vicerrector, que es la pantalla donde ve la tarjeta.
- **El camino:** un puerto de salida nuevo, `AvisoDePlazoVencido`. Su adaptador, `PublicadorDeNotificaciones`, publica el mismo evento `validacionocs.NotificacionesSolicitadas` en la transacción (D-108, MD-03): se entrega después de confirmarla y llega aunque `identidad` esté caído (SC-004).
  - El contrato de Notificaciones no cambia: `autor`, `momento` y `mensajes` ya admiten este contenido.
- **Rationale:** el issue dice «el rol de la etapa y el Vicerrector». En BORRADOR_CARRERA, avisar a las carreras que ya enviaron su parcial sería ruido: el atraso es de las demás.
- **Alternatives considered:**
  - **Un tipo de evento nuevo para Notificaciones:** D-108 hizo genérico el contenido justamente para no tener que hacerlo.
  - **El rol `VICERRECTOR` fijo en el código:** contradice el principio VI y lo que se eligió en D-113.
  - **Avisar también al firmante pendiente en CONGELADO_EN_FIRMA:** F16-H02 ya le notifica su turno, y la spec no lo pide.

## §5. El registro del vencimiento

- **Decision:** una tabla nueva, solo de inserción, `validacionocs.plazo_vencido`, en la migración V9. Lleva:
  - el periodo, la etapa, la llegada, los días del plazo, la fecha límite y cuándo se detectó;
  - una clave única `(periodo, etapa, llegada)`.

  En la bitácora va `CambioDeFlujo.Accion.PLAZO_VENCIDO`, por `sistema`: el detalle `{periodo, etapa}` y el `nuevo` `{fechaLimite, diasDeAtraso}`.
- **Rationale:**
  - La tabla es la que impide repetir el aviso: hay uno por llegada, según la aclaración del 2026-11-20.
  - La bitácora cumple el principio VII y FR-008, como las demás acciones del sistema (la cadena completa).
- **Alternatives considered:**
  - **Una transición `PLAZO_VENCIDO`:** no cambia de etapa, y obligaría a ajustar cada regla que recorre las transiciones (la llegada, `rechazoPendiente`, la reforma en curso).
  - **Solo la bitácora:** la bitácora vive en otro proceso, así que no puede servir de clave de idempotencia local.

## §6. El plazo en la vista del flujo

- **Decision:** `EstadoDelFlujo` suma dos campos opcionales:
  - `plazo`, la etapa actual: la llegada, los días, la fecha límite, los días que faltan (negativos si venció) y el estado `A_TIEMPO`, `VENCE_HOY` o `VENCIDO`. No va en PUBLICADO.
  - `etapas`, las etapas activas de la copia, en orden: el tipo, el nombre, los días (ausentes en PUBLICADO) y si es la actual.

  Los calcula `FlujoDelDistributivoServicio` con `EtapasAlCrearse` y el mismo reloj que la revisión: la tarjeta y el aviso dicen la misma fecha. Los ve todo el que lee el estado del flujo; la matriz D-16 no cambia.
- **Rationale:** la tarjeta del estado del flujo ya está en «Consolidado del periodo», «Oferta y propuesta de docentes» y «Resolución del OCS» (FR-009). Calcular «hoy» en el servidor evita que el reloj del navegador o su zona horaria cambien el estado.
- **Alternatives considered:**
  - **Calcular en la SPA con la fecha límite:** el navegador podría estar en otra zona horaria y decir «vence hoy» cuando ya venció.
  - **Una operación aparte `consultarPlazos`:** sería una llamada más para la misma tarjeta.

## §7. La advertencia de RN-23

- **Decision:** se calcula al cargar la configuración, después de validarla y antes de guardarla, con `AdvertenciaDeRemision.de(hoy, configuracion, inicio, dias)`, en el dominio.
  - **El último plazo** es la fecha de la carga en Ecuador más la suma de los plazos de las etapas activas sin PUBLICADO, es decir, hasta APROBADO, según la aclaración del 2026-11-20.
  - **La fecha que pide RN-23** es el inicio del periodo menos los días de la normativa.
  - Si el último plazo cae **después** de esa fecha, la advertencia es `RN23_MARGEN_INSUFICIENTE`, con el último plazo, la fecha de RN-23, el inicio y cuántos días sobran, que son negativos. Si cae en esa fecha o antes, no hay advertencia (SC-006).
  - Si el periodo **no tiene versión de normativa** (en Planificación puede no tenerla todavía), la advertencia es `RN23_SIN_NORMATIVA`: no se puede comprobar, y se dice por qué.
- **Los datos:**
  - **El inicio del periodo:** el esquema interno `EstadoDelPeriodo` de Académico suma `inicio` (date), y la carga ya hace esa consulta para saber si el periodo está en Planificación (`DatosDelPeriodo.estado`). No suma ninguna llamada.
  - **Los días de RN-23:** Normativa suma la regla `RN-23` (sin severidad, como RN-24) con el parámetro `dias_antes_del_inicio = 30`. Lo agrega la migración V7 de Normativa a cada versión, con su `VERSION_EDITADA`, como D-86 hizo con RN-05.
  - Una ruta interna nueva, `GET /interno/normativa/periodos/{periodo}/remision`, devuelve `{version, diasAntesDelInicio}`, o 404 si el periodo no tiene versión. La lee el puerto `DatosDelPeriodo.diasDeRemision(periodo)` por la ruta interna del proceso `datos` (ADR-023), donde vive Normativa.
  - Si Normativa no responde, la carga responde el 502 o el 504 común y no carga nada, como cuando falla Académico (FR-024 de F12-H01).
- **Dónde queda:** la columna `advertencia jsonb` de `configuracion_flujo`, nula si no hay advertencia, en la migración V9. La consulta la devuelve tal como se guardó con esa carga (FR-014), y la configuración por defecto no la tiene. El `nuevo` de `CONFIGURACION_FLUJO_CARGADA` suma el código de la advertencia, si la hay.
- **No es un error del catálogo:** la carga responde 200 con `advertencias`, como las advertencias de las cargas de datos y de los cargos (`contratos/cargas`, `contratos/docentes`). Cada advertencia lleva un `codigo` estable del contrato y un texto que dice qué hacer. Por eso la spec cambia su FR-016: los errores nuevos van al catálogo, y las advertencias, al contrato.
- **Rationale:** el principio VI pide que las cifras de la política sean parámetros versionados por periodo, y RN-23 es una cifra de POL 5.2. Normativa ya guarda las cifras de cada versión y las expone por rutas internas (reglas del panel, proporción, pesos).
- **Alternatives considered:**
  - **La cifra en el YAML del flujo:** el YAML es de la configuración del flujo, no de la política. Habría dos lugares para cifras de la normativa.
  - **Los 30 días fijos en el código:** contradice el principio VI.
  - **Recalcularla en cada consulta:** cambiaría con los días aunque la carga sea la misma; FR-014 pide la de la carga.
  - **Rechazar la carga:** el issue dice «advertencia», y la spec dice que no rechaza (FR-013).

## §8. La pantalla

- **Decision:**
  - **La tarjeta del estado del flujo** (`EstadoDelFlujo.tsx`) suma un bloque «Plazo de la etapa»: «En <etapa> desde el <fecha>. Fecha límite: <fecha>», con «Faltan N días», «Vence hoy» o «Vencido hace N días».
    - El estado vencido lleva un ícono y el texto, no solo el color (FR-011).
    - Debajo va la lista ordenada de las etapas activas con sus días, y la actual se marca con `aria-current="step"`.
  - **«Configuración del flujo»** (`Flujo.tsx`) muestra la advertencia después de cargar, con `role="status"`, y en «Qué rige»: el último plazo, la fecha de RN-23, los días y qué hacer.
- **Pruebas:** Vitest de los tres estados del plazo, la lista y la advertencia; Playwright en `consolidado.spec.ts` y `flujo.spec.ts`, con axe-core.
- **Alternatives considered:** una pantalla «Plazos»: la spec la deja fuera, y la vista del estado del periodo es de F02-H03.

## §9. La prueba de punta a punta y la fecha simulada

- **Decision:** no hay una opción de «fecha simulada» en el sistema.
  - **El vencimiento:** `probar_avance.sh` termina con el distributivo en EN_CONSOLIDACION. Ahí suma estos pasos:
    1. detiene `identidad`;
    2. **corre hacia atrás 6 días** las transiciones del periodo de prueba, con `psql` como superusuario. Solo las de ese periodo, y todas juntas, para no cambiar su orden;
    3. reinicia `distributivo`: la revisión del arranque detecta el vencimiento;
    4. levanta `identidad` y espera la notificación;
    5. comprueba una sola notificación para `vicerrectorado.demo` y una para quien consolida; la entrada `PLAZO_VENCIDO` de la bitácora; la fila de `plazo_vencido`; y que la tarjeta dice `VENCIDO` con 1 día de atraso;
    6. comprueba que el distributivo se sigue enviando a revisión como siempre, si el script ya lo hace, o al menos que la acción sigue en `acciones`.
  - **RN-23:** `probar_flujo.sh` carga la plantilla en un periodo que empieza dentro de 60 días (advertencia) y en otro dentro de 120 (sin advertencia).
  - Las pruebas de integración y de aplicación usan un `Clock` fijo, y prueban la idempotencia corriendo la revisión tres veces.
- **Rationale:**
  - Una opción de fecha simulada en el sistema sería código que en producción no debe existir. Correr hacia atrás los datos de prueba, en cambio, solo toca la base de la CI o del perfil local, como ya hacen los scripts con `psql`.
  - El reinicio prueba además la revisión al arrancar (FR-004) y la caída de `identidad` (SC-004) en el mismo recorrido.
- **Alternatives considered:**
  - **Un endpoint de demo para correr la revisión con una fecha:** sería una superficie de ataque y código muerto en producción.
  - **Un desfase del reloj por variable de entorno:** obligaría a recrear el contenedor con otra configuración, y la tarjeta también se desfasaría.
  - **Esperar días reales:** no.
- **Supuesto de la spec que cambia:** «el entorno de demostración permite simular la fecha» pasa a «la prueba de punta a punta corre hacia atrás las transiciones de sus datos de prueba». Se actualiza en la spec.

## §10. Lo que no cambia

- La matriz D-16, los roles y el contrato de Notificaciones.
- Firma y Generación no se tocan.
- Ninguna transición nueva ni regla del agregado nueva para avanzar: el plazo no bloquea nada (FR-003). Se prueba que con la etapa vencida el avance, la devolución y el envío a revisión siguen igual (SC-003).
- La decisión **D-114** recoge §1 a §9 antes de implementar.

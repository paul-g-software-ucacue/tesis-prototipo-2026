# Research: Vista previa y confirmación de la carga

Fase 0 del plan de F01-H04 (issue #30). Cada punto dice la decisión, por qué y qué se descartó.

## 1. Un lenguaje publicado de las cargas, en un módulo de plataforma (ADR-022 propuesto)

- **Decisión:** un módulo nuevo, `:plataforma:cargas`, con el lenguaje común entre Cargas y los tres dueños:
  - `LoteDeCarga`: la carga, el hash y las filas, cada una con su número de fila del archivo y sus valores por campo, ya sin espacios;
  - `TraductorDeCarga<T>`, que cada dueño implementa por carga:
    - la clave de una fila;
    - la entidad de una fila;
    - las entidades que ya existen, por clave;
    - los campos que se comparan, con su texto canónico;
    - registrar y corregir;
  - `MotorDelLote`, que arma la vista previa con los nuevos, los que cambian (con sus campos), las bajas y los iguales, calcula su huella y aplica el lote dentro de la transacción del dueño;
  - `AplicadorDeCargas`: lo que Cargas ve, con las cargas que atiende, `previsualizar(lote)` y `aplicar(lote, huella, quien)`;
  - `RechazoDelLote`: la fila, el campo, el motivo y el código del dueño.
- **Cómo se conecta:** cada dueño publica un bean `AplicadorDeCargas`. Cargas recibe la lista y elige el de cada carga: Académico atiende las cargas 01 a 05 y 10, Docentes la 06, la 08 y la 09, y Normativa la 07. Al arrancar se comprueba que cada una de las 10 cargas tiene exactamente un dueño (MD-13).
- **Por qué:**
  - La comparación, la huella y el «todo o nada» son iguales en las 10 cargas: escritos una vez, se prueban una vez.
  - Cada dueño escribe solo la traducción de sus columnas (FR-012) y sigue aplicando sus reglas, porque registrar y corregir llaman a sus casos de uso de siempre.
  - Es el camino de `:plataforma:eventos`, que ya comparten Académico y Generación.
- **Descartado:**
  - **Un puerto propio en cada dueño** (`ec.mdd.academico.puertos.entrada.AplicarCargas`, y así en los demás). Repite los mismos registros tres veces, y Cargas tendría tres adaptadores casi iguales.
  - **El puerto en Cargas** (`puertos.salida`), con los dueños implementándolo. Los dueños dependerían de Cargas, y `ModulosTest` solo permite usar los `puertos.entrada` de otro servicio.
- **Registro:** cambia la arquitectura de más de una feature, así que va como ADR-022, con D-74 en el índice. Lo apruebas tú antes de implementar.

## 2. La vista previa viaja con la validación; la confirmación vuelve a enviar el archivo

- **Decisión:**
  - **`validarArchivo`:** agrega `vistaPrevia` a `LecturaDeArchivo` cuando las filas no tienen errores: los totales (`nuevos`, `cambian`, `bajas`, `iguales`), los primeros 100 de cada grupo y la `huella`. Con errores, solo trae `filasConErrores`.
  - **`confirmarCarga`** (nueva): `POST …/archivo/confirmacion`, con el mismo `multipart` más la `huella`. El servidor valida otra vez (F01-H02 y F01-H03), comprueba el hash en el historial, y el dueño recalcula la vista previa dentro de su transacción y la aplica solo si su huella es la misma.
  - **`listarHistorial`** (nueva): `GET …/plantillas/{codigo}/historial`.
- **Por qué:**
  - Sin estado entre pasos, como el informe de F01-H03: el servidor no guarda el archivo.
  - La huella asegura que se aplica lo que se revisó (FR-006).
  - Con errores no hay vista previa útil, porque una fila con errores no se puede traducir a la entidad.
- **Descartado:**
  - **Guardar la vista previa en el servidor con un identificador:** obliga a guardar el contenido del archivo con datos personales y a vencerlo.
  - **Una operación de vista previa aparte:** sería otro envío del mismo archivo sin ganar nada.

## 3. La huella y la idempotencia

- **Hash del archivo:** SHA-256 de sus bytes, en hexadecimal. Es la clave de idempotencia de MD-13. En el historial, un hash con el resultado `APLICADA` en la misma carga hace que una nueva confirmación responda `REPETIDA` sin llamar al dueño (FR-007). El intento también queda registrado como `REPETIDA`.
- **Huella de la vista previa:** SHA-256 de su forma canónica: cada registro con su tipo y sus campos cambiados, en orden de clave. La calcula el dueño en `previsualizar` y de nuevo en `aplicar`, después de bloquear sus tablas. Si no coincide, el dueño lanza `VistaPreviaDesactualizada` y no aplica nada.
- **Por qué no basta con el hash:** el mismo archivo puede dar otra vista previa si alguien registró algo a mano entre los dos pasos.
- **La aplicación se basa en las diferencias:** un registro que ya está igual no se toca. Aunque el historial fallara después de aplicar, confirmar otra vez no duplica nada, porque su vista previa tendría 0 nuevos.

## 4. El motor del lote y las transacciones

- **Una transacción por lote:** `aplicar` corre dentro de `Transaccion.ejecutar` del dueño. Los casos de uso que llama, como `registrarCarrera` o `corregir`, abren su propia `Transaccion.ejecutar`, que con `TransactionTemplate` y propagación `REQUIRED` se une a la del lote. La primera excepción deshace todo (FR-004).
- **Bloqueo:** antes de recalcular la huella, el dueño bloquea las tablas de la carga con su `LOCK TABLE … SHARE ROW EXCLUSIVE MODE` de siempre, así que dos confirmaciones a la vez no se mezclan.
- **Rechazos:** cada excepción de dominio de un caso de uso se traduce a `RechazoDelLote` con la fila que se estaba aplicando, el campo (si la excepción lo trae, como `DatoInvalido`) y el motivo de su catálogo. Cargas lo responde como `CARGAS_CARGA_RECHAZADA` (409), con un detalle `fila N · campo` (FR-005).
- **Orden dentro del archivo:** se aplica en el orden del archivo, salvo dos casos:
  - en la carga 01, los padres antes que los hijos, en orden topológico;
  - en la carga 04, las mallas (`codigo_version_malla`) antes que sus asignaturas.
- **Registro de cambios:** los casos de uso escriben en el registro de su dueño como siempre (principio VII), con el usuario que confirma. El hash queda en el historial de Cargas.

## 5. Qué compara cada carga, y qué columnas no guarda todavía su dueño

Cada traductor declara sus campos comparables. Los que su dueño no guarda se validan (F01-H03), pero no cuentan como cambio. Los traductores nacen del código de prueba que ya lee estos archivos: `DatosMaestrosDePrueba` de Académico y `DocentesDePrueba` y `CargasDePrueba` de Docentes, unas 660 líneas. Pasan a producción y las pruebas los reusan.

| Carga | Dueño · caso de uso | Clave | No se guardan (no cuentan como cambio) |
|---|---|---|---|
| 01 estructura | Académico · `MantenerEstructura` | `codigo_unidad` | — (`estado` → `cambiarEstado`) |
| 02 carreras | Académico · `MantenerCarreras` | `codigo_carrera` | — (`estado` → `cambiarEstadoCarrera`) |
| 03 ciclos | Académico · `RegistrarPeriodo`, `CorregirPeriodo` | `codigo_periodo` | `semanas`, `fecha_limite_envio_senescyt`, `resolucion_aprobacion`, `estado` (el estado del periodo en el MDD es otro: Planificación…Cerrado) |
| 04 asignaturas | Académico · `MantenerCarreras` (malla y asignatura) | `codigo_asignatura` | — |
| 05 oferta | Académico · `MantenerOferta` | periodo + asignatura + paralelo | `franjas_horarias` (RF-60, una historia futura) |
| 06 docentes | Docentes · `MantenerDocentes` | `numero_identificacion` | las que la ficha de F03-H04 no guarda; las enumera la tarea, tomadas de `DocentesDePrueba` (`estado` → `cambiarEstado`) |
| 07 política | Normativa · `CrearVersion`, `EditarVersion` | versión + tipo de IST + perfil | `tipo_regla`, `referencia_normativa`, `observacion` (la edición no los cambia, FR-002 de F04-H03) |
| 08 cargos | Docentes · `MantenerCargos` | identificación + cargo + fecha de designación | — |
| 09 proyectos | Docentes · `MantenerProyectos` | `codigo_proyecto` | — |
| 10 matriculados | Académico · nuevo (§6) | periodo + carrera | — |

- **Bajas:** las entidades del dueño cuya clave no viene en el archivo. En las cargas por periodo (05 y 10), solo en los periodos que trae el archivo. En la 07, solo en las versiones que trae.
- **Igualdad:** por el texto canónico de cada campo, sin espacios en los extremos. Los decimales se comparan por valor (`2.50` es igual a `2.5`) y las listas, en su orden.

## 6. Los matriculados en Académico

- **Decisión:**
  - `Matricula(periodo, carrera, total, fechaCorte, fuente)`, un valor del dominio de Académico, sin datos personales;
  - la migración `V6__matriculados.sql`, con la tabla `matricula` y la clave periodo + carrera, que referencian `periodo_academico` y `carrera`;
  - las acciones `MATRICULA_REGISTRADA` y `MATRICULA_CORREGIDA` en su registro de cambios;
  - `RepositorioMatriculas` y el traductor de la carga 10.
- **Sin pantalla ni operación propia en el contrato:** los matriculados entran por la carga 10. El tipo de IST por matriculados es F23 (Should), fuera de la ola.
- **Por qué:** el glosario pone «Matriculados» en Datos maestros (Académico), y RF-20 lo necesitará.

## 7. La carga 07 en Normativa

- **Una versión que no existe:** `CrearVersion` copia la última versión, para conservar sus reglas y sus parámetros, con el código, el origen (`referencia_normativa` de su primera fila) y `vigenteDesde` (`fecha_vigencia_desde`). Después `EditarVersion` le pone las filas del archivo.
- **Una versión que existe:** `EditarVersion` con sus filas, si no está asociada a un periodo. Si lo está, lanza `VersionEnUso` y la carga se rechaza con ese motivo (principio VII).
- **La tabla completa:** la edición de F04-H03 es completa, así que el archivo trae todas las filas de cada versión que nombra. Una fila de más o de menos la rechaza Normativa con su motivo.
- **Las horas son enteros en Normativa:** un `2.5` en una columna de horas lo rechaza el dueño al confirmar, aunque el diccionario las declare decimales.
- **Lo que verás con la carga 07 de prueba:** la fila 22 completada con 0 y 0 (F01-H03) es distinta de la versión 1, que tiene esas dos celdas en nulo. La vista previa mostrará 1 que cambia, con esos dos campos. Confirmarla la cambia si la versión 1 no está asociada a ningún periodo; si lo está, se rechaza con «versión en uso». El equipo de demostración tiene 2 versiones y 2 periodos asociados: la prueba allí dirá cuál de los dos casos es.

## 8. El historial en el esquema de Cargas

- **Decisión:** la migración `V2__historial_de_cargas.sql` crea `cargas.historial_carga` con:
  - `id`, `carga`, `usuario`, `momento`, `archivo` (el nombre, sin ruta) y `hash`;
  - `resultado`: `APLICADA`, `RECHAZADA` o `REPETIDA`;
  - `nuevos`, `cambiados` y `bajas_sin_aplicar`;
  - `motivo`: el código del rechazo, si lo hubo.

  Nada del contenido del archivo (principio IX). `ConsultarHistorial` lo lee por carga, de lo más reciente a lo más antiguo.
- **Cuándo se escribe:** después de la respuesta del dueño, en la transacción de Cargas. Si falla después de aplicar, el error queda en el log (sin valores) y el registro de cambios del dueño ya tiene cada cambio, con el usuario. Una nueva confirmación da 0 nuevos (§3), así que no duplica nada.

## 9. Errores nuevos del catálogo (D-72)

Los tres son 409, de clasificación «Negocio», con sus 8 columnas en `contratos/comun/errores.md`:

| Código | Cuándo | Detalle |
|---|---|---|
| `CARGAS_CARGA_CON_ERRORES` | Se confirma un archivo con errores en sus filas (FR-003). | Cuántos; el informe se pide con `descargarInforme`. |
| `CARGAS_VISTA_PREVIA_DESACTUALIZADA` | La huella cambió entre la vista previa y la confirmación (FR-006). | — |
| `CARGAS_CARGA_RECHAZADA` | Una regla del dueño falla al aplicar (FR-005). | `fila N · campo`, con el motivo del dueño y su código. |

## 10. Los mensajes, en «tú»

- **Decisión:** el catálogo trata al usuario de «tú» («Guárdalo como CSV UTF-8»), pero los motivos y la pantalla de F01-H03 quedaron en «usted» («Escriba», «Corríjalo y vuelva a subirlo»). Esta historia los pasa a «tú», para que sean consistentes (principio XIII). FR-014 cambia el de las referencias de todos modos: «No existe la carrera en el sistema: regístrala o confirma antes su carga.».
- **Descartado:** dejarlos así, que da dos tratamientos en la misma pantalla.

## 11. Permisos

- `confirmarCarga` exige `CARGAS:ESCRITURA`, como subir (D-71).
- `listarHistorial` exige `[CARGAS:LECTURA, DISTRIBUTIVO:LECTURA]`, como las plantillas (D-70).
- Los casos de uso de los dueños no revisan permisos por su cuenta (lo hace cada controlador), así que el lote los usa con el usuario que confirma.

## 12. Tiempo

- **Vista previa:** lee las entidades del dueño una vez y compara en memoria.
- **Aplicar:** llama a un caso de uso por registro, y cada uno vuelve a leer y bloquear, así que es lo más caro.
- **Metas:** 5 MB de docentes (unos 11 600) en menos de 60 s (SC-004), con la meta del plan en 20 s. El script lo mide. Si no se cumple, la tarea de optimización está en el plan: un camino por lotes en el repositorio del dueño, con las mismas reglas.

## 13. La pantalla

- **Al subir un archivo sin errores**, «Plantillas de carga» muestra:
  - la vista previa, con los cuatro totales;
  - tres pestañas («Nuevos», «Cambian», «Se dan de baja»), con la clave y, en «Cambian», los campos con su valor de ahora y el del archivo;
  - un aviso: «Las bajas no se aplican: hazlas a mano en su pantalla.»;
  - «Confirmar» y «Cancelar».
- **Con errores:** el informe de F01-H03 y «Confirmar» deshabilitado, con su motivo.
- **Después de confirmar:** el resultado (aplicada o repetida, con sus totales) o el rechazo con su fila.
- **Historial:** «Historial» en cada carga abre su lista.

## 14. Pruebas que demuestran SC-001 y SC-002

- **`ConfirmarLasDiezCargasIntegrationTest`**, en `apps/datos`, con Testcontainers y el contexto del proceso `datos`:
  - confirma las 10 cargas de prueba en su orden sobre una base vacía (SC-001);
  - vuelve a subirlas y todas dan 0 nuevos, 0 que cambian y 0 bajas (SC-002);
  - confirma otra vez una de ellas: `REPETIDA`.
- **`probar_confirmar_carga.sh`**, en el perfil: hace lo mismo con las 10 cargas de prueba.
  - Sus códigos y cédulas son fijos, así que primero comprueba que ninguno existe en la base. Si alguno existe, falla sin tocar nada, como `probar_docentes.sh`.
  - Al terminar borra todo lo que creó, también los periodos copiados en Roles y los eventos.

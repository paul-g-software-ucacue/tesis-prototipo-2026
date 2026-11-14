# Feature Specification: Validar que el PDF subido es el mismo documento con una firma íntegra

**Feature Branch**: `048-validar-firma`

**Created**: 2026-11-14

**Status**: Draft

**Input**: User description: "F14-H02 · Validar que el PDF subido es el mismo documento con una firma íntegra (#63). Usar el texto y los criterios del issue. Respetar .specify/memory/constitution.md, docs/decisiones.md (D-23, D-46, ADR-017, ADR-007, D-101, D-102, D-88) y el diseño de firmas (Diseno_firmas_cadena_aprobacion.md §5.1, §6 y §9: POST /documentos/{id}/firmas, el informe de validación). Decisiones del autor: (1) si el PDF subido pasa las comprobaciones de esta historia (es el mismo documento: empieza exactamente con los bytes del archivo vigente y tiene una sola firma nueva; la firma nueva es íntegra sobre su ByteRange; el certificado llega a una raíz de la lista de confianza), el sistema registra la firma: guarda el PDF firmado por su SHA-256 como nuevo archivo vigente (sin sobrescribir nada), registra la firma del paso en turno y el turno pasa al siguiente paso; F14-H03 sumará el CN del firmante, la vigencia y el paso a EN_OCS; solo el firmante en turno puede subir (la regla de F14-H01), los demás reciben el 403 común; (2) la SPA incluye el botón «Subir el PDF firmado» en cada fila de «Pendientes de firma», junto a «Descargar el PDF», y muestra el informe con el resultado de cada comprobación, o el error 422 si se rechaza; (3) la lista de confianza es un archivo de configuración por entorno: el perfil local y la demo confían en la CA de prueba y en la raíz pública del BCE (versionada en el repo, es pública); la CI solo en la CA de prueba; el certificado real del autor y sus PDF firmados nunca entran al repositorio (ADR-017, principio IX). El resultado de cada comprobación queda en el informe de validación y en la bitácora, también para los rechazos. Fuera de alcance: CN del firmante, vigencia del certificado, orden por identidad y paso a EN_OCS (F14-H03), rechazo en firma (F14-H04), notificaciones (F16), sello de tiempo, CRL/OCSP, LTV y DocMDP (ADR-007)."

**Origen**: issue #63 · historia (`tipo:historia`, `prioridad:must`, `RF-39`) · feature F14 (#13) · milestone «Ola 3 · Aprobado y firmado» · servicio 10-firma · tamaño L. Depende de F14-H01 (#62), terminada.

**Requerimientos que cubre**:
- **RF-39:** validar cada PDF firmado que se sube. Esta historia cubre las comprobaciones 1 a 4 y 8 del diseño de firmas §6: el mismo documento, las firmas anteriores intactas, la integridad de la firma nueva, la confianza y que no altera el contenido.
- **RNF-SEG-08:** nadie cuela un documento distinto del vigente.

**Decisiones que aplica**:
- D-23: la validación básica.
- D-46: lo que el spike encontró en las firmas de FirmaEC.
- ADR-017: la confianza por entorno, sin interruptores.
- ADR-007: lo que queda fuera del prototipo.
- D-101: el documento firmable y su archivo nombrado por su hash.
- D-102: el firmante en turno.
- D-88: los archivos fuera de la base.

Diseño de firmas §5.1, §6 y §9. **Principios de la constitución**: IV, V, VII, VIII, IX, XIII y XIV.

## Clarifications

### Session 2026-11-14

- Q: Si el PDF pasa las comprobaciones de esta historia, ¿qué hace el sistema? → A: Registra la firma. El PDF firmado pasa a ser el archivo vigente, guardado por su SHA-256 sin sobrescribir nada, y el turno pasa al siguiente paso. F14-H03 suma el firmante por su certificado, la vigencia y el paso a EN_OCS. Solo el firmante en turno puede subir.
- Q: ¿Se sube desde la pantalla? → A: Sí. Cada fila de «Pendientes de firma» tiene «Subir el PDF firmado», junto a «Descargar el PDF». La pantalla muestra el informe con el resultado de cada comprobación, o el motivo del rechazo.
- Q: ¿En qué raíces se confía? → A: La lista de confianza es un archivo de configuración por entorno. El perfil local y la demo confían en la CA de prueba y en la raíz pública del BCE, que se versiona porque es pública. La CI confía solo en la CA de prueba. El certificado real del autor y los PDF firmados con él nunca entran al repositorio.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - El firmante en turno sube su firma y se registra (Priority: P1)

Como Responsable de consolidación, quiero subir el PDF que firmé con FirmaEC y que el sistema lo acepte si es el vigente con mi firma íntegra, para que el documento pase al siguiente firmante.

**Why this priority**: es el camino que cierra cada paso de la cadena. Sin él, el documento descargado en F14-H01 no vuelve al sistema.

**Independent Test**:
1. Con el paso en turno en F1, el Responsable designado descarga el PDF vigente y lo firma una vez, con un certificado de la CA de prueba.
2. Lo sube desde «Pendientes de firma».
3. El sistema responde con el informe: todas las comprobaciones aprobadas.
4. El documento desaparece de sus pendientes y aparece en los del Vicerrector, que descarga el PDF con la primera firma.

**Acceptance Scenarios**:

1. **Dado** que soy el firmante en turno y subo el PDF vigente con una sola firma nueva, íntegra y de un certificado que llega a una raíz de confianza, **cuando** termina la validación, **entonces** la firma queda registrada en el paso en turno y el informe muestra cada comprobación aprobada.
2. **Dado** una firma registrada, **cuando** se consulta el documento, **entonces** su archivo vigente es el PDF subido, guardado por su SHA-256; el archivo anterior se conserva sin cambios.
3. **Dado** la firma de F1 registrada, **cuando** el firmante esperado de F2 abre «Pendientes de firma», **entonces** ve el documento con «Revisado por» y descarga el PDF con la primera firma. El Responsable ya no lo ve.
4. **Dado** las firmas de F1, F2 y F3 registradas, **cuando** cualquiera abre «Pendientes de firma», **entonces** el documento no aparece para nadie. El paso a EN_OCS es de F14-H03.

---

### User Story 2 - Un PDF que no es el vigente o cuya firma no es válida se rechaza (Priority: P1)

Como Vicerrector académico, quiero que el sistema rechace un PDF distinto del vigente, con más de una firma nueva, con una firma alterada o de un certificado sin raíz de confianza, para que nadie cuele un documento distinto.

**Why this priority**: son los cuatro primeros escenarios del issue y el motivo de la historia (RNF-SEG-08).

**Independent Test**: el firmante en turno sube, uno a uno:
- un PDF que no empieza con los bytes del vigente;
- el vigente con dos firmas nuevas;
- el vigente sin ninguna firma nueva;
- uno firmado y modificado después;
- uno firmado con un certificado de una CA que no está en la lista de confianza.

Cada uno se rechaza con un 422 del catálogo que dice qué falló. El archivo vigente y el turno no cambian.

**Acceptance Scenarios**:

1. **Dado** el PDF vigente de una versión congelada, **cuando** subo un PDF que no empieza con los bytes del vigente, **entonces** se rechaza con un error 422 que dice que no es la versión vigente. *(issue)*
2. **Dado** el PDF vigente, **cuando** subo un PDF con dos firmas nuevas, **entonces** se rechaza con un error 422. *(issue)*
3. **Dado** el PDF vigente, **cuando** subo un PDF sin ninguna firma nueva, **entonces** se rechaza con un error 422.
4. **Dado** un PDF modificado después de firmado, **cuando** lo subo, **entonces** se rechaza porque la firma no es íntegra sobre su ByteRange. *(issue)*
5. **Dado** un PDF firmado con un certificado cuya cadena no llega a la lista de confianza, **cuando** lo subo, **entonces** se rechaza con un error 422. *(issue)*
6. **Dado** cualquier rechazo, **cuando** se consulta el documento, **entonces** su archivo vigente y su paso en turno siguen iguales, y no se guarda el archivo rechazado.

---

### User Story 3 - Cada validación deja su informe y su entrada en la bitácora (Priority: P1)

Como auditor, quiero que cada PDF subido deje el resultado de cada comprobación, aceptado o rechazado, para poder reconstruir quién subió qué y por qué se aceptó o no.

**Why this priority**: es el último escenario del issue. El informe de validación es la evidencia de cada firma (diseño de firmas §6 y §8).

**Independent Test**: después de una subida aceptada y de una rechazada, cada una tiene su informe con el resultado de cada comprobación, y la bitácora tiene una entrada por cada una, con quién subió, el documento, el SHA-256 del archivo subido y el resultado.

**Acceptance Scenarios**:

1. **Dado** cualquier PDF subido por el firmante en turno, **cuando** termina la validación, **entonces** el resultado de cada comprobación queda en el informe de validación y en la bitácora. *(issue)*
2. **Dado** una comprobación que no se pudo hacer porque falló una anterior de la que depende, **cuando** se lee el informe, **entonces** aparece como no evaluada, no como aprobada.
3. **Dado** una subida aceptada, **cuando** se lee su informe, **entonces** también dice el certificado de la firma nueva: el nombre (CN), el emisor y el número de serie. El nombre no se compara todavía con el firmante (F14-H03).

---

### User Story 4 - Quien no está en turno no sube nada (Priority: P1)

Como Rector, quiero que solo el firmante en turno pueda subir un PDF firmado, para que nadie firme fuera de orden ni por otro.

**Why this priority**: sin ella, cualquiera con el permiso de firma podría registrar una firma en el paso de otro.

**Independent Test**: con el paso en turno en F1, el Vicerrector, el Rector, otro usuario con el rol de Responsable y uno sin permiso de firma suben un PDF. Todos reciben el 403 común, sin informe ni entrada de validación.

**Acceptance Scenarios**:

1. **Dado** un documento cuyo paso en turno no es mío, **cuando** subo un PDF, **entonces** se rechaza con el 403 común, sin decir si el documento existe ni de quién es el turno.
2. **Dado** un documento sin generar, en curso, con la generación fallida o inexistente, **cuando** cualquiera sube un PDF, **entonces** recibe el mismo 403 común.

---

### Edge Cases

- **Dos subidas a la vez para el mismo paso**: se valida contra el archivo vigente en el momento de registrar. Se acepta una sola. La otra se rechaza con un 422 porque ya no empieza con los bytes del vigente.
- **El mismo PDF firmado se sube otra vez después de aceptado**: quien lo firmó ya no está en turno y recibe el 403 común. Si lo sube el firmante siguiente sin firmarlo, no tiene firma nueva y se rechaza con un 422.
- **El archivo subido no es un PDF o está dañado**: se rechaza con un 422; las comprobaciones que no se pudieron hacer quedan como no evaluadas.
- **Un archivo vacío o más grande que el límite de subida**: se rechaza antes de validar, con un error del catálogo, sin informe.
- **La firma nueva usa un campo de firma distinto del de su paso** (por ejemplo, el de F3 cuando el turno es F1): en esta historia se acepta si pasa las comprobaciones. Comprobar el paso por la persona y el campo es de F14-H03.
- **El certificado ya venció o su nombre no es el del firmante**: en esta historia no se comprueba (F14-H03). El informe muestra el CN para que se vea.
- **La lista de confianza está vacía o no se puede leer**: el servicio no arranca. No hay forma de aceptar firmas sin raíces.
- **El archivo vigente falta o no coincide con su SHA-256 registrado**: la subida falla con el error del catálogo de F14-H01 (archivo no disponible), sin registrar nada.

## Requirements *(mandatory)*

### Functional Requirements

**Subida**

- **FR-001**: El firmante en turno DEBE poder subir un PDF firmado para el documento de su paso, desde «Pendientes de firma» y desde la API.
- **FR-002**: Cualquier otro usuario que suba un PDF DEBE recibir el 403 común del catálogo, sin informe, sin entrada de validación y sin saber si el documento existe ni de quién es el turno. Lo mismo para un documento sin generar, en curso, fallido o inexistente.
- **FR-003**: Un archivo vacío o más grande que el límite de subida DEBE rechazarse antes de validar, con un error del catálogo.

**Comprobaciones (D-23)**

- **FR-004**: **Mismo documento.** El archivo subido DEBE empezar exactamente con los bytes del archivo vigente. Si no, se rechaza con un 422 que dice que no es la versión vigente. Esto también garantiza que las firmas anteriores y el contenido no cambiaron.
- **FR-005**: **Una sola firma nueva.** El archivo subido DEBE tener exactamente una firma más que el vigente. Sin firma nueva o con más de una, se rechaza con un 422.
- **FR-006**: **Integridad.** La firma nueva DEBE ser criptográficamente válida sobre los bytes que cubre (su ByteRange), y cubrir todo el archivo salvo su propio contenido de firma. Si no, se rechaza con un 422 que dice que la firma no es íntegra.
- **FR-007**: **Confianza.** El certificado de la firma nueva DEBE llegar, por su cadena, a una raíz de la lista de confianza del entorno. Si no, se rechaza con un 422 que dice que el certificado no es de confianza.
- **FR-008**: Las comprobaciones DEBEN hacerse en este orden: mismo documento, una sola firma nueva, integridad y confianza. Si una falla, el archivo se rechaza con el error de la primera que falló. Una comprobación que depende de otra que falló DEBE quedar como no evaluada.
- **FR-009**: La validación NO DEBE tener un interruptor que la apague en ningún entorno (ADR-017). Lo que cambia por entorno es solo la lista de confianza.

**Registro de la firma**

- **FR-010**: Si todas las comprobaciones pasan, el sistema DEBE registrar la firma del paso en turno con: quién subió, cuándo, el SHA-256 del archivo resultante y los datos del certificado (nombre, emisor, número de serie y vigencia).
- **FR-011**: El archivo subido aceptado DEBE guardarse por su SHA-256 y pasar a ser el archivo vigente del documento. Ningún archivo anterior DEBE sobrescribirse ni borrarse.
- **FR-012**: Con la firma registrada, el paso en turno DEBE pasar al siguiente paso sin firma: «Pendientes de firma» y la descarga de F14-H01 DEBEN seguir al nuevo firmante en turno y al nuevo archivo vigente. Con todos los pasos firmados, el documento no está en turno para nadie.
- **FR-013**: Registrar la firma, el nuevo archivo vigente y el informe DEBE ser todo o nada: si algo falla, no queda una firma sin archivo ni un archivo vigente sin firma.
- **FR-014**: Dos subidas a la vez para el mismo paso NO DEBEN registrar dos firmas: se acepta a lo sumo una.

**Informe y bitácora**

- **FR-015**: Cada subida del firmante en turno, aceptada o rechazada, DEBE dejar un informe de validación con el resultado de cada comprobación (aprobada, rechazada o no evaluada), el SHA-256 del archivo subido, quién lo subió y cuándo.
- **FR-016**: La respuesta a la subida DEBE incluir el informe. Un rechazo DEBE incluirlo en los detalles del error 422.
- **FR-017**: Cada subida del firmante en turno DEBE dejar una entrada en la bitácora, con el documento, el paso, el SHA-256 del archivo subido y el resultado de cada comprobación (RF-44).
- **FR-018**: Un archivo rechazado NO DEBE guardarse; solo queda su SHA-256 en el informe.

**Pantalla**

- **FR-019**: Cada fila de «Pendientes de firma» DEBE tener «Subir el PDF firmado», junto a «Descargar el PDF».
- **FR-020**: Al terminar, la pantalla DEBE mostrar el informe: cada comprobación con su resultado. Si se aceptó, la fila desaparece de los pendientes del usuario. Si se rechazó, dice el motivo y la fila sigue.

**Confianza y datos personales**

- **FR-021**: La lista de confianza DEBE ser un archivo de configuración por entorno, leído al arrancar:
  - la CI y las pruebas automáticas confían solo en la CA de prueba;
  - el perfil local y la demo confían en la CA de prueba y en la raíz pública del BCE.
- **FR-022**: El certificado real del autor y los PDF firmados con él NO DEBEN entrar al repositorio (principio IX). Las pruebas automáticas firman con los certificados ficticios de la CA de prueba.

**Errores y seguridad**

- **FR-023**: Los errores nuevos DEBEN estar en el catálogo con sus 8 columnas (principio XIII).
- **FR-024**: El turno, el permiso y todas las comprobaciones DEBEN hacerse en el servidor. La pantalla solo refleja el resultado.

### Key Entities

- **Firma** (nueva): la firma aceptada de un paso de un documento. Guarda el orden del paso, quién subió, cuándo, el SHA-256 del archivo resultante y los datos del certificado. Es una por paso y no cambia.
- **Informe de validación** (nuevo): el resultado de una subida del firmante en turno, aceptada o rechazada. Guarda el documento, el paso, el SHA-256 del archivo subido, quién y cuándo, el resultado de cada comprobación y, si hay firma nueva, los datos de su certificado.
- **Documento firmable** (de F13-H02 y F14-H01): su archivo vigente pasa a ser el de la última firma aceptada; sin firmas, el original.
- **Lista de confianza**: las raíces en las que confía el entorno, en un archivo de configuración.
- **Paso en turno** (de F14-H01): el primer paso sin firma. Ahora avanza con cada firma registrada.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: El firmante en turno sube su PDF firmado y ve el informe en menos de 1 minuto desde que abre «Pendientes de firma».
- **SC-002**: En las pruebas, el 100 % de los PDF que no son el vigente con una sola firma nueva, íntegra y de confianza se rechazan. Se prueba con los cuatro escenarios del issue y con el PDF sin firma nueva.
- **SC-003**: En las pruebas, el 100 % de las subidas del firmante en turno dejan su informe y su entrada en la bitácora, aceptadas y rechazadas.
- **SC-004**: Después de cualquier rechazo, el archivo vigente y el paso en turno siguen iguales.
- **SC-005**: Las tres firmas sucesivas de prueba (F1, F2 y F3) se aceptan y el PDF resultante tiene las tres firmas válidas para un validador independiente.
- **SC-006**: «Pendientes de firma», con el informe aceptado y con un rechazo, pasa la revisión automática de accesibilidad (WCAG 2.1 AA) sin fallas.
- **SC-007**: Los escenarios de F14-H01, F13-H02, F13-H01, F12-H05 y #215 siguen pasando sin cambios.

## Assumptions

- **Qué valida esta historia**: las comprobaciones 1 a 4 y 8 del diseño de firmas §6. La 5 (vigencia), la 6 (la persona por el CN) y la 7 (el orden por la persona) son de F14-H03. Hasta entonces, el orden lo da que solo el firmante en turno puede subir.
- **El límite de subida** es el que ya tiene el proceso para archivos, unos 10 MB. El PDF de 165 paralelos pesa cientos de KB y cada firma de FirmaEC agrega unos 32 KB.
- **Lo que FirmaEC escribe** (D-46): una firma `adbe.pkcs7.detached` con la cadena completa dentro y relleno de ceros después. La validación lo admite. Si FirmaEC firma en el campo vacío del paso o agrega uno nuevo, se averigua en el plan; la comprobación de una sola firma nueva vale para los dos casos.
- **La integridad se verifica con la llave pública del certificado**, sin mezclarla con la vigencia (D-46, hallazgo 4). La vigencia es de F14-H03.
- **El informe lo genera el propio sistema** (diseño de firmas §6). EU DSS queda como trabajo futuro (ADR-007).
- **Las pruebas firman con la CA de prueba**, con los certificados ficticios F1, F2 y F3 del spike (ADR-017). El script de punta a punta también. El autor prueba a mano con FirmaEC y su certificado real en el perfil local, que confía en la raíz del BCE.
- **La raíz del BCE** es un certificado público. Versionarlo no expone datos personales.
- **La bitácora** registra la subida como un cambio del documento, aceptada o rechazada (F17-H01).
- **Una subida rechazada no cuenta** para nada: se puede volver a subir las veces que haga falta.
- **La demo**: 2026-I está en EN_CONSOLIDACION, así que no hay documentos en firma. El recorrido completo se prueba en el perfil local y en la CI.

## Out of Scope

- Comparar el CN del certificado con el firmante, la vigencia del certificado, el orden por la persona y el paso a EN_OCS (F14-H03).
- Rechazar en firma (F14-H04).
- Notificar al siguiente firmante y mostrar a otros el estado de la cadena (F16-H01, F16-H02).
- Sello de tiempo, revocación por CRL u OCSP, validación a largo plazo (LTV) y DocMDP (ADR-007).
- Administrar la lista de confianza desde la pantalla: es un archivo de configuración (D-23).
- Firmar en el servidor o lanzar FirmaEC desde el sistema (diseño de firmas §5.2).

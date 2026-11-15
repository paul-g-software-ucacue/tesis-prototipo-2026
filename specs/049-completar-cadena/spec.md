# Feature Specification: Validar firmante, turno y vigencia, y completar la cadena

**Feature Branch**: `049-completar-cadena`

**Created**: 2026-11-14

**Status**: Draft

**Input**: User description: "F14-H03 · Validar firmante, turno y vigencia, y completar la cadena (#64). Usar el texto y los criterios del issue. Respetar .specify/memory/constitution.md, docs/decisiones.md (D-23, D-46, D-101, D-102, D-103, ADR-017, ADR-007, MD-05) y el diseño de firmas (Diseno_firmas_cadena_aprobacion.md §6 comprobaciones 5, 6 y 7, §7 y §9). Parte de F14-H02: la subida, las cuatro comprobaciones (mismo documento, una sola firma nueva, integridad, confianza), validacion_firma, el resultado de validación y la bitácora. Decisiones del autor: (1) el firmante se reconoce por el CN del certificado de la firma nueva, normalizado en mayúsculas y sin tildes, que debe coincidir con el nombre que el PDF imprime en el paso en turno (D-23); no se usa el SERIALNUMBER; no distingue homónimos (limitación aceptada); en los certificados de la ECIBCE el CN comparte componente con el SERIALNUMBER y se busca por su tipo (hallazgo de F14-H02); (2) la CA de prueba se regenera: generar.sh emite una CA nueva y tres firmantes ficticios cuyo CN es el nombre que el PDF imprime en cada paso con los datos de prueba (el del docente designado, la Vicerrectora y el Rector de los cargos vigentes); cambia la huella de ca-prueba.crt en D-103 y en su prueba; todo sigue siendo ficticio; (3) para la prueba a mano del autor con FirmaEC, un archivo local fuera del repositorio (ADR-017, punto 3) hace que en su perfil local el nombre impreso de los tres pasos sea el suyo, para firmar tres veces con su certificado real; su nombre nunca entra al repositorio. Comprobaciones nuevas: vigencia (el certificado está vigente al momento de subir; sin sello de tiempo, CRL/OCSP ni LTV, ADR-007), firmante (CN = nombre impreso del paso en turno) y orden (una firma de la persona de otro paso se rechaza por fuera de orden). Con la última firma aceptada, la cadena queda completa y el distributivo pasa a EN_OCS: Firma avisa a Aprobación (MD-05) y Aprobación registra la transición. Cada rechazo, 422 del catálogo. Recordar: FirmaEC agrega sus propios campos de firma (Signature1..3) y no llena los vacíos; el paso no se reconoce por el campo. Fuera de alcance: rechazo en firma (F14-H04), versión nueva y anulación de firmas (F12-H04), resolución del OCS (F15), notificaciones (F16), sello de tiempo, CRL/OCSP, LTV y DocMDP (ADR-007)."

**Origen**: issue #64 · historia (`tipo:historia`, `prioridad:must`, `RF-39`, `RF-58`, `RF-38`) · feature F14 (#13) · milestone «Ola 3 · Aprobado y firmado» · servicio 10-firma · tamaño L. Depende de F14-H02 (#63), terminada.

**Requerimientos que cubre**:
- **RF-39:** validar cada PDF firmado. Esta historia cubre las comprobaciones 5 (vigencia), 6 (firma la persona correcta) y 7 (respeta el orden) del diseño de firmas §6. Las demás son de F14-H02.
- **RF-58:** el árbol de firmas: el orden y la persona de cada paso.
- **RF-38:** las tres firmas, en orden, sobre la misma versión congelada. Con la última, la cadena queda completa y el distributivo sigue a EN_OCS.

**Decisiones que aplica**:
- D-23: la validación básica, con la identidad por el CN.
- ADR-017: los firmantes y la confianza por entorno; un solo firmante real en la instalación de pruebas del autor, asignado desde un archivo local.
- ADR-007: lo que queda fuera del prototipo.
- MD-05: Firma no conoce el flujo; avisa a Aprobación.
- D-101, D-102 y D-103: el documento firmable, el firmante esperado y la validación de F14-H02.

Diseño de firmas §6, §7 y §9. **Principios de la constitución**: IV, V, VI, VII, VIII, IX, XIII y XIV.

## Clarifications

### Session 2026-11-14

- Q: ¿Cómo se reconoce al firmante? → A: Por el CN del certificado de la firma nueva, en mayúsculas y sin tildes, igual al nombre que el PDF imprime en el paso en turno (D-23). No se usa el número de serie del sujeto. No distingue homónimos: es una limitación aceptada.
- Q: Las pruebas automáticas necesitan certificados cuyo CN sea el nombre impreso en cada paso. ¿Qué se hace con la CA de prueba? → A: Se regenera: una CA nueva y tres firmantes ficticios con los nombres que el PDF imprime en cada paso con los datos de prueba. Cambia la huella de la CA de prueba en la lista de confianza.
- Q: ¿Cómo prueba el autor con FirmaEC y su certificado real, si su nombre no es ninguno de los impresos? → A: Con un archivo local fuera del repositorio que, en su perfil local, pone su nombre como el impreso en los tres pasos (ADR-017). Su nombre nunca entra al repositorio.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Solo firma la persona del paso, con un certificado vigente (Priority: P1)

Como Rector, quiero que el MDD compruebe que firmo yo, en mi turno y con un certificado vigente, para que F1, F2 y F3 sean verificables.

**Why this priority**: son los tres primeros escenarios del issue. Sin ellos, cualquier firma de confianza se acepta en cualquier paso, aunque sea de otra persona.

**Independent Test**: con el paso F3 en turno, el Rector sube el PDF vigente firmado:
- con un certificado cuyo CN es de otra persona: se rechaza;
- con el certificado del Vicerrector o del Responsable: se rechaza por fuera de orden;
- con un certificado vencido: se rechaza;
- con su propio certificado vigente: se acepta.

**Acceptance Scenarios**:

1. **Dado** el paso F3 en turno, asignado al Rector, **cuando** se sube un PDF firmado con un certificado cuyo CN es de otra persona, **entonces** se rechaza con un error 422. *(issue)*
2. **Dado** el paso F2 en turno, **cuando** se sube la firma de F3 (con el certificado de la persona de F3), **entonces** se rechaza con un error 422 que dice que está fuera de orden. *(issue)*
3. **Dado** un certificado que ya no está vigente al subir el PDF, **cuando** se sube, **entonces** se rechaza con un error 422. *(issue)*
4. **Dado** un certificado que todavía no está vigente al subir, **cuando** se sube, **entonces** se rechaza con el mismo error de vigencia.
5. **Dado** el CN del certificado con otras mayúsculas, tildes o espacios que el nombre impreso, **cuando** se compara, **entonces** coinciden: se compara normalizado.
6. **Dado** cualquier rechazo de esta historia, **cuando** se consulta el documento, **entonces** el archivo vigente y el paso en turno siguen iguales, y el resultado de cada comprobación queda en el resultado de validación y en la bitácora, como en F14-H02.

---

### User Story 2 - La cadena completa pasa el distributivo al OCS (Priority: P1)

Como Vicerrector académico, quiero que, aceptada la última firma, el distributivo pase solo a la etapa del OCS, para que la aprobación siga sin que nadie tenga que moverla a mano.

**Why this priority**: es el último escenario del issue y cierra la cadena de firmas (RF-38).

**Independent Test**: con F1 y F2 aceptadas, el Rector sube su firma; se acepta, el distributivo queda en EN_OCS con su transición registrada, y el PDF final tiene las tres firmas válidas.

**Acceptance Scenarios**:

1. **Dado** F1 y F2 aceptadas, **cuando** se acepta F3, **entonces** el distributivo pasa a EN_OCS. *(issue)*
2. **Dado** el paso a EN_OCS, **cuando** se consulta el flujo del distributivo, **entonces** la transición de CONGELADO_EN_FIRMA a EN_OCS queda registrada por el sistema, con la versión firmada, y aparece en la bitácora.
3. **Dado** el PDF con las tres firmas, **cuando** se valida en FirmaEC, **entonces** las tres firmas son válidas. *(issue)*
4. **Dado** solo F1 o solo F1 y F2 aceptadas, **cuando** se consulta el flujo, **entonces** el distributivo sigue en CONGELADO_EN_FIRMA.
5. **Dado** que el aviso a Aprobación falla después de registrar la última firma, **cuando** se reintenta, **entonces** el distributivo llega a EN_OCS una sola vez, sin perder la firma ni duplicar la transición.

---

### User Story 3 - El autor firma la cadena con su certificado real (Priority: P2)

Como autor de la tesis, quiero firmar las tres etapas con FirmaEC y mi certificado real en mi perfil local, para probar el flujo completo sin poner mi nombre en el repositorio (ADR-017).

**Why this priority**: es la única prueba con un certificado real y FirmaEC. Las pruebas automáticas usan certificados ficticios.

**Independent Test**: con el archivo local que asigna el nombre del autor a los tres pasos, el PDF generado imprime ese nombre en los tres pasos; el autor firma tres veces con FirmaEC y la cadena se completa.

**Acceptance Scenarios**:

1. **Dado** el archivo local con el nombre del autor para los tres pasos, **cuando** se genera el documento en el perfil local, **entonces** el PDF imprime ese nombre en «Elaborado por», «Revisado por» y «Aprobado por».
2. **Dado** ese documento, **cuando** el autor sube cada firma con su certificado real desde la cuenta en turno, **entonces** las tres se aceptan y el distributivo pasa a EN_OCS.
3. **Dado** que el archivo local no existe, **cuando** se genera el documento, **entonces** se imprimen los nombres de siempre, los de los datos del periodo.
4. **Dado** el repositorio, **cuando** se revisa, **entonces** ni el archivo local ni el nombre del autor están en él.

---

### Edge Cases

- **El CN coincide con la persona de un paso ya firmado** (por ejemplo, F1 vuelve a firmar con F2 en turno): se rechaza por firmante equivocado, no por fuera de orden. Fuera de orden es solo para la persona de un paso siguiente.
- **La misma persona ocupa dos pasos** (como en la instalación del autor): su CN coincide con el paso en turno y se acepta en ese paso. El orden lo sigue dando el turno.
- **Dos personas distintas con el mismo nombre**: no se distinguen (D-23). Queda como limitación.
- **El certificado de la firma nueva no tiene CN**: se rechaza por firmante equivocado.
- **El certificado vence entre la descarga y la subida**: se rechaza por vigencia; se firma otra vez con uno vigente.
- **El nombre impreso del paso no se guardó** (un documento generado antes de esta historia): el paso no se puede validar y la subida se rechaza con un error del catálogo, sin aceptar una firma que no se puede comprobar. En la demo no hay documentos en firma.
- **Aprobación no responde al aviso de cadena completa**: el aviso se reintenta hasta entregarse (ADR-018). La firma ya está registrada y no se repite.
- **El distributivo ya no está en CONGELADO_EN_FIRMA cuando llega el aviso**: Aprobación no lo mueve y lo registra; el aviso no se reintenta para siempre.

## Requirements *(mandatory)*

### Functional Requirements

**Comprobaciones nuevas**

- **FR-001**: **Vigencia.** El certificado de la firma nueva DEBE estar vigente al momento de subir el PDF. Si ya venció o todavía no empieza, se rechaza con un 422 que dice que el certificado no está vigente. Sin sello de tiempo, revocación ni validación a largo plazo (ADR-007).
- **FR-002**: **Firmante.** El CN del certificado de la firma nueva, normalizado (mayúsculas, sin tildes, espacios simples y sin espacios al inicio ni al final), DEBE ser igual al nombre que el PDF imprime en el paso en turno, normalizado igual. Si no, se rechaza con un 422 que dice que el firmante no es el del paso.
- **FR-003**: **Orden.** Si el CN de la firma nueva es el nombre impreso de un paso posterior al paso en turno, se rechaza con un 422 que dice que está fuera de orden. Se prueba antes que el firmante equivocado, para dar el motivo más útil.
- **FR-004**: El CN DEBE buscarse por su tipo en el sujeto del certificado, aunque comparta componente con otros valores, como en los certificados de la ECIBCE. El número de serie del sujeto NO DEBE usarse para reconocer al firmante (D-23).
- **FR-005**: Las comprobaciones nuevas DEBEN evaluarse después de las de F14-H02, en este orden: vigencia, orden y firmante. Una que depende de otra rechazada queda «no evaluada», como en F14-H02. Todas DEBEN quedar en el resultado de validación y en la bitácora.
- **FR-006**: El paso en turno NO DEBE reconocerse por el nombre del campo de firma: FirmaEC agrega sus propios campos y no llena los vacíos (hallazgo de F14-H02).

**El nombre impreso de cada paso**

- **FR-007**: Al generar el documento, el sistema DEBE guardar con cada firmante esperado el nombre que el PDF imprime en su paso, para compararlo con el CN. No cambia después, aunque cambie la designación o el cargo (D-102).
- **FR-008**: Un documento sin el nombre impreso de su paso en turno NO DEBE aceptar firmas: la subida se rechaza con un error del catálogo.

**Cadena completa**

- **FR-009**: Con la última firma de la cadena aceptada, Firma DEBE avisar a Aprobación que la cadena del documento está completa, sin conocer el flujo (MD-05).
- **FR-010**: Aprobación DEBE pasar el distributivo de CONGELADO_EN_FIRMA a EN_OCS, como una transición del sistema, con la versión firmada, y registrarla en la bitácora.
- **FR-011**: El aviso DEBE llegar aunque Aprobación falle un momento, una sola vez: la firma registrada no se repite y la transición no se duplica.
- **FR-012**: Si el distributivo ya no está en CONGELADO_EN_FIRMA o la versión no es la congelada, Aprobación NO DEBE moverlo, y lo DEBE dejar registrado sin reintentar para siempre.

**Pruebas y datos**

- **FR-013**: La CA de prueba DEBE regenerarse con tres firmantes ficticios cuyo CN sea el nombre que el PDF imprime en cada paso con los datos de prueba. Su llave privada no se guarda. La huella nueva de su certificado DEBE quedar en D-103 y en su prueba.
- **FR-014**: El sistema DEBE admitir, solo por configuración local fuera del repositorio, un nombre impreso distinto para cada paso, con el que el autor firma con su certificado real (ADR-017). Sin esa configuración, los nombres son los de los datos del periodo. El repositorio NO DEBE contener el nombre real del autor.

**Errores y seguridad**

- **FR-015**: Los errores nuevos DEBEN estar en el catálogo con sus 8 columnas (principio XIII).
- **FR-016**: Todas las comprobaciones DEBEN hacerse en el servidor; la pantalla de F14-H02 muestra las nuevas sin cambiar su forma.

### Key Entities

- **Firmante esperado** (de F14-H01): ahora también guarda el nombre impreso de su paso.
- **Resultado de validación** (de F14-H02): suma las comprobaciones de vigencia, orden y firmante.
- **Aviso de cadena completa** (nuevo): de Firma a Aprobación, con el periodo y la versión, cuando se acepta la última firma.
- **Transición a EN_OCS** (de Aprobación): la del sistema al completarse la cadena.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: En las pruebas, el 100 % de las firmas de otra persona, fuera de orden o con un certificado vencido o todavía no vigente se rechazan, y el 100 % de las de la persona del paso, en su turno y vigentes, se aceptan.
- **SC-002**: Con la tercera firma aceptada, el distributivo está en EN_OCS en menos de 30 segundos, sin que nadie lo mueva.
- **SC-003**: El PDF con las tres firmas se valida en FirmaEC con las tres firmas válidas.
- **SC-004**: El autor completa la cadena con su certificado real en su perfil local, y el repositorio no contiene su nombre.
- **SC-005**: Los escenarios de F14-H02, F14-H01, F13-H02, F13-H01, F12-H05 y #215 siguen pasando.

## Assumptions

- **La normalización** es la de D-23: mayúsculas y sin tildes. Además se unifican los espacios. La «Ñ» se conserva.
- **El nombre impreso** es el mismo que se escribe en el PDF al generar el documento (D-101). En los datos de prueba: el del docente designado para F1, y los de los docentes de los cargos vigentes de Vicerrector y Rector para F2 y F3.
- **Los documentos generados antes** de esta historia no tienen el nombre impreso guardado. En la demo no hay ninguno en firma; en un perfil local basta empezar con una base sin datos.
- **El aviso de cadena completa** usa los eventos persistentes entre servicios del mismo proceso, como el paso a firma de F13-H02, y se entrega después de confirmar la firma.
- **La transición a EN_OCS** la registra Aprobación como «sistema», igual que las demás transiciones automáticas.
- **La CA de prueba regenerada** sigue siendo ficticia (principio IX). Los certificados viejos de F1, F2 y F3 se reemplazan.
- **El archivo local del autor** solo cambia los nombres impresos; no cambia quién está en turno ni la lista de confianza.
- **La pantalla** de F14-H02 ya muestra cualquier comprobación por su nombre; solo suma los tres nombres nuevos.

## Out of Scope

- Rechazar en firma (F14-H04).
- Crear una versión nueva y anular las firmas tras un rechazo (F12-H04).
- Registrar la resolución del OCS (F15) y lo que pasa en EN_OCS.
- Notificar al siguiente firmante (F16).
- Sello de tiempo, CRL u OCSP, LTV y DocMDP (ADR-007).
- Reconocer al firmante por el número de serie o la cédula.

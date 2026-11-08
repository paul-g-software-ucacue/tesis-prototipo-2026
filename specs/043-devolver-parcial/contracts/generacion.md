# Contrato: `ParcialesDelDistributivo.marcarParcialesDevueltos` (interfaz publicada de Generación)

Un método nuevo en la interfaz de F12-H02 (`ec.mdd.generacion.puertos.entrada`, `@NamedInterface("api")`). Solo lo usa Validación OCS, en el mismo proceso `distributivo`. No tiene ruta HTTP.

| Método | Entrada | Salida | Errores |
|---|---|---|---|
| `marcarParcialesDevueltos` | `periodo`, `carreras` (lista, sin repetidas) | `int`: el número de la versión vigente | `DistributivoNoEncontrado`; `ParcialNoEncontrado` si alguna carrera no tiene parcial |

**Garantías:**
- Una transacción, con la versión vigente bloqueada, como `marcarParcialEnviado`.
- Todo o nada: si una carrera no tiene parcial, no marca ninguna (FR-005).
- Idempotente: un parcial ya DEVUELTO queda igual.
- No registra en el registro de cambios de Generación: la transición la registra Aprobación (D-95).
- No toca propuestas, actividades, la consolidación ni el informe: el consolidado no queda desactualizado por devolver (research §7).
- Desde ese momento, el parcial admite propuestas, asignaciones y quitas: `ParcialCarrera.editable()` ya es verdadero para todo lo que no está ENVIADO.

**Pruebas:**
- Aplicación con Mockito: marca todas, rechaza sin marcar ninguna si falta una y es idempotente.
- Integración con PostgreSQL real: las dos carreras quedan DEVUELTO en la misma transacción, y una propuesta en el parcial devuelto se guarda.

# Contrato: las pruebas de punta a punta

F12-H03 (#58). Corren sobre el perfil consolidado levantado, en la CI (`.github/workflows/perfil-consolidado.yml`) y en el perfil local. Nunca imprimen una clave ni un token, y solo usan los datos ficticios de `cargas/datos_prueba/`.

## `deploy/probar_avance.sh`: el plazo vencido

El script termina hoy con el distributivo de 2026-I en EN_CONSOLIDACION. Suma estos pasos al final, antes de limpiar. En el script, P1 a P8 son los pasos 9 a 15 (P2 y P3 van juntos en el 10):

| Paso | Qué hace | Qué comprueba |
|---|---|---|
| P1 | Consulta el estado del flujo como `vicerrectorado.demo` | `plazo.estado = A_TIEMPO`, `plazo.dias = 5`, `diasRestantes = 5`; `etapas` con las 7 activas por defecto y EN_CONSOLIDACION como `actual` |
| P2 | Detiene `identidad` | — |
| P3 | Corre hacia atrás **6 días** todas las transiciones de 2026-I, con `psql` como superusuario (research §9) | Que el orden de las transiciones no cambia |
| P4 | Reinicia `distributivo` y espera su *healthcheck* | La revisión del arranque deja una fila en `validacionocs.plazo_vencido` para EN_CONSOLIDACION, con `fecha_limite` = llegada + 5 días |
| P5 | Levanta `identidad` y espera, hasta el reenvío de eventos | `vicerrectorado.demo` y la cuenta de quien consolida tienen **una** notificación «Venció el plazo de …», con el enlace a `/consolidado` (SC-004) |
| P6 | Consulta la bitácora | Una entrada `PLAZO_VENCIDO` de `validacionocs`, por `sistema`, con `diasDeAtraso = 1` |
| P7 | Consulta el estado del flujo | `plazo.estado = VENCIDO`, `diasRestantes = -1`; `acciones` de quien consolida sin cambios (FR-003, SC-003) |
| P8 | Reinicia `distributivo` otra vez y espera la revisión del arranque | Sigue habiendo **una** fila en `plazo_vencido` y **una** notificación por usuario (SC-002) |

- Si P8 alarga demasiado el job, se queda solo en la prueba de integración (`RevisarPlazosIntegrationTest`), que corre la revisión tres veces. La decisión se toma al implementar, midiendo el reinicio.
- La limpieza del script borra también las filas de `plazo_vencido` de 2026-I.
- Con un paso fallido, el script levanta `identidad` y `distributivo` antes de salir, como ya hace con `identidad`.

## `deploy/probar_flujo.sh`: la advertencia de RN-23

Suma tres pasos con periodos de prueba nuevos (`prueba-…`), registrados con fechas relativas a hoy:

| Paso | Periodo | Qué comprueba |
|---|---|---|
| R1 | Inicio dentro de 60 días, abierto con la versión de normativa sembrada | Cargar la plantilla responde 200 con `RN23_MARGEN_INSUFICIENTE`: `ultimoPlazo` = hoy + 43, `fechaDeRemision` = inicio − 30, `diasDeMargen` = −13. La consulta la devuelve igual |
| R2 | Inicio dentro de 120 días, abierto con la misma versión | La plantilla se carga sin `advertencias` |
| R3 | Sin versión de normativa | La carga responde 200 con `RN23_SIN_NORMATIVA` |

- Los pasos que ya existen siguen igual: su periodo no tiene versión, así que ahora sus cargas traen `RN23_SIN_NORMATIVA`, y ninguno comprueba que no haya advertencias.
- Si alguno lo comprueba, se ajusta.

## La CI

- El workflow no cambia de pasos: los dos scripts ya corren.
- El tiempo del job se revisa con P4 y P8. Hoy usa unos 25 minutos de 45.

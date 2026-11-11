# Contrato: prueba de punta a punta

## `deploy/probar_aprobacion.sh` (nuevo)

Parte de `probar_consolidado_en_revision.sh`: exige una base sin datos, carga los de prueba, borra al final lo que creó y nunca imprime una clave ni un token.

| Paso | Qué comprueba |
|---|---|
| 1 | Consolidado listo: los 165 paralelos con la sugerencia, las blandas justificadas, enviado a revisión (EN_REVISION_VRA); guarda el hash de esa transición |
| 2 | Antes de aprobar: `coordinacion.demo` y `consolidacion.demo` no tienen `APROBAR_CONTENIDO`, y su intento responde 403 sin cambiar nada; `vicerrectorado.demo` sí la tiene |
| 3 | `vicerrectorado.demo` aprueba: 200, etapa CONGELADO_EN_FIRMA, `congelacion` con la versión, quién y un hash igual al del envío (SC-002); `versionCongelada` verdadero en el consolidado y el panel |
| 4 | Una segunda aprobación responde 409 `VALIDACIONOCS_ETAPA_NO_ADMITE_APROBACION`; hay una sola transición desde EN_REVISION_VRA (SC-004) |
| 5 | Las ediciones (proponer, quitar una propuesta, asignar una actividad, consolidar, justificar, adjuntar) responden 409 `GENERACION_VERSION_CONGELADA`, y el hash no cambia (SC-001) |
| 6 | Consultar el consolidado y el panel, y descargar el informe, siguen (FR-010) |
| 7 | Bitácora: exactamente una `CONTENIDO_APROBADO`, con versión y hash; ninguna entrada por los rechazos (FR-009) |

**Para mirar la pantalla (`MDD_CONSERVAR=1`).** Hace solo el paso 1 y no borra nada: deja el distributivo en EN_REVISION_VRA en el perfil local, para recorrer `quickstart.md` §3. Se niega a correr si `MDD_GATEWAY` no es `http://localhost:…` o `http://127.0.0.1:…`. Al terminar, imprime cómo limpiar, con el proyecto nombrado: `docker compose -p tesis-prototipo-2026 down -v` (la demo, `mdd`, corre en el mismo equipo y no se toca). Sin la variable (en la CI), hace los 7 pasos y limpia.

**CI:** paso «Aprobar y congelar» en `.github/workflows/perfil-consolidado.yml`, después de «El consolidado en revisión».

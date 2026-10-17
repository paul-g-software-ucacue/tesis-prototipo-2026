# Contrato: cierre de ola sin URL pública (`cierre-ola.yml`, D-55)

## Disparo

Sin cambios: el tag `ola-*` o a mano (`workflow_dispatch`). No bloquea pull requests.

## Job `ZAP y k6 sobre el perfil`

| Paso | Qué hace |
|---|---|
| Sistema | `docker compose build` y `docker compose up -d --wait` con `.env.ejemplo`, como `perfil-consolidado` |
| ZAP · SPA | `zap-full-scan.py` sobre `http://localhost:8081`, con `deploy/zap/sin_altos.sh` |
| ZAP · API | Por cada `contratos/<servicio>/openapi.yaml` (sin `comun/`): `zap-api-scan.py`, con el token de `autor.prueba` del perfil local en `Authorization` y el API Gateway como destino; `sin_altos.sh` |
| k6 | Si existe `pruebas/k6/<tag>.js`, lo corre contra el API Gateway; si no, `::notice::` y sigue |
| Artefactos | Informes de ZAP y, si corrió, de k6 |

Ningún paso usa `vars.*` ni secretos: el perfil local tiene sus valores ficticios en `.env.ejemplo`.

## Job `Evidencia de la ola`

| Cambio | Antes | Después |
|---|---|---|
| SBOM | `cyclonedxDirectBom cyclonedxBom` | `cyclonedxDirectBom` y `deploy/sca/verificar_sbom.sh` (B00-H07) |

Lo demás no cambia: cobertura, empaquetado y `gh release create` en el tag.

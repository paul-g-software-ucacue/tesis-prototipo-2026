# Quickstart: Rechazar en firma con una observación

## 1. Pruebas automáticas

```bash
cd backend
./gradlew :servicios:firma:test :servicios:firma:integrationTest \
          :servicios:validacionocs:test :servicios:validacionocs:integrationTest \
          :apps:distributivo:test :apps:distributivo:integrationTest
```

- **Dominio:** `RechazoEnFirma` (la observación), `FirmasDelDocumento` sin turno con rechazo, `ProcesoAprobacion.rechazarEnFirma` y el bloqueo de aprobar.
- **Aplicación:** `RechazarEnFirmaServicio` (turno antes que observación, el aviso en la transacción), `RegistrarRechazoEnFirmaServicio`.
- **Integración:** V6 de Firma y V5 de Validación OCS; el rechazo y la subida a la vez; en `apps:distributivo`, F1 aceptada, el rechazo de F2 y el distributivo en EN_REVISION_VRA.

## 2. Punta a punta en el perfil local

```bash
docker compose -p tesis-prototipo-2026 up -d --build --wait
deploy/probar_rechazo_en_firma.sh
```

Debe terminar 10/10 (`contracts/demostracion.md`).

## 3. A mano, en la SPA

1. `MDD_CONSERVAR=1 deploy/probar_rechazo_en_firma.sh`: deja 2026-I en CONGELADO_EN_FIRMA con F1 aceptada.
2. Como `vicerrectorado.demo`, en «Pendientes de firma», elegir «Rechazar»:
   - sin observación o con menos de 10 caracteres, el diálogo no envía;
   - con una observación, «Rechazo registrado» y la fila desaparece (SC-003: menos de 1 minuto).
3. En «Consolidado del periodo», en menos de 30 s: EN_REVISION_VRA, el aviso «Rechazado en firma» con la observación y sin «Aprobar el contenido» (SC-002).
4. Como `rectorado.demo`: «Pendientes de firma» vacía.
5. Limpiar: `docker compose -p tesis-prototipo-2026 down -v`.

## 4. Demo

En la demo, 2026-I está en EN_CONSOLIDACION. La comprobación es solo de lectura:
- la V6 de `firma` y la V5 de `validacionocs`;
- `firma.rechazo_en_firma` y `validacionocs.rechazo_en_firma` vacías.

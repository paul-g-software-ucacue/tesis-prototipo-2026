# Quickstart: Crear una versión nueva y anular las firmas tras un rechazo en firma

## 1. Pruebas automáticas

```bash
cd backend
./gradlew :servicios:generacion:test :servicios:generacion:integrationTest \
          :servicios:firma:test :servicios:firma:integrationTest \
          :servicios:validacionocs:test :servicios:validacionocs:integrationTest \
          :apps:distributivo:test :apps:distributivo:integrationTest
```

- **Dominio:** `ProcesoAprobacion.registrarVersionNueva` y `rechazoVigente`; `FirmasDelDocumento` sin turno con anulación.
- **Aplicación:** `ParcialesDelDistributivoServicio.crearVersionNueva` (los tres casos); `RegistrarRechazoEnFirmaServicio` con la versión nueva y el aviso; `AnularFirmasServicio`.
- **Integración:**
  - V8 de Generación: la copia completa y la versión anulada intacta;
  - V7 de Firma, con el rol del proceso;
  - V6 de Validación OCS;
  - en `apps:distributivo`: rechazo en F2, v2 creada, firmas anuladas, aprobar la v2 y F1 en turno de la v2.

## 2. Punta a punta en el perfil local

```bash
docker compose -p tesis-prototipo-2026 up -d --build --wait
deploy/probar_rechazo_en_firma.sh
```

Debe terminar 10/10 (`contracts/demostracion.md`).

## 3. A mano, en la SPA

1. `MDD_CONSERVAR=1 deploy/probar_rechazo_en_firma.sh`: deja 2026-I en CONGELADO_EN_FIRMA, versión 1, con F1 aceptada.
2. Como `vicerrectorado.demo`, en «Pendientes de firma», rechazar con una observación.
3. En «Consolidado del periodo», en menos de 30 s:
   - la versión 2 y EN_REVISION_VRA;
   - el aviso «Rechazado en firma» con la observación y «La versión 1 quedó anulada con sus firmas; el distributivo sigue con la versión 2.»;
   - «Aprobar el contenido».
4. Aprobar el contenido: el distributivo pasa a CONGELADO_EN_FIRMA con la versión 2 y el PDF nuevo se genera.
5. Como `docente.demo`, «Pendientes de firma» muestra la versión 2, paso «Elaborado por».
6. Limpiar: `docker compose -p tesis-prototipo-2026 down -v`.

## 4. Demo

Solo de lectura (`contracts/demostracion.md`).

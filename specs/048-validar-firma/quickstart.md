# Quickstart: Validar que el PDF subido es el mismo documento con una firma íntegra

## 1. Pruebas automáticas

```bash
cd backend
./gradlew :servicios:firma:test :servicios:firma:integrationTest :apps:distributivo:test
```

- **Dominio:** el informe y el mismo documento.
- **Aplicación:** la subida con su transacción y su bloqueo.
- **Adaptador:** `ValidadorPades`, con PDF firmados por `FirmadorDePrueba` y la CA de prueba: los casos del issue, sin firma nueva y las tres firmas sucesivas.
- **Contrato y permisos.**
- **Integración:** la V4, el índice único con dos subidas a la vez y el rol del proceso que no actualiza.

En la SPA:

```bash
npm run test -- FirmasPendientes
npx playwright test firmas-pendientes
```

## 2. Punta a punta en el perfil local

```bash
docker compose -p tesis-prototipo-2026 up -d --build --wait
deploy/probar_documento_firmable.sh
```

Debe terminar 12/12 (`contracts/demostracion.md`).

## 3. A mano, con FirmaEC y el certificado real del autor (perfil local)

Con una base sin datos, dejar 2026-I en EN_REVISION_VRA con `docente.demo` designado:

```bash
MDD_CONSERVAR=1 deploy/probar_documento_firmable.sh
```

1. Como `vicerrectorado.demo`, aprobar el contenido y esperar «Documento firmable generado».
2. **F1:**
   - como `docente.demo`, en «Pendientes de firma», descargar el PDF y firmarlo con FirmaEC;
   - anotar si FirmaEC llenó el campo «Elaborado por» o agregó una firma aparte (research §7);
   - subirlo con «Subir el PDF firmado» y medir desde que abre la pantalla hasta ver el informe (SC-001, menos de 1 minuto);
   - el informe debe tener las cuatro comprobaciones aprobadas y el nombre del certificado.
3. **Los rechazos, con el turno ya en F2:**
   - `docente.demo` ya no tiene la fila, y no está en turno para subir;
   - como `vicerrectorado.demo`, subir el PDF original, el que se descargó antes de firmar: debe decir «No es la versión vigente»;
   - subir el PDF con la firma de F1 sin firmarlo: debe decir «Firma nueva no encontrada».
4. **F2 y F3:** como `vicerrectorado.demo` y luego `rectorado.demo`, descargar, firmar con FirmaEC y subir. Sin la comparación del CN (F14-H03), el mismo certificado sirve para los tres pasos (ADR-017).
5. **El PDF final:** abrir el último PDF descargado con FirmaEC («Verificar documento») o Adobe Reader. Deben aparecer tres firmas válidas (SC-005).
6. **La bitácora:** como `vicerrectorado.demo`, «Bitácora» con el servicio `firma` debe mostrar las tres `FIRMA_ACEPTADA` y las dos `FIRMA_RECHAZADA`.
7. **Limpiar.** Los PDF firmados con el certificado real no se copian al repositorio (principio IX):

   ```bash
   docker compose -p tesis-prototipo-2026 down -v
   ```

## 4. Demo

En la demo, 2026-I está en EN_CONSOLIDACION. La comprobación es solo de lectura:
- en la base, la migración V4 de `firma` y `firma.validacion_firma` vacía;
- en «Pendientes de firma», nada cambia: «No tienes firmas pendientes.».

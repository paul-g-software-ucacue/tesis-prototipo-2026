# Quickstart: Registrar la resolución del OCS

## 1. Pruebas automáticas

```bash
cd backend
./gradlew :servicios:validacionocs:test :servicios:validacionocs:integrationTest \
          :apps:distributivo:test :apps:distributivo:integrationTest
```

- **Dominio:** `ResolucionOcs`, `ArchivoDeResolucion`, `ProcesoAprobacion.registrarResolucion` (rol, etapa, fecha) y el avance desde EN_OCS.
- **Aplicación:** `RegistrarResolucionServicio` y `DescargarResolucionServicio`.
- **Integración:** V7, el almacén en disco, el repositorio con la transición y su resolución, y dos registros a la vez.
- **Contrato:** `RegistrarResolucionContratoTest` y `FlujoContratoTest`.

## 2. Punta a punta en el perfil local

```bash
docker compose -p tesis-prototipo-2026 up -d --build --wait
deploy/probar_documento_firmable.sh
```

Debe terminar 16/16 (`contracts/demostracion.md`).

## 3. A mano, en la SPA

1. `MDD_CONSERVAR=EN_OCS deploy/probar_documento_firmable.sh` (deja 2026-I en EN_OCS, con las tres firmas de la CA de prueba; `MDD_CONSERVAR=1` sigue dejándolo en EN_REVISION_VRA, como en F14-H03).
2. Como `secretaria.demo`, en «Consolidado del periodo»: completar el número, la fecha y un PDF, confirmar (SC-003: menos de 2 minutos).
3. Ver APROBADO, la constancia de la resolución y descargarla.
4. Como `vicerrectorado.demo`: ver la constancia y descargarla; sin el formulario.
5. Limpiar: `docker compose -p tesis-prototipo-2026 down -v`.

## 4. Demo

Solo de lectura (`contracts/demostracion.md`).

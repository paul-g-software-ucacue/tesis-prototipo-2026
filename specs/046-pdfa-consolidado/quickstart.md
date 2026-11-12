# Quickstart: Generar el PDF/A del consolidado con su hash

## 1. Pruebas

```bash
./gradlew build integrationTest
./gradlew :apps:distributivo:test             # Spring Modulith
./gradlew :servicios:usuarios:test --rerun    # BitacoraSinBorradoContratoTest, tras cambiar contratos
```

SPA (en `node:24-alpine`): tipos, ESLint, Prettier y `vitest run`. Playwright con `consolidado`.

## 2. Perfil local, de punta a punta

Con el perfil consolidado levantado y una base sin datos:

```bash
deploy/probar_documento_firmable.sh        # 6/6
```

## 3. A mano, en el perfil local (http://localhost:8081)

Con una base sin datos, dejar el distributivo en EN_REVISION_VRA:

```bash
MDD_CONSERVAR=1 deploy/probar_aprobacion.sh
```

1. Como `vicerrectorado.demo`, aprobar el contenido. En unos segundos, la tarjeta pasa de «Generando el documento firmable…» a «Documento firmable generado el …», con la huella del PDF.
2. Sacar el PDF del volumen, que todavía no se descarga por la pantalla (F14-H01):

   ```bash
   docker compose -p tesis-prototipo-2026 exec -T distributivo sh -c 'ls /var/mdd/archivos/documentos'
   docker compose -p tesis-prototipo-2026 cp distributivo:/var/mdd/archivos/documentos/<sha256>.pdf /tmp/
   ```

   Abrirlo: el resumen por carrera, los totales, el detalle por docente, el pie con la versión y el hash en cada página, y el bloque «Elaborado por», «Revisado por» y «Aprobado por» con el cargo, el nombre y el campo de firma.
3. El camino de fallo (SC-006), con otra base sin datos:
   - antes de aprobar, quitar la designación del Responsable de 2026-I. No hay operación para quitarla (solo para designar o reemplazar), así que se hace en la base del perfil local, nunca en la demo:

     ```bash
     docker compose -p tesis-prototipo-2026 exec -T postgres psql -U postgres -d mdd \
       -c "delete from roles.designacion_responsable where periodo = '2026-I'"
     ```

   - aprobar: la tarjeta dice «El PDF no se pudo generar» con el cargo que falta;
   - como `vicerrectorado.demo`, designar otra vez a `consolidacion.demo` (`designarResponsable`, desde su pantalla) y, como `consolidacion.demo`, pulsar «Generar el PDF»; medir desde que se abre la pantalla (menos de 1 minuto).
4. Limpiar con el proyecto nombrado (la demo, `mdd`, corre en el mismo equipo):

   ```bash
   docker compose -p tesis-prototipo-2026 down -v
   ```

## 4. Demo

En la demo, 2026-I está en EN_CONSOLIDACION. La comprobación es que nada cambia: no aparece la sección del documento firmable.

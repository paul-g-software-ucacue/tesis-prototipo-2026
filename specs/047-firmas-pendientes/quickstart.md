# Quickstart: Ver mis firmas pendientes y descargar el PDF vigente

## 1. Pruebas

```bash
./gradlew build integrationTest
./gradlew :apps:distributivo:test :apps:identidad:test   # Spring Modulith
./gradlew :servicios:usuarios:test --rerun               # contratos de Usuarios
```

SPA (en `node:24-alpine`): tipos, ESLint, Prettier y `vitest run`. Playwright con `firmas-pendientes` y `consolidado`.

## 2. Perfil local, de punta a punta

Con el perfil consolidado levantado (`docker compose -p tesis-prototipo-2026 up -d --build --wait`) y una base sin datos:

```bash
deploy/probar_documento_firmable.sh        # 8/8
```

## 3. A mano, en el perfil local (http://localhost:8081)

Con una base sin datos, dejar 2026-I en EN_REVISION_VRA con `docente.demo` designado:

```bash
MDD_CONSERVAR=1 deploy/probar_documento_firmable.sh
```

1. Como `vicerrectorado.demo`, aprobar el contenido en «Consolidado del periodo» y esperar «Documento firmable generado».
2. Como `vicerrectorado.demo`, abrir «Pendientes de firma»: debe decir «No tienes firmas pendientes.», porque no es su turno.
3. Como `consolidacion.demo`, igual: tiene el rol, pero no es el designado.
4. Como `docente.demo`, abrir «Pendientes de firma». Debe haber una fila: 2026-I, versión 1, «Elaborado por» y la fecha. Pulsar «Descargar el PDF» y medir desde el inicio de sesión (SC-001, menos de 1 minuto).
5. Comprobar que el archivo descargado tiene la huella de la tarjeta:

   ```bash
   sha256sum ~/Downloads/distributivo-2026-I-v1.pdf
   ```

6. Limpiar con el proyecto nombrado (la demo, `mdd`, corre en el mismo equipo):

   ```bash
   docker compose -p tesis-prototipo-2026 down -v
   ```

## 4. Demo

En la demo, 2026-I está en EN_CONSOLIDACION. La comprobación es solo de lectura:
- la entrada «Pendientes de firma» aparece para `consolidacion.demo`, `vicerrectorado.demo` y `rectorado.demo`, y dice «No tienes firmas pendientes.»;
- en la base, `usuarios.usuario` tiene la identificación de `rectorado.demo` y `vicerrectorado.demo`;
- `firma.firmante_esperado` está vacía.

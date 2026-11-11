# Quickstart: Aprobar el contenido y congelar la versión

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
deploy/probar_aprobacion.sh        # 7/7
```

## 3. A mano, en el perfil local (http://localhost:8081)

Con una base sin datos, dejar el distributivo en EN_REVISION_VRA:

```bash
MDD_CONSERVAR=1 deploy/probar_aprobacion.sh    # solo el paso 1, sin limpiar
```

1. Como `vicerrectorado.demo`, en «Consolidado del periodo»:
   - la tarjeta del flujo ofrece «Aprobar el contenido»;
   - el diálogo dice que la versión N queda congelada;
   - «Cancelar» no cambia nada.
2. «Aprobar y congelar», midiendo desde que se abre la pantalla (SC-003: menos de 1 minuto):
   - la etapa es «En firma»;
   - la tarjeta dice «Versión N congelada por vicerrectorado.demo el …», con la huella abreviada;
   - «Copiar la huella» copia los 64 caracteres.
3. Como `consolidacion.demo`: no hay botones de edición, y aparece «La versión N está congelada para la firma: ya no cambia.». «Descargar el informe» sigue.
4. En la bitácora: una entrada `CONTENIDO_APROBADO`.
5. Limpiar: bajar el perfil local con sus volúmenes, nombrando el proyecto para no tocar otro (la demo, `mdd`, corre en el mismo equipo):

   ```bash
   docker compose -p tesis-prototipo-2026 down -v
   ```

## 4. Demo

En la demo, 2026-I está en EN_CONSOLIDACION. La comprobación es que nada cambia:
- «Enviar a revisión», «Devolver parciales», «Volver a consolidar» y «Justificar» siguen;
- no hay aviso de versión congelada.

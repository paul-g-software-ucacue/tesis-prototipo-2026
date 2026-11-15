# Quickstart: Validar firmante, turno y vigencia, y completar la cadena

## 1. Pruebas automáticas

```bash
cd backend
./gradlew :servicios:firma:test :servicios:firma:integrationTest \
          :servicios:validacionocs:test :apps:distributivo:test :apps:distributivo:integrationTest
```

- **Dominio:** la normalización del nombre, las tres comprobaciones y `ProcesoAprobacion.completarFirmas`.
- **Aplicación:** la subida con el paso sin nombre y el aviso de la última firma; `CompletarFirmasServicio`.
- **Integración:**
  - la V5;
  - el nombre impreso guardado al generar;
  - en `apps:distributivo`, las tres firmas con la CA de prueba regenerada y el distributivo en EN_OCS.

## 2. Punta a punta en el perfil local

```bash
docker compose -p tesis-prototipo-2026 up -d --build --wait
deploy/probar_documento_firmable.sh
```

Debe terminar 13/13 (`contracts/demostracion.md`).

## 3. A mano, con FirmaEC y el certificado real del autor

1. En tu `.env` local (Git lo ignora), tu nombre tal como está en el CN de tu certificado:

   ```bash
   MDD_FIRMA_NOMBRE_LOCAL=<tu nombre completo>
   ```

2. Levantar el perfil con ese `.env` y dejar 2026-I en revisión:

   ```bash
   docker compose -p tesis-prototipo-2026 up -d --build --wait
   MDD_CONSERVAR=1 deploy/probar_documento_firmable.sh
   ```

3. Como `vicerrectorado.demo`, aprobar. El PDF debe imprimir tu nombre en los tres pasos.
4. **Rechazos:**
   - como `docente.demo`, descargar, firmar y subir: se acepta (F1);
   - como `vicerrectorado.demo`, subir el PDF firmado con un certificado ficticio de la CA de prueba (`firmante-1.p12`): «Firmante equivocado». Es opcional: necesita firmar con la herramienta de prueba.
5. **F2 y F3:** como `vicerrectorado.demo` y `rectorado.demo`, descargar, firmar con FirmaEC y subir. Después de F3, en menos de 30 s, «Consolidado del periodo» muestra EN_OCS (SC-002).
6. Validar el PDF final con FirmaEC: tres firmas válidas (SC-003).
7. Quitar `MDD_FIRMA_NOMBRE_LOCAL` del `.env` y limpiar:

   ```bash
   docker compose -p tesis-prototipo-2026 down -v
   ```

## 4. Demo

En la demo, 2026-I está en EN_CONSOLIDACION. La comprobación es solo de lectura:
- la V5 de `firma`;
- `firma.validacion_firma` vacía;
- `MDD_FIRMA_NOMBRE_LOCAL` no está definida en el contenedor `distributivo`.

# Evidencia: F03-H05

Registro de las comprobaciones de la historia (issue #39): registrar los cargos y los proyectos aprobados en el servicio de Docentes, con las referencias de Académico y los permisos de D-70 (RF-18, RN-12, RN-20, RN-25; D-72, D-73). Cada fila dice el comando y la salida resumida, o enlaza al run de GitHub Actions. Los datos son ficticios; las contraseñas de local y de la CI también.

## Entorno

| Pieza | Versión | Notas |
|---|---|---|
| Sistema | Fedora Linux 44 (SELinux en modo *enforcing*) | |
| Java | OpenJDK 25.0.4.1 | Gradle con el *wrapper* del repositorio. |
| Node | 24 (`~/.local/node-24`) | Vitest y Playwright. |
| Docker Engine | 29.8.2 (Fedora) | Con `sg docker -c '…'`. |

## Base: contrato, catálogo, referencias de Académico, dominio, migración y repositorios

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| Datos de prueba de las cargas 01, 03, 08 y 09 en Docentes | `01_estructura_organica.csv`, `03_ciclos_academicos.csv`, `08_cargos_autoridades.csv` (21 cargos) y `09_proyectos_practicas.csv` (11 proyectos), ficticios (RNF-SEG-06), copiados de `backend/servicios/cargas/src/test/resources/cargas/datos-de-prueba/` a `backend/servicios/docentes/src/test/resources/docentes/datos-de-prueba/`, junto a las 02, 04 y 06 | SHA-256 `314995c68138…`, `5217bb27074d…`, `4784c87fcbac…`, `fd955f518167…` | 2026-10-25 |

## Cargos (US1)

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|

## Requisitos del cargo (US2)

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|

## Proyectos (US3)

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|

## Asignables (US4)

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|

## Corregir y finalizar (US5)

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|

## Pantallas (US6)

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|

## Cierre

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|

## Equipo de demostración

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|

## Notas

- El número de gestores por tipo de IST (RN-10), el aviso del coordinador académico institucional (RN-09), las horas de gestión por cargo y el máximo de 16 h de docencia con proyecto son del motor (F06).

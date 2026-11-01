# Evidencia: F06-H03

Registro de las comprobaciones de la historia (issue #48): crear el distributivo, proponer un docente por asignatura-paralelo, justificar, ver el parcial y las horas reales (RF-24, RF-25, RF-56, D-19, RN-19). Los datos son ficticios.

## Entorno

| Pieza | Versión | Notas |
|---|---|---|
| Sistema | Fedora Linux 44 (SELinux en modo *enforcing*) | |
| Java | OpenJDK 25 | Gradle con el *wrapper* del repositorio. |
| Node | 24 (`~/.local/node-24`) | Vitest y Playwright. |
| Docker Engine | Fedora | Con `sg docker -c '…'`. |

## Crear el distributivo

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| El periodo debe estar listo | Sin preparar, cerrado o inexistente no se crea y no se guarda nada | `CrearDistributivoServicioTest`, `probar_propuestas.sh`, paso 2 | 2026-10-31 |
| Versión 1 y parciales | Los 7 parciales de las 7 carreras con oferta; una carrera con todo retirado no tiene parcial | `CrearDistributivoServicioTest`, `probar_propuestas.sh`, paso 3 | 2026-10-31 |
| Uno por periodo | Otro intento responde 409, también por la llave única de la base | `RepositorioDistributivosIntegrationTest`, `probar_propuestas.sh`, paso 3 | 2026-10-31 |
| El tablero | Dice que ya existe y deja de ofrecer crear | `Preparacion.test.tsx`, `e2e/preparacion.spec.ts` | 2026-10-31 |

## Proponer y justificar

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| Una sola propuesta (SC-001) | Proponer, reemplazar y quitar dejan una como mucho, y el registro guarda al docente anterior | `probar_propuestas.sh`, paso 4 | 2026-10-31 |
| Las tres justificaciones (SC-002) | Afinidad, disponibilidad en tronco común y excepción con texto; el texto de los otros dos casos no se conserva | `JustificacionTest`, `ProponerDocenteServicioTest`, `probar_propuestas.sh`, paso 5 | 2026-10-31 |
| Sin texto | 400 con el campo `texto`, sin guardar | `PropuestaContratoTest`, `probar_propuestas.sh`, paso 5 | 2026-10-31 |
| Sin horas suficientes (D-78) | Se guarda, con el aviso | `ProponerDocenteServicioTest`, `Oferta.test.tsx` | 2026-10-31 |
| Otra carrera | 403 sin datos; el alcance se mira en el permiso de escribir, no en los de leer | `PropuestaContratoTest`, `probar_propuestas.sh`, paso 9 | 2026-10-31 |
| El Vicerrector | Crea y ve, pero no propone: 403 | `probar_propuestas.sh`, paso 9 | 2026-10-31 |

## El parcial y las horas

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| Parcial completo (SC-003) | Un coordinador completa los 5 paralelos de su carrera | `probar_propuestas.sh`, paso 6 | 2026-10-31 |
| Paralelo retirado | La propuesta queda marcada y no cuenta; al volver a ofertar, cuenta de nuevo | `ConsultarParcialesServicioTest`, `probar_propuestas.sh`, paso 7 | 2026-10-31 |
| Horas reales (SC-004) | Con el mismo docente en dos carreras, las horas de cada coordinador coinciden con una suma hecha aparte: 6 h y 4 h en otras carreras. Cierra lo pendiente de F06-H02 | `HorasDelPeriodoTest`, `probar_propuestas.sh`, paso 8 | 2026-10-31 |

## Rendimiento y fallos

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| Crear y proponer (SC-006) | Crear, 0,09 s (meta: menos de 5 s); proponer, 0,10 s (meta: menos de 3 s) | `probar_propuestas.sh`, pasos 3 y 4 | 2026-10-31 |
| Sin el proceso `datos` (SC-007) | 504 del catálogo en 5,0 s (meta: menos de 10 s), sin guardar nada, y vuelve | `probar_propuestas.sh`, paso 10 | 2026-10-31 |
| Registros | La justificación no aparece en los logs ni en `generacion.registro_cambio` | `probar_propuestas.sh`, pasos 5 y 11 | 2026-10-31 |

## Cierre

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| Backend `./gradlew check` | 1675 pruebas unitarias, 0 fallas, 4 omitidas | Local | 2026-10-31 |
| `integrationTest` | Generación 14 de 14, Cargas y Académico sin fallas; 184 en los resultados locales, 0 fallas. En F06-H05 el total de 1690 ya las incluía: con ese conteo hoy son 1859 | Local | 2026-10-31 |
| Líneas por módulo | Generación 89,8 %, Cargas 93,3 %, Académico 82,5 %, `errores-nucleo` 92,3 %. Generación baja de 98,0 % porque sus repositorios JDBC solo se prueban en `integrationTest`, que JaCoCo no cuenta | JaCoCo, regenerado | 2026-10-31 |
| Roturas a propósito | 7 de 7 detectadas: retirados que suman, el paralelo mirado que suma, sin `exigirEditable`, crear en un periodo cerrado, alcance por lectura, el texto en el registro y parciales sin filtro. Se hizo porque en parte del dominio y la aplicación el código se escribió antes que sus pruebas, contra el orden del principio VIII | Local | 2026-10-31 |
| SPA | Vitest 336 de 336 con cobertura: 94,15 % de sentencias y 94,26 % de líneas; ESLint, Prettier y TypeScript sin observaciones | Local | 2026-10-31 |
| Playwright | 99 de 99, con axe sobre los diálogos nuevos | Local | 2026-10-31 |
| Scripts | Los 21 de `deploy/probar_*.sh` terminan en 0; el nuevo deja la base limpia | Local | 2026-10-31 |
| `probar_equipo.sh` | Código 0 | Local | 2026-10-31 |
| shellcheck y Gitleaks | Sin observaciones | Local | 2026-10-31 |
| CI del PR #169 | 12 de 12 en verde, con `test-integracion`, `sca`, los dos `sbom` y `perfil-consolidado`, que corre `probar_propuestas.sh` | GitHub Actions | 2026-10-31 |

## Equipo de demostración

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| El despliegue | `a27e14a` (#169): «success · Sano en 58 s». Los 8 contenedores sanos, con las 6 imágenes propias en `…-a27e14ac4dbe`, y la migración V3 de Generación aplicada | GitHub Deployments, `docker ps`, la base (solo lectura) | 2026-10-31 |
| Crear | Con `vicerrectorado.demo`, en 003, el tablero dice «ya tiene su distributivo, en la versión 1» y deja de ofrecer el botón. La base tiene 1 distributivo y 1 parcial en elaboración | Capturas del autor (no se copian al repositorio), la base | 2026-10-31 |
| Proponer | Con `coordinacion.demo`, «1 de 3 propuestas» y cada fila con su docente y «Disponibilidad», por ser tronco común | Capturas del autor | 2026-10-31 |
| Parcial completo | «3 de 3 propuestas» y «Parcial completo»; D, retirada, no cuenta | Capturas del autor | 2026-10-31 |
| Quitar | La fila queda sin docente y el contador baja a «2 de 3». La base tiene 2 propuestas sin texto, y el registro 1 distributivo creado, 4 propuestas guardadas y 2 quitadas: cuadra | Capturas del autor, la base | 2026-10-31 |
| «Horas a cubrir» | Se lee sin «del periodo» en la oferta del coordinador | Capturas del autor | 2026-10-31 |
| SC-005, menos de 2 minutos | Entre dos propuestas seguidas pasaron 12 s y 64 s, según las horas del registro. No se cronometró, y con 1 solo candidato: el criterio pide varios, así que queda indicado, no demostrado | La base | 2026-10-31 |

**No se vio en la demo**, porque tiene 1 docente y 1 carrera con oferta, de tronco común. Se comprobó con los 47 docentes de prueba en `probar_propuestas.sh`, en CI:

- varios candidatos;
- «Docentes bajo el mínimo» y la justificación escrita (la pantalla, solo en Vitest y Playwright);
- el aviso «Sin horas suficientes»;
- las horas en otras carreras;
- la marca «Paralelo retirado» sobre una propuesta.

## Notas

- Las pruebas de Vitest esperan hasta 5 s en lugar de 1 s (`src/pruebas/configuracion.ts`): con la consulta nueva del tablero, dos pruebas fallaban de forma intermitente con toda la suite en paralelo.
- En la oferta, el coordinador ve «Solo lectura: tu rol no cambia la oferta» aunque sí puede proponer. Es cierto para la oferta, pero se presta a confusión; queda anotado para ajustar el texto.
- El periodo cerrado se prueba con dobles (`ProponerDocenteServicioTest`, `CrearDistributivoServicioTest`), no en el script punta a punta.

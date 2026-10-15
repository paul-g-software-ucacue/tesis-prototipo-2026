// Escenario 1 de B00-H06 (FR-001 a FR-003, ADR-019): un cambio de contrato en un campo que la SPA
// usa rompe la compilación, y uno en un campo que no usa, no. Trabaja sobre una copia de
// contratos/ en una carpeta temporal y siempre deja los tipos del contrato real.
// Uso: npm run verificar:ruptura. Termina con 0 si todos los pasos pasan y con 1 si alguno falla.
import { spawnSync } from 'node:child_process';
import { cpSync, existsSync, mkdtempSync, readFileSync, rmSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { dirname, join, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const frontend = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const contratos = resolve(frontend, '..', 'contratos');
const generado = join(frontend, 'src', 'api', 'generado', 'usuarios.d.ts');
const bin = (nombre) => join(frontend, 'node_modules', '.bin', nombre);

let fallas = 0;
function verificar(numero, descripcion, condicion) {
  console.log(`${condicion ? 'OK   ' : 'FALLA'}  ${numero}. ${descripcion}`);
  if (!condicion) fallas += 1;
}

function ejecutar(comando, argumentos) {
  const resultado = spawnSync(comando, argumentos, { cwd: frontend, encoding: 'utf-8' });
  return { codigo: resultado.status, salida: `${resultado.stdout}${resultado.stderr}` };
}

function generarDesde(contrato) {
  const { codigo, salida } = ejecutar(bin('openapi-typescript'), [contrato, '--output', generado]);
  if (codigo !== 0) throw new Error(`openapi-typescript falló:\n${salida}`);
}

function compilar() {
  return ejecutar(bin('tsc'), ['--noEmit', '-p', 'tsconfig.app.json']);
}

// Cambia un fragmento exacto del contrato; si no está, el script falla en lugar de probar nada.
function cambiar(archivo, antes, despues) {
  const texto = readFileSync(archivo, 'utf-8');
  if (!texto.includes(antes)) throw new Error(`No se encontró en el contrato:\n${antes}`);
  writeFileSync(archivo, texto.replace(antes, despues));
}

const temporal = mkdtempSync(join(tmpdir(), 'mdd-ruptura-'));
const original = existsSync(generado) ? readFileSync(generado, 'utf-8') : null;

function copia() {
  rmSync(join(temporal, 'contratos'), { recursive: true, force: true });
  cpSync(contratos, join(temporal, 'contratos'), { recursive: true });
  return join(temporal, 'contratos', 'usuarios', 'openapi.yaml');
}

try {
  // 1. Cambio de nombre de un campo que la SPA usa: TokenSesion.token → jwt.
  let contrato = copia();
  cambiar(
    contrato,
    '      required: [token, tipo, venceEn]\n      properties:\n        token:',
    '      required: [jwt, tipo, venceEn]\n      properties:\n        jwt:',
  );
  generarDesde(contrato);
  let resultado = compilar();
  verificar(
    1,
    'Renombrar TokenSesion.token rompe la compilación',
    resultado.codigo !== 0 && resultado.salida.includes("'token'"),
  );

  // 2. Cambio de tipo de un campo que la SPA usa: TokenSesion.venceEn de texto a entero.
  contrato = copia();
  cambiar(
    contrato,
    '        venceEn:\n          type: string\n          format: date-time',
    '        venceEn:\n          type: integer',
  );
  generarDesde(contrato);
  resultado = compilar();
  verificar(
    2,
    'Cambiar TokenSesion.venceEn a entero rompe la compilación',
    resultado.codigo !== 0 && resultado.salida.includes('number'),
  );

  // 3. Cambio en un campo que la SPA no usa: Jwk.kty (la SPA no lee el JWKS).
  contrato = copia();
  cambiar(
    contrato,
    '      required: [kty, kid, use, alg, n, e]\n      properties:\n        kty:',
    '      required: [tipoDeLlave, kid, use, alg, n, e]\n      properties:\n        tipoDeLlave:',
  );
  generarDesde(contrato);
  resultado = compilar();
  verificar(
    3,
    'Renombrar Jwk.kty, que la SPA no usa, no rompe la compilación',
    resultado.codigo === 0,
  );
} catch (error) {
  console.log(`FALLA  ${error instanceof Error ? error.message : String(error)}`);
  fallas += 1;
} finally {
  // 4. Siempre se vuelve a los tipos del contrato real, y deben compilar.
  if (original === null) {
    generarDesde(join(contratos, 'usuarios', 'openapi.yaml'));
  } else {
    writeFileSync(generado, original);
  }
  rmSync(temporal, { recursive: true, force: true });
  verificar(4, 'Con el contrato real, la compilación pasa', compilar().codigo === 0);
}

console.log(
  fallas === 0
    ? 'Un cambio de contrato rompe la compilación: todos los pasos pasan.'
    : `Un cambio de contrato rompe la compilación: ${fallas} paso(s) fallan.`,
);
process.exit(fallas === 0 ? 0 : 1);

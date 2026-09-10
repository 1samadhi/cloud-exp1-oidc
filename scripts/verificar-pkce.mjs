/**
 * Verifica que el frontend inicie sesion con Authorization Code + PKCE y
 * captura las evidencias que pide la evaluacion.
 *
 * No basta con comprobar que el login funciona: eso tambien pasaria con un
 * flujo implicito o sin PKCE. Lo que demuestra PKCE es la peticion a
 * /authorize, que debe llevar code_challenge y code_challenge_method=S256.
 * El script la intercepta y la inspecciona.
 *
 * Uso:
 *   node scripts/verificar-pkce.mjs [url]
 *
 * Sin argumento usa la URL del stage. Las credenciales salen de .env
 * (ENTRA_USUARIO_ADMIN y ENTRA_PASSWORD_ADMIN).
 *
 * Requiere playwright. Si no esta instalado en este proyecto, se indica donde
 * buscarlo (los modulos ESM no respetan NODE_PATH):
 *   npm install playwright --prefix /tmp/pw
 *   PLAYWRIGHT_DIR=/tmp/pw/node_modules node scripts/verificar-pkce.mjs
 */
let chromium;
try {
  ({ chromium } = await import('playwright'));
} catch {
  const dir = process.env.PLAYWRIGHT_DIR;
  if (!dir) {
    console.error('No se encontro playwright. Instalalo con "npm install playwright"');
    console.error('o define PLAYWRIGHT_DIR con la ruta de un node_modules que lo tenga.');
    process.exit(2);
  }
  // playwright se distribuye como CommonJS: al importarlo por ruta las
  // exportaciones llegan bajo "default".
  const modulo = await import(`${dir}/playwright/index.js`);
  chromium = modulo.chromium ?? modulo.default?.chromium;
}
import { readFileSync, mkdirSync, writeFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const RAIZ = join(dirname(fileURLToPath(import.meta.url)), '..');
const EVIDENCIAS = join(RAIZ, 'docs', 'evidencias');
const URL_BASE = process.argv[2]
  || 'https://j37oj1wn16.execute-api.us-east-1.amazonaws.com/desarrollo/';

function leerEnv() {
  const env = {};
  try {
    for (const linea of readFileSync(join(RAIZ, '.env'), 'utf8').split('\n')) {
      const m = linea.match(/^(\w+)=(.*)$/);
      if (m) env[m[1]] = m[2];
    }
  } catch {
    // sin .env se usan las variables del entorno
  }
  return { ...env, ...process.env };
}

const env = leerEnv();
const USUARIO = env.ENTRA_USUARIO_ADMIN;
const CLAVE = env.ENTRA_PASSWORD_ADMIN;

if (!USUARIO || !CLAVE) {
  console.error('Faltan ENTRA_USUARIO_ADMIN y ENTRA_PASSWORD_ADMIN en .env');
  process.exit(2);
}

mkdirSync(EVIDENCIAS, { recursive: true });

const resultados = [];
function comprobar(descripcion, condicion, detalle = '') {
  resultados.push({ descripcion, ok: Boolean(condicion), detalle });
  const marca = condicion ? '\x1b[32mOK\x1b[0m   ' : '\x1b[31mFALLA\x1b[0m';
  console.log(`  ${marca} ${descripcion}${detalle ? '  ' + detalle : ''}`);
}

/**
 * Playwright trae su propia version de Chromium. Si no esta descargada, se
 * recurre al navegador del sistema, que sirve igual para este recorrido.
 */
async function abrirNavegador() {
  try {
    return await chromium.launch();
  } catch (e) {
    const ruta = process.env.CHROMIUM_PATH || '/usr/bin/chromium';
    console.log(`  (usando el Chromium del sistema: ${ruta})`);
    return await chromium.launch({ executablePath: ruta });
  }
}

const navegador = await abrirNavegador();
const contexto = await navegador.newContext({ viewport: { width: 1280, height: 900 } });
const pagina = await contexto.newPage();

// Se capturan las peticiones al endpoint de autorizacion y al de token
let urlAutorizacion = null;
let cuerpoToken = null;

pagina.on('request', (peticion) => {
  const u = peticion.url();
  if (u.includes('/oauth2/v2.0/authorize')) urlAutorizacion ??= u;
  if (u.includes('/oauth2/v2.0/token') && peticion.method() === 'POST') {
    cuerpoToken ??= peticion.postData();
  }
});

let codigoSalida = 0;
try {
  console.log(`\nVerificando ${URL_BASE}\n`);

  await pagina.goto(URL_BASE, { waitUntil: 'networkidle', timeout: 60000 });
  await pagina.screenshot({ path: join(EVIDENCIAS, '01-inicio.png'), fullPage: true });
  comprobar('La aplicacion carga', await pagina.locator('text=Pedidos360').first().isVisible());

  // ---- el flujo de login ----
  await pagina.getByRole('button', { name: /Iniciar sesion con Microsoft/i }).click();
  await pagina.waitForURL(/login\.microsoftonline\.com/, { timeout: 60000 });
  await pagina.screenshot({ path: join(EVIDENCIAS, '02-login-microsoft.png'), fullPage: true });

  // ---- lo que demuestra PKCE ----
  comprobar('Se llamo al endpoint de autorizacion de Entra ID', Boolean(urlAutorizacion));
  const parametros = new URL(urlAutorizacion).searchParams;
  comprobar('response_type=code (Authorization Code, no implicito)',
    parametros.get('response_type') === 'code',
    `response_type=${parametros.get('response_type')}`);
  comprobar('La peticion incluye code_challenge',
    Boolean(parametros.get('code_challenge')),
    `${(parametros.get('code_challenge') || '').slice(0, 16)}...`);
  comprobar('code_challenge_method=S256',
    parametros.get('code_challenge_method') === 'S256',
    `method=${parametros.get('code_challenge_method')}`);
  comprobar('El cliente no envia secreto',
    !parametros.get('client_secret'));

  // ---- el detalle de la peticion de autorizacion, como evidencia escrita ----
  const detalle = [...parametros.entries()]
    .filter(([k]) => !['nonce', 'state', 'client-request-id', 'x-client-SKU',
                       'x-client-VER', 'client_info'].includes(k))
    .map(([k, v]) => `${k} = ${v.length > 90 ? v.slice(0, 90) + '...' : v}`)
    .join('\n');
  writeFileSync(join(EVIDENCIAS, '03-peticion-authorize.txt'),
    `Peticion al endpoint de autorizacion de Microsoft Entra ID\n` +
    `Capturada por scripts/verificar-pkce.mjs el ${new Date().toISOString()}\n\n` +
    `${new URL(urlAutorizacion).origin}${new URL(urlAutorizacion).pathname}\n\n${detalle}\n\n` +
    `code_challenge_method=S256 confirma Authorization Code con PKCE:\n` +
    `el navegador genera un code_verifier al azar, envia solo su hash SHA-256\n` +
    `como code_challenge, y presenta el original al canjear el codigo. Un codigo\n` +
    `interceptado no sirve sin el verifier.\n`);
  console.log('  captura  docs/evidencias/03-peticion-authorize.txt');

  // ---- completar el login ----
  await pagina.fill('input[type="email"]', USUARIO);
  await pagina.click('input[type="submit"]');
  await pagina.waitForSelector('input[type="password"]', { timeout: 30000 });
  await pagina.fill('input[type="password"]', CLAVE);
  await pagina.click('input[type="submit"]');

  // "¿Quiere mantener la sesion iniciada?" aparece a veces
  try {
    await pagina.waitForSelector('input[type="submit"]', { timeout: 8000 });
    if (/Stay signed in|mantener la sesion/i.test(await pagina.content())) {
      await pagina.click('input[value="No"], input[type="submit"]');
    }
  } catch {
    // no aparecio, se sigue
  }

  await pagina.waitForURL((u) => !u.href.includes('login.microsoftonline.com'), { timeout: 60000 });
  await pagina.waitForLoadState('networkidle');

  comprobar('El canje del codigo incluye code_verifier',
    Boolean(cuerpoToken && cuerpoToken.includes('code_verifier')));
  comprobar('El canje usa grant_type=authorization_code',
    Boolean(cuerpoToken && cuerpoToken.includes('grant_type=authorization_code')));

  const nombreVisible = await pagina.locator('.usuario').first().textContent().catch(() => null);
  comprobar('Sesion iniciada en la aplicacion', Boolean(nombreVisible), nombreVisible || '');
  await pagina.screenshot({ path: join(EVIDENCIAS, '04-sesion-iniciada.png'), fullPage: true });

  // ---- recorrido con sesion activa ----
  for (const [ruta, archivo] of [
    ['catalogo', '05-catalogo.png'],
    ['pedidos', '06-pedidos.png'],
    ['perfil', '07-claims-del-token.png']
  ]) {
    await pagina.getByRole('link', { name: new RegExp(ruta, 'i') }).first().click();
    await pagina.waitForLoadState('networkidle');
    await pagina.waitForTimeout(1500);
    await pagina.screenshot({ path: join(EVIDENCIAS, archivo), fullPage: true });
    console.log(`  captura  docs/evidencias/${archivo}`);
  }



} catch (e) {
  console.error(`\n  Error durante la verificacion: ${e.message}`);
  await pagina.screenshot({ path: join(EVIDENCIAS, 'error.png'), fullPage: true }).catch(() => {});
  console.error('  Se guardo docs/evidencias/error.png con el estado de la pagina');
  codigoSalida = 1;
} finally {
  await navegador.close();
}

const fallidas = resultados.filter((r) => !r.ok).length;
console.log('\n======================================');
console.log(`  correctas: ${resultados.length - fallidas}   fallidas: ${fallidas}`);
console.log('======================================\n');
process.exit(fallidas > 0 ? 1 : codigoSalida);

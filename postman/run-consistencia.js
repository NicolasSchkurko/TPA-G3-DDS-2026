/*
 * Corredor minimo para "DonaTrack - Consistencia API vs BD.postman_collection.json".
 *
 * Por que existe: newman no esta instalado en esta maquina y no queremos agregar
 * una dependencia npm solo para validar la coleccion. Esto implementa el subconjunto
 * de la API de Postman que la coleccion realmente usa (pm.test, pm.expect con
 * eql/be.an/be.a/have.property/be.at.most, pm.response.to.have.status,
 * pm.response.json y pm.collectionVariables), ejecuta las requests en orden contra
 * el stack levantado y corre los scripts de test tal cual estan en el JSON.
 *
 * Uso:
 *   node postman/run-consistencia.js
 *   node postman/run-consistencia.js --donantes 4 --entidades 2 --donaciones 4
 *
 * Los tres flags son opcionales y equivalen a cargar a mano las variables
 * sqlDonantesActivos / sqlEntidadesActivas / sqlDonaciones. Sin ellos, los
 * chequeos contra la base se saltean (igual que en Postman).
 *
 * Esto NO reemplaza al Collection Runner, es para verificar en CI/consola que la
 * coleccion pasa. La coleccion sigue siendo la fuente de verdad.
 */

'use strict';

const fs = require('fs');
const path = require('path');
const assert = require('assert');

const COLLECTION = path.join(__dirname, 'DonaTrack - Consistencia API vs BD.postman_collection.json');

// ---------------------------------------------------------------- CLI
const argv = process.argv.slice(2);
function flag(nombre) {
  const i = argv.indexOf('--' + nombre);
  return i !== -1 && argv[i + 1] ? argv[i + 1] : null;
}

// ---------------------------------------------------------------- chai minimo
// Solo lo que la coleccion usa. Cualquier otra cosa explota a proposito, para no
// dar por pasado un assert que en realidad no se evaluo.
function expect(actual, mensaje) {
  const etiqueta = mensaje ? mensaje + ': ' : '';

  const tipoDe = (v) => (Array.isArray(v) ? 'array' : v === null ? 'null' : typeof v);

  // Mismo criterio que chai: string/array por length, objeto por cantidad de claves.
  const vacio = (v) => {
    if (v === null || v === undefined) return true;
    if (typeof v === 'string' || Array.isArray(v)) return v.length === 0;
    if (typeof v === 'object') return Object.keys(v).length === 0;
    return false;
  };

  const nucleo = {
    eql(esperado) {
      assert.deepStrictEqual(actual, esperado,
        etiqueta + 'esperaba ' + JSON.stringify(esperado) + ' y recibi ' + JSON.stringify(actual));
      return this;
    },
    an(tipo) {
      assert.strictEqual(tipoDe(actual), tipo,
        etiqueta + 'esperaba un ' + tipo + ' y recibi ' + tipoDe(actual));
      return this;
    },
    property(nombre) {
      assert.ok(actual && Object.prototype.hasOwnProperty.call(actual, nombre),
        etiqueta + 'esperaba que tuviera la propiedad "' + nombre + '"');
      return this;
    },
    get true() {
      assert.strictEqual(actual, true, etiqueta + 'esperaba true y recibi ' + JSON.stringify(actual));
      return this;
    },
    get most() {
      return (limite) => {
        assert.ok(actual <= limite, etiqueta + 'esperaba <= ' + limite + ' y recibi ' + actual);
        return nucleo;
      };
    },
    get empty() {
      assert.ok(vacio(actual), etiqueta + 'esperaba algo vacio y recibi ' + JSON.stringify(actual));
      return this;
    },
    // Solo se niega `.empty`, que es lo unico que la coleccion encadena con `.not`.
    // Para cualquier otra cosa conviene que explote antes que dar un falso ok.
    get not() {
      return {
        get empty() {
          assert.ok(!vacio(actual),
            etiqueta + 'esperaba algo NO vacio y recibi ' + JSON.stringify(actual));
          return nucleo;
        },
      };
    },
  };
  nucleo.a = nucleo.an;
  // `.to`, `.be`, `.at`, `.have` y `.and` son azucar encadenable de chai:
  // devuelven el mismo nucleo para que siga la cadena.
  nucleo.to = nucleo;
  nucleo.be = nucleo;
  nucleo.at = nucleo;
  nucleo.have = nucleo;
  nucleo.and = nucleo;
  return nucleo;
}

// ---------------------------------------------------------------- estado
const coleccion = JSON.parse(fs.readFileSync(COLLECTION, 'utf8'));
const vars = new Map();
(coleccion.variable || []).forEach((v) => vars.set(v.key, v.value));

const desdeCli = {
  sqlDonantesActivos: flag('donantes'),
  sqlEntidadesActivas: flag('entidades'),
  sqlDonaciones: flag('donaciones'),
};
Object.keys(desdeCli).forEach((k) => {
  if (desdeCli[k] !== null) vars.set(k, desdeCli[k]);
});

function resolver(plantilla) {
  return String(plantilla).replace(/\{\{([^}]+)\}\}/g, (_, nombre) => {
    const v = vars.get(nombre.trim());
    return v === undefined || v === null ? '' : String(v);
  });
}

// ---------------------------------------------------------------- resultados
let totalTests = 0;
const fallos = [];
const avisos = [];
const saltados = [];

function construirPm(respuesta, nombreRequest) {
  return {
    test(nombre, fn) {
      totalTests += 1;
      try {
        fn();
        console.log('    ok   ' + nombre);
      } catch (e) {
        console.log('    FAIL ' + nombre);
        console.log('         ' + e.message.split('\n')[0]);
        fallos.push({ request: nombreRequest, test: nombre, error: e.message });
      }
    },
    expect,
    response: {
      code: respuesta.code,
      json: () => JSON.parse(respuesta.body),
      text: () => respuesta.body,
      responseTime: respuesta.ms,
      to: {
        have: {
          status(esperado) {
            assert.strictEqual(respuesta.code, esperado,
              'esperaba HTTP ' + esperado + ' y recibi ' + respuesta.code +
              (respuesta.code === 403
                ? ' (403 = IP no permitida, revisar POSTMAN_HOST_IP en el .env)'
                : ''));
          },
        },
      },
    },
    collectionVariables: {
      get: (k) => vars.get(k),
      set: (k, v) => vars.set(k, v),
      has: (k) => vars.has(k),
      unset: (k) => vars.delete(k),
    },
    environment: {
      get: (k) => vars.get(k),
      set: (k, v) => vars.set(k, v),
    },
    info: { requestName: nombreRequest },
  };
}

// ---------------------------------------------------------------- http
function pedir(url) {
  const { request } = url.startsWith('https') ? require('https') : require('http');
  return new Promise((resolve, reject) => {
    const comenzo = Date.now();
    const req = request(url, { method: 'GET', headers: { Accept: 'application/json' } }, (res) => {
      let cuerpo = '';
      res.setEncoding('utf8');
      res.on('data', (c) => { cuerpo += c; });
      res.on('end', () => resolve({ code: res.statusCode, body: cuerpo, ms: Date.now() - comenzo }));
    });
    req.on('error', reject);
    req.setTimeout(15000, () => req.destroy(new Error('timeout de 15s')));
    req.end();
  });
}

function urlDe(item) {
  const u = item.request.url;
  if (typeof u === 'string') return resolver(u);
  if (u.raw) return resolver(u.raw);
  throw new Error('no supe armar la URL de ' + item.name);
}

// Aplana carpetas conservando el orden de la coleccion, que es significativo:
// las requests de listado guardan variables que usan las de detalle.
function aplanar(items, carpeta) {
  return (items || []).reduce((acc, it) => {
    if (it.item) return acc.concat(aplanar(it.item, it.name));
    return acc.concat([{ item: it, carpeta: carpeta || '(raiz)' }]);
  }, []);
}

// ---------------------------------------------------------------- corrida
(async () => {
  const planas = aplanar(coleccion.item);
  console.log('Coleccion: ' + coleccion.info.name);
  console.log('Requests:  ' + planas.length);
  const faltantes = ['sqlDonantesActivos', 'sqlEntidadesActivas', 'sqlDonaciones']
    .filter((k) => !vars.get(k));
  console.log(faltantes.length
    ? 'Base:      sin numeros de SQL (' + faltantes.join(', ') + ') -> esos chequeos se saltean'
    : 'Base:      comparando contra SQL cargado por CLI');
  console.log('');

  let carpetaActual = null;
  for (const { item, carpeta } of planas) {
    if (carpeta !== carpetaActual) {
      carpetaActual = carpeta;
      console.log('# ' + carpeta);
    }

    const url = urlDe(item);
    // Una request con {{var}} sin resolver quedaria con un id vacio en la URL y
    // devolveria 404/400 confundiendo el diagnostico. Mejor saltearla explicito.
    if (/\/\/|\/$/.test(url.replace(/^https?:\/\//, '').replace(/\?.*$/, ''))) {
      console.log('  -- ' + item.name + ' (salteada: falto una variable de id)');
      saltados.push(item.name);
      continue;
    }

    let respuesta;
    try {
      respuesta = await pedir(url);
    } catch (e) {
      console.log('  !! ' + item.name + ' -> error de red: ' + e.message);
      fallos.push({ request: item.name, test: '(red)', error: e.message });
      continue;
    }

    console.log('  -> ' + item.name + ' [' + respuesta.code + ', ' + respuesta.ms + 'ms]');

    const guion = (item.event || [])
      .filter((e) => e.listen === 'test')
      .map((e) => e.script.exec.join('\n'))
      .join('\n');
    if (!guion.trim()) continue;

    const pm = construirPm(respuesta, item.name);
    const consola = {
      log: (...a) => avisos.push('[log] ' + a.join(' ')),
      warn: (...a) => avisos.push('[warn] ' + a.join(' ')),
      error: (...a) => avisos.push('[error] ' + a.join(' ')),
      info: (...a) => avisos.push('[info] ' + a.join(' ')),
      table: (d) => avisos.push('[table] ' + JSON.stringify(d)),
    };
    try {
      // eslint-disable-next-line no-new-func
      new Function('pm', 'console', 'responseBody', guion)(pm, consola, respuesta.body);
    } catch (e) {
      console.log('    FAIL (el script de test se rompio) ' + e.message);
      fallos.push({ request: item.name, test: '(script)', error: e.message });
    }
  }

  if (avisos.length) {
    console.log('\n---- salida de consola de los scripts ----');
    avisos.forEach((a) => console.log(a));
  }

  console.log('\n---- resumen ----');
  console.log('asserts corridos: ' + totalTests);
  console.log('fallidos:         ' + fallos.length);
  if (saltados.length) console.log('requests salteadas: ' + saltados.join(', '));
  if (fallos.length) {
    console.log('');
    fallos.forEach((f) => console.log('FAIL [' + f.request + '] ' + f.test + '\n  ' + f.error));
  }
  process.exit(fallos.length ? 1 : 0);
})();

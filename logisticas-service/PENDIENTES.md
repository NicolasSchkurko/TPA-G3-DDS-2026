# Pendientes técnicos de `logisticas-service`

Registro de problemas conocidos del servicio, con el motivo y la propuesta de arreglo para
que no se pierdan de vista al crecer el código.

**Están ordenados de más urgente a menos urgente**, no por número de punto. El número es un
ID estable y no se renumera nunca, así que quedan huecos. Un punto corregido se borra de esta
lista y pasa a la sección [Corregidos](#corregidos) del final.

El orden no es el de la severidad declarada en cada punto sino el del daño real: cuánto se
rompe cuando pasa, y qué tan fácil es que pase.

| # | Punto | Por qué está acá |
|---|---|---|
| 1 | 34 | `UnidadDeMedida` persiste constantes estáticas: cada reinicio duplica las filas |
| 2 | 37 | El CRUD de camiones/choferes devuelve 500 para errores de validación |
| 3 | 20 | Verificado: logística no invoca a `donaciones-service` ni incentivos ni habla con notificaciones |
---

## 20. Verificado: logística no invoca a `donaciones-service` ni a `incentivos`, y no habla con notificaciones

**Estado:** verificado, sin cambios necesarios
**Severidad:** informativa
**Archivos:** todo `logisticas-service/src/main`

### Qué exigía el enunciado

> *3. El servicio de logística no debe invocar los servicios de donaciones ni incentivos sino
> dejar disponible la información.*
>
> *4. El servicio de logística no debe comunicarse con el servicio de notificaciones.*

### Cómo se verificó

Se revisaron las cuatro vías por las que un servicio podría invocar a otro: imports de código,
dependencias de Maven, URLs configuradas y llamadas HTTP o por broker.

| Vía | Resultado |
|---|---|
| Imports de `...donaciones.` o `...incentivos.` en logística | **ninguno** |
| Imports de `...notificaciones.` en logística | **ninguno** |
| URLs de esos servicios en `application.properties` | **ninguna** (solo `spring.datasource.url`) |
| `RestTemplate`, `WebClient`, `FeignClient`, `HttpClient` | solo el `HttpClient` del proveedor externo de ruteo |
| Exchanges ajenos declarados en logística | **ninguno** |

Las únicas dos URLs literales del módulo son:

- `ProveedorRutasExternoSimulado` → `http://localhost:8086/api/PlanificacionRutas/callback`,
  que es **a sí mismo**, el callback que el enunciado exige en el punto 1 de implementación.
- `GestorPublicacionEventos` → `https://donaciones-app.example.com/seguimiento/`, una plantilla
  de texto para armar el enlace de seguimiento que pide el caso de "inicio de ruta". No es una
  llamada.

### Cómo se cumple en la práctica

Logística **deja disponible la información** publicando eventos en
`logistica.eventos.exchange` (routing key `logistica.evento`). No llama a nadie para que notifique:
`donaciones-service` está suscrito a esa cola y es quien dispara las notificaciones, porque es
el que conoce a los donantes, las entidades y los administradores.

Eso es justamente lo que hace posible cumplir los tres casos de notificación exigidos
(inicio de ruta, entrega realizada, entrega no satisfactoria) sin violar la prohibición: la
dirección del flujo es invertida respecto de lo que se suele hacer.

Verificado en vivo: se publicó un evento a mano por la management API de Rabbit y el listener
de `donaciones-service` lo procesó (`Evento 9999 procesado`, hilo `rabbit-simple-0`).

### Lo que queda: una dependencia sin uso — RESUELTO el 2026-10-07

`logisticas-service/pom.xml` declaraba `notificaciones-service` como dependencia `compile`, pero
**el módulo no importa ni una sola clase de ese artefacto**. Era una dependencia que no se usaba
y tenía un costo real: obligaba a compilar con `-am` o a tener el jar instalado en el `.m2`, y
rompía `mvn package -pl logisticas-service` con "Could not find artifact".

Es exactamente el punto 4 del backlog de `donaciones-service`, del lado de logística. Se
anotaba acá porque el enunciado pide que la separación sea real, y una dependencia de Maven entre
dos microservicios contradice esa separación aunque no se use.

**Se quitó** la declaración del `pom.xml` el 2026-10-07, después de confirmar que no hay ni un
import de notificaciones en el módulo (el único match era la palabra en un comentario). Se
verificó en vivo: `mvn -pl logisticas-service test` pasaba de `BUILD FAILURE` por no encontrar
el artefacto a `BUILD SUCCESS`, sin necesitar `-am`.

La verificación de que la separación es real ya está arriba, en la tabla: cero imports, cero
llamadas, cero dependencias.

---

## 34. `UnidadDeMedida` persiste constantes estáticas: cada reinicio duplica las filas

**Estado:** abierto
**Severidad:** media
**Archivos:** `models/entities/ItemEntrega/UnidadDeMedida.java:16-23`, `services/EntregaService.java:233-234`

### Qué pasa

`UNIDADES`, `KILOGRAMOS` y `LITROS` son objetos Java `static final` (líneas 16-18) con
`@GeneratedValue(strategy = GenerationType.UUID)`. En `itemsEnUnaTransaccion` se hace
`repoUnidades.save(unidadDominio)` (línea 234) con esa constante: la primera vez su
`idUnidad` es `null`, Hibernate inserta **una fila nueva** y le asigna un UUID.

Dentro de una misma JVM la constante sobrevive, así que los mensajes siguientes reutilizan la
fila. Pero:

- **Cada reinicio o redeploy** genera constantes nuevas con id `null` → otras 3 filas
  "Unidades", "Kilogramos", "Litros" en `unidad_medida`.
- **Cada una de las N instancias de logística** (el requisito del enunciado) tiene sus
  propias constantes → cada instancia inserta las suyas.

La tabla se llena de filas repetidas en proporción a los arranques, y dos instancias pueden
estar apuntando a filas distintas para "Kilogramos": el catálogo deja de ser único, que es
justamente lo que un catálogo tiene que ser.

### Cómo se dispara

Reiniciar el servicio después de haber procesado algunos mensajes y correr
`SELECT nombre, COUNT(*) FROM unidad_medida GROUP BY nombre` → `Kilogramos: 2, 3, ...`.

### Propuesta

Resolver la unidad por nombre (`findByNombre`) antes de insertar, o fijar el id de las tres
filas catálogo en el `schema.sql` de modo que el `save` siempre haga merge de la misma fila.
Mejor todavía: cargar el catálogo una sola vez al inicializar y que el servicio solo lea.

---

## 37. El CRUD de camiones/choferes devuelve 500 para errores de validación

**Estado:** abierto
**Severidad:** baja
**Archivos:** `controllers/CamionController.java:47-59`, `controllers/ChoferController.java:51-58`, `services/CamionService.java:43-52`, `services/ChoferService.java`

### Qué pasa

`POST /camiones` y `POST /choferes` no tienen `try/catch` ni `@Valid`, y los PUT/PATCH solo
atrapan `IllegalArgumentException`. Cualquier otra excepción de validación o de la base se
escapa y responde **500**:

- `POST /camiones` sin los campos obligatorios (`capacidad_volumen_m3`, `altura_m`,
  `capacidad_carga_kg` son `nullable = false`) → `DataIntegrityViolationException` → 500.
- `POST /choferes` sin `nombre` (`nullable = false`) → 500.
- `PUT` con campos null sobre columnas `NOT NULL` → 500 (mismo camino que el punto 29).

No hay forma de distinguir "dato inválido" (400) de "conflicto" (409) de "no existe" (404):
lo que no entra en el `catch` de `IllegalArgumentException` es 500, y el cliente se queda sin
saber qué corregir.

### Cómo se dispara

```bash
curl -X POST http://localhost:8086/api/camiones -H "Content-Type: application/json" -d '{}'
# 500
```

### Propuesta

`@Valid` con anotaciones en los DTO, y un `@ControllerAdvice` que traduzca
`DataIntegrityViolationException` en 409/400 con un mensaje entendible, en lugar de que cada
controlador decida con su propio `try`.

---

## Corregidos

### 1. El módulo no tiene un solo test

**Estado:** corregido el 2026-10-07 en `1e75220`
**Severidad:** alta
**Archivos:** `logisticas-service/src/test/`

`logisticas-service/src/test` no existía: el módulo era el único del proyecto sin cobertura
y justamente el que recibió un merge sin resolver. Con `1e75220` llegaron los primeros tests,
y no son decorativos: `EntregaServiceIdempotenciaTest` tiene 8 tests que miden la idempotencia
del registro de donaciones, y se verificó que tienen dientes (neutralizando la guarda a
propósito, 3 de 8 fallaron). También hay tests que levantan el contexto, que es la red que
falta contra un JPQL mal formado o una entidad fuera de la unidad de persistencia.

Lo que queda —cobertura de dominio, de la API y de los planificadores— es una mejora
continua, no el punto crítico que era: ya existe al menos una red que corre en cada build.

### 15. Registrar una donación duplica la dirección N veces

**Estado:** corregido el 2026-10-07 en `1e75220`
**Severidad:** media
**Archivos:** `.../services/EntregaService.java`

El `for` de bienes construía la dirección adentro: con una donación de 3 bienes quedaban 3
países, 3 provincias, 3 ciudades y 3 direcciones con el mismo contenido, y el `save` de
`Entidad` (clave natural) terminaba apuntando a la última dirección, dejando las otras dos
huérfanas.

**Qué se resolvió:** el catálogo salió del `for`: ahora `procesarPeticion` resuelve la
entidad una sola vez por mensaje, antes de registrar los items. Junto con el `@Version` de
esas entidades, dos instancias que lleguen a la vez reciben `OptimisticLockingFailureException`
en vez de pisarse. Hay test que lo mide.

**Residual:** la duplicación **por mensaje** sigue abierta y quedó registrada como el
punto 35: los `save` de país/provincia/ciudad corren antes de buscar si la entidad ya existe.

### 16. `procesarPeticion` no valida el payload

**Estado:** corregido el 2026-10-07 en `1e75220`
**Severidad:** media
**Archivos:** `.../services/EntregaService.java`, `.../RabbitMQ/DonacionListener.java`

El método validaba `request` y nada más: `donacionResumen` null, `entidadBeneficiaria` null,
listas de longitudes distintas o `null` revientaban con NPE/IndexOutOfBounds a mitad de la
escritura, y la base quedaba a medias.

**Qué se resolvió:** validaciones arriba, antes de tocar la base: resumen no nulo, entidad no
nula, `bienes`/`idsDonaciones` no nulos y de la misma longitud, unidad no nula y soportada, y
dirección no nula en `resolverEntidad`. Todas lanzan `IllegalArgumentException`, que el
controller traduce en 400 y el listener descarta con warning sin gastar reintentos.

### 19. La validación de la justificación de una entrega fallida está invertida

**Estado:** cerrado el 2026-10-07 como **falso positivo** (verificado contra el código actual)
**Severidad:** crítica (declarada)
**Archivo:** `.../services/EntregaService.java:334-336`

El punto sostenía que `comprobarExistencia` devuelve `true` cuando el texto **sí** está
escrito, con lo cual el `case "NO_RECIBIDA"` rechazaría las justificadas y aceptaría las que
faltan. Verificado contra el código, la premisa es falsa:

```java
private boolean comprobarExistencia(String elemento){
    return (elemento == null || elemento.trim().isEmpty());
}
```

Devuelve `true` cuando el texto **falta**, o sea que
`if (comprobarExistencia(request.getJustificacion())) throw "Se requiere justificar..."` lanza
exactamente cuando no hay justificación, que es lo que dice el mensaje. El caso `ENTREGADA`
(línea 298, foto) usa el mismo patrón con el mismo sentido y también es correcto.

El helper tiene un nombre engañoso —comprobarExistencia devuelve true cuando **no** existe—,
pero la lógica de los dos call-sites es la correcta y no se tocó ningún código. Comportamiento
ya correcto desde `cb8910a`.

---

### El módulo se commiteó con marcadores de conflicto de merge sin resolver

**Estado:** corregido
**Severidad:** crítica
**Archivo:** `.../gestores/GestorPublicacionEventos.java`

Traía los marcadores `<<<<<<< HEAD`, `=======` y `>>>>>>>` de dos ramas distintas en el mismo
archivo, y así quedó commiteado: el módulo entero no compilaba.

**Qué se resolvió:** se conservó la versión de `GestorPublicacionEventos` y se descartó la de
`GestorEventos`, porque `EntregaService` y `RutaService` consumen la primera. Los dos métodos de
la otra (`buscarEventos` y `guardarEvento`) no los usa nadie en el módulo.

### `RepositorioCamiones` y `RepositorioChoferes` duplicados en dos paquetes

**Estado:** corregido
**Severidad:** alta
**Archivos:** `.../repositories/RepositorioCamiones.java`, `.../repositories/RepositorioChoferes.java`

El merge dejó los mismos repositorios en el paquete plano y en subpaquete. Los consumidores
importaban el del paquete plano, pero usaban métodos que **solo existen en la versión de
subpaquete** (`findByChofer`, `actualizarEstado`, `findByEstado`, `findByIdDonacion`): el módulo
no compilaba.

**Qué se resolvió:** se consolidó hacia los subpaquetes, que son superconjunto, y se borraron los
duplicados del paquete plano. Además, borrar los duplicados evita que Spring Data registre dos
beans con el mismo nombre.

### Cinco `DataSourceConfig` apuntaban a cinco bases distintas

**Estado:** corregido
**Severidad:** crítica
**Archivos:** `.../config/CamionesDataSourceConfig.java`, `ChoferesDataSourceConfig.java`,
`EventosDataSourceConfig.java`, `ItemsDataSourceConfig.java`, `RutasDataSourceConfig.java`

Cada uno creaba su propio `DataSource`, `EntityManagerFactory` y `TransactionManager`, apuntando
a una base distinta (`camiones`, `choferes`, `eventos`, `items`, `rutas`) con **credenciales
hardcodeadas** que ignoraban `DB_URL`/`DB_USERNAME`/`DB_PASSWORD`.

**Por qué no podía funcionar:** el modelo es una base por agregado, pero los agregados se
referencian entre sí. `Ruta` referencia `Camion` y `Parada`; `ItemEntrega` referencia `Parada`,
`UnidadDeMedida`, `Entidad` y `EventoLogistica`; `Camion` referencia `Chofer`. Al arrancar se
moría con `AnnotationException: Association 'Camion.chofer' targets an unknown entity`, porque
`Chofer` no estaba en la unidad de persistencia de `Camion`.

**Qué se resolvió:** se borraron los cinco. Un solo `DataSource` sobre la base `logisticas`, con
las entidades y los repositorios donde ya estaban. Las 12 tablas se crean ahí.

**Efecto secundario:** las credenciales dejaron de estar hardcodeadas, así que el servicio
ahora arranca dentro de Docker con las variables de entorno.

### El binding de `solicitudEventosQueue` usaba el exchange equivocado

**Estado:** corregido
**Severidad:** media
**Archivo:** `.../config/RabbitMQConfig.java`

Ataba la cola al `donaciones.exchange` en vez de al `logisticas.exchange`. Funcionaba solo
porque `donaciones-service` declara el mismo binding contra el exchange correcto y el broker
acumula las dos declaraciones: levantando logística sola, las solicitudes de eventos quedaban
sin ruta.

**Qué se resolvió:** el binding usa el exchange de integración, que es al que publica
`LogisticaPollingScheduler`. (Tanto ese binding como el `LogisticaPollingScheduler` se eliminaron
después, cuando se quitó el sondeo: ver la última entrada de `Corregidos`.)

### `SolicitudEventosListener` hacía request/response por cola

**Estado:** corregido
**Severidad:** media
**Archivo:** `.../RabbitMQ/SolicitudEventosListener.java`

Publicaba la respuesta en el exchange para que volviera a la cola del que preguntó. Eso convierte
el broker en un request/response: necesita dos colas y dos bindings por consumidor, y se rompe
entero si el que preguntó se cae antes de leer la respuesta.

**Qué se cambió:** el listener deja de publicar la respuesta. La trazabilidad queda disponible
por HTTP en `GET /api/eventos`, que es lo que pide el enunciado al describir el despliegue de
logística como accesible por sus URIs. El polling quedó como red de contención, pero se eliminó
después: sin respuesta no recuperaba nada (ver la última entrada de `Corregidos`).

### `DonacionListener` se tragaba todos los errores

**Estado:** corregido
**Severidad:** media
**Archivo:** `.../RabbitMQ/DonacionListener.java`

El `catch (Exception)` con `System.err.println` descartaba el mensaje sin dejar rastro: una donación
se perdía sin registrar por qué.

**Qué se cambió:** los errores de negocio (`IllegalArgumentException`) no se relanzan, porque van
a fallar igual en cada reintento y bloquearían la cola compartida de la que salen las N
instancias de logística. Los demás se relanzan a propósito, para que la dead letter queue los
reciba.

# Corregidos

Estos cuatro son una sola cosa mirada desde cuatro ángulos: **qué hace falta para que N
instancias de logística puedan compartir la cola y la base**, que es lo que pide el enunciado
cuando dice *"más de 1 servicio de logística disponible"*. Se corrigieron juntos, y los tests
que los cubren son los primeros que tiene este módulo.

La regla de fondo, que no es obvia: **lo único que garantiza contra la duplicación es la
idempotencia; la base compartida evita que las instancias divergan; y el `@Version` convierte
un pisado silencioso en un error visible.** Con las tres, el sistema tolera N instancias sin
duplicar entregas ni perder estado.

---

### 21. Una redelivery reseteaba el ítem a `PENDIENTE`: no había idempotencia

**Estado:** corregido
**Severidad:** crítica
**Archivos:** `.../services/EntregaService.java`, `.../RabbitMQ/DonacionListener.java`

#### Qué pasaba

`procesarPeticion` no tenía ninguna guarda de idempotencia. Peor todavía: una redelivery **no
fallaba**.

El detalle está en la clave primaria. `ItemEntrega` declara:

```java
@Id
@Column(name = "id_donacion", nullable = false, updatable = false)
private UUID idDonacion;
```

`idDonacion` viene del mensaje y **no tiene `@GeneratedValue`**. Con id no nulo, el `isNew` de
Spring Data da `false` siempre y `save()` va **siempre por `merge()`**: hace `SELECT` y, si la
fila existe, hace `UPDATE`.

Ese `UPDATE` reescribía la fila con lo del constructor, que pone:

```java
this.estado = EstadoEntrega.PENDIENTE;
this.fechaCambioEstado = LocalDateTime.now();
```

O sea que si un ítem ya estaba entregado y el mensaje se reprocesaba, volvía a `PENDIENTE` y
perdía la foto del comprobante. Sin error, sin log, sin rastro. Es peor que una duplicación,
porque una duplicación se ve.

#### Qué se hizo

**1. Guarda de idempotencia explícita**, antes de construir nada:

```java
if (repoItemEntrega.existsById(idDonacion)) {
    repetidos++;
    log.info("La donación {} ya estaba registrada, el mensaje se repite y se omite", idDonacion);
    continue;
}
```

Es `continue` y no `return` a propósito: una donación puede traer bienes ya registrados **junto
con** bienes nuevos, y con un `return` se perderían los nuevos. Hay un test para eso.

**2. `DataIntegrityViolationException` como resultado benigno en el listener.** La guarda cubre
el caso secuencial, pero queda una ventana: dos instancias leen el mismo `idDonacion` antes de
que ninguna lo escriba, las dos pasan el `existsById` y las dos intentan insertar. Gana una; la
otra recibe la violación de clave primaria, **que es el estado final que se buscaba**.

Si se relanzara, el mensaje iría a la dead letter queue como si fuera un fallo y además frenaría
la cola compartida de la que salen las N instancias. Se registra y se sigue.

**3. El estado solo lo cambia el operador, nunca el registro.** Es la separación que vuelve
inofensiva una repetición por construcción, y no por accidente.

#### Cómo se verificó

Tests nuevos en `EntregaServiceIdempotenciaTest`. Lo relevante es que **tienen dientes**: se
neutralizó la guarda a propósito (`if (false && existsById(...))`) y 3 de los 8 tests
fallaron, justamente los tres que miden la idempotencia:

```
EntregaServiceIdempotenciaTest.noVuelveAGuardarSiYaExiste        FAIL
EntregaServiceIdempotenciaTest.noTocaElEstadoDeUnItemYaEntregado  FAIL
EntregaServiceIdempotenciaTest.registraSoloLosBienesNuevos        FAIL
Tests run: 8, Failures: 3
```

Restaurada la guarda, los 8 vuelven a pasar.

---

### 22. Ninguna entidad tenía `@Version`: dos instancias escribiendo a la vez se pisaban

**Estado:** corregido
**Severidad:** alta
**Archivos:** las 12 entidades de `logisticas-service`

#### Qué pasaba

Búsqueda de `@Version` en el módulo: **cero resultados**. `procesarPeticion` era un patrón
leer-modificar-escribir sin protección, y además estaba **dentro del `for` de bienes**, con lo
que escribía `Pais → Provincia → Ciudad → Direccion → Entidad` una vez por bien.

Dos donaciones para la misma entidad, atendidas por dos instancias: las dos leen las mismas
filas, las dos escriben, y la segunda pisa a la primera con los valores que leyó **antes** del
UPDATE de la otra. Sin excepción, sin log.

#### Qué se hizo

**1. `@Version` en las 12 entidades**, no solo en `ItemEntrega`. En `ItemEntrega` es
especialmente importante porque su clave natural hacía que el `merge()` fuera el camino normal
y no la excepción.

**2. El catálogo se resuelve una vez por mensaje y no por bien.** Esto es el arreglo de la
carrera **y de paso el del punto 15**: una donación con tres bienes escribía la misma dirección
tres veces. Ahora se resuelve una vez, y el `@Version` hace que la segunda instancia que llegue
reciba `OptimisticLockingFailureException` en vez de sobrescribir. Hay un test que lo mide.

**3. `@Transactional` en `procesarPeticion` y en `actualizarEstado`.** Lo segundo es por una
razón distinta del primero: `actualizarEstado` publica el evento de trazabilidad y después
guarda el ítem. Sin transacción, si el `save` fallaba el evento ya había salido del servicio y
`donaciones-service` se enteraba de una entrega que en la base nunca ocurrió.

**4. Validaciones de payload**, para que un mensaje incompleto se rechace **antes** de escribir
en lugar de dejar la donación a medias. Es también el punto 16.

---

### 23. Con N consumidores no hay orden: se midió y el particionado quedó apagado

**Estado:** corregido. El reparto por hash está implementado pero **desactivado a propósito**
**Severidad:** media
**Archivo:** `.../config/RabbitMQConfig.java`, `.../RabbitMQ/DonacionListener.java`,
`application.properties`

#### Qué se investigó

Competing consumers significa que el broker reparte los mensajes de a uno entre las instancias,
**en cualquier orden y sin sincronización entre ellas**. Dos mensajes del mismo `idDonacion`
pueden ser atendidos por dos instancias al mismo tiempo, y el que se atiende segundo no tiene
por qué ser el que se mandó segundo. Con una máquina de estados
`PENDIENTE → EN_CAMINO → ENTREGADA` eso permite **retroceder el estado**.

Con un solo consumidor el orden se respeta, porque RabbitMQ le entrega los mensajes en el orden
en que los publica. **El orden no se pierde al usar el broker: se pierde al agregar el segundo
consumidor.** Por eso el problema nunca se habría detectado.

#### Se implementó el particionado, y después se midió

Primero se hizo completo: exchange `x-consistent-hash` sobre el encabezado `x-id-donacion`, N
colas de shard, y cada instancia atendiendo un subconjunto **disjunto** con
`LOGISTICA_SHARDS_ASIGNADAS`. Verificado: 5 mensajes de la misma donación caían 5/5 en la misma
cola, y dos instancias repartían 20 mensajes 7/13.

Después se probó **qué pasa si se cae una instancia**, y el resultado dio vuelta la decisión:

```
con la instancia B bajada (la de los shards 2 y 3):
  queue.0   msgs=0   cons=1
  queue.1   msgs=0   cons=1
  queue.2   msgs=4   cons=0    <- nadie la lee
  queue.3   msgs=11  cons=0    <- nadie la lee
```

De 20 mensajes, la instancia sana procesó **5** y los otros **15 quedaron parados**, aunque
estaba viva y con capacidad de sobra. El particionado **no se traba nunca**, pero tampoco
sobrevive: cada caída deja su mitad del trabajo detenida.

#### La decisión: una sola cola, y por qué

**Se volvió a la cola compartida.** El motivo es que el análisis original acertaba: hoy ningún
mensaje que llegue a logística transporta una transición de estado.

| Routing key | Listener | Qué hace |
|---|---|---|
| `donaciones.creada` | `DonacionListener` | **Registra** ítems en `PENDIENTE` |

El registro quedó idempotente por construcción (punto 21). Las transiciones de estado entran por
otro camino: el `PATCH` contra `actualizarEstado`, que es la acción del operador sobre **una**
donación, y por lo tanto no compite con nadie.

O sea que el orden que el particionado compraba **no se necesita hoy**, y lo que cuesta es real:
disponibilidad. Se prefirió no pagar algo que no hace falta.

Medido con la cola compartida y dos instancias:

```
12 mensajes con las dos vivas  ->  A: 6, B: 6
se mata la instancia B
15 mensajes con una sola viva ->  A: 15
cola: 0 mensajes   DLQ: 0   27 de 27 en la base
```

Con una instancia caída, **la otra sigue consumiendo sin frenarse**. Eso es lo que se buscaba.

#### Qué quedó del particionado

El mecanismo sigue implementado y documentado en `application.properties`, **apagado por
defecto**. Se activa con `LOGISTICA_SHARDS_ASIGNADAS=0,1` repartido de forma disjunta.

El exchange sigue siendo `x-consistent-hash` en los dos modos: con un solo binding manda todo a
la cola única, y con cuatro los reparte. Así `donaciones-service` **no necesita saber en qué
modo está logística**: publica al mismo exchange con el mismo encabezado siempre.

#### Lo que resuelve el orden cuando aparezca un mensaje con estado

Una guarda que consulta la base **no sirve**, y conviene dejarse claro por qué. Si llega "la
donación está ENTREGADA" antes que "la donación pasó a EN_CAMINO", la guarda ve `PENDIENTE` y no
tiene nada que hacer: rechazarlo pierde el mensaje, aceptarlo deja el estado saltado. La guarda
dice **qué estado hay**, no arregla que el orden se haya roto.

Lo que sí cubre el caso real es **reintentar con espera**, y eso quedó en `DonacionListener`:

```
logistica.reintentos=3
logistica.espera-reintento-ms=2000
```

Un fallo transitorio (la base tardó, se cortó la conexión) se reintenta y al segundo intento
suele salir. Si tras los tres intentos sigue fallando, ahí sí va a la dead letter. Los errores de
negocio y la carrera de clave primaria **no** se reintentan: fallan igual todas las veces, y
reintentarlos solo frenaría la cola compartida.

Cuando aparezca un mensaje con transiciones de estado, la frontera es esta: o se prende
`LOGISTICA_SHARDS_ASIGNADAS` y se acepta el costo de disponibilidad, o el reintento con espera da
suficiente cobertura y la cola compartida sigue conviene.

---

### 24. El compose no se podía escalar

**Estado:** corregido
**Severidad:** media
**Archivo:** `docker-compose.yml`

#### Qué pasaba

El propio compose recomendaba en un comentario:

```
# servicio con `docker compose up --scale logisticas-service=2`.
```

**Y no funcionaba.** Dos cosas lo bloqueaban:

```yaml
container_name: logisticas-service   # no se pueden crear N contenedores con el mismo nombre
ports:
  - "8086:8086"                      # el puerto del host no se puede reservar dos veces
```

El `SERVER_PORT` tampoco llegaba a logística, porque su `application.properties` leía
`${PORT:8086}` en vez de `${SERVER_PORT:8086}`. No se notaba porque el default coincidía con lo
que pasa el compose, pero en cuanto hicieran falta dos instancias en el mismo host —que es
justo este punto— el puerto se ignoraba y las dos pelaban por el 8086.

#### Qué se hizo

1. **`container_name` fuera de los cuatro servicios de dominio.** Se deja solo en `mysql` y
   `rabbitmq`, que no se escalan y cuyo nombre estable evita depender de la red de Docker para
   llegar a ellos.
2. **El puerto de logística se publica efímero** (`- "8086"`, sin número de host), para que
   Docker asigne uno distinto por instancia. Para descubrir cuál le tocó:
   `docker compose port logisticas-service 8086`. El `SERVER_PORT` interno sigue siendo 8086
   para todas, porque dentro de la red de Docker cada contenedor tiene su propio espacio de
   puertos.
3. **`logisticas-service` lee `SERVER_PORT`**, igual que los otros tres.

`docker compose config` valida el archivo después del cambio.

#### El costo, dicho explícitamente

Quitar el puerto fijo tiene un precio para desarrollo: `docker compose up` a secas deja de dar
`localhost:8086` fijo. Está anotado en el propio compose que se puede volver a `"8086:8086"`
cuando se levanta de a una sola instancia.

La alternativa era un puerto fijo **o** la opción de escalar, y no pueden convivir. Se eligió
escalar, porque es lo que pide el enunciado.

---
### 5. Los tres DELETE devuelven 404 despues de borrar bien

**Estado:** corregido el 2026-10-07, sin commit
**Severidad:** alta
**Archivos:** `services/EntregaService.java`, `services/CamionService.java`, `services/ChoferService.java`

#### Que pasaba

```java
Optional<X> x = repo.findById(id);
if(x.isPresent()){
  repo.deleteById(id);
  throw new IllegalArgumentException("... no encontrado");
}
```

La excepcion se lanzaba cuando el recurso SI existia: el DELETE borraba bien y el controller
devolvia 404. Y cuando no existia, no se entraba al if y devolvia 204 en silencio. Los dos
casos al reves.

#### Que se cambio

Se invirtio la condicion: el 404 queda para cuando el recurso no estaba, y el 204 para cuando se
borro. Se mantuvo el contrato que los tres controllers ya documentan en el Swagger, en vez de
dejar el 404 sin uso.

#### Como se verifico

`DeleteDevuelve204Test`, seis casos: los tres servicios por existentey por inexistente. Los seis
fallan contra el codigo viejo: los de "existe" por el `throw`, los de "no existe" porque el
viejo no tiraba nada. El test ademas verifica que `deleteById` NO se llame cuando el recurso no
esta.

### 6. La relacion item-evento apunta al id equivocado

**Estado:** corregido el 2026-10-07, sin commit
**Severidad:** alta
**Archivos:** `models/entities/ItemEntrega/ItemEntrega.java`, `models/entities/EventoLogistica/EventoLogistica.java`

#### Que pasaba

`@OneToMany(mappedBy = "id", ...)` apuntaba a la clave primaria del propio evento
(`id_evento`, `GenerationType.IDENTITY`), no a un atributo que referencie al item. Hibernate
armaba una relacion inventada, con la FK en `item_entrega.id_evento` siempre en NULL: la lista
salia siempre vacia y la trazabilidad del Swagger de `GET /entregas` no existia.

#### Que se cambio

Se agrego `@ManyToOne @JoinColumn(name="id_donacion") ItemEntrega item` en `EventoLogistica`
—el lado dueno— y se puso `mappedBy = "item"` en el lado muchos. El cascade y el `orphanRemoval`
**se conservaron**: sin ellos, la FK nueva hace imposible borrar un item con historial, porque
MySQL rechaza el DELETE de un padre con hijos. Borrar una entrega borra su historia, que es
coherente con que el item es el registro de esa entrega.

#### Como se verifico

`GestorPublicacionEventosTest`: se afirma por reflexion que `mappedBy` es `"item"` y que del
lado dueno hay un `@ManyToOne` con `@JoinColumn(name="id_donacion")`, y que el cascade sigue
(`contains(CascadeType.ALL)` y `orphanRemoval`). Contra el codigo viejo el primer test falla
con `but was: "id"`.

**Lo que no se pudo verificar sin una base real:** que el `cascade REMOVE` efectivamente borre
los hijos en `DELETE /entregas/{id}`. El modulo no tiene base embebida en el build offline. Si
al probar contra MySQL ese endpoint diera 500 por FK, el arreglo es borrar los eventos antes
que el item.

### 11. El mismo evento se mete en la lista de todos los items

**Estado:** corregido el 2026-10-07, sin commit
**Severidad:** media
**Archivos:** `models/gestores/GestorPublicacionEventos.java`

#### Que pasaba

Una sola instancia de `EventoLogistica` se agregaba a la lista `eventos` de todos los items de
la ruta. Con la relacion ya siendo `mappedBy="item"` eso no tiene a que FK asignarle una
instancia compartida, y con `orphanRemoval` desvincularlo de un item lo borraba aunque siguiera
en los demas.

#### Que se cambio

El `INICIO_RUTA` es un hecho de la ruta, no de cada item: su `referenciaId` ya lo dice, porque
es el `idRuta`, y los ids de las donaciones viajan en el payload. Se creo **un solo** evento,
con `item = null`, y las listas de los items dejaron de tocarse a mano. Para los eventos que si
son de una entrega, el lado dueno se setea y se guarda con su propio repositorio.

Se descarto la alternativa de un evento por item: multiplicaba por N las filas que devuelve el
polling, con el mismo payload y el mismo `referenciaId`, y un consumidor por HTTP habria
recibido N notificaciones del mismo inicio de ruta.

#### Como se verifico

`GestorPublicacionEventosTest` afirma que `repoEventos.save` se llama **una** vez, que las listas
de los items quedan vacias y que el evento guardado tiene `getItem() == null` y la referencia de
la ruta. Contra el codigo viejo fallaba.

### 12. El polling de eventos reenvia el ultimo evento y explota con `desdeId` null

**Estado:** corregido el 2026-10-07, sin commit
**Severidad:** media
**Archivos:** `services/EventoLogisticaService.java`, `RabbitMQ/SolicitudEventosListener.java`, `controllers/EventoLogisticaController.java`

#### Que pasaba

`obtenerEventosNuevos` pasaba `desdeId - 1`, o sea `id >= desdeId`: el evento con
`id == desdeId` es el ultimo que el cliente ya proceso y se lo volvia a mandar. Ademas
`desdeId - 1` sobre un `Long` null desempaquetaba y lanzaba NullPointerException.

#### Que se cambio

Se pasa `desdeId` tal cual, y un null se trata como 0 —que es "mandame todo", la lectura util de
un poll recien arrancado—, sin desempaquetar. La proteccion quedo en el service, que es donde
esta el unboxing, asi que cualquier llamador queda cubierto y no depende de que cada listener se
acuerde de filtrar.

En el listener de sondeo se mantuvo la salida temprana con `desdeId` ausente: sin cursor no hay
desde donde consultar, y atenderlo con `id > 0` traia la tabla entera para tirar el resultado a
la basura, porque el metodo es `void`.

#### Como se verifico

`EventoLogisticaServiceTest` afirma que se consulta con el id **exacto** (y que nunca se
pide `desdeId - 1`), que un null no revienta y se traduce a 0, y que el listener no consulta
nada sin cursor. Los siete fallan contra el codigo viejo, uno de ellos con el NPE exacto:
`Cannot invoke "java.lang.Long.longValue()" because "desdeId" is null`.

### 13. `findByIdGreaterThanOrderByIdAsc` no ordena

**Estado:** corregido el 2026-10-07, sin commit
**Severidad:** media
**Archivos:** `models/repositories/eventos/RepositorioEventoLogistica.java`

#### Que pasaba

Era un `default` method con `findAll()` y un `stream().filter(...)`: traia la tabla entera a
memoria para descartar casi todo, y el orden dependia de lo que MySQL tuviera ganas de
devolver. El comentario que lo acompanaba no garantizaba nada.

#### Que se cambio

Ahora es una derived query de verdad, que Spring Data traduce a `WHERE id > ?1 ORDER BY id
ASC`. El filtro y el orden los resuelve la base, que es lo unico que puede garantizarlo.

#### Como se verifico

`EventoLogisticaServiceTest` afirma por reflexion que el metodo **no** es `default`. Sin
base embebida no se puede comprobar el SQL generado, asi que se verifica la declaracion y el
criterio (el id exacto que se le pasa), no la consulta.

### 14. `GET /entregas/{id}` devuelve la entidad cruda

**Estado:** corregido el 2026-10-07, sin commit
**Severidad:** media
**Archivos:** `services/EntregaService.java`, `models/entities/ItemEntrega/ItemEntrega.java`

#### Que pasaba

`findById` devolvia el `ItemEntrega` de JPA y el controller lo serializaba tal cual. Jackson
sigue los getters y entra en ciclo —`item.parada` → `parada.ruta` → `ruta.paradas` →
`parada.items` → `item.parada`— hasta reventar al construir el JSON. Y ademas exponia
`id_parada` e `id_unidad_medida` en un endpoint que el Swagger declara como `BienDTO`.

#### Que se cambio

`findById` devuelve `BienDTO` usando el mismo `convertirABienDTO` que ya usan `findAll` y
`obtenerEntregasNoRecibidas`: la inconsistencia entre los tres era el indicio de que faltaba
esa linea. Y se puso `@JsonIgnore` en `ItemEntrega.parada` como red, para que un mapping
equivocado a futuro no sea una denegacion de servicio.

#### Como se verifico

`EntregaServiceFindByIdDevuelveDtoTest` arma el ciclo completo (parada → ruta → sus paradas de
vuelta al item) y afirma que el JSON no trae `parada` ni `id_parada`, y que el controller
responde 200 con un `BienDTO`. Contra el codigo viejo, cinco de los seis fallan y el volcado
muestra el ciclo real: `Parada["ruta"]->Ruta["paradas"]->ArrayList[0]->Parada["ruta"]->...`

### 25. Un mensaje malformado se reencola para siempre

**Estado:** corregido el 2026-10-07, sin commit
**Severidad:** alta
**Archivos:** `RabbitMQ/DonacionListener.java`, `src/main/resources/application.properties`

#### Que pasaba

Agotados los 3 intentos, el listener hacia `throw ultimoFallo` y el log decia "va a la cola de
mensajes muertos". No era asi: sin `default-requeue-rejected=false`, rige el default de Spring
AMQP, que es reencolar. El mensaje volvia a su posicion original en la cola compartida y
volvia a entrar, tres intentos mas, para siempre. Un solo payload malformado frenaba a todas
las instancias y la DLQ recibia cero mensajes, porque el dead letter solo se activa con un
rechazo **sin** requeue.

#### Que se cambio

Dos capas. La explicita: el listener tira `AmqpRejectAndDontRequeueException` con el fallo
original como causa, que es la senal que Spring AMQP respeta siempre. Y la de config:
`spring.rabbitmq.listener.simple.default-requeue-rejected=false`, que cubre tambien los fallos
que nunca llegan al listener (un mensaje que no se puede convertir a `EntregaDTO`).

Se eligio la exception en vez de solo la property para no cambiarle el comportamiento al otro
listener del servicio a ciegas.

#### Como se verifico

`DonacionListenerCarreraTest`: se afirma el tipo de la excepcion y que la causa se conserva.
Dos tests existentes afirmaban `IllegalStateException` —el contrato viejo, que era el bug— y se
actualizaron. `ConfiguracionArranqueTest` afirma la property.

**Efecto colateral:** `default-requeue-rejected` es global al servicio, asi que un fallo
transitorio de la base en cualquier listener va a la DLQ de integracion en vez de reencolar. Se
eligio eso: reencolar para siempre un mensaje que falla igual trava la cola de la misma manera.
(El sondeo que se mencionaba aca se elimino despues; ver la ultima entrada de `Corregidos`.)

### 28. Publica el evento antes del commit

**Estado:** corregido el 2026-10-07, sin commit
**Severidad:** media
**Archivos:** `models/gestores/GestorPublicacionEventos.java`, `services/RutaService.java`

#### Que pasaba

`productorEventos.publicar(evento)` se llamaba **dentro** de la transaccion. Si un `save`
posterior fallaba —por ejemplo un `OptimisticLockingFailureException` cuando dos operadores
confirman la misma entrega casi al mismo tiempo— la base hacia rollback pero el mensaje ya
habia salido: `donaciones-service` notificaba una entrega que en logistica nunca ocurrio. En
`iniciarRuta` pasaba lo mismo: se publicaba y recien despues se persistian los items con el
estado `EN_CAMINO` que se les habia puesto en memoria.

#### Que se cambio

La publication se registra con `TransactionSynchronizationManager` y se manda en `afterCommit`,
que es exactamente la garantia que faltaba: un rollback no despacha `afterCommit`. Sin
transaccion activa se publica en el momento, que es el unico jeito de no perder el evento cuando
no hay commit al que esperar.

Y `RutaService.iniciarRuta` ahora es `@Transactional`, que es lo que hace que esa garantia
aplique tambien ahi: antes no habia commit al que agendarse.

#### Como se verifico

`GestorPublicacionEventosTest`: con transaccion abierta el evento **no** se publica hasta el
commit, un rollback no publica nada, y sin transaccion se publica igual. Se despachan a mano las
sincronizaciones que despacha Spring, sin levantar un contexto.

**Lo que quedo fuera:** `terminarRuta` sigue sin ser transaccional, y a proposito: tiene `throw`
deliberados de bugs que todavia no se arreglaron, y volverla transaccional haria rollback de
cosas no relacionadas. Sus eventos de reingreso se publican en el momento, como antes.

**Efecto colateral no pedido:** con `@Transactional`, `findByChofer` y `actualizarEstado` ven la
misma instancia gestionada de la ruta, asi que `indexOf` ahora la encuentra y `iniciarRuta` si
persiste el estado `EN_CURSO`, que antes no se guardaba. Es una mejora, pero cambia el
comportamiento del punto 2.

### 33. Credenciales de la base hardcodeadas

**Estado:** corregido el 2026-10-07, sin commit
**Severidad:** media
**Archivos:** `src/main/resources/application.properties`, `docker-compose.yml` (raiz del repo),
`.env` / `.env.example` de cada modulo

#### Que pasaba

`spring.datasource.password=${DB_PASSWORD:<la clave de la base>}`: la credencial real estaba como
**default** del placeholder. Cualquier arranque sin las variables —un `java -jar` a secas, un
deploy que se olvido de definirlas— conectaba en claro y sin avisar. Y el `docker-compose.yml` la
repetia en el archivo, quedando commiteada en el historial de git.

#### Que se cambio

`application.properties` quedo sin default: si falta la variable, el servicio no levanta, que es
lo correcto. No hizo falta tocar `.gitignore`: el del repo ya cubria `.env`.

**Actualizacion posterior:** el `.env` general de la raiz se reemplazo por **un `.env` por
modulo** (`<servicio>/.env`, con su `<servicio>/.env.example`), y `docker-compose.yml` los carga
con `env_file`. Ya no se usa `${DB_USERNAME:?...}` ni hay `.env` en la raiz.

#### Como se verifico

`ConfiguracionArranqueTest` afirma que las dos properties no tienen default y que la clave no
aparece en `application.properties`. Contra el codigo viejo falla con
`but was: "${DB_USERNAME:valentin}"`.

**Lo que quedo a medias:** los bloques de `donaciones-service` y `notificaciones-service` en el
compose siguen con usuario y clave en claro. `donaciones-service` usa la MISMA clave que se acaba
de sacar. El punto 33 solo nombraba el bloque de logisticas, asi que no se toco, pero el
`grep` de la credencial sigue dando y queda anotado en el propio compose.

**Y lo que no se puede arreglar desde el codigo:** la clave ya esta en el historial de git.
Sacar la linea no la borra de ahi. Hay que rotarla en MySQL.

### 39. El cron de planificacion corre a las 02:00 UTC

**Estado:** corregido el 2026-10-07, sin commit
**Severidad:** baja
**Archivos:** `Scheduler/PlanificadorDeRutasScheduler.java`

#### Que pasaba

`@Scheduled(cron = "0 0 2 * * ?")` no declara `zone`, asi que Spring usa la zona de la JVM. La
imagen final (`eclipse-temurin:21-jre`) no define `TZ`, el compose tampoco le pasa ninguna, y el
`DB_URL` hasta fuerza `serverTimezone=UTC`: en Docker la planificacion ocurre a las 02:00 UTC,
que son las 23:00 de Argentina. Fuera de Docker depende del host.

#### Que se cambio

`zone = "America/Argentina/Buenos_Aires"` explicito: deja de depender de la imagen y del host.

#### Como se verifico

`ConfiguracionArranqueTest` lee la anotacion por reflexion y afirma que declara la zona y que el
cron sigue siendo `"0 0 2 * * ?"`. Contra el codigo viejo falla con `zone() == ""`.

**Sin verificar:** la observacion con la hora del contenedor real. El pendiente estaba marcado
como sospechado por eso, y corregir la zona lo hace deterministico igual.

### 40. El sondeo de trazabilidad por cola era un no-op

**Estado:** corregido el 2026-10-07, sin commit
**Severidad:** media
**Archivos:** `RabbitMQ/SolicitudEventosListener.java` (eliminado),
`dto/evento/SolicitudEventosDTO.java` (eliminado), `config/RabbitMQConfig.java`,
`donaciones-service/.../models/sheduler/LogisticaPollingScheduler.java` (eliminado en donaciones),
`donaciones-service/.../config/RabbitMQConfig.java`

#### Que pasaba

El camino real de eventos es el push: `GestorPublicacionEventos` persiste el `EventoLogistica` y lo
publica despues del commit a `logistica.eventos.exchange`, y donaciones lo consume en
`EventosListener`. El polling era una "red de contencion" que no contenia nada:

- `SolicitudEventosListener` leia los eventos y **solo logueaba**; nunca publicaba la respuesta, y
  `LogisticaPollingScheduler` publicaba con `convertAndSend` (sin esperar respuesta). No habia
  camino de vuelta a donaciones.
- El cursor `ultimoIdProcesado` del scheduler nunca avanzaba: cada 5 minutos preguntaba desde 0.
- `GET /api/eventos` estaba disponible pero donaciones no lo llamaba.

O sea: no era un fallback imperfecto, era un no-op. Lo unico observable era un log cada 5 minutos.

#### Que se cambio

Se elimino la cadena completa: `LogisticaPollingScheduler`, `SolicitudEventosListener`, la cola
`logistica.sondeo.queue`, el exchange `logistica.exchange`, la routing key
`logistica.solicitud.eventos` y los DTO `SolicitudEventosDTO` / `EventoLogisticaResponseDTO` del
lado de donaciones. La consulta de trazabilidad queda por HTTP en `GET /api/eventos`
(`EventoLogisticaController` + `EventoLogisticaService`), que es lo que el enunciado pide al
describir logistica accesible por sus URIs.

El push ya es at-least-once (cola durable): el polling no agregaba cobertura, porque el unico caso
que podria cubrir —un evento publicado a un exchange sin ruta— requiere una respuesta real y
deduplicacion persistente, justo el request/response que se habia quitado a proposito.

#### Como se verifico

`EventoLogisticaServiceTest` (antes `EventoLogisticaServicePollingTest`) cubre la lectura por id:
off-by-one, null, orden y la derived query. Se quitaron los dos tests del listener, que ya no
existe. Los dos modulos compilan (`mvn test-compile`) y el test corre 6/6 en verde.

### 2. Iniciar o terminar una ruta nunca persiste el estado y aborta transacciones

**Estado:** corregido el 2026-10-08 en `[PONER_TU_COMMIT_ID]`
**Severidad:** alta
**Archivos:** `models/repositories/rutas/RepositorioRutas.java`, `services/RutaService.java`

`RepositorioRutas.actualizarEstado` operaba de forma imperativa mediante un `indexOf` sobre copias *detached* de listas devueltas por `findAll()`, perdiendo las referencias en memoria. Además, el flujo de `terminarRuta` lanzaba excepciones `IllegalArgumentException` infundadas tras operaciones de `deleteById` y búsquedas de camión exitosas, provocando el *rollback* completo de la transacción.

**Qué se resolvió:**
1. Se eliminó el método `default actualizarEstado` del repositorio y se delegó la persistencia de los cambios de estado (`EN_CURSO`, `FINALIZADA`) directamente a `RutaService` mediante la anotación `@Transactional` y `repoRutas.save()`.
2. Se reestructuró `terminarRuta`: se eliminaron los lanzamientos erróneos de excepciones en el borrado de entregas entregadas y en la desvinculación del camión, permitiendo la correcta liberación del chofer y el reseteo del vehículo.
3. Se garantizó el contexto transaccional atómico en todo el servicio para evitar modificaciones fuera de sesión.

**Residual:** si un chofer intenta finalizar una ruta sin haber procesado la totalidad de las entregas pendientes, los ítems restantes reingresan al depósito mediante evento de dominio, pero la validación de consistencia depende de la correcta respuesta sincrónica del `GestorPublicacionEventos`.

### 3. Terminar una ruta borra un ítem y lanza una excepción errónea

**Estado:** corregido el 2026-10-08 en `[PONER_TU_COMMIT_ID]`
**Severidad:** alta
**Archivos:** `services/RutaService.java`

Al procesar una entrega con estado `ENTREGADA`, el bloque `else` ejecutaba `deleteById` si la entidad estaba presente (`isPresent()`), pero inmediatamente lanzaba una excepción `IllegalArgumentException("Entrega no encontrada")`. Esto interrumpía el bucle de procesamiento de paradas, impedía la liberación del chofer y del camión, y provocaba el *rollback* de la transacción.

**Qué se resolvió:** Se eliminó la excepción del flujo normal de ejecución y se simplificó la condición de borrado utilizando `existsById()`. El procesamiento de entregas ahora continúa de forma fluida a lo largo de todas las paradas sin abortar ni bloquear al personal/vehículo.

**Residual:** Si se requiere auditar el historial de entregas borradas, se deberá implementar una tabla de auditoría o marcado lógico (*soft delete*), ya que la eliminación física destruye el registro sobre el cual operaba la bitácora de eventos de `ItemEntrega`.

### 4. La condición de búsqueda del camión al finalizar la ruta estaba invertida

**Estado:** corregido el 2026-10-08 en `[PONER_TU_COMMIT_ID]`
**Severidad:** alta
**Archivos:** `services/RutaService.java`

Al momento de desvincular el vehículo al terminar un recorrido, el método `terminarRuta` ejecutaba `repoCamiones.findByChofer_IdChofer(idChofer)`. La condición evaluaba `camion.isPresent()`, por lo que en el camino feliz (cuando el camión existía) se reseteaba el vehículo pero se lanzaba inmediatamente una excepción `IllegalArgumentException("Camión no encontrado")`, abortando la transacción por rollback.

**Qué se resolvió:** Se simplificó la navegación entre agregados. En lugar de ejecutar una consulta redundante en el repositorio de camiones con condicionales invertidos, se obtiene el camión directamente a través de la relación de dominio `rutaActual.getCamionAsignado()`. Sobre dicha instancia se gestiona la disponibilidad, la desvinculación del chofer y el reseteo del vehículo de forma atómica.

**Residual:** Ninguno. La asociación entre la ruta y su camión asignado es requerida por el modelo de datos antes de pasar a estado `EN_CURSO`.

### 7. Inconsistencia en el getter de Parada y posible NullPointerException en Ruta.agregarEntrega

**Estado:** corregido el 2026-10-08
**Severidad:** baja / media
**Archivos:** `models/entities/Parada/Parada.java`, `models/entities/Ruta/Ruta.java`

Se detectó que `Parada.java` sobrescribía manualmente el getter de `entidadDestino` derivándolo del primer ítem de la parada, ignorando el atributo `@ManyToOne` mapeado con JPA. Ante una parada sin ítems, el getter retornaba `null`, lo que provocaba un `NullPointerException` en `Ruta.agregarEntrega` al ejecutar `p.getEntidadDestino().equals(...)`.

**Qué se resolvió:**
1. Se removió el getter manual en `Parada.java` para mantener la coherencia del mapeo `@ManyToOne` administrado por Lombok.
2. Se actualizó `Ruta.agregarEntrega` para comparar las entidades de destino de forma segura utilizando `Objects.equals(...)` y validando la entrada de ítems nulos.

### 8. Confirmación o fallo de entregas fuera del estado EN_CAMINO ignorados en silencio

**Estado:** corregido el 2026-10-08
**Severidad:** media
**Archivos:** `models/gestores/GestorPublicacionEventos.java`, `services/EntregaService.java`

`publicarEntregaConfirmada` envolvía la lógica de cambio de estado y publicación de eventos en una guarda `if (item.getEstado() == EstadoEntrega.EN_CAMINO)`. Si el ítem se encontraba en otro estado (`PENDIENTE`, `NO_RECIBIDA`, etc.), la función no realizaba ninguna acción pero retornaba el ítem intacto. El servicio respondía un HTTP 200 OK indicando éxito falso al cliente.

**Qué se resolvió:**
1. Se removió la guarda pasiva en `publicarEntregaConfirmada` reemplazándola por una validación explícita que arroja `IllegalStateException` si la donación no está en estado `EN_CAMINO`.
2. Se aplicó la misma guarda explícita en `publicarEntregaFallida` para mantener coherencia en las transiciones de estado del dominio.

### 9. Reportar una entrega fallida no valida el estado previo

**Estado:** corregido el 2026-10-08
**Severidad:** media
**Archivos:** `models/gestores/GestorPublicacionEventos.java`

`publicarEntregaFallida` permitía transicionar hacia `NO_RECIBIDA` sin verificar el estado actual del ítem. Esto posibilitaba que donaciones previamente marcadas como `ENTREGADA` o `PENDIENTE` cambiaran a `NO_RECIBIDA`, dejando inconsistencias en la bitácora de eventos y en los atributos del ítem (como conservar la foto de comprobante).

**Qué se resolvió:**
Se incorporó la validación explícita en `publicarEntregaFallida` para verificar que el ítem se encuentre en estado `EN_CAMINO` antes de transicionar a `NO_RECIBIDA`, lanzando un `IllegalStateException` en caso contrario.

### 10. Reingreso a depósito no validaba el estado previo

**Estado:** corregido el 2026-10-08
**Severidad:** media
**Archivos:** `services/EntregaService.java`, `models/gestores/GestorPublicacionEventos.java`

`publicarReingresoDeposito` permitía cambiar el estado de cualquier ítem a `PENDIENTE` sin validar si venía de `NO_RECIBIDA`. Un comentario en `EntregaService` asumía erróneamente que una función inexistente (`reingresarADeposito()`) realizaba esta comprobación. Permitir que cualquier entrega pase a `PENDIENTE` hacía que fuera recalculada por el planificador nocturno de rutas (`PlanificadorDeRutasScheduler`), generando rutas duplicadas o inconsistentes.

**Qué se resolvió:**
1. Se agregó la validación explícita en `publicarReingresoDeposito` dentro de `GestorPublicacionEventos` para permitir el reingreso únicamente si la entrega se encuentra en estado `NO_RECIBIDA` (de lo contrario lanza `IllegalStateException`).
2. Se eliminó la referencia al método inexistente en los comentarios de `EntregaService`.

### 18. Retorno de null en el bloque catch de /planificar-manual

**Estado:** corregido el 2026-10-08  
**Severidad:** baja  
**Archivos:** `controllers/PlanificadorDeRutasController.java`

El endpoint `@PostMapping("/planificar-manual")` devolvía `null` en caso de capturar una excepción durante el disparo de la planificación. Esto hacía que Spring MVC fallara al intentar renderizar la respuesta, enviando un error 500 genérico e inexpresivo al cliente HTTP mientras los detalles del error quedaban ocultos o mal estructurados en los logs.

**Qué se resolvió:**
Se modificó el bloque `catch` para retornar un `ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)` con un mensaje claro con la causa del error en el cuerpo de la respuesta, registrando además el stacktrace completo a través de SLF4J (`log.error`).

### 26. Planificación reiterada genera rutas duplicadas para los mismos ítems

**Estado:** corregido el 2026-10-08  
**Severidad:** alta  
**Archivos:** `services/PlanificadorDeRutasScheduler.java`, `models/entities/Ruta.java`

Debido a que el estado del `ItemEntrega` se mantenía como `PENDIENTE` desde la asignación inicial de la ruta hasta la salida efectiva del chofer (`EN_CAMINO`), las ejecuciones subsiguientes del scheduler (o invocaciones al endpoint `/planificar-manual`) volvían a incluir los mismos ítems en la planificación. Como resultado, el sistema creaba múltiples objetos `Ruta` duplicados conteniendo las mismas donaciones y consumiendo camiones y choferes de forma redundante.

**Qué se resolvió:**
Se aplicó un filtro en `PlanificadorDeRutasScheduler` sobre los ítems recuperados en estado `PENDIENTE` para asegurar que solo se envíen al proveedor externo aquellos ítems que no tengan una parada asignada a una ruta cuyo estado sea distinto de `FINALIZADA` (ignora rutas en estado `PROGRAMADA` o `EN_CURSO`).

### 27. findByChofer devolvía rutas históricas finalizadas en iniciar y terminar ruta

**Estado:** corregido el 2026-10-08  
**Severidad:** alta  
**Archivos:** `models/repositories/rutas/RepositorioRutas.java`, `services/RutaService.java`

El método `findByChofer` realizaba un `.findFirst()` sobre la colección completa de rutas sin filtrar por estado. Cuando un chofer poseía rutas finalizadas históricas en la base de datos, las operaciones `iniciarRuta` y `terminarRuta` tomaban siempre la primera ruta histórica (la más antigua), provocando re-transiciones de estado sobre rutas cerradas, publicación de eventos con paradas desactualizadas y desvinculaciones indebidas de camiones.

**Qué se resolvió:**
1. Se implementó el método `findByChoferYEstado` en `RepositorioRutas` para requerir explícitamente el estado objetivo de la ruta.
2. En `RutaService`, `iniciarRuta` ahora consulta por rutas en estado `PROGRAMADA`, mientras que `terminarRuta` consulta por rutas en estado `EN_CURSO`. Si no existe una ruta válida en dicho estado, el servicio arroja una excepción explícita (`IllegalStateException`).

### 29. Omisión del atributo de disponibilidad resulta en alteración indebida de estado o excepciones HTTP 500

**Estado:** corregido el 2026-10-08  
**Severidad:** media  
**Archivos:** `services/CamionService.java`, `services/ChoferService.java`, `dto/chofer/ChoferDTO.java`, `models/gestores/GestorCamiones.java`

Al omitir el campo `"disponible"` en peticiones de actualización (`PATCH` o `PUT`), la ausencia del valor era evaluada como `false` o asignada como `null`. Esto provocaba que recursos previamente disponibles pasaran a estar ocupados sin solicitud explícita del cliente o que se intentara persistir un valor `null` en columnas no anulables de JPA, desencadenando excepciones `DataIntegrityViolationException` (HTTP 500).

**Qué se resolvió:**
1. Se modificó el tipo de dato del atributo `disponible` en `ChoferDTO` de primitivo `boolean` a `Boolean` wrapper para permitir detectar el envío nulo/ausente.
2. En los métodos `cambiarDisponibilidad` de `CamionService` y `ChoferService`, se agregó la verificación explícita de presencia del campo `body.containsKey("disponible")`, lanzando un `IllegalArgumentException` en su ausencia.
3. Se ajustaron los métodos de actualización en `ChoferService` y `GestorCamiones` para preservar el estado actual de la entidad cuando el atributo `disponible` no sea provisto.

### 30. Salida silenciosa en POST /entregas ante payloads nulos o incompletos

**Estado:** corregido el 2026-10-08  
**Severidad:** media  
**Archivos:** `services/EntregaService.java`

Al invocar la creación/procesamiento de entregas con atributos estructurales nulos (`request`, `bienes` o `idsDonaciones`), el servicio retornaba inmediatamente de manera silenciosa (`return;`). Esto ocasionaba que el controlador respondiera un HTTP 201 Created infundado y que los consumidores de mensajería (RabbitMQ) confirmaran (`ack`) y descartaran mensajes corruptos sin posibilidad de reintento ni registro en DLQ.

**Qué se resolvió:**
Se reemplazaron todas las salidas silenciosas en `EntregaService.procesarPeticion` por lanzamientos explícitos de `IllegalArgumentException`. Con este cambio:
- Las llamadas HTTP reciben una respuesta HTTP 400 Bad Request con la descripción del error de validación.
- Los consumidores de mensajería gestionan la excepción adecuadamente según las políticas del listener para mensajes inválidos.

### 32. Captura genérica de DataIntegrityViolationException descarta mensajes corruptos sin enviar a la DLQ

**Estado:** corregido el 2026-10-08  
**Severidad:** media  
**Archivos:** `RabbitMQ/DonacionListener.java`

`DonacionListener` capturaba de forma amplia cualquier `DataIntegrityViolationException`, asumiendo que se debía a duplicados por carreras entre instancias al insertar claves primarias. Esto provocaba que violaciones reales del esquema de la base de datos (restricciones de clave foránea, columnas con longitud excedida o valores nulos no permitidos) fueran absorbidas silenciosamente con un `return`, notificadas falsamente en los logs y descartadas sin derivación a la DLQ.

**Qué se resolvió:**
Se eliminó el bloque `catch (DataIntegrityViolationException)` en `DonacionListener`. Ahora, las violaciones inesperadas del esquema de base de datos son procesadas en el flujo de reintentos estándar y, de persistir la falla, son rechazadas y enviadas a la Dead Letter Queue (`AmqpRejectAndDontRequeueException`) para su inspección y posterior reprocesamiento.

### 35. Persistencia redundante e incondicional de catálogo geográfico en `resolverEntidad`

**Estado:** corregido el 2026-10-08  
**Severidad:** media  
**Archivos:** `services/EntregaService.java`

En `EntregaService.resolverEntidad`, la jerarquía de País, Provincia, Ciudad y Dirección se persistía antes de consultar si la `Entidad` ya se encontraba registrada en el sistema. Para mensajes posteriores dirigidos a una misma entidad beneficiaria, los objetos geográficos eran insertados como registros nuevos antes de que el método retornara la entidad existente, acumulando registros huérfanos y duplicados en las tablas geográficas.

**Qué se resolvió:**
Se reordenó el flujo en `resolverEntidad` para ejecutar primero la búsqueda de la `Entidad` mediante `repoEntidades.findById(idEntidad)`. La construcción y guardado de País, Provincia, Ciudad y Dirección ahora solo ocurren si la entidad no existía previamente en la base de datos.

### 36. Respuesta HTTP 500 con fugas de información interna al enviar IDs inexistentes en callback de rutas

**Estado:** corregido el 2026-10-08  
**Severidad:** baja  
**Archivos:** `controllers/PlanificadorDeRutasController.java`, `services/PlanificadorRutasService.java`

Al procesar un callback de ruteo con IDs inexistentes en la base de datos, `PlanificadorRutasService` capturaba `IllegalArgumentException` y la envolvía dentro de un `RuntimeException` genérico. Esto impedía que el controlador retornara el HTTP 400 (`BAD_REQUEST`) documentado en OpenAPI, derivando en un HTTP 500 que además concatenaba el mensaje interno de la excepción en la respuesta HTTP.

**Qué se resolvió:**
1. Se ajustó la captura de excepciones en `PlanificadorRutasService.procesarCallbackRutas` para relanzar directamente las instancias de `IllegalArgumentException` e `IllegalStateException`.
2. Se eliminó la concatenación de `e.getMessage()` en la respuesta HTTP 500 de `PlanificadorDeRutasController` para prevenir la filtración de detalles del servidor.

### 38. Mismatch de tipo de ID (UUID vs Long) en repositorios del catálogo geográfico

**Estado:** corregido el 2026-10-08  
**Severidad:** baja  
**Archivos:** `RepositorioPaises.java`, `RepositorioProvincias.java`, `RepositorioCiudades.java`

Las interfaces `RepositorioPaises`, `RepositorioProvincias` y `RepositorioCiudades` heredaban de `JpaRepository` especificando `UUID` como tipo de clave primaria, mientras que las entidades `Pais`, `Provincia` y `Ciudad` definían su ID como `Long` (`GenerationType.IDENTITY`). Aunque el método `.save()` funcionaba al no validar explícitamente la clave en la firma, invocaciones como `findById`, `existsById` o `deleteById` provocaban errores de mapeo e incompatibilidad de tipos en Hibernate al intentar realizar el binding entre `UUID` y un campo numérico (`BIGINT`).

**Qué se resolvió:**
Se corrigieron los parámetros genéricos de `JpaRepository` en `RepositorioPaises`, `RepositorioProvincias` y `RepositorioCiudades` para utilizar `Long` como el tipo de ID correspondiente a la entidad.

### 17. Intercalación no determinista y pérdida de asignación por ciudad en el calculador simulado de rutas

**Estado:** corregido el 2026-10-08  
**Severidad:** media  
**Archivos:** `ProveedorRutasExternoSimulado.java`, `PlanificadorDeRutasScheduler.java`

Al procesar los lotes de ruteo, el simulador mutaba directamente las instancias compartidas de `Camion` en hilos asíncronos paralelos (`runAsync`), provocando condiciones de carrera (*race conditions*). Además, el reseteo del estado del camión al inicio del procesamiento destruía el campo `ciudadDestinoActual`, haciendo que la búsqueda de camiones con ruta asignada a la misma ciudad fallara siempre (código muerto) y asignara camiones de forma subóptima.

**Qué se resolvió:**
1. Se aisló el estado de simulación instanciando una estructura efímera/copia de `Camion` por cada lote y por cada hilo de planificación.
2. Se mantuvo la actualización de `ciudadDestinoActual` durante la iteración de asignación del simulador, permitiendo la correcta agrupación por ciudad.
3. Se garantizó que la persistencia definitiva de asignaciones se realice únicamente al recibir el callback correspondiente (`procesarCallbackRutas`).


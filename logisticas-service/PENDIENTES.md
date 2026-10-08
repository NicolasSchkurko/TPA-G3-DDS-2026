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
| 1 | 1 | No tiene ni un test: nada de lo que hay adentro está verificado |
---

## 1. El módulo no tiene un solo test

**Estado:** abierto
**Severidad:** alta
**Archivos:** todo `logisticas-service/src/main`

### Qué pasa

`logisticas-service/src/test` no existe. Es el único módulo del proyecto sin cobertura, y es
justamente el que recibió un merge sin resolver: el compilador era la única red de seguridad y
esa red no avisó.

No es una observación estética. Los bugs que se listan abajo son de la clase que un test
atrapa con dos líneas y que se descubren tarde: un JPQL mal formado, un método que se borra y
deja la llamada colgando, imports que apuntan al paquete viejo.

### Qué había que haber atrapado

1. `GestorPublicacionEventos.java` se commiteó con marcadores de conflicto de merge
   (`<<<<<<< HEAD`, `=======`, `>>>>>>>`). El módulo entero no compilaba.
2. `RepositorioBienes.buscarPorId` se borró dejando la llamada colgando (esto fue en
   `donaciones-service`, mismo patrón).
3. Un `@Query` sin `FROM` hacía que el bean del repositorio no se pudiera crear y el servicio
   no arrancara, con los tests en verde.

Los tres son fallos que un `mvn compile` o un test de arranque los muestran en segundos.

### Propuesta

Un `@SpringBootTest` que levante el contexto ya cubre el punto 1 y el 3: el contexto no
arranca si un JPQL está mal o si una entidad referencia a otra que no está en la unidad de
persistencia. Cuesta un test y es la red que falta.

Después, tests sobre `GestorPublicacionEventos` y sobre los `default` de los repositorios,
que son la lógica que más se toca.

---
## Corregidos

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
`LogisticaPollingScheduler`.

### `SolicitudEventosListener` hacía request/response por cola

**Estado:** corregido
**Severidad:** media
**Archivo:** `.../RabbitMQ/SolicitudEventosListener.java`

Publicaba la respuesta en el exchange para que volviera a la cola del que preguntó. Eso convierte
el broker en un request/response: necesita dos colas y dos bindings por consumidor, y se rompe
entero si el que preguntó se cae antes de leer la respuesta.

**Qué se cambió:** el listener deja de publicar la respuesta. La trazabilidad queda disponible
por HTTP en `GET /api/eventos`, que es lo que pide el enunciado al describir el despliegue de
logística como accesible por sus URIs. El polling queda como red de contención.

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

---

## 21. Una redelivery resetea el ítem a `PENDIENTE`: no hay idempotencia en `procesarPeticion`

**Estado:** abierto
**Severidad:** crítica
**Archivo:** `.../services/EntregaService.java:78-107`, `.../models/entities/ItemEntrega/ItemEntrega.java`

### Qué pasa

RabbitMQ es de entrega **al menos una vez**. Un mensaje se puede volver a entregar cuando el
consumidor procesa pero no llega a hacer el *ack*: se cae la conexión, se reinicia la instancia,
pasa lo que pase. Con N instancias de logística consumiendo la cola compartida, la probabilidad de
que algune vez pase no es teórica.

`EntregaService.procesarPeticion` no tiene **ninguna guarda de idempotencia**: no consulta si el
ítem ya existe, va directo a `repoItemEntrega.saveAndFlush(nuevoItem)`.

Y el detalle que convierte esto en pérdida de datos silenciosa está en la clave primaria.
`ItemEntrega` declara:

```java
@Id
@Column(name = "id_donacion", nullable = false, updatable = false)
private UUID idDonacion;
```

`idDonacion` viene del mensaje y **no tiene `@GeneratedValue`**. Con id no nulo, el `isNew` de
Spring Data da `false` siempre y `save()` va **siempre por `merge()`**: hace `SELECT` y, si la fila
existe, hace `UPDATE`.

O sea que una redelivery **no falla con error de clave duplicada**. Hace un `UPDATE` que
reescribe la fila con los valores del constructor, y el constructor pone:

```java
this.estado = EstadoEntrega.PENDIENTE;
this.fechaCambioEstado = LocalDateTime.now();
```

**Resultado:** si un ítem ya estaba `ENTREGADO` y el mensaje se reprocesa, vuelve a `PENDIENTE` y
pierde la foto del comprobante. Sin error, sin log, sin rastro. La entrega más importante del
sistema desaparece y la base dice que nunca pasó.

Es peor que una duplicación, porque una duplicación al menos se ve.

### Por qué importa más con N instancias

Con una sola instancia el problema es raro. Con varias, cada una tiene su propia conexión al
broker y cada corte de conexión genera redeliverías, así que la frecuencia sube justo en el
escenario que el enunciado pide soportar.

### Propuesta

1. **Guarda de idempotencia explícita** antes de insertar: `findById(idDonacion)` y, si ya
   existe, no volver a escribir. Con el `merge()` actual, "no hacer nada" y "reescribir" son
   cosas distintas que hay que decidir a mano.
2. O, más simple y más fuerte: que la columna tenga un `ON DUPLICATE KEY` / que el `UPDATE` solo
   toque los campos que son de alta y **nunca** `estado` ni `fechaCambioEstado`. El estado solo lo
   cambia quien opera la entrega, no quien la registra.
3. Un test que mande el mismo mensaje dos veces y verifique que la fila queda igual.

Lo segundo es lo que yo haría: separa "registrar que esta donación existe" de "operar la
entrega", que hoy están mezcladas en el mismo `save`.

---

## 22. Ninguna entidad tiene `@Version`: dos instancias escribiendo a la vez se pisan sin error

**Estado:** abierto
**Severidad:** alta
**Archivo:** todas las entidades de `logisticas-service`

### Qué pasa

Búsqueda de `@Version` en el módulo: **cero resultados**. No hay ni una sola entidad con control
de concurrencia optimista.

`EntregaService.procesarPeticion` es un patrón leer-modificar-escribir clásico, y sin `@Version`
la última escritura gana en silencio:

```java
repoPaises.save(direccionEntidad.getCiudad().getProvincia().getPais());
repoProvincias.save(...getProvincia());
repoCiudades.save(...getCiudad());
repoDirecciones.save(direccionEntidad);
Entidad nuevaEntidad = new Entidad(request.getEntidadBeneficiaria().getIdEntidad(), direccionEntidad);
repoEntidades.save(nuevaEntidad);
```

Y esto está **dentro del `for` de los bienes**, o sea que se repite por cada bien de la donación.

### El escenario concreto

Dos donaciones distintas para la misma entidad beneficiaria llegan a la vez y las toman dos
instancias distintas. Las dos hacen:

1. SELECT de `Pais` por el mismo nombre → las dos leen la misma fila
2. INSERT de `Ciudad` con la misma provincia
3. UPDATE de la fila de `Ciudad`

La instancia A hace su UPDATE. La B lo hace después, con los valores que leyó **antes** del
UPDATE de A. El resultado es que se pierde lo que A había escrito, sin excepción ni log.

Con `merge()` y sin versión, Hibernate no detecta el conflicto porque no hay nada que comparar:
el `UPDATE` se manda con los valores que la entidad tiene en memoria.

El caso peor es `Direccion`, que por el punto 15 ya se duplica N veces en una sola peticion (ver
punto 15): el problema 15 es una única instancia, y este es el mismo defecto con dos encima.

### Relación con el punto 15

Los dos puntos son el mismo bug en dos escalas. El 15 es "una donación con 3 bienes triplica la
dirección", que pasa **con una sola instancia**. Este es "dos instancias se pisan la dirección",
que pasa **con N instancias**. Arreglar el 15 sin este sigue dejando el problema en un despliegue
con competing consumers, que es lo que el enunciado pide.

### Propuesta

1. `@Version` en las entidades que se escriben desde el listener —`ItemEntrega` sobre todo, que es
   la que cambia de estado— y relanzar `OptimisticLockingFailureException` para que el mensaje
   vaya a la dead letter queue en vez de pisar.
2. `@Transactional` en `procesarPeticion`, para que el bloque `Pais → Provincia → Ciudad →
   Direccion → Entidad → Item` sea atómico. Hoy no lo hay: si el quinto `save` falla, los cuatro
   anteriores ya quedaron escritos.
3. Sacar `Pais`/`Provincia`/`Ciudad` del loop. Son datos de catálogo que no cambian por donación;
   resolverlos una vez y cachearlos.

`incentivos-service` ya resolvió el punto análogo con `@Version` + reintento + 409 (punto 36 de su
backlog). El patrón está escrito, se puede copiar.

---

## 23. Con N consumidores no hay orden: dos mensajes del mismo agregado se procesan a la vez

**Estado:** abierto
**Severidad:** media
**Archivo:** `.../RabbitMQ/DonacionListener.java`, topología de `RabbitConfig`

### Qué pasa

Competing consumers significa que el broker reparte los mensajes de a uno entre las instancias,
**en cualquier orden y sin sincronización entre ellas**. Dos mensajes del mismo `idDonacion` pueden
estar siendo atendidos por dos instancias al mismo tiempo, y el que se atiende segundo no tiene por
qué ser el que se mandó segundo.

Si el dominio es una máquina de estados —`PENDIENTE → EN_TRASLADO → ENTREGADO`, más los reingresos
a depósito— eso permite **retroceder el estado**: un "confirmar entrega" puede caer después de un
"iniciar traslado" y dejar el ítem como `PENDIENTE` cuando debería estar `ENTREGADO`.

Es el mismo efecto observable del punto 21, pero por otra causa: ahí es una redelivery del mismo
mensaje, acá son dos mensajes distintos que llegan desordenados.

### Por qué con una sola instancia no pasa

Con un consumidor, RabbitMQ le entrega los mensajes en el orden en que los publica, así que el
procesamiento es secuencial y el orden se respeta. **El orden no se pierde al usar el broker: se
pierde al agregar el segundo consumidor.** Por eso el punto no se habría detectado nunca.

### Qué decide el diseño

Depende de si el orden importa para el dominio, y eso es una decisión del equipo:

- **Si el estado tiene que avanzar siempre hacia adelante**, hay que garantizar el orden. El patrón
  es particionar por agregado: un exchange *consistent-hash* por `idDonacion`, o N colas con el
  id de la donación en la routing key. Así todos los mensajes de una donación caen en la misma
  instancia, en orden, y las donaciones distintas se reparten en paralelo. Es la solución
  estándar y es la que escala de verdad.

- **Si el estado tiene que ser válido sin importar el orden**, entonces alcanza con que cada
  transición valide su precondición en la base —el punto 8, el 9 y el 10 de este backlog—atrás de un
  `UPDATE` condicional. El handler rechaza el mensaje obsoleto en vez de aplicarlo.

Hoy no está guaranteed ninguna de las dos.

### Propuesta

Definir primero cuál de los dos modelos aplica, y después implementarlo. Si va por particionado,
el `RabbitConfig` de logística cambia: en vez de una cola compartida con N consumidores, N colas
con un binding por hash del `idDonacion` en la routing key.

**Lo que no conviene:** dejar la cola compartida y agregar un `@Version` esperando que eso ordene.
`@Version` **detecta** el conflicto, no lo resuelve: las dos instancias siguen en cualquier
orden, solo que ahora una de las dos recibe un error en vez de pisar a la otra. Para el estado final
es lo mismo, pero el mensaje va a la dead letter en lugar de procesarse.

---

## 24. El compose no se puede escalar: `container_name` y puerto fijo impiden el `--scale`

**Estado:** abierto
**Severidad:** media
**Archivo:** `docker-compose.yml`

### Qué pasa

El propio compose recomienda en un comentario hacer esto:

```
# servicio con `docker compose up --scale logisticas-service=2`.
```

**Y no funciona.** Hay dos cosas en `docker-compose.yml` que lo bloquean:

```yaml
logisticas-service:
  build: ./logisticas-service
  container_name: logisticas-service     # <-- impide --scale
  ports:
    - "8086:8086"                        # <-- no se puede bindear dos veces
```

1. **`container_name` fijo**: Compose no puede crear N contenedores con el mismo nombre. Con
   `--scale` y `container_name` declarado, el comando falla.
2. **Puerto de host fijo**: `"8086:8086"` intenta reservar el puerto 8086 del host para cada
   instancia. La segunda falla con `port is already allocated`.

Además, aunque las dos cosas se arreglaran, `SERVER_PORT` tendría que llegar a cada instancia con
un valor distinto —y hasta hace un rato no llegaba, porque el `application.properties` de logística
leía `${PORT:8086}` en vez de `${SERVER_PORT:8086}`. Eso ya está corregido.

### Por qué importa más allá de la defensa

El enunciado pide que el broker *"permita seleccionar entre más de 1 servicio de logística
disponible"*. La topología del broker **sí** lo cumple: `logistica.integracion.queue` es una cola
compartida con N consumidores, que es exactamente competing consumers.

Lo que no llega a existir es la forma de levantar la segunda instancia. Así que hoy el requisito se
cumple a nivel de diseño y no a nivel de despliegue, y la diferencia se nota justo cuando se
intenta probar.

La forma de correr N instancias sin compose es levantar el mismo jar varias veces apuntando al
mismo broker y a la misma base, que es lo que describe el `DonacionListener`. Eso funciona hoy.

### Propuesta

1. Sacar `container_name` de los cuatro servicios de dominio (dejándolo solo en `mysql` y
   `rabbitmq`, donde no hace falta escalar).
2. Sacar el mapeo de puerto fijo de logística, o dejarlo solo en el perfil por defecto para no
   romper el desarrollo de a uno.
3. Para dar puertos distintos por instancia, dejar que Docker asigne con `- "8086"` (puerto
   efímero) y documentar cómo descubrir el asignado.

---
# Corregidos

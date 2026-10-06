# Pendientes técnicos de `notificaciones-service`

Registro de problemas conocidos del servicio, con el motivo y la propuesta de arreglo para
que no se pierdan de vista al crecer el código.

**Están ordenados de más urgente a menos urgente**, no por número de punto. El número es un
ID estable y no se renumera nunca, así que quedan huecos. Un punto corregido se borra de esta
lista y pasa a la sección [Corregidos](#corregidos) del final.

El orden no es el de la severidad declarada en cada punto sino el del daño real: cuánto se
rompe cuando pasa, y qué tan fácil es que pase.

| # | Punto | Por qué está acá |
|---|---|---|
| 1 | 1 | Un test contra n8n sin `@Disabled` rompe `mvn verify` en cualquier máquina sin n8n |
| 2 | 2 | El consumidor no relanza los fallos, así que no hay reintento ni cola de muertas |
| 3 | 3 | Solo hay dos tests, y ninguno cubre el camino de Rabbit |
| 6 | 6 | El `id_mensaje` no viaja en el JSON: el consumidor vuelve con un UUID nuevo y pisa la FK |
| 8 | 8 | Sin validación en el borde: un `asunto` o `cuerpo` faltante revienta en MySQL y devuelve 500 |
| 10 | 10 | `RestTemplate` sin timeouts: si n8n cuelga, el hilo del consumidor se bloquea para siempre |
| 11 | 11 | Credenciales de MySQL hardcodeadas y RabbitMQ sin configurar en `application.properties` |
| 12 | 12 | `NotificacionMapper` nunca setea `tipoMedioDeContacto`: el GET siempre lo devuelve `null` |
| 14 | 14 | El DTO de entrada no valida nada y el manejador de excepciones está comentado |
| 15 | 15 | La cola no tiene dead letter y los errores de conversión se reintentan en loop |
---

## 1. Un test contra n8n sin `@Disabled` rompe `mvn verify`

**Estado:** abierto
**Severidad:** alta
**Archivo:** `src/test/java/ar/edu/utn/frba/ddsi/notificaciones/test_integracion/N8nIntegrationTest.java`

### Qué pasa

`N8nIntegrationTest.deberiaEnviarMailRealAN8n` construye un `Mail` y lo manda por el
`NotificacionGateway`, o sea que sale por HTTP a la URL de `servicio.n8n.url`. Si n8n no está
corriendo, falla con `ResourceAccess I/O error: Connection refused` y el reactor se detiene en
`notificaciones-service`: los tres módulos siguientes no se ejecutan.

El nombre del método dice lo que es: envía un mail real. No es un test unitario, es un test de
integración contra un servicio externo, y está sin `@Disabled`, sin `@Tag` y sin ninguna
condición que lo saltee.

### Por qué importa más de lo que parece

Es preexistente, pero no es inocuo: hace que `mvn verify` no pueda usarse como criterio de
"¿está todo bien?" sin tener n8n andando. En este repo pasó: el fallo se vio primero como si
fuera del módulo de notificaciones, escondido detrás de un error de MySQL.

### Estado después de levantar n8n

Con n8n corriendo el test **pasa**: `Tests run: 1, Failures: 0, Errors: 0`. El `mvn verify`
completo queda en verde y los 330 tests del reactor pasan.

Eso no cierra el punto, y conviene que quede claro por qué: el test pasó porque se corrigió la
URL por defecto (punto 17 de la sección `# Corregidos`), no porque el test sea independiente de
n8n. Sigue siendo un test que depende de un servicio externo para decidir si el build del equipo
pasa, y mañana vuelve a fallar con `Connection refused` en la máquina de cualquiera que no
tenga n8n levantado.

### Propuesta

Dos opciones, y la elección es del equipo:

- `@Tag("integracion")` más la exclusión del tag en el perfil por defecto, para que corra solo
  con `-Pintegration` o un perfil activo.
- `@Disabled` con el motivo en el mensaje, y que quien quiera lo levante a mano.

Lo que no conviene es dejarlo como está: un test que depende de un servicio de terceros no
debe decidir si el build del equipo pasa.

---

## 2. El consumidor no relanza los fallos, así que no hay reintento ni cola de muertas

**Estado:** abierto, pero es una mejora pendiente y no un bug
**Severidad:** baja
**Archivo:** `src/main/java/ar/edu/utn/frba/ddsi/notificaciones/messaging/ConsumidorNotificaciones.java`

### Qué pasa

`ConsumidorNotificaciones.recibir` marca la notificación como fallida, loguea el error y
**no relanza**. Con eso el mensaje se da por consumido y desaparece de la cola.

Un `IllegalArgumentException` de `MedioDeEnvioFactory` (un medio de contacto mal mapeado) es
determinista: va a fallar igual las 5 veces que se reintente. Por eso no relanzar es
defensible. El problema es que no hay ninguna de las dos salidas que quedan:

- Reintento con espera para lo que sí puede fallar por una causa transitoria (el webhook de
  n8n caído, un timeout).
- Cola de mensajes muertos (dead letter exchange) para lo que no.

### Lo que sí está bien

Como el productor guarda la notificación en la base **antes** de publicar, toda notification
fallida queda en la fila con estado `FALLIDA` y se puede reintentar desde ahí. Ese es el
agente de recuperación. Lo que falta es el automatismo.

### Qué se corrigió de este punto

El "se traga en silencio" ya no es exacto. Hoy **cada** salida del listener loguea:

```
log.warn("LLEGA un mensaje vacío a la cola de notificaciones, se descarta");
log.warn("La notificación {} no existe, el mensaje se descarta", mensaje.id());
log.warn("LLEGA una solicitud sin medio de contacto, se descarta: {}", cuerpoCrudo);
log.error("No se pudo procesar el mensaje de la cola: {}", ...);
```

Y el descarte en vez de reencolar está justificado por escrito en el javadoc de la clase: para
un mail que no se puede entregar, reintentar cinco veces seguido es peor que dejarlo, porque la
fila queda en `FALLIDA` y se puede reintentar desde la base.

Lo que queda es exactamente lo mismo de antes: el automatismo.

### Propuesta

Configurar la dead letter queue en `RabbitConfig` y relanzar en el caso transitorio. Con la
base ya guardando el estado, el reintento manual es posible hoy; la propuesta es no
depender de que alguien se acuerde.

**Esta propuesta es la misma del punto 15**, que la tiene más completa y con la evidencia del
log de 1.9 GB. Queda acá la referencia al caso transitorio, que es el ángulo que el 15 no
cubre.

---

## 3. Solo hay dos tests, y ninguno cubre el camino de Rabbit

**Estado:** abierto
**Severidad:** media
**Archivos:** `src/test/`

### Qué pasa

El módulo tiene dos tests:

- `NotificacionesServiceApplicationTests`: carga el contexto de Spring.
- `N8nIntegrationTest`: el del punto 1.

No hay ningún test de `GestorNotificaciones` (el orden guardar → publicar), de
`Notificacion` (el constructor de tres parámetros que se agregó) ni de `ConsumidorNotificaciones`.
Es decir, el código que cambió en el último merge no tiene nada que lo cubra.

### Propuesta

- `GestorNotificacionesTest` con `ProductorNotificaciones` mockeado, verificando que la
  notificación se guarda antes de publicar. El orden importa y es el motivo del comentario en
  el código, así que debería estar protegido por un test.
- `NotificacionTest` sobre el constructor y los tres `marcar*`, en especial que `marcarEnviada`
  setea `fechaEnvio` y que `fechaEnvio` es nullable en una notificación nueva.

---

## 6. El `id_mensaje` no viaja en el JSON: el consumidor vuelve con un UUID nuevo y pisa la FK

**Estado:** abierto
**Severidad:** alta
**Archivos:**
`src/main/java/ar/edu/utn/frba/ddsi/notificaciones/models/entities/Mensaje/Mensaje.java:16`,
`src/main/java/ar/edu/utn/frba/ddsi/notificaciones/messaging/ConsumidorNotificaciones.java:65`

### Qué pasa

`Mensaje` tiene el `@Getter` en `asunto` (línea 17) y en `cuerpo` (línea 20), pero **no en
`id_mensaje`**. Jackson serializa por getters, así que el `id` del mensaje nunca entra en el JSON que
va por Rabbit.

Del otro lado, `Mensaje` tiene `@NoArgsConstructor`, y el inicializador de campo de la línea 16 se
vuelve a ejecutar: `id_mensaje = UUID.randomUUID()`. O sea, la `Notificacion` que recibe el consumidor
trae un `Mensaje` con **asunto y cuerpo correctos pero un UUID recién generado que no existe en la
tabla `mensajes`**.

Después, `ConsumidorNotificaciones.recibir:65` hace
`repositorioNotificaciones.save(notificacion)` con ese mensaje colgando. Y como `Notificacion.id` es un
UUID asignado (no hay `@GeneratedValue`), el `isNew` de Spring Data siempre da `false` y `save` va
siempre por `merge`: el UPDATE reescribe la columna `id_mensaje` con el UUID nuevo.

Dos desenlaces, y los dos son malos:

- Con la FK que generó `ddl-auto=update` (ver punto 5), el UPDATE viola la restricción y revienta.
- Sin FK, la fila queda apuntando a un `mensajes` inexistente, y el `GET /notificaciones/{id}` devuelve
  `asunto: null`.

Y en el primer caso el error escapa: la línea 65 está **fuera** del `try/catch` de las líneas 50-63,
así que sube al container del listener, que reencola el mensaje. Como es determinista, la notificación
queda en loop de reentrega infinita y nunca se marca `ENVIADA`.

### El `equals`/`hashCode`

`Notificacion` y `Mensaje` tienen `@Getter`/`@Setter` y nada más: no hay `@Data` ni
`@EqualsAndHashCode`, así que la igualdad es por identidad. Cualquier deduplicación, comparación o uso
en un `Set` sobre estas entidades compara referencias, no contenido. Es la razón por la que el punto 3
(donde ningún test cubre el camino de Rabbit) no se detectó antes.

### Propuesta

- `@Getter` en `id_mensaje`, o el `@Data` de Lombok en ambas entidades (que además resuelve el
  `equals`/`hashCode`).
- Sacar el `save` de la línea 65 de la ecuación del id: el consumidor solo tiene que actualizar el
  estado, y para eso alcanza con un update por id en lugar de un `save` de la entidad entera. Así el
  mensaje que llega por Rabbit nunca se persiste.

---

## 8. Sin validación en el borde: un `asunto` o `cuerpo` faltante revienta en MySQL y devuelve 500

**Estado:** abierto
**Severidad:** media
**Archivos:**
`src/main/java/ar/edu/utn/frba/ddsi/notificaciones/dto/SolicitudNotificacionDTO.java:10`,
`src/main/java/ar/edu/utn/frba/ddsi/notificaciones/controllers/NotificadorController.java:42`

### Qué pasa

`SolicitudNotificacionDTO` es un POJO pelado con cuatro `String`, sin `@NotBlank`, sin `@Valid`, y
`NotificadorController.recibirSolicitudNotificacion:42` no valida nada: toma el body tal cual.

Un pedido razonable y mal formado:

```json
{ "medioDeContacto": "email", "direccionDeContacto": "ana@example.com" }
```

pasa el `@RequestBody`, pasa `NotificadorService`, llega a
`GestorNotificaciones.crearNotificacion:78` y construye `new Mensaje(null, null)`. Como
`mensajes.asunto` y `mensajes.cuerpo` están declarados `nullable = false`
(`Mensaje.java:18` y `Mensaje.java:21`), el INSERT revienta por violación de restricción. Igual con
`direccionDeContacto` en null (`Notificacion.java:33`).

Como el `catch` del controller solo cubre `IllegalArgumentException`, la respuesta es un **500 con la
excepción de MySQL adentro**, no un 400. Para el que llama es indistinguible de que se cayó el servicio.

### El manejador global no existe

`exceptions/GlobalExceptionHandler.java` está **comentado entero** (líneas 8-27, dentro de un bloque
`/* ... */`). Es decir que hoy no hay ningún `@RestControllerAdvice`: ningún `@ExceptionHandler` mapea
una excepción de base de datos, ni la de persistencia, ni nada. Cualquier error interno sale como 500
con el mensaje crudo.

### Propuesta

- `@NotBlank` en los cuatro campos del DTO y `@Valid` en el parámetro del controller, para que un body
  incompleto se rechace con 400 y un mensaje que diga qué falta.
- Descomentar y arreglar `GlobalExceptionHandler`: un 400 para los errores de validación, un 500 con
  cuerpo genérico para el resto, y log del error real en el log y no en la respuesta.
- De paso, validar el medio de contacto en el borde (punto 7), que es la otra validación que hoy no
  existe y por eso los tipos inválidos se descubren tarde.

---

## 10. `RestTemplate` sin timeouts: si n8n cuelga, el hilo del consumidor se bloquea para siempre

**Estado:** abierto
**Severidad:** media
**Archivo:**
`src/main/java/ar/edu/utn/frba/ddsi/notificaciones/config/RestTemplateConfig.java:11`

### Qué pasa

`RestTemplateConfig.restTemplate()` devuelve un `new RestTemplate()` pelado. Eso usa
`SimpleClientHttpRequestFactory`, cuyos timeouts de conexión y de lectura son infinitos (`-1`): el
default de `HttpURLConnection`.

`N8nClient.enviarNotificacion:19-25` se llama de forma **síncrona desde el hilo del
`@RabbitListener`**. Con la concurrencia por defecto del container (1 consumidor), un único n8n que
acepta la conexión TCP y no contesta deja ese hilo colgado para siempre: la cola deja de vaciarse, las
notificaciones se acumulan en `PENDIENTE` en la base, y no hay timeout, ni log, ni error. El síntoma es
"las notificaciones dejaron de salir" sin ninguna pista de por qué.

Aclaración: hoy esto no se siente, porque el punto 4 revienta antes de llegar a publicar. Pasa a ser el
problema dominante en el momento en que se agregue el converter.

### Propuesta

`SimpleClientHttpRequestFactory` con `setConnectTimeout` y `setReadTimeout` en milisegundos (3-5 s para
conectar, 10 s para leer un webhook). A futuro, `ClientHttpRequestFactorySettings` o el
`RestClient` de Spring 6.1, que ya permite definir los timeouts por properties.

---

## 11. Credenciales de MySQL hardcodeadas y RabbitMQ sin configurar en `application.properties`

**Estado:** abierto
**Severidad:** media
**Archivo:** `src/main/resources/application.properties:8`

### Qué pasa

Las líneas 9-10 son valores literales:

```properties
spring.datasource.username=marcelo
spring.datasource.password=losbabasonicos
```

Sin `${...}` ni fallback, al contrario que en las otras dos que sí están parametrizadas
(`server.port=${SERVER_PORT:8083}` en la línea 4 y `servicio.n8n.url=${N8N_URL:...}` en la línea 6). Dos
consecuencias: la contraseña de la base está en el repositorio, y el servicio solo levanta contra un MySQL
que tenga exactamente ese usuario y esa contraseña. En cualquier otro entorno hay que editar el archivo.

### RabbitMQ sin configurar

**Esta parte ya está resuelta.** `application.properties` hoy tiene el bloque completo, con
variables de entorno y valor por defecto:

```properties
spring.rabbitmq.host=${RABBITMQ_HOST:localhost}
spring.rabbitmq.port=${RABBITMQ_PORT:5672}
spring.rabbitmq.username=${RABBITMQ_USERNAME:guest}
spring.rabbitmq.password=${RABBITMQ_PASSWORD:guest}
```

Era necesario: sin esto, dentro de un contenedor el servicio buscaba `localhost` y no encontraba
al broker, y la cola quedaba sin consumidor. Verificado en vivo — la cola `notificaciones` tiene
consumidor y los avisos de los dos servicios de dominio salen por ella.

### Qué sigue pendiente de este punto

Lo de MySQL, que es la mitad del título original y sigue igual:

```properties
spring.datasource.username=marcelo
spring.datasource.password=losbabasonicos
```

Sin `${...}` ni fallback, al contrario que el puerto y la URL de n8n, que sí están
parametrizadas. Dos consecuencias: la contraseña de la base está en el repositorio, y el
servicio solo levanta contra una base que tenga exactamente ese usuario y esa contraseña.

También queda `spring.jpa.show-sql=true`, que deja cada statement SQL en el log de una instancia
desplegada: es ruido y puede filtrar datos.

### Propuesta

- `spring.datasource.username=${DB_USERNAME:marcelo}` y
  `spring.datasource.password=${DB_PASSWORD:...}`, con las credenciales reales solo en variables de
  entorno o en un `.env` que no se commite.
- Mover `spring.jpa.show-sql` a un perfil de desarrollo.

Lo de Rabbit ya no hace falta: está hecho, y la verificación está arriba.

---

## 12. `NotificacionMapper` nunca setea `tipoMedioDeContacto`: el GET siempre lo devuelve `null`

**Estado:** abierto
**Severidad:** baja
**Archivos:**
`src/main/java/ar/edu/utn/frba/ddsi/notificaciones/mappers/NotificacionMapper.java:9`,
`src/main/java/ar/edu/utn/frba/ddsi/notificaciones/dto/NotificacionDTO.java:11`

### Qué pasa

`NotificacionDTO` declara el campo `tipoMedioDeContacto` (línea 11) y la entidad lo tiene con getter
(`Notificacion.java:56`), pero `NotificacionMapper.notificacionDTO` solo setea `asunto`, `cuerpo`,
`direccionDeContacto`, `estado`, `fechaCreacion` y `fechaEnvio`. La línea para el tipo no está.

Resultado: `GET /notificaciones/{id}` devuelve siempre `"tipoMedioDeContacto": null`, aunque la columna
es `nullable = false` y el valor está en la base. Como `Jackson` serializa el null sin problema, nadie se
entera, y el que consume el endpoint no puede distinguir por el campo por qué medio se pidió la
notificación.

De paso, las líneas 15 y 16 del mapper hacen `getEstado().toString()` y
`getFechaCreacion().toString()` sin null check, al contrario que la 17-19 que sí cubre `fechaEnvio`.
Hoy el constructor y las restricciones los garantizan, pero es la única parte del mapper que confía en
que el dato está.

### Propuesta

Una línea: `notificacionDTO.setTipoMedioDeContacto(notificacion.getTipoMedioDeContacto());`. Conviene
sumar un `NotificacionMapperTest`, que hoy no existe (punto 3), y un
`GET /notificaciones/{id}` de integración que verifique el JSON completo.

---

## 14. El DTO de entrada no tiene validación de Bean Validation y el manejador de excepciones está comentado

**Estado:** abierto
**Severidad:** alta
**Archivos:** `src/main/java/ar/edu/utn/frba/ddsi/notificaciones/dto/SolicitudNotificacionDTO.java`,
`src/main/java/ar/edu/utn/frba/ddsi/notificaciones/exceptions/GlobalExceptionHandler.java`

### Qué pasa

`SolicitudNotificacionDTO` no tiene ni una anotación de validación: `@NotBlank` sobre
`medioDeContacto`, `direccionDeContacto`, `asuntoMensaje` y `cuerpoMensaje`. Y el controller
recibe el body **sin `@Valid`**:

```java
@PostMapping
public ResponseEntity<String> recibirSolicitudNotificacion(@RequestBody SolicitudNotificacionDTO dto) {
```

Esos cuatro campos alimentan columnas con `nullable = false` (`Mensaje.asunto`,
`Mensaje.cuerpo`, `Notificacion.direccionDeContacto`, `Notificacion.tipoMedioDeContacto`). Un
`null` en cualquiera de ellos no se rechaza en el borde: viaja entero y muere en el INSERT con
violación de restricción, que es un error de base de datos difícil de leer.

### Y el manejador de excepciones está comentado

`GlobalExceptionHandler` existe pero **todo el archivo está comentado**. Eso es lo que convierte
el `null` en un `500` genérico sin cuerpo, en vez de un `400` que diga qué campo faltó. El
controller sí captura `IllegalArgumentException` y devuelve `400`, pero un
`DataIntegrityViolationException` de JPA no es de esa clase y se escapa.

### Detalle relacionado que va a aparecer después

`incentivos-service` manda un DTO con el campo `direccionContacto` y este módulo espera
`direccionDeContacto`. Jackson deja el campo en `null` sin error. O sea que **este bug se
activaría en el momento exacto en que se arregle el 401 del punto 13**: cambiaría el `401` por
un `500` con un `null` en el log, y parecería un problema nuevo. Está documentado como punto
38 del backlog de `incentivos-service`.

### Propuesta

1. `@NotBlank` en los cuatro campos del DTO y `@Valid` en el controller.
2. Descomentar `GlobalExceptionHandler` y agregar un `@ExceptionHandler` para
   `DataIntegrityViolationException` que devuelva `400` con el nombre del campo.
3. Agregar `spring-boot-starter-validation`, que no está en el pom: las anotaciones de
   jakarta.validation no se procesan sin él, y hoy no hay ni una en el módulo.

## 15. La cola de notificaciones no tiene dead letter y los errores de conversión se reintentan en loop

**Estado:** abierto
**Severidad:** media
**Archivos:** `.../config/rabbit/MessageConverterConfig.java`

### Qué pasa

El punto 2 de este mismo backlog propone una dead letter queue y sigue sin implementarse. Se
volvió urgente por algo concreto y medido: un error de conversión de mensaje dispara el
`ConditionalRejectingErrorHandler`, que reintenta, y cada intento escribe el stack trace
completo. Con cuatro servicios publicando en la misma cola, un encabezado mal puesto multiplica
el log por cada mensaje y cada intento.

En la corrida donde se midió el problema del tipo de mensaje, el log del servicio llegó a
**1.9 GB**. Corregir la causa lo bajó a 73 KB, pero la falta de freno sigue: cualquier error de
conversión futuro tiene el mismo comportamiento.

### Propuesta

- Dead letter queue en `RabbitConfig`, con la cola principal configurada para derivar ahí lo que
  no se puede procesar.
- Un `RepetirYRechazarSiFalla` con tope de reintentos y delay, o el `RepetirConDeadLetter` de
  Spring AMQP, para que un error de conversión no se reintente tres veces.
- Bajar el nivel de log de esos reintentos: un error de conversión es irrecuperable y necesita
  una línea, no veinte.

**Lo que no se hizo y por qué:** es una decisión de infraestructura que afecta la operación del
broker, no un bug de integración. Corregir el tipo de mensaje elimina la causa concreta que se
midió; la dead letter previene la clase de problema, no este caso.

---

# Corregidos

### 4. No había converter de mensajes: `RabbitTemplate` publicaba con `SimpleMessageConverter`

**Estado:** corregido
**Severidad:** crítica
**Archivos:** `.../config/rabbit/MessageConverterConfig.java`, `.../config/rabbit/RabbitConfig.java`

### Qué pasaba

Sin declarar un `MessageConverter`, Spring Boot deja el `RabbitTemplate` con
`SimpleMessageConverter`, que solo sabe manejar `byte[]`, `String` y `Serializable`. Los DTO de
integración no son ninguno de los tres, así que la publicación fallaba con
`IllegalArgumentException` **dentro del `catch` del productor**: el servicio respondía 202 y el
mensaje nunca salía. Los otros dos módulos con Rabbit sí definían su converter en un
`RabbitMQConfig` propio; a este lo que le faltaba era esa cuarta pieza.

### Qué se hizo

`MessageConverterConfig` declara un `MessageConverter` propio, con la conversión a mano en las
dos direcciones: al publicar serializa con el `ObjectMapper` de la aplicación, y al recibir
devuelve el body como `String` sin deserializar a nada.

Que sea propio y no un `Jackson2JsonMessageConverter` con el tipo deshabilitado es
intencionado: el `__TypeId__` rompe la integración entre servicios y, con el mapper en `null`, el
converter de Jackson tira `NullPointerException` antes de llegar al listener. Ver punto 16.

El `RabbitConfig` conserva solo la topología (exchange, colas, bindings).

### Cómo se verificó

Con los cuatro servicios levantados contra MySQL y RabbitMQ reales, se crea un donante en
`donaciones-service` y se hace `PATCH /api/perfiles/donacion/{id}` hasta completar la misión.
Los tres avisos que dispara el evento de dominio llegan por Rabbit y quedan `ENVIADA` con
`fecha_envio` puesta. Sin el converter, ninguno habría salido del proceso.

---

### 5. `@OneToOne` sin cascada: el `Mensaje` nunca se insertaba y la FK `id_mensaje` reventaba el INSERT

**Estado:** corregido
**Severidad:** alta
**Archivo:** `.../models/entities/Notificacion/Notificacion.java:50`

### Qué pasaba

`Notificacion.mensaje` estaba declarado como `@OneToOne` **sin `cascade`**, y
`GestorNotificaciones.crearNotificacion` arma un `new Mensaje(asunto, cuerpo)` —una entidad
nueva, transient— y guarda solo la notificación.

Sin cascada, Hibernate no emite el `INSERT INTO mensajes`: escribe el UUID del `Mensaje`
transient en `notificaciones.id_mensaje`, y la FK revienta el INSERT. O sea que **no se
persistía ni una sola notificación**, y la respuesta era un `500` con la excepción de MySQL
adentro.

### Por qué `ALL` y no `PERSIST`

Este es el detalle que costó entender. **`CascadeType.PERSIST` no sirve acá, y no por poco.**
`Notificacion.id` es un UUID asignado a mano, sin `@GeneratedValue`, así que el `isNew` de Spring
Data siempre da `false` y `JpaRepository.save()` va **siempre por `merge()`**. El `PERSIST` solo
actúa en `persist()`, que nunca se llama: con `PERSIST` el `INSERT` del mensaje no sale igual.

Por eso es `ALL`, que además cubre el `merge` que sí ocurre.

### Cómo se verificó

Con la base limpia, las notificaciones de los flujos reales quedan persistidas y con su mensaje:

```
ENVIADA  ana@test.com   Nuevo Registro en DonaTrack
ENVIADA  luis@test.com  ¡Misión completada!
ENVIADA  luis@test.com  Nueva categoría
ENVIADA  luis@test.com  Nueva misión disponible
```

Cada fila tiene su `id_mensaje` apuntando a una fila real de `mensajes`.

---

### 7. El factory no normalizaba el tipo: `incentivos-service` manda `"EMAIL"` y todo quedaba `FALLIDA`

**Estado:** corregido
**Severidad:** alta
**Archivo:** `.../models/entities/MedioDeEnvio/MedioDeEnvioFactory.java`

### Qué pasaba

`MedioDeEnvioFactory` recibía `Map<String, MedioDeEnvio>` por constructor, o sea que el mapa está
indexado por **nombre de bean** en minúsculas (`email`, `telefono`, `whatsapp`), y buscaba con
`get()` **case-sensitive**. Los servicios mandan `"EMAIL"` o `"WHATSAPP"`.

El resultado era `medios.get("EMAIL")` → `null` → `IllegalArgumentException` → `FALLIDA`. Y como
el error se producía en el consumidor y no en el borde, **todo el flujo de incentivización**
—misiones completadas, cambio de misión, cambio de categoría— fallaba en silencio.

### Qué se hizo

Normalización en la frontera del factory, que es el punto donde el error todavía es recuperable:
`trim().toLowerCase()`. Se agregan además alias (`mail`, `gmail`, `correo`, `tel`, `celular`,
`wa`), porque el tipo viene de otro servicio y no hay contrato que obligue a una forma sola.

La alternativa que estaba propuesta en el punto original —un enum con `@JsonCreator`— es mejor a
largo plazo, pero cambia el contrato de la API pública. La normalización arregla el bug sin
romper nada.

### Cómo se verificó

En la base, las notificaciones guardadas quedaron con el tipo en minúsculas, que es lo que el
mapper de la base espera:

```
estado  tipo_medio_contacto
ENVIADA email
```

Y todas llegaron a `ENVIADA`, o sea que el factory las resolvió y el medio se usó de verdad.

---

### 9. El consumidor no era idempotente: una reentrega de Rabbit reenviaba la notificación y pisaba el estado

**Estado:** corregido
**Severidad:** media
**Archivo:** `.../messaging/ConsumidorNotificaciones.java:98-119`

### Qué pasaba

`recibir` mandaba la notificación sin mirar en qué estado estaba. RabbitMQ es de entrega *al
menos una vez*: si la conexión se cae después de que n8n ya recibió el webhook pero antes de que
el listener ackee, el mensaje vuelve a la cola y **la notificación sale dos veces**. El mail
duplicado al donante es visible; el historial es lo peor, porque el `save` sobrescribía el
estado sin condición y se perdía que en algún momento salió.

Escenario: una notificación quedó `ENVIADA`, se reentrega y ahora n8n no responde.
`marcarFallida` la deja en `FALLIDA` y el registro pierde que ya había salido. Al revés también.

### Qué se hizo

`procesarAvisoDeNotificacionExistente` busca por id y **no reenvía si el estado ya es `ENVIADA`**:

```java
if (notificacion.getEstado() == EstadoNotificacion.ENVIADA) {
    log.debug("La notificación {} ya estaba enviada, no se reenvía", notificacion.getId());
    return;
}
```

También se agregó el caso del mensaje que apunta a una notificación inexistente, que antes
provocaba el loop de reentrega infinita descrito en el punto 6: ahora se descarta con `log.warn`.

### Lo que quedó de este punto

El `save` sigue escribiendo la entidad entera, así que **queda sin resolver la parte de las
transiciones**: `marcarEnviada`, `marcarFallida` y `marcarPendiente` no validan nada, y en
particular `marcarPendiente()` no limpia `fechaEnvio`, con lo que queda una fila `PENDIENTE` con
fecha de envío, que no significa nada y contradice lo que el mapper muestra en el GET.

Es un problema más chico que el que se corrigió y no se tocó: la fila ya no se pisa por
redelivería, que era lo que rompía el historial.

---

### 13. Todos los endpoints devolvían 401: la seguridad por defecto bloqueaba la integración

**Estado:** corregido
**Severidad:** crítica
**Archivo:** `pom.xml` (padre), `notificaciones-service/pom.xml`

### Qué pasaba

`spring-boot-starter-security` era dependencia **directa** de este pom, y el módulo no declaraba
ninguna clase de seguridad: no había `SecurityFilterChain`, ni `permitAll`. Spring Boot aplicaba
su configuración por defecto, generaba una password aleatoria al arrancar y **toda** petición
sin credenciales recibía `401`, incluido `POST /api/notificaciones`.

Comprobado en su momento: `POST /api/notificaciones` con un payload bien formado devolvía
`401` y la tabla `notificaciones` quedaba en 0 filas.

### Por qué era el bug más caro del módulo

Es el único módulo de los cuatro con seguridad activa, así que era el único que podía
explicar un `401`. Y explica por qué el problema estuvo invisible tanto tiempo: el síntoma
observable desde afuera es "no llegan notificaciones", que se lee como "n8n no está levantado" o
"falta algo de configuración", y no como "el receptor exige autenticación".

Peor: **tienta a un arreglo que rompe la seguridad**. La respuesta obvia cuando se ve un 401 en
un servicio interno es aflojar la autorización, y eso lo haría sin que nadie lo pidiera.

### Qué se hizo

Se sacó `spring-boot-starter-security` de las **dependencias comunes del pom padre**, no solo de
este módulo. Ese es el punto clave: estaba en el padre, así que lo heredaban los cuatro módulos
y cualquiera de ellos podía devolver 401.

De `incentivos-service` se conserva la dependencia y su `SecurityConfig` explícita, porque ese
módulo sí tiene endpoints de administración que necesitan una política escrita. Los otros tres
quedan sin filtro de seguridad, que es lo que corresponde a servicios internos de una red
interna.

### Cómo se verificó

Con los cuatro servicios levantados:

| Endpoint | Antes | Ahora |
|---|---|---|
| `POST /api/notificaciones` | **401** | **202** `solicitud procesada con éxito` |
| Notificaciones persistidas | 0 | 1 |

El `202` es además lo que el Swagger promete, así que de paso se cierra parte del punto 4: el
controller devolvía `400` con el texto interno de la excepción en vez del `202`.

### Lo que sigue pendiente del otro lado

El `202` no significa que la notificación se haya enviado. El servicio la persiste, la publica a
su propia cola y devuelve; el consumidor la descarta con

```
WARN  ConsumidorNotificaciones : La notificación 88e8a438-... no existe, el mensaje se descarta
```

porque **el id no viaja en el JSON**: es el punto 6 de este mismo backlog, y sigue abierto. Ojo
con la diferencia entre los dos caminos, que es fácil de confundir:

- **Los avisos de los servicios de dominio** (donaciones e incentivos publican por Rabbit) quedan
  `ENVIADA` con `fecha_envio` puesta. Ese camino está completo.
- **El `POST` directo a notificaciones** deja la fila en `PENDIENTE`, porque es el único que pasa
  por el guardado local y después por la re-publicación a la cola propia.

Que el consumidor descarte en vez de reencolar en loop es lo correcto y ya está corregido; lo que
falta es que el id viaje.

---

### 16. El `__TypeId__` del converter rompía la integración entre servicios

**Estado:** corregido
**Severidad:** crítica
**Archivos:** `.../config/rabbit/MessageConverterConfig.java`, `.../config/rabbit/RabbitConfig.java`

### Qué pasaba

`Jackson2JsonMessageConverter` escribe por defecto un encabezado `__TypeId__` con el nombre de
la clase Java del mensaje, y del otro lado intenta resolver esa clase para deserializar. El
resultado medido con el flujo real: cada aviso de `incentivos-service` moría en el consumidor con

```
MessageConversionException: failed to resolve class name.
Class not found [ar.edu.utn.frba.ddsi.incentivos.dto.Notificaciones.PerfilNotificacionDTO]
```

porque este módulo no tiene —ni debe tener— la clase del productor.

**El síntoma era confuso en un punto clave:** el productor logueaba
`Notificación publicada para <email>` y el mensaje salía del broker (`routed=true`), así que
todo parecía correcto. El fallo estaba del lado del consumidor, y el mensaje se perdía ahí.

Además el mensaje provocaba un **ciclo de reintentos**: el error de conversión dispara el
`ConditionalRejectingErrorHandler`, que reintenta, y el log del servicio llegó a **1.9 GB** en
una sola corrida de pruebas. `ConditionalRejectingErrorHandler` descarta el mensaje, pero antes
de eso escribe varias líneas de stack trace por cada intento.

### Por qué rompe una regla del enunciado

El enunciado pide que los servicios de dominio **no compartan modelo**, y el `__TypeId__` es
exactamente un acoplamiento a nivel de bytecode entre servicios que no se conocen: el productor
le está diciendo al consumidor "deserializá esta clase", y el consumidor tiene que tenerla en su
classpath. El contrato que define el enunciado es **el JSON**, o sea los nombres de los campos.

### Por qué no alcanza con desactivar el tipo

La solución obvia es `converter.setAlwaysConvertToInferredType(false)` con un
`Jackson2JavaTypeMapper` que devuelva `null`. **Se probó y no funciona**: con el tipo en `null`,
el converter lo desreferencia y tira `NullPointerException` antes de llegar al listener. Es
decir, desactivar el tipo no alcanza; hay que **evitar el converter de Jackson**.

### Qué se hizo

`MessageConverterConfig` declara un `MessageConverter` propio con la conversión a mano en las
dos direcciones:

- **Al publicar:** Jackson serializa con el `ObjectMapper` de la aplicación, así respeta los
  módulos ya registrados (fechas ISO, parámetros nulos). El encabezado `__TypeId__` se borra.
- **Al recibir:** se devuelve el body como `String` sin deserializar a nada. El listener decide
  el tipo por el contenido del JSON.

El `RabbitConfig` conserva solo la topología (exchange, colas, bindings) y su comentario
apunta al `MessageConverterConfig` para la explicación.

### Cómo se verificó

Flujo real end-to-end: se crea un donante en `donaciones-service` (que crea su perfil en
`incentivos-service` por HTTP), se hace `PATCH /api/perfiles/donacion/{id}` y la misión se
completa. Los tres avisos que dispara el evento de dominio llegan y quedan persistidos:

```
¡Misión completada!     Completaste 'Primera donación' y obtuviste la insignia 'Primer paso'...
Nueva categoría         Completaste la categoría 'Colaborador' y avanzaste a 'Sostenedor'.
Nueva misión disponible Completaste 'Primera donación'. Tu nueva misión es 'Racha'.
```

El log del servicio bajó de **1.9 GB a 73 KB** y la cola quedó en 0 mensajes: no hay reintentos.

Con n8n andando los tres quedan `ENVIADA` con `fecha_envio` puesta. Antes quedaban en `FALLIDA`,
que era el comportamiento correcto dado que n8n no estaba corriendo: la notificación se encoló,
se intentó enviar, el envío externo falló y quedó registrado.

---

### 17. El default de `servicio.n8n.url` apuntaba a `/webhook/` pelado y daba 404

**Estado:** corregido
**Severidad:** crítica
**Archivo:** `src/main/resources/application.properties:6`

### Qué pasaba

El default de la URL de n8n terminaba en el prefijo, sin el nombre del webhook:

```properties
servicio.n8n.url=${N8N_URL:http://localhost:5678/webhook/}
```

El endpoint real es `http://localhost:5678/webhook/notificaciones`. Con el default, cada
notificación terminaba en `FALLIDA` y el error era un `404` que **no distinguía una URL mal
configurada de un webhook que no existe**:

```
IllegalArgumentException: Ocurrió un problema inesperado al enviar la notificación:
404 Not Found: "<!DOCTYPE html>..."
Caused by: HttpClientErrorException$NotFound
```

`incentivos-service` ya tenía el default correcto (`.../webhook/incentivos`), así que la
asimetría era solo de un lado.

### Por qué estaba escondido

Es el mismo patrón que el punto 4: **el servicio responde sano y el problema aparece un salto
después**. El `POST /api/personas` devuelve `201`, la notificación se encola, el consumidor la
toma, intenta mandarla, falla y la marca `FALLIDA`. Nada en el log dice "la URL está mal",
porque el 404 parece un webhook inexistente y no una variable de configuración sin completar.

Además `docker-compose.yml` **sí** pasaba la URL correcta por variable de entorno
(`N8N_URL: http://host.docker.internal:5678/webhook/notificaciones`), así que el compose
funcionaba y el default nunca se ejercía. El bug solo aparecía corriendo con Maven, que es como
se levanta en desarrollo.

### Qué se hizo

El default ahora es el endpoint completo:

```properties
servicio.n8n.url=${N8N_URL:http://localhost:5678/webhook/notificaciones}
```

### Cómo se verificó

Con n8n andando, los cuatro servicios levantados y la base limpia, las cinco notificaciones de
dos flujos distintos quedaron `ENVIADA` con `fecha_envio` puesta:

```
ENVIADA  ana@test.com   2026-10-06 12:28:29  Nuevo Registro en DonaTrack
ENVIADA  luis@test.com  2026-10-06 12:28:48  Nuevo Registro en DonaTrack
ENVIADA  luis@test.com  2026-10-06 12:28:49  ¡Misión completada!
ENVIADA  luis@test.com  2026-10-06 12:28:49  Nueva categoría
ENVIADA  luis@test.com  2026-10-06 12:28:49  Nueva misión disponible
```

La primera viene de `donaciones-service` (webhook `notificaciones`) y las otras cuatro de
`incentivos-service` (webhook `incentivos`), así que los dos endpoints quedaron probados.

De paso **el `N8nIntegrationTest` dejó de fallar**: usa `Mail` → `NotificacionGateway` →
`N8nClient`, o sea la misma URL que estaba rota. Con el default anterior, y con n8n ya
levantado, el test fallaba con 404. Antes de este commit fallaba por `Connection refused`.
Ver punto 1.

---


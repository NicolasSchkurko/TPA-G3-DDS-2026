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
| 4 | 4 | No hay `Jackson2JsonMessageConverter`: `RabbitTemplate` publica con `SimpleMessageConverter` y toda publicación revienta |
| 5 | 5 | `@OneToOne` sin cascada: el `Mensaje` nunca se inserta y la FK `id_mensaje` hace fallar el INSERT |
| 6 | 6 | El `id_mensaje` no viaja en el JSON: el consumidor vuelve con un UUID nuevo y pisa la FK |
| 7 | 7 | El factory no normaliza el tipo: `incentivos-service` manda `"EMAIL"` y todas sus notificaciones quedan `FALLIDA` |
| 8 | 8 | Sin validación en el borde: un `asunto` o `cuerpo` faltante revienta en MySQL y devuelve 500 |
| 9 | 9 | El consumidor no es idempotente: una reentrega de Rabbit reenvía la notificación y pisa el estado |
| 10 | 10 | `RestTemplate` sin timeouts: si n8n cuelga, el hilo del consumidor se bloquea para siempre |
| 11 | 11 | Credenciales de MySQL hardcodeadas y RabbitMQ sin configurar en `application.properties` |
| 12 | 12 | `NotificacionMapper` nunca setea `tipoMedioDeContacto`: el GET siempre lo devuelve `null` |

---

## 1. Un test contra n8n sin `@Disabled` rompe `mvn verify`

**Estado:** abierto
**Severidad:** alta
**Archivo:** `src/test/java/ar/edu/utn/frba/ddsi/notificaciones/test_integracion/N8nIntegrationTest.java`

### Qué pasa

`N8nIntegrationTest.deberiaEnviarMailRealAN8n` hace `POST` a
`http://localhost:5678/webhook/`, que es n8n. Si n8n no está corriendo, falla con
`ResourceAccess I/O error: Connection refused` y el reactor se detiene en
`notificaciones-service`: los tres módulos siguientes no se ejecutan.

El nombre del método dice lo que es: envía un mail real. No es un test unitario, es un test de
integración contra un servicio externo, y está sin `@Disabled`, sin `@Tag` y sin ninguna
condición que lo saltee.

### Por qué importa más de lo que parece

Es preexistente, pero no es inocuo: hace que `mvn verify` no pueda usarse como criterio de
"¿está todo bien?" sin tener n8n andando. En este repo pasó: el fallo se vio primero como si
fuera del módulo de notificaciones, escondido detrás de un error de MySQL.

### Propuesta

Dos opciones, y la elección es del equipo:

- `@Tag("integracion")` más la exclusión del tag en el perfil por defecto, para que corra solo
  con `-Pintegration` o un perfil activo.
- `@Disabled` con el motivo en el mensaje, y que quien quiera lo levante a mano.

Lo que no conviene es dejarlo como está: un test que depende de un servicio de terceros no
debe decidir si el build del equipo pasa.

---

## 2. El consumidor no relanza los fallos, así que no hay reintento ni cola de muertas

**Estado:** abierto
**Severidad:** media
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

### Propuesta

Configurar la dead letter queue en `RabbitConfig` y relanzar en el caso transitorio. Con la
base ya guardando el estado, el reintento manual es posible hoy; la propuesta es no
depender de que alguien se acuerde.

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

## 4. No hay `Jackson2JsonMessageConverter`: `RabbitTemplate` publica con `SimpleMessageConverter`

**Estado:** abierto
**Severidad:** alta
**Archivos:**
`src/main/java/ar/edu/utn/frba/ddsi/notificaciones/config/rabbit/RabbitConfig.java:8`,
`src/main/java/ar/edu/utn/frba/ddsi/notificaciones/messaging/ProductorNotificaciones.java:17`

### Qué pasa

`ProductorNotificaciones.enviar` hace `rabbitTemplate.convertAndSend(COLA_NOTIFICACIONES, notificacion)`
pasando la entidad. Para eso hace falta un `MessageConverter` que sepa serializar un POJO, y en este
módulo **no hay ninguno**: no existe ningún bean `MessageConverter` en todo el servicio.

Spring Boot 3.2.5 no lo agrega solo. En `RabbitAutoConfiguration.RabbitTemplateConfiguration`
(`RabbitAutoConfiguration.java:146-155`) se hace
`configurer.setMessageConverter(messageConverter.getIfUnique())`; sin bean queda `null` y el
`RabbitTemplate` conserva el `SimpleMessageConverter` por defecto.

Ese converter solo maneja `byte[]`, `String` y `Serializable`
(`SimpleMessageConverter.java:111-143`). `Notificacion` no es ninguna de las tres, así que en las
líneas 142-143 tira:

```
IllegalArgumentException: SimpleMessageConverter only supports String, byte[] and Serializable
payloads, received: ar.edu.utn.frba.ddsi.notificaciones.models.entities.Notificacion.Notificacion
```

El error sube por `GestorNotificaciones.enviarSolicitudDeNotificacion:64` **después** de que la
notificación ya se guardó, y lo atrapa el `catch (IllegalArgumentException)` de
`NotificadorController.recibirSolicitudNotificacion:46`, que devuelve **400** con ese texto interno
como cuerpo de la respuesta.

### Lo que queda

- El `POST /notificaciones` nunca devuelve el 202 que promete el Swagger.
- No se publica ningún mensaje: la cola `notificaciones` queda vacía para siempre.
- Cada intento deja una fila en `PENDIENTE` que nadie va a procesar. Notificaciones perdidas, y el
  `GET /notificaciones/{id}` las muestra como si estuvieran en curso.

No queda ni un log: el 400 se va al caller y el servicio sigue pareciendo sano.

### Por qué no lo detectó nadie

Los otros dos módulos que usan Rabbit **sí** definen el converter, en un `RabbitMQConfig` propio:
`logisticas-service/.../config/RabbitMQConfig.java:61` y
`donaciones-service/.../config/RabbitMQConfig.java:64`. El merge trajo a `notificaciones-service` las
tres clases de mensajería y se quedó sin esa cuarta pieza.

El mismo hueco está del lado del consumidor: sin converter, el `SimpleMessageConverter` del listener
devuelve el payload crudo como `byte[]` y `@RabbitListener` tampoco puede armar un `Notificacion`.

### Propuesta

Un bean `Jackson2JsonMessageConverter` en `RabbitConfig`, igual que en los otros dos módulos. Conviene
configurarlo con un `ObjectMapper` propio: el default no registra `JavaTimeModule`, así que
`LocalDateTime` de `fechaCreacion` y `fechaEnvio` no va a serializar bien.

---

## 5. `@OneToOne` sin cascada: el `Mensaje` nunca se inserta y la FK `id_mensaje` hace fallar el INSERT

**Estado:** abierto
**Severidad:** alta
**Archivos:**
`src/main/java/ar/edu/utn/frba/ddsi/notificaciones/models/entities/Notificacion/Notificacion.java:29`,
`src/main/java/ar/edu/utn/frba/ddsi/notificaciones/models/gestores/GestorNotificaciones.java:78`

### Qué pasa

`Notificacion.mensaje` está declarado como

```java
@OneToOne
@JoinColumn(name = "id_mensaje", referencedColumnName = "id_mensaje")
private Mensaje mensaje;
```

**sin `cascade`**. Y `GestorNotificaciones.crearNotificacion:78-84` arma un `new Mensaje(asunto, cuerpo)`
— una entidad nueva, transient — y solo guarda la notificación:

```java
Notificacion notificacion = new Notificacion(
        direccionDeContacto,
        tipoMedioDeContacto,
        new Mensaje(asunto, cuerpo)   // <- nunca se persiste
);
return repositorioNotificaciones.save(notificacion);
```

Sin cascada, Hibernate no emite ningún `INSERT INTO mensajes`: simplemente escribe el UUID del
`Mensaje` transient en la columna `notificaciones.id_mensaje`. Y existe `RepositorioMensajes`
(`models/repositories/RepositorioMensajes.java:9`) pero **no se usa en ningún lado del repo**: es la
pista de que faltó guardar el mensaje.

### Verificado contra la base

El esquema que generó `ddl-auto=update` sobre `notificaciones` (localhost:3306) tiene la restricción
 Foreign Key creada, y las dos tablas están vacías:

```
CONSTRAINT `FK99l0v21gfbuc7ymnevh67mpjy` FOREIGN KEY (`id_mensaje`) REFERENCES `mensajes` (`id_mensaje`)
notificaciones: 0 filas
mensajes:       0 filas
```

Es decir que hoy, en la base de desarrollo, **no se persistió ni una sola notificación**. MySQL rechaza
el `INSERT` con errno 1452, Hibernate lo traduce a `DataIntegrityViolationException`, la transacción
revierte y el `POST` responde 500 (el controller solo atrapa `IllegalArgumentException`).

Este punto es el que hoy tapa al 4: mientras el `save` reviente, nunca se llega a publicar. Arreglando
el 5 sin el 4, el servicio pasa de "no persiste nada" a "persiste y después explota al publicar".

### Propuesta

Cascada explícita en la asociación más `orphanRemoval`, o guardar el `Mensaje` con
`RepositorioMensajes` antes de guardar la notificación. Lo primero es menos código, pero hay que
decidir quién es el dueño de la relación: hoy el `id_mensaje` de `notificaciones` es único y nullable,
y el mensaje solo existe para ser leído por la notificación.

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

## 7. El factory no normaliza el tipo: `incentivos-service` manda `"EMAIL"` y todo queda `FALLIDA`

**Estado:** abierto
**Severidad:** alta
**Archivo:**
`src/main/java/ar/edu/utn/frba/ddsi/notificaciones/models/entities/MedioDeEnvio/MedioDeEnvioFactory.java:17`

### Qué pasa

`MedioDeEnvioFactory` recibe `Map<String, MedioDeEnvio>` por constructor, o sea que el mapa está indexado
por **nombre de bean**. Los tres medios se registran así:

| Clase | Anotación | Clave en el mapa |
|---|---|---|
| `Mail.java:8` | `@Component("email")` | `email` |
| `Telefono.java:8` | `@Component("telefono")` | `telefono` |
| `Whatsapp.java:9` | `@Component("whatsapp")` | `whatsapp` |

Y el lookup es `medios.get(tipo)` en la línea 18: exacto y **sensible a mayúsculas**. Si no encuentra,
`IllegalArgumentException("Tipo desconocido: " + tipo)` en la línea 20.

El problema es quién llama. `incentivos-service` toma el tipo tal cual viene de
`donaciones-service /api/personas/{id}/medios-contacto`, sin transformarlo
(`incentivos-service/.../clients/DonacionClient.java:60-63` usa `dto.get("tipo")`), y ese endpoint
responde en mayúsculas: `donaciones-service/.../MedioDeContacto/Mail.java:39` devuelve `"EMAIL"`, y el
propio test lo asegura en `incentivos-service/src/test/.../NotificacionClientTest.java:85`
(`jsonPath("$.medioDeContacto").value("EMAIL")`).

O sea: el POST real es

```json
{ "medioDeContacto": "EMAIL", "direccionDeContacto": "ana@example.com", ... }
```

`medios.get("EMAIL")` da `null` → `IllegalArgumentException` → `GestorNotificaciones.enviarNotificacion:100`
marca `FALLIDA` y re-lanza → `ConsumidorNotificaciones.recibir:58` la loguea y sigue → la notificación
queda `FALLIDA` sin haber salido nunca. **Todo el flujo de incentivización** (misiones completadas, cambio
de misión, cambio de categoría) falla en silencio.

`donaciones-service` lo hace bien: `ServicioNotificaciones.mapearTipo:82-86` normaliza a minúsculas antes
de llamar. La asimetría entre los dos clientes es justamente el bug.

### Sobre los medios soportados

Son tres, y solo tres: `email`, `telefono` y `whatsapp`. No hay `sms`, ni `push`, ni un default. El
nombre del bean es además el contrato implícito de la API: `POST /notificaciones` acepta cualquier
string en `medioDeContacto` y responde 202, y recién en el consumidor se descubre que no era válido.

Existe `TipoMedioDeContactoInvalidoException` en
`exceptions/NotificacionExceptions/TipoMedioDeContactoInvalidoException.java`, con la firma y el mensaje
justos para este caso, y no la usa nadie: el factory tira `IllegalArgumentException` pelada.

### Propuesta

- Normalizar en la frontera del factory (`tipo.toLowerCase(Locale.ROOT).trim()`), que es el punto donde
  el error es recuperable. O mejor, un enum `MedioDeContacto` con `@JsonCreator` y `@JsonValue`, que
  saca el problema de raíz.
- Tirar `TipoMedioDeContactoInvalidoException` en vez de `IllegalArgumentException`.
- Rechazar el `medioDeContacto` desconocido **en el `POST`**, con 400, en vez de devolver 202 y fallar
  después en el consumidor. Ver punto 8.

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

## 9. El consumidor no es idempotente: una reentrega de Rabbit reenvía la notificación y pisa el estado

**Estado:** abierto
**Severidad:** media
**Archivo:**
`src/main/java/ar/edu/utn/frba/ddsi/notificaciones/messaging/ConsumidorNotificaciones.java:49`

### Qué pasa

`recibir` manda la notificación sin mirar en qué estado está. RabbitMQ es de entrega *al menos una vez*:
si la conexión se cae después de que n8n ya recibió el webhook pero antes de que el listener ackee, el
mensaje vuelve a la cola y la notificación sale **dos veces**. El mail duplicado al usuario es visible; el
problema peor es el historial, porque en la línea 65 el `save` sobrescribe el estado sin condición.

Escenario concreto: una notificación quedó `ENVIADA`, se reentrega por un reinicio del broker y esta vez
n8n no responde. `marcarFallida` la deja en `FALLIDA` y el registro pierde que en algún momento salió.
Al revés también: una que estaba `FALLIDA` y ahora sale bien queda `ENVIADA`, y con eso se pierde el
histórico del intento fallido. En los dos casos la fila deja de contar la historia.

### Transiciones

`Notificacion` expone los tres `marcar*` sin ninguna validación
(`Notificacion.java:80-91`): `marcarEnviada`, `marcarFallida` y `marcarPendiente` escriben el estado sin
mirar cuál era. `marcarPendiente` en particular es público y pone en `PENDIENTE` una notificación que ya
fue enviada, **conservando el `fechaEnvio` viejo**: queda una fila PENDIENTE con fecha de envío, que no
significa nada y rompe la interpretación de la columna que el propio mapper muestra en el GET.

Además, `marcarEnviada` y `marcarPendiente` se contradicen entre sí en el flujo de producción:
`GestorNotificaciones.enviarSolicitudDeNotificacion:59` llama `marcarPendiente()` sobre una
notificación que el constructor de la línea 74 ya dejó en `PENDIENTE`.

El punto 2 (que el consumidor no relanza) está bien resuelto. Esto es el otro lado: cuando el reintento
exista, va a reprocesar notificaciones que ya se mandaron.

### Propuesta

- Si `estado == ENVIADA`, hacer ack y volver sin mandar nada.
- Reencapsular las transiciones: que `marcarEnviada` y `marcarFallida` rechacen una notificación ya
  terminal, y que exista un `reintentar()` explícito en vez de `marcarPendiente` a secas.
- Si `marcarPendiente` sigue existiendo, que limpie `fechaEnvio`, para que la columna no mienta.
- Sacar el `marcarPendiente()` redundante de `GestorNotificaciones.enviarSolicitudDeNotificacion:59`.

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

`application.properties` **no tiene ningún bloque `spring.rabbitmq.*`** (18 líneas, ninguna de AMQP). El
`CachingConnectionFactory` se crea con los defaults de Spring Boot: `localhost:5672`, `guest`/`guest`. En
la máquina de desarrollo funciona; en cualquier otro lado no. Y `guest` es un caso especial de RabbitMQ:
por su propia regla solo puede conectarse desde `localhost`, así que el día que el broker se mueva a un
contenedor o a otra host la conexión falla sin que el properties diga nada.

De paso, `spring.jpa.show-sql=true` (línea 18) deja cada statement SQL en el log de una instancia
desplegada, que es ruido y puede filtrar datos.

### Propuesta

- `spring.datasource.username=${DB_USERNAME:marcelo}` y
  `spring.datasource.password=${DB_PASSWORD:...}`, con las credenciales reales solo en variables de
  entorno o en un `.env` que no se commite.
- Agregar `spring.rabbitmq.host`, `port`, `username` y `password` con las mismas variables de entorno,
  para que el servicio no dependa de estar en la misma máquina que el broker.
- Mover `spring.jpa.show-sql` a un perfil de desarrollo.

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

# Corregidos

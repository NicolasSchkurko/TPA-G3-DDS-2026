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
| 20 | 20 | El reintento manual que prometen los comentarios no existe: ni endpoint, ni scheduler, ni forma de buscar por estado |
| 3 | 3 | Solo hay dos tests, y ninguno cubre el camino de Rabbit |
| 6 | 6 | El `id_mensaje` no viaja en el JSON: el consumidor vuelve con un UUID nuevo y pisa la FK |
| 21 | 21 | La idempotencia es check-then-act sin lock: dos entregas concurrentes mandan dos mails |
| 22 | 22 | Binding de `notificaciones.evento.logistica` que ningún servicio publica |
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

### Nota (2026-10-07): este punto parece no ser reproducible con el código actual

El mecanismo del punto depende de que el consumidor deserialice la entidad `Notificacion` con su
`Mensaje` y haga `save` sobre eso. Hoy `recibir` lee JSON crudo y arma un
`ConsumidorNotificaciones.AvisoNotificacion` (solo id, medio y dirección) o un
`SolicitudNotificacionDTO`, y en ninguno de los dos casos entra un `Mensaje` con UUID nuevo: el de
la fila sale de la base con su `id_mensaje` correcto, y el de una solicitud es una entidad nueva
que se inserta junto con su notificación por el cascade. La descripción de las líneas 167-187 ya
no corresponde al flujo actual. Conviene re-verificarlo contra una base real y cerrarlo si no se
reproduce.

---

## 20. El reintento manual que prometen los comentarios no existe

**Estado:** abierto
**Severidad:** media
**Archivos:**
`.../controllers/NotificadorController.java`,
`.../models/repositories/RepositorioNotificaciones.java:19`

### Qué pasa

El Javadoc del gestor (líneas 69-70) y el del consumidor (líneas 45-46) dicen que una notificación
fallida *"se puede reintentar desde la base"*, y el punto 2 de este backlog repite que *"el
reintento manual es posible hoy"*. No lo es: el único endpoint del servicio es
`GET /notificaciones/{id}`. No hay POST de reintento, no hay scheduler, y no hay ningún método para
buscar las `PENDIENTE` y `FALLIDA` (el `findByEstado` que existía sin uso se borró con el punto 23).

O sea que hoy toda fila que no llega a `ENVIADA` es terminal: queda el registro como única
evidencia, sin ningún camino de recuperación, ni automatizado ni manual por API.

### Propuesta

- `POST /notificaciones/{id}/reintento` que republica el aviso, pasando por el mismo camino
  post-commit del punto 18.
- Un `@Scheduled` que reintente las `PENDIENTE` con fecha de creación antigua, con tope de
  intentos: es el automatismo que pide el punto 2.

---

## 21. La idempotencia es check-then-act sin lock

**Estado:** abierto
**Severidad:** baja
**Archivo:** `.../messaging/ConsumidorNotificaciones.java:111-118`

### Qué pasa

`procesarAvisoDeNotificacionExistente` lee el estado, decide y envía, todo sin transacción ni lock:
dos entregas concurrentes del mismo aviso (reentrega del broker con más de un consumidor, o un
doble publicado) pasan ambas el control de `ENVIADA` antes de que ninguna escriba el resultado, y
la notificación sale dos veces.

Hoy lo evita la configuración, no el diseño: el container levanta un consumidor por cola y la
ventana entre el `findById` y el `save` final es chica. Con `concurrency > 1` o dos instancias del
servicio apuntando a la misma cola, deja de ser gratis.

### Propuesta

- Update condicional de estado (`UPDATE notificaciones SET estado='ENVIADA' WHERE id=? AND
  estado<>'ENVIADA'`) y solo enviar si afectó una fila, o bloqueo pesimista al leerla.
- Es la misma alternativa que ya propone el punto 6 para reemplazar el `save` de la entidad
  entera: un solo cambio cerraría los dos.

---

## 22. La cola escucha `notificaciones.evento.logistica` y ningún servicio publica esa clave

**Estado:** abierto
**Severidad:** baja
**Archivo:** `.../config/rabbit/RabbitConfig.java:70-76`

### Qué pasa

`bindingNotificacionesEventosLogistica` ata la cola al routing key `notificaciones.evento.logistica`
y no hay ningún productor que lo use. `logisticas-service` publica en `logistica.eventos.exchange`
con `logistica.evento` (`ProductorEventosLogistica.java:44-47`), y este servicio recibe esos hechos
recién cuando `donaciones-service` los reenvía por `notificaciones.donacion`.

No rompe nada: es un binding sin tráfico. El riesgo es de lectura, no de runtime: invita a pensar
que logística habla directo con este servicio, que es exactamente lo que el enunciado prohíbe.

### Propuesta

- Sacar el binding y la constante `RK_EVENTO_LOGISTICA`, o dejar en ambos un comentario que diga
  que la ruta real es logística → donaciones → notificaciones.

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
transiciones**: `marcarEnviada` y `marcarFallida` no validan que la transición tenga sentido.

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

#### Nota (2026-10-07): el id ya viaja, y el warn restante lo explica el punto 18

`ProductorNotificaciones.enviar(Notificacion)` publica un `AvisoNotificacion` con
`notificacion.getId().toString()` (líneas 55-60), así que el aviso propio **sí** lleva id y el
camino del `POST` directo ya no debería descartarse por esa causa. Si el warn *"no existe, el
mensaje se descarta"* sigue apareciendo, la causa ya no es el id: es la carrera del punto 18,
publicar antes del commit.

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

### 18. Se publica antes del commit y, si el broker falla, el rollback borra la fila

**Estado:** corregido
**Severidad:** alta
**Corregido:** 2026-10-07 · sin commit
**Archivos:**
`src/main/java/ar/edu/utn/frba/ddsi/notificaciones/models/gestores/GestorNotificaciones.java`,
`src/test/java/ar/edu/utn/frba/ddsi/notificaciones/models/gestores/GestorNotificacionesTest.java` (nuevo)

### Qué pasaba

`enviarSolicitudDeNotificacion` es `@Transactional` y llamaba a `productorNotificaciones.enviar()`
**adentro** de la transacción, o sea antes del commit. El INSERT recién se ejecuta en el commit,
pero el mensaje salía al broker en el instante. Si el consumidor hacía `findById` antes de que la
fila fuera visible para su conexión, `encontrada.isEmpty()` y la línea 104 descartaba el mensaje
con `log.warn("La notificación {} no existe, el mensaje se descarta")`. La fila quedaba
`PENDIENTE` y nadie la retomaba (ver punto 20).

El Javadoc decía lo contrario de lo que hacía el código: *"el commit ocurre al salir del método y
recién ahí el mensaje llega a un registro que ya existe"*. RabbitMQ no espera al commit: entrega
tan pronto se publica.

La segunda mitad era peor: si el broker estaba caído, la `AmqpException` hacía rollback y el
INSERT se deshacía, con lo que la fila **no** quedaba `PENDIENTE` como prometían las líneas 69-70.
El caller recibía un 500 y no quedaba registro de que la notificación existió: el escenario de
recuperación que documentaba el comentario no existía.

### Qué se cambió

La publicación se registró con `TransactionSynchronizationManager.registerSynchronization` y
ocurre en `afterCommit`, no en el momento. Así el orden lo pone la transacción y no la suerte:

- El mensaje sale recién cuando la fila está commiteada y visible para el consumidor: se elimina
  la carrera que descartaba avisos.
- Si la publicación falla, el commit ya se hizo: la fila queda `PENDIENTE` (el estado desde el
  que se puede reintentar) y el error igual sube al llamador — Spring propaga la excepción de
  `afterCommit` sin deshacer el commit.
- En rollback no se publica nada: no puede quedar un aviso en la cola apuntando a una fila que se
  deshizo.

De paso se reescribió el Javadoc del método, que describía el mecanismo viejo como si
funcionara.

### Cómo se verificó

`GestorNotificacionesTest` (nuevo) con el productor mockeado y el ciclo de transacción simulado
con `TransactionSynchronizationManager`, que es el mismo ThreadLocal que usa el transaction
manager en producción:

- `noPublicaAntesDeQueLaTransaccionCommitee`: verifica que con la transacción abierta no se haya
  publicado nada, y que recién después de `afterCommit` se publique una vez.
- `noPublicaSiLaTransaccionSeRevirtio`: verifica que un rollback no publique nada.

**Antes del arreglo: 2/2 fallaban** (el `enviar()` se disparaba desde la línea 71, antes del
commit). **Después: 4/4 en verde**, incluyendo los dos tests preexistentes del módulo.

La suite completa da 5 tests con 1 error: `N8nIntegrationTest`, que necesita n8n corriendo
(`Connection refused` en `localhost:5678`) — era el punto 1, preexistente y ajeno a este cambio,
corregido después.

La revisión independiente (subagente con contexto limpio) dio **PASS**: además de leer el diff,
clonó el repo al HEAD limpio, corrió el test nuevo contra el código viejo (2 failures, o sea que
el test no pasa siempre) y verificó contra las fuentes de Spring que el error en `afterCommit` se
propaga al llamador sin deshacer el commit, que ambos llamadores pasan por el proxy —siempre hay
sincronización activa— y que no hay accesos a campos lazy después del commit.

---

### 1. Un test contra n8n sin `@Disabled` rompe `mvn verify`

**Estado:** corregido
**Severidad:** alta
**Corregido:** 2026-10-07 · sin commit
**Archivo:** `src/test/java/ar/edu/utn/frba/ddsi/notificaciones/test_integracion/N8nIntegrationTest.java`

### Qué pasaba

`N8nIntegrationTest.deberiaEnviarMailRealAN8n` era un `@SpringBootTest` que construía un `Mail`
y lo mandaba por el `NotificacionGateway`, o sea que salía por HTTP a `servicio.n8n.url`. Sin n8n
corriendo fallaba con `Connection refused` y el reactor se detenía en `notificaciones-service`.
Además, aun con n8n andando el test no tenía una sola aserción: no verificaba ni la URL ni el
payload, solo que no reventara.

### Qué se cambió

Se reescribió como un test hermético con `MockRestServiceServer` (de `spring-test`, sin
dependencias nuevas), en el mismo archivo, en lugar de `@Disabled`, `@Tag` o borrarlo: el pedido
fue que no dependa de un servicio externo y que, si un mock tenía sentido, se mockee.

- No hay `@SpringBootTest`: un `RestTemplate` propio atado al mock. No levanta contexto, no
  necesita MySQL, RabbitMQ ni n8n.
- La URL del test (`http://n8n.test/webhook/notificaciones`) es de un TLD reservado que no
  resuelve: si el mock no interceptara, el test fallaría. Es la prueba de que no hay red real.
- Se agregaron las aserciones que faltaban:
  - `deberiaPublicarElPayloadEnElWebhookConfigurado`: POST a la URL configurada, content-type
    JSON y los cuatro campos del payload (`canal`, `direccionContacto`, `mensaje.asunto`,
    `mensaje.cuerpo`).
  - `deberiaPropagarElErrorCuandoN8nNoEncuentraElWebhook`: un 404 sube como excepción, que es lo
    que deja la notificación `FALLIDA` en el consumidor en vez de `ENVIADA`.

La URL del campo `@Value` se setea con `ReflectionTestUtils.setField` para no levantar el
contexto.

### Cómo se verificó

- **Antes:** la corrida previa del reactor falló con `Connection refused` en
  `deberiaEnviarMailRealAN8n` (`Tests run: 5, Errors: 1`).
- **Después:** `mvn -pl notificaciones-service test` → `Tests run: 6, Failures: 0, Errors: 0`
  (GestorNotificacionesTest 2, NotificacionesServiceApplicationTests 2, N8nIntegrationTest 2).
- `mvn test` a nivel raíz (reactor de 5 módulos) → `BUILD SUCCESS`, y `mvn verify` → `BUILD
  SUCCESS`.
- Sin n8n corriendo el módulo pasa igual, que es exactamente el punto del arreglo.

La revisión independiente (subagente con contexto limpio) dio **PASS**: confirmó el hermetismo
(TLD no resoluble, sin `@SpringBootTest`), el uso correcto del API de Spring (`bindTo(...).build()`,
`jsonPath`, `setField` sobre el campo real, `HttpClientErrorException.NotFound` ante 404) y que no
quedaron referencias colgadas al método viejo en CI, scripts ni docs.

**Nota:** al quitar el `@SpringBootTest` se pierde el único smoke test que levantaba el contexto
de Spring. El trade-off es el pedido: ese test dependía de MySQL, RabbitMQ y n8n, y encima no
asertaba nada. Si se quiere reponer ese seguro, corresponde hacerlo con dobles, no contra
infraestructura real.

---

### 8. Sin validación en el borde: un `asunto` o `cuerpo` faltante revienta en MySQL y devuelve 500

**Estado:** corregido
**Severidad:** media
**Corregido:** 2026-10-07 · sin commit
**Archivos:** los mismos que el punto 14 (misma corrida)

### Qué pasaba

`SolicitudNotificacionDTO` era un POJO pelado con cuatro `String`, sin `@NotBlank`, y el controller
recibía el body sin `@Valid`. Un pedido como
`{"medioDeContacto":"email","direccionDeContacto":"ana@example.com"}` pasaba entero hasta
`new Mensaje(null, null)`; como `mensajes.asunto`, `mensajes.cuerpo`,
`notificaciones.direccion_contacto` y `tipo_medio_contacto` son `nullable = false`, el INSERT
reventaba por violación de restricción. El `catch` del controller solo cubría
`IllegalArgumentException`, así que la respuesta era un **500 con la excepción de MySQL adentro** en
vez de un 400: para el que llamaba, indistinguible de un servicio caído.

### Qué se cambió

Se arregló junto con el punto 14, que pedía exactamente lo mismo; el detalle del manejador está en la
entrada del 14. Para este punto, lo concreto:

- `@NotBlank` en los cuatro campos del DTO y `@Valid` en el parámetro del controller: un body
  incompleto se rechaza antes de tocar el service.
- `DataIntegrityViolationException` → **400** con mensaje genérico y el SQL crudo al log: si algo se
  cuela, ya no es un 500.
- `spring-boot-starter-validation` en el pom: sin él las anotaciones de jakarta.validation ni se
  procesan.

### Cómo se verificó

- Antes (código viejo en un clon limpio del HEAD): el test nuevo falla con
  `Status expected:<400> but was:<202>` en los dos casos de validación, o sea que el 400 no existía.
- Después: `mvn -pl notificaciones-service test` → `Tests run: 13, Failures: 0, Errors: 0`.
- `unBodySinAsuntoNiCuerpoSeRechazaCon400YNoLlegaAlServicio` verifica 400, la lista de campos
  faltantes y `verifyNoInteractions(notificadorService)`.
- `unaViolacionDeIntegridadDevuelve400YNo500` verifica que una violación de integridad da 400 y que
  el body no filtra el detalle de la base.
- Reactor de 5 módulos: `BUILD SUCCESS`.
- Revisión independiente: **PASS** (sondeó 404/405/415/409 del advice y reprodujo el "antes").

Nota: validar el **valor** del medio de contacto sigue pendiente y es el punto 7, fuera de esta
corrida.

---

### 14. El DTO de entrada no tiene validación de Bean Validation y el manejador de excepciones está comentado

**Estado:** corregido
**Severidad:** alta
**Corregido:** 2026-10-07 · sin commit
**Archivos:**
`notificaciones-service/pom.xml`,
`src/main/java/ar/edu/utn/frba/ddsi/notificaciones/dto/SolicitudNotificacionDTO.java`,
`src/main/java/ar/edu/utn/frba/ddsi/notificaciones/controllers/NotificadorController.java`,
`src/main/java/ar/edu/utn/frba/ddsi/notificaciones/dto/ErrorResponseDTO.java` (nuevo),
`src/main/java/ar/edu/utn/frba/ddsi/notificaciones/exceptions/GlobalExceptionHandler.java`,
`src/test/java/ar/edu/utn/frba/ddsi/notificaciones/controllers/NotificadorControllerTest.java` (nuevo)

### Qué pasaba

`SolicitudNotificacionDTO` no tenía ninguna anotación de validación y el controller recibía el body
sin `@Valid`, así que un `null` en cualquiera de los cuatro campos alimentaba columnas
`nullable = false` y moría en el INSERT. Y `exceptions/GlobalExceptionHandler.java` estaba
**comentado entero** (un bloque `/* ... */`): no había ningún `@RestControllerAdvice`, así que
ninguna excepción de persistencia se traducía a un status razonable y los errores internos salían sin
cuerpo uniforme. Faltaba además `spring-boot-starter-validation` en el pom.

### Qué se cambió

`GlobalExceptionHandler` se reescribió como `@RestControllerAdvice` que **extiende
`ResponseEntityExceptionHandler`**. Esa herencia es la parte no obvia: las excepciones propias de
Spring MVC (404, 405, 415, binding) tienen handlers heredados con su status correcto, así que el
`@ExceptionHandler(Exception.class)` de contención —el menos específico— solo actúa cuando ninguna
coincide. Sin la clase base, una red de contención genérica convertiría un 404 en 500.

Handlers:

- `MethodArgumentNotValidException` → **400** con la lista `campo: mensaje` de lo que falta.
- `DataIntegrityViolationException` → **400** genérico + `log.warn` con el detalle real.
- `IllegalArgumentException` → **400** (conserva lo que el controller hacía a mano).
- `ResponseStatusException` → respeta el status elegido (si no, la red de contención lo volvería 500).
- `Exception` → **500** con mensaje genérico y `log.error` del stack trace; el detalle nunca va a la
  respuesta.

Se creó `ErrorResponseDTO` (record `mensaje`/`status`/`detalles`), que el handler comentado
referenciaba pero que no existía en el repo. Se agregó `spring-boot-starter-validation` al pom y
`@Valid` en el controller. Se quitó el `try/catch` de `IllegalArgumentException` del controller,
ahora redundante: ese 400 pasa a tener el mismo cuerpo JSON que los demás. No hay consumidores HTTP
de `POST /notificaciones` en el repo (donaciones e incentivos publican por Rabbit), así que no se
rompe ningún contrato.

### Cómo se verificó

Misma corrida que el punto 8: 13/13 en el módulo y `BUILD SUCCESS` del reactor. La revisión
independiente confirmó contra las fuentes de Spring 6.1.6 que el `handleException` heredado es
`final` y cubre unos 20 tipos, y lo comprobó con sondas: 404 de ruta inexistente, 405 de método no
soportado, 415 de media type, 409 de `ResponseStatusException` y 400 de JSON malformado conservan su
status. Dos de esas sondas quedaron como tests de regresión en `NotificadorControllerTest`
(`unaRutaInexistenteSigueDevolviendo404YNo500`, `unMetodoNoSoportadoSigueDevolviendo405YNo500`).

---

### 10. `RestTemplate` sin timeouts: si n8n cuelga, el hilo del consumidor se bloquea para siempre

**Estado:** corregido
**Severidad:** media
**Corregido:** 2026-10-07 · sin commit
**Archivos:**
`src/main/java/ar/edu/utn/frba/ddsi/notificaciones/config/RestTemplateConfig.java`,
`src/main/resources/application.properties`,
`src/test/java/ar/edu/utn/frba/ddsi/notificaciones/config/RestTemplateConfigTest.java` (nuevo)

### Qué pasaba

`RestTemplateConfig.restTemplate()` devolvía `new RestTemplate()` pelado, o sea
`SimpleClientHttpRequestFactory` con los timeouts infinitos (`-1`) de `HttpURLConnection`.
`N8nClient.enviarNotificacion` se llama de forma síncrona desde el hilo del `@RabbitListener`; con un
solo consumidor, un n8n que acepta la conexión TCP y no responde dejaba ese hilo colgado para
siempre: la cola dejaba de vaciarse, las notificaciones se acumulaban en `PENDIENTE` y no había
timeout, ni log, ni error que lo explicara.

### Qué se cambió

El bean arma un `SimpleClientHttpRequestFactory` con `setConnectTimeout` y `setReadTimeout`. Los
valores salen de `application.properties` y son ajustables por entorno:

- `servicio.n8n.connect-timeout-ms` → default **5000**.
- `servicio.n8n.read-timeout-ms` → default **10000**.

Si alguno llega en `0` o negativo, el bean **no arranca**: en `HttpURLConnection` un `0` significa
"sin límite", así que dejarlo pasar reintroduciría exactamente el cuelgue. Es preferible fallar al
levantar que quedarse esperando para siempre en producción.

Se eligió `SimpleClientHttpRequestFactory` explícito en vez de `RestTemplateBuilder` porque el
proyecto no tiene Apache HttpClient ni OkHttp en ningún pom: el factory estándar es el que
corresponde y no depende de detección de auto-configuración.

### Cómo se verificó

- Antes (clon limpio del HEAD): con `new RestTemplate()` el escenario del servidor que no responde
  devuelve `200 OK` recién a los ~2035 ms, o sea que sin timeout el hilo no tiene cota.
- Después: `mvn -pl notificaciones-service test` → `Tests run: 15, Failures: 0, Errors: 0`.
- `RestTemplateConfigTest.unN8nQueNoRespondeCortaPorTimeoutEnVezDeColarElHilo` levanta un
  `HttpServer` en localhost que acepta y no responde, y verifica que la llamada corta con
  `ResourceAccessException` causada por `SocketTimeoutException`. Hermético: no toca n8n, MySQL ni
  Rabbit, y tarda ~200 ms (contra 2 s de sleep del servidor, margen de 10x: no es flaky).
- `unTimeoutEnCeroSeRechazaPorqueSeríaInfinito` cubre la guarda de configuración.
- Reactor de 5 módulos: `BUILD SUCCESS`.
- Revisión independiente: **PASS** (verificó el cableado bean → `N8nClient` y reprodujo el "antes").

Nota: el timeout de conexión queda configurado pero no tiene test comportamental propio; el caso
reportado —aceptar la conexión y no responder— es de lectura, y es el que cubre el test.

---

### 11. Credenciales de MySQL hardcodeadas y RabbitMQ sin configurar en `application.properties`

**Estado:** corregido (con una decisión explícita del equipo)
**Severidad:** media
**Corregido:** 2026-10-07 · sin commit
**Archivos:**
`src/main/resources/application.properties`,
`src/main/resources/application-dev.properties` (nuevo),
`src/test/java/ar/edu/utn/frba/ddsi/notificaciones/ConfiguracionArranqueTest.java` (nuevo)

### Aclaración: la descripción del punto estaba desactualizada

Al abrir el archivo, las dos mitades del título original ya no eran ciertas:

- **RabbitMQ ya estaba configurado**: `spring.rabbitmq.host/port/username/password` con `${...}` y
  default. Verificado en vivo: la cola `notificaciones` tiene consumidor.
- **Las credenciales de MySQL ya estaban parametrizadas**: `${DB_USERNAME:marcelo}` /
  `${DB_PASSWORD:losbabasonicos}`, no literales pelados como decía el pendiente.

Lo único que seguía vivo era `spring.jpa.show-sql=true` en la configuración por defecto.

### Qué se cambió

`spring.jpa.show-sql=true` se movió a `application-dev.properties`, que Spring Boot carga solo con el
perfil `dev` activo (`--spring.profiles.active=dev` o `SPRING_PROFILES_ACTIVE=dev`). Por defecto
Spring Boot aplica `spring.jpa.show-sql=false`: cada statement SQL deja de salir en el log de una
instancia desplegada, donde es ruido y puede filtrar datos, sin perder la posibilidad de depurar
contra la base local.

### Decisión del equipo sobre la credencial (explícita, no es un descuido)

La credencial real **se deja como default** en `application.properties`
(`${DB_USERNAME:marcelo}` / `${DB_PASSWORD:losbabasonicos}`) y `docker-compose.yml` **no se toca**.
La propuesta de este punto era sacarla del repo (o al menos quitarle el default), y el módulo hermano
`logisticas-service` fue por ese camino en su punto 33; acá se decidió lo contrario a conciencia. La
contraseña sigue, por lo tanto, en el repositorio: es un riesgo aceptado, no algo que este cambio
haya resuelto. Queda asentado para que la decisión no se confunda con un olvido.

### Cómo se verificó

- `ConfiguracionArranqueTest.showSqlNoEstaEnLaConfiguracionPorDefecto` lee el `application.properties`
  del classpath y falla si `spring.jpa.show-sql` está en `true` (normalizando mayúsculas: Spring
  parsea el booleano sin distinguirlas).
- `ConfiguracionArranqueTest.elPerfilDevPrendeShowSql` verifica que `application-dev.properties` esté
  en el classpath y prenda el log.
- `mvn -pl notificaciones-service test` → `Tests run: 17, Failures: 0, Errors: 0`.
- Reactor de 5 módulos: `BUILD SUCCESS`.
- Revisión independiente: **PASS** (confirmó el mecanismo de perfiles de Spring Boot 3.2, que el
  classpath resuelve el `application.properties` de producción, y que el módulo sigue empaquetando).

---

### 12. `NotificacionMapper` nunca setea `tipoMedioDeContacto`: el GET siempre lo devuelve `null`

**Estado:** corregido
**Severidad:** baja
**Corregido:** 2026-10-07 · sin commit
**Archivos:**
`src/main/java/ar/edu/utn/frba/ddsi/notificaciones/mappers/NotificacionMapper.java`,
`src/test/java/ar/edu/utn/frba/ddsi/notificaciones/mappers/NotificacionMapperTest.java` (nuevo)

### Qué pasaba

`NotificacionDTO` declaraba el campo `tipoMedioDeContacto` y la entidad lo tenía con getter, pero
`NotificacionMapper.notificacionDTO` no lo seteaba. El `GET /notificaciones/{id}` devolvía siempre
`"tipoMedioDeContacto": null` aunque la columna es `nullable = false` y el valor está en la base;
Jackson serializa el null sin quejarse, así que el campo faltaba sin que nada lo delatara.

### Qué se cambió

Una línea en el mapper:
`notificacionDTO.setTipoMedioDeContacto(notificacion.getTipoMedioDeContacto());`.

El mapper queda cubriendo los 7 campos del DTO (`asunto`, `cuerpo`, `tipoMedioDeContacto`,
`direccionDeContacto`, `fechaCreacion`, `fechaEnvio`, `estado`); los únicos campos de la entidad que no
se mapean son `id` y `mensaje`, que no existen en el DTO.

### Cómo se verificó

- Antes (worktree limpio del HEAD, sin la línea): `NotificacionMapperTest` falla con
  `expected: <email> but was: <null>` en la aserción del campo. El test no es tautológico.
- Después: `mvn -pl notificaciones-service test` → `Tests run: 19, Failures: 0, Errors: 0`.
- `NotificacionMapperTest` (nuevo) cubre todos los campos del DTO, el caso `fechaEnvio` null (nace
  PENDIENTE) y el caso `fechaEnvio` seteada (después de `marcarEnviada()`).
- Reactor de 5 módulos: `BUILD SUCCESS`.
- Revisión independiente: **PASS**.

Nota: las líneas de `getEstado().toString()` y `getFechaCreacion().toString()` siguen sin null check;
el revisor confirmó que por el GET son inalcanzables con null (columnas `not null` y el constructor
las asigna), así que se dejó como estaba.

---

### 2. El consumidor no relanza los fallos, así que no hay reintento ni cola de muertas

**Estado:** corregido (parcial: se hizo la infraestructura; el relanzado transitorio, a propósito, no)
**Severidad:** baja
**Corregido:** 2026-10-07 · sin commit
**Archivos:** los mismos que el punto 15 (misma corrida)

### Qué pasaba

`ConsumidorNotificaciones.recibir` marca la notificación como `FALLIDA`, loguea el error y no
relanza: el mensaje se da por consumido. No había ninguna de las dos salidas que quedan —reintento
con espera para lo transitorio y dead letter para lo que no—.

### Qué se cambió

Se resolvió junto con el punto 15 (misma infraestructura; ver esa entrada). Para este punto, lo
concreto es que ahora existen la **DLQ** y un **reintento acotado**: lo que falla se reintenta con
tope y, si no se recupera, queda en `notificaciones.dlq` en vez de perderse.

**Lo que deliberadamente NO se hizo:** relanzar los fallos transitorios desde el `catch` del
consumidor. Hoy el listener traga la excepción (marca `FALLIDA` y sigue), así que el reintento de
Spring no se dispara para esos casos. Relanzarlo daría el reintento automático, pero también
reprocesaría: si el fallo fue después de guardar, un reintento sobre una solicitud sin `id`
crearía una notificación duplicada. Es el ángulo transitorio que este punto pedía y queda como
brecha consciente: la recuperación sigue siendo el estado `FALLIDA` en la base.

### Cómo se verificó

La topología y la configuración están cubiertas por `RabbitConfigTest` y `ConfiguracionArranqueTest`
(ver punto 15), y la revisión independiente verificó contra las fuentes de Spring Boot 3.2.5 /
spring-amqp 3.1.4 el reintento acotado y el dead letter (**PASS**). El relanzado transitorio no se
implementó, así que no hay test que lo cubra: queda la brecha documentada.

---

### 15. La cola de notificaciones no tiene dead letter y los errores de conversión se reintentan en loop

**Estado:** corregido
**Severidad:** media
**Corregido:** 2026-10-07 · sin commit
**Archivos:**
`src/main/java/ar/edu/utn/frba/ddsi/notificaciones/config/rabbit/RabbitConfig.java`,
`src/main/resources/application.properties`,
`src/test/java/ar/edu/utn/frba/ddsi/notificaciones/config/rabbit/RabbitConfigTest.java` (nuevo),
`src/test/java/ar/edu/utn/frba/ddsi/notificaciones/ConfiguracionArranqueTest.java`

### Qué pasaba

La cola `notificaciones` no declaraba dead letter. Un error de conversión de mensaje disparaba el
`ConditionalRejectingErrorHandler`, que reencolaba y reintentaba, escribiendo el stack trace completo
en cada intento: en una corrida real el log llegó a **1.9 GB** con cuatro servicios publicando en la
misma cola.

### Qué se cambió

- `RabbitConfig`: la cola principal se declara con `QueueBuilder.durable(...)
  .deadLetterExchange(EXCHANGE_DLQ).deadLetterRoutingKey(COLA_DLQ)`, y se agregaron el exchange
  `notificaciones.dlq.exchange` (topic durable), la cola `notificaciones.dlq` (durable) y su binding.
- `application.properties`: `spring.rabbitmq.listener.simple.default-requeue-rejected=false` (lo
  rechazado no vuelve a la cola) y `spring.rabbitmq.listener.simple.retry.*` con `max-attempts=3` y
  backoff 1s → ×2 → tope 10s. Con eso el `RejectAndDontRequeueRecoverer` que arma Spring Boot rechaza
  sin reencolar al agotar los intentos y el mensaje cae en la DLQ.

**Cuidado al desplegar:** una cola de RabbitMQ es inmutable. Si `notificaciones` ya estaba declarada
sin estos argumentos, el broker rechaza la redeclaración con `PRECONDITION_FAILED` y el listener no
arranca: hay que borrar la cola una vez (o recrear el broker). Está anotado en el javadoc de
`RabbitConfig`.

### Cómo se verificó

- `mvn -pl notificaciones-service test` → `Tests run: 23, Failures: 0, Errors: 0`.
- `RabbitConfigTest` fija que la cola principal declare el `x-dead-letter-exchange` y el
  `x-dead-letter-routing-key`, que sea durable, y que el exchange, la cola muerta y el binding
  (routing key y destino) estén bien armados.
- `ConfiguracionArranqueTest` fija `default-requeue-rejected=false` y el reintento acotado.
- Reactor de 5 módulos: `BUILD SUCCESS`.
- Revisión independiente (**PASS**) verificó contra las fuentes de Spring Boot 3.2.5 y spring-amqp
  3.1.4 que `QueueBuilder` escribe exactamente los argumentos que RabbitMQ espera, que
  `retry.enabled=true` configura el `RetryInterceptor` con `RejectAndDontRequeueRecoverer`, y que
  `MessageConversionException` se considera fatal y termina en la DLQ.

**Residual:** no se bajó el nivel de log de los rechazos (el loop ya está cortado: como máximo 3+1
líneas por mensaje). El reintento automático de lo transitorio tampoco: ver el punto 2.

---

### 19. `GestorNotificaciones.enviarNotificacion` ignora el parámetro `direccionContacto`

**Estado:** corregido
**Severidad:** media
**Corregido:** 2026-10-07 · sin commit
**Archivos:**
`src/main/java/ar/edu/utn/frba/ddsi/notificaciones/models/gestores/GestorNotificaciones.java`,
`src/test/java/ar/edu/utn/frba/ddsi/notificaciones/models/gestores/GestorNotificacionesTest.java`

### Qué pasaba

`enviarNotificacion(tipoMedioContacto, direccionContacto, notificacion)` no leía `direccionContacto`:
armaba el medio con el tipo y llamaba `medioDeContacto.enviarNotificacion(notificacion)`, que
internamente usa `notificacion.getDireccionDeContacto()`. El fallback que el consumidor armó —usar la
dirección del mensaje si trae una, y la de la fila si no— no hacía nada: cualquier aviso con dirección
propia se ignoraba en silencio y el mail salía al destinatario viejo. Era latente porque el productor
interno publica la misma dirección que la fila.

### Qué se cambió

En `enviarNotificacion`, si `direccionContacto` no es null ni blanco, se setea en la notificación antes
de delegar al medio (`notificacion.setDireccionDeContacto(direccionContacto)`). Los medios leen de la
entidad, así que con eso la dirección del mensaje gana cuando viene; si viene vacía, se usa la de la
fila. Se eligió esto por sobre cambiar la interfaz de `MedioDeEnvio` para que reciba la dirección, por
ser el cambio más chico.

La fila queda con la dirección realmente usada: el consumidor guarda la entidad después de enviar, así
que el registro refleja el destino. En el flujo actual es neutro (misma dirección); solo cambia cuando
el mensaje trae una dirección propia, que es exactamente el caso latente.

### Cómo se verificó

- `GestorNotificacionesTest.usaLaDireccionDelMensajeCuandoViene` usa un `Mail` real con el gateway
  mockeado y captura el payload: con el código viejo devolvía la dirección de la fila, así que el test
  es efectivo.
- `siElMensajeNoTraeDireccionUsaLaDeLaFila` cubre null y blanco (que el guard no rompa el fallback).
- `mvn -pl notificaciones-service test` → `Tests run: 25, Failures: 0, Errors: 0`.
- Reactor de 5 módulos: `BUILD SUCCESS`.
- Revisión independiente: **PASS**.

---

### 23. Código muerto y comentarios que ya no describen el código

**Estado:** corregido (salvo un ítem de `docker-compose.yml`, fuera del módulo)
**Severidad:** baja
**Corregido:** 2026-10-07 · sin commit
**Archivos:** varios

### Qué se hizo

Se revisó todo el módulo y se borró lo que estaba muerto o desactualizado:

- `RepositorioNotificaciones`: se borró la implementación vieja comentada (45 líneas) y el javadoc
  que decía *"Repositorio en memoria"* (es un `JpaRepository` contra MySQL).
- Se borraron las clases que nadie usaba: `NotificacionMensajeDTO`, `RepositorioMensajes` y las cinco
  excepciones de `exceptions/NotificacionExceptions` (`SolicitudInvalidaException`,
  `MensajeInvalidoException`, `DireccionInvalidoException`, `TipoMedioDeContactoInvalidoException`,
  `ErrorAlEnviarNotificacion`).
- `RepositorioNotificaciones.findByEstado`, el constructor `Notificacion(String, Mensaje)` y
  `Notificacion.marcarPendiente()` tampoco tenían usos: se borraron.
- `ConsumidorNotificaciones`: se quitó el `cuerpoCrudo == null` inalcanzable.
- `NotificadorController`: el campo pasó de `NotificacionMapper` a `notificacionMapper`.
- `NotificadorService`: se sacaron los imports sin uso (`MedioDeEnvioFactory`,
  `RepositorioNotificaciones`) y el `@Autowired` redundante.
- Comentarios: se eliminaron todos los javadocs largos de varios párrafos (`<p>`) del módulo, main y
  tests. Quedan comentarios de una línea solo para lo que el código no expresa solo.

### Lo que queda

`docker-compose.yml` todavía define `NOTIFICACIONES_URL`, que ningún `@Value` lee desde que los
clientes publican por Rabbit. Está fuera de este módulo y no se tocó.

### Cómo se verificó

- `mvn -pl notificaciones-service test` → `Tests run: 25, Failures: 0, Errors: 0`.
- `mvn test` (reactor de 5 módulos) → `BUILD SUCCESS`.


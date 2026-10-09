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
| 21 | 21 | La idempotencia es check-then-act sin lock: dos entregas concurrentes mandan dos mails |
| 22 | 22 | Binding de `notificaciones.evento.logistica` que ningún servicio publica |
| 24 | 24 | La organización de paquetes no sigue la de `incentivos-service` (repos, dto, clientes) |
| 25 | 25 | `GestorNotificaciones` mezcla responsabilidades: no está partido por comportamiento |
| 26 | 26 | No hay endpoint de colección ni paginación (`GET /notificaciones` paginado) |
| 27 | 27 | El canal SMS del enunciado no está contemplado: solo email, teléfono y WhatsApp |
| 28 | 28 | El mapa de la ruta viaja como URL en el texto, no como adjunto |
| 29 | 29 | El camino de solicitud no es idempotente: una reentrega crea notificación y mail duplicados |
| 30 | 30 | El consumidor no valida `asunto`/`cuerpo`: una solicitud sin ellos se pierde sin dejar rastro |
| 31 | 31 | El aviso interno se publica con la routing key `notificaciones.incentivo` |
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

## 24. La organización de paquetes no sigue la de `incentivos-service`

**Estado:** abierto
**Severidad:** baja
**Archivos:** `.../models/repositories/`, `.../dto/`, `.../clientes/`

### Qué pasa

`incentivos-service` ordena por responsabilidad y por área; acá quedó plano. Diferencias concretas:

- Los Spring Data van en `models/repositories/SpringRepositories/`; acá están sueltos en
  `models/repositories/`.
- Los DTO se agrupan por área (`dto/Perfil/`, `dto/Admin/`, `dto/n8n/`); acá hay un `dto/` plano con
  todo mezclado.
- El paquete de clientes HTTP se llama `clients/`; acá se llama `clientes/`.
- Tiene `controllers/request/` para los filtros de query y `models/events/` para los eventos de
  dominio; acá no hay equivalentes (todavía no hacen falta, pero conviene la misma forma).

No rompe nada: es de lectura y de consistencia entre módulos.

### Propuesta

Renombrar y mover sin cambiar comportamiento:

- `models/repositories/*.java` → `models/repositories/SpringRepositories/`.
- `dto/` → subcarpetas por área (por ejemplo `dto/Notificacion/` para `NotificacionDTO`,
  `SolicitudNotificacionDTO` y `NotificacionPayload`; `dto/Error/` para `ErrorResponseDTO`).
- `clientes/` → `clients/`.
- Cuando aparezcan filtros de query, `controllers/request/`.

---

## 25. `GestorNotificaciones` mezcla responsabilidades: no está partido por comportamiento

**Estado:** abierto
**Severidad:** baja
**Archivo:** `.../models/gestores/GestorNotificaciones.java`

### Qué pasa

`incentivos-service` nombra los gestores por lo que hacen (`ValidadorAdmin`, `SincronizacionPerfiles`,
`SecuenciaCategoria`), no por la entidad que tocan. Acá hay un único `GestorNotificaciones` que hace
cuatro cosas distintas:

- guardar y publicar la solicitud (`enviarSolicitudDeNotificacion`, con el `afterCommit`),
- construir y persistir (`crearNotificacion`),
- elegir el medio y despachar (`enviarNotificacion`),
- consultar (`obtenerNotificacionPorId`).

### Propuesta

Partirlo por comportamiento, con nombres que digan qué hacen. Una división posible:
`RegistroNotificaciones` (guardar + publicar), `DespachoNotificaciones` (elegir el medio y enviar) y
`ConsultaNotificaciones` (buscar por id / listar). Ajustar los llamadores
(`NotificadorService`, `ConsumidorNotificaciones`) y los tests.

---

## 26. No hay endpoint de colección ni paginación

**Estado:** abierto
**Severidad:** baja
**Archivos:** `.../controllers/NotificadorController.java`,
`.../models/repositories/RepositorioNotificaciones.java`

### Qué pasa

El servicio solo expone `POST /notificaciones` y `GET /notificaciones/{id}`. No hay forma de listar
las notificaciones ni de filtrarlas por estado, aunque el punto 20 pide poder encontrar las
`PENDIENTE`/`FALLIDA` para reintentar.

`incentivos-service` expone sus colecciones paginadas: `GET` con
`@PageableDefault(page = 0, size = 10, sort = ...) Pageable` y `ResponseEntity<Page<DTO>>`
(`RankingController`, `MisionController`, `CategoriaController`, las insignias de `PerfilController`).

### Propuesta

Agregar `GET /notificaciones` siguiendo ese patrón:

- Controller: `ResponseEntity<Page<NotificacionDTO>>` con
  `@RequestParam(required = false) EstadoNotificacion estado` y
  `@PageableDefault(page = 0, size = 10, sort = "fechaCreacion", direction = Sort.Direction.DESC)`.
- Service: `Page<NotificacionDTO> listar(EstadoNotificacion estado, Pageable pageable)`.
- Repo: `Page<Notificacion> findByEstado(EstadoNotificacion estado, Pageable pageable)` y
  `Page<Notificacion> findAll(Pageable pageable)` (el `findByEstado` que había, sin paginar, se borró
  por muerto con el punto 23).
- Tests de controller con `MockMvc`: 200, tamaño de página y filtro por estado.

---

## 27. El canal SMS del enunciado no está contemplado: solo email, teléfono y WhatsApp

**Estado:** abierto
**Severidad:** media
**Archivos:** `.../models/entities/MedioDeEnvio/MedioDeEnvioFactory.java`,
`.../models/entities/MedioDeEnvio/Telefono.java`

### Qué pasa

El enunciado pide comunicación multicanal por **correo electrónico, SMS y WhatsApp**. El servicio
tiene tres medios —`Mail` (`email`), `Telefono` (`telefono`) y `Whatsapp` (`whatsapp`)—, pero
ninguno se llama `sms` y `MedioDeEnvioFactory` no reconoce ese valor.

`mapearAMedioEnvio` busca el tipo tal cual (`sms`) en el mapa de beans, después prueba los alias
(`mail`/`gmail`/`tel`/`celular`/`wa`/…) y, si no lo encuentra, lanza
`IllegalArgumentException("Tipo de medio de contacto desconocido: …")`. Eso deja la notificación en
`FALLIDA`: un productor que mande `medioDeContacto: "SMS"` no envía nada.

En todo el repo no hay una sola referencia a `SMS`: los servicios de dominio emiten `EMAIL`/`MAIL`,
`TELEFONO` y `WHATSAPP`. O sea que el "SMS" del enunciado hoy solo se cumple si se lo trata como el
medio `telefono`, y por convención, no por contrato.

### Propuesta

- Agregar el alias `sms` en `MedioDeEnvioFactory` (apuntando a `telefono`, o a un `MedioDeEnvio`
  `Sms` propio si el envío por n8n necesita un canal distinto del telefónico).
- Que el servicio de dominio que corresponda emita `SMS`, o dejar asentado que `TELEFONO` **es** el
  canal SMS del enunciado.
- Test que publique con `medioDeContacto: "SMS"` y verifique que se despacha por el medio esperado.

---

## 28. El mapa de la ruta viaja como URL en el texto, no como adjunto

**Estado:** abierto
**Severidad:** baja
**Archivo:** `.../dto/NotificacionPayload.java`

### Qué pasa

El escenario "inicio de ruta adjuntando un mapa interactivo" **se despacha**: `logisticas-service`
arma `Ruta.urlSeguimiento` en `publicarInicioRuta`, lo manda en el evento `INICIO_RUTA`,
`donaciones-service` lo reenvía y `NotificacionViaje` arma el cuerpo con
`"Sigue la entrega: <url>"`. El link llega, pero:

- Viaja como texto dentro de `mensaje.cuerpo`. `NotificacionPayload` solo tiene `canal`,
  `direccionContacto` y `mensaje` (asunto/cuerpo): no hay campo de adjunto ni de link estructurado,
  así que el "mapa adjunto" es una URL pegada en el texto.
- La URL es un template placeholder (`https://donaciones-app.example.com/seguimiento/<idRuta>`), no
  un mapa real; eso vive en `logisticas-service`.

No es un incumplimiento estricto (el link se envía), pero si se espera un adjunto o un campo
propio, hoy no hay dónde ponerlo.

### Propuesta

- Si alcanza con el link, dejarlo escrito en el contrato y no cambiar nada.
- Si se quiere un adjunto real, agregar un campo opcional (por ejemplo `urlMapa` o `adjuntos`) a
  `NotificacionPayload` y que el `MedioDeEnvio`/n8n lo usen.

---

## 29. El camino de solicitud no es idempotente: una reentrega crea notificación y mail duplicados

**Estado:** abierto
**Severidad:** media
**Archivos:** `.../messaging/ConsumidorNotificaciones.java:127-150`,
`.../models/gestores/GestorNotificaciones.java:66-85`

### Qué pasa

Para una **solicitud** (mensaje sin `id`, el que publican `donaciones-service` e
`incentivos-service`), `procesarSolicitud` llama a `gestor.enviarSolicitudDeNotificacion`, que crea
una `Notificacion` nueva con un `UUID` al azar. No hay ninguna clave de idempotencia en
`SolicitudNotificacionDTO` ni en el flujo: la misma solicitud procesada dos veces genera dos filas
y dos mails.

El camino del **aviso** (mensaje con `id`) sí chequea `ENVIADA` antes de reenviar; el de solicitud
no tiene nada. Es la contracara del punto 21: ahí el problema es la concurrencia sobre el mismo id;
acá no hay id que valga.

### Cómo se dispara

RabbitMQ es at-least-once. Cualquier reentrega del mensaje de dominio —el proceso muere después de
guardar y antes del ack, o el broker reinicia la conexión— vuelve a ejecutar `recibir` con el mismo
JSON y produce una segunda notificación y un segundo envío. El enunciado pide la integración por
cola justamente por alta carga, que es cuando más reentregas hay.

### Propuesta

- Definir una clave de idempotencia por solicitud: que el servicio de dominio mande un
  `idSolicitud` (o `clave`) estable por evento y guardarla con índice único, o deduplicar por
  `hash(medio + direccion + asunto + cuerpo)` en una ventana.
- Tabla de mensajes procesados con TTL, o `INSERT ... ON DUPLICATE KEY` sobre esa clave, y publicar
  el aviso solo si se insertó.

---

## 30. El consumidor no valida `asunto`/`cuerpo`: una solicitud sin ellos se pierde sin dejar rastro

**Estado:** abierto
**Severidad:** media
**Archivos:** `.../messaging/ConsumidorNotificaciones.java:127-150`,
`.../dto/SolicitudNotificacionDTO.java`

### Qué pasa

`procesarSolicitud` chequea a mano que `medioDeContacto` y `direccionDeContacto` no estén en blanco,
pero **no** mira `asuntoMensaje` ni `cuerpoMensaje`. El DTO tiene `@NotBlank` en los cuatro campos
(punto 8/14), pero esas anotaciones las dispara `@Valid` en el controller HTTP: el consumidor
deserializa con Jackson, sin Bean Validation, así que no se aplican.

Con asunto o cuerpo `null`, `enviarSolicitudDeNotificacion` llega al `INSERT` de `mensajes` (cuyas
columnas son `nullable = false`), tira `DataIntegrityViolationException`, la atrapa el
`catch (RuntimeException)` de `recibir` —que loguea y **no reencola**— y el mensaje se da por
consumido: la notificación nunca se crea y no queda ni una fila en `FALLIDA`.

Es la misma clase de defecto que el punto 8/14 pero en el otro borde: el HTTP quedó validado, el de
la cola no. Hoy no lo dispara ningún productor (los dos mandan asunto y cuerpo), pero es una
pérdida silenciosa esperando al primer campo que falte.

### Propuesta

- Validar los cuatro campos con el mismo criterio que el controller (llamar a un `Validator` de
  Bean Validation sobre el DTO deserializado, o chequear asunto/cuerpo a mano como ya se hace con
  los otros dos).

---

## 31. El aviso interno se publica con la routing key `notificaciones.incentivo`

**Estado:** abierto
**Severidad:** baja
**Archivo:** `.../messaging/ProductorNotificaciones.java:55`

### Qué pasa

`ProductorNotificaciones.enviar(Notificacion)` —el aviso que este mismo servicio publica para
despachar una notificación recién guardada— usa `RabbitConfig.RK_INCENTIVO`
(`"notificaciones.incentivo"`), que es la clave de los avisos de incentivos, no una propia.

Hoy no rompe porque la única cola es la de este servicio y está atada a las tres routing keys. Pero
la clave es semánticamente falsa: si `incentivos-service` (o cualquier otro) declara una cola atada
a `notificaciones.incentivo` para sus propios fines, empieza a recibir los avisos internos de
notificaciones, que no son `PerfilNotificacionDTO` y no puede procesar.

### Cómo se dispara

Se declara un consumidor sobre `notificaciones.incentivo` → cada notificación que este servicio se
publica a sí mismo llega ahí y falla al deserializar.

### Propuesta

- Publicar el aviso interno con una routing key propia (por ejemplo `notificaciones.aviso`),
  declarada en `RabbitConfig` y atada a la cola de notificaciones. Actualizar la constante y el
  binding.

---

# Corregidos

Un bullet por fix. El detalle largo (mecanismo, verificación y evidencia) quedó en el historial de git.

- **1. Test de n8n no hermético** — `N8nIntegrationTest` reescrito con `MockRestServiceServer`: sin `@SpringBootTest`, sin red, con aserciones de URL, payload y 404.
- **2. El consumidor no relanzaba** — se agregó dead letter y reintento acotado; relanzar lo transitorio se evitó a propósito (duplicaría envíos). Ver 15.
- **4. Faltaba converter de mensajes** — `MessageConverterConfig` publica JSON con Jackson sin `__TypeId__`; el `RabbitTemplate` dejó de usar `SimpleMessageConverter`.
- **5. `@OneToOne` sin cascada** — `cascade = ALL` en `Notificacion.mensaje`: el `Mensaje` no se insertaba y la FK reventaba el INSERT.
- **6. El `id_mensaje` no viajaba en el JSON** — el flujo que lo disparaba ya no existe (el consumidor lee JSON crudo y arma un aviso o una solicitud): no reproducible con el código actual.
- **7. El factory no normalizaba el tipo** — `MedioDeEnvioFactory` resuelve en minúsculas y por alias (`EMAIL`, `MAIL`, `WHATSAPP`).
- **8. Sin validación en el borde HTTP** — `@NotBlank` en el DTO y `@Valid` en el controller. Ver 14.
- **9. El consumidor no era idempotente** — `procesarAvisoDeNotificacionExistente` no reenvía si el estado ya es `ENVIADA`.
- **10. `RestTemplate` sin timeouts** — connect 5 s / read 10 s configurables; se niega a arrancar si el timeout es 0.
- **11. Credenciales y Rabbit en properties** — Rabbit ya estaba configurado; `show-sql` movido al perfil dev; la credencial de MySQL quedó como default por decisión del equipo.
- **12. El GET devolvía `tipoMedioDeContacto` en null** — el mapeo pasó a `NotificacionDTO.desdeEntidad`.
- **13. Todo devolvía 401** — se sacó `spring-boot-starter-security` del pom padre; incentivos conserva su `SecurityConfig`.
- **14. DTO sin validación y handler comentado** — `GlobalExceptionHandler` real, `spring-boot-starter-validation` y `ErrorResponseDTO`. Ver 8.
- **15. Cola sin dead letter** — DLX + DLQ, `default-requeue-rejected=false` y retry acotado. Ver 2.
- **16. El `__TypeId__` rompía la integración** — converter propio sin encabezado de tipo; el log bajó de 1.9 GB a 73 KB. Ver 4.
- **17. El default de `servicio.n8n.url` daba 404** — se puso el webhook completo por defecto.
- **18. Se publicaba antes del commit** — la publicación pasó a `afterCommit` con `registerSynchronization`.
- **19. `enviarNotificacion` ignoraba `direccionContacto`** — la dirección del mensaje gana y se setea en la entidad.
- **23. Código muerto y comentarios obsoletos** — se borraron clases y métodos sin uso, y todos los javadocs largos.

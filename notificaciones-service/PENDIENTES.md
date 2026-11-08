## Revisión del enunciado hasta Entrega 6

El servicio dispone de API, persistencia relacional, consumo/publicación por Rabbit y estrategias de
canal. E2/E4 requieren avisos asincrónicos de los casos funcionales y soporte de los canales del
 enunciado; confirmar que el alcance implementado los cubre. Los pendientes técnicos de resiliencia,
idempotencia y operación de colas siguen detallados abajo.

Correcciones de vigencia respecto de este backlog: el POST ahora responde 201 y el DTO expone `id`;
por tanto, las descripciones antiguas de los puntos 36 y 39 que dicen que el ID no se devuelve están
resueltas parcialmente. La cabecera `Location` aún debe verificarse/implementarse. El punto 34 debe
revisarse contra las validaciones actuales antes de tratar la aceptación de medio inválido como vigente.

---
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
| 38 | 38 | `ConsumidorNotificaciones` traga fallos de red/timeout como `IllegalArgumentException`, marca `FALLIDA` y da ACK a RabbitMQ sin reintento |
| 34 | 34 | `POST /api/notificaciones` no valida `medioDeContacto`: acepta con 202 medios inválidos que mueren asíncronamente en `FALLIDA` |
| 35 | 35 | `GestorNotificaciones.enviarNotificacion` enmascara errores de red y timeout como `IllegalArgumentException` |
| 39 | 39 | `POST /api/notificaciones` responde 202 sin retornar el ID asignado ni cabecera `Location` |
| 37 | 37 | `NotificacionMapper` falla con `NullPointerException` si `mensaje` o `fechaCreacion` son nulos |
| 20 | 20 | El reintento manual que prometen los comentarios no existe: ni endpoint, ni scheduler, ni forma de buscar por estado |
| 27 | 27 | El canal SMS del enunciado no está contemplado: solo email, teléfono y WhatsApp |
| 3 | 3 | Faltan tests de `ConsumidorNotificaciones` y de las transiciones de estado de `Notificacion` |
| 21 | 21 | La idempotencia es check-then-act sin lock: dos entregas concurrentes mandan dos mails |
| 22 | 22 | Binding de `notificaciones.evento.logistica` que ningún servicio publica |
| 28 | 28 | El mapa de la ruta viaja como URL en el texto, no como adjunto |
| 26 | 26 | No hay endpoint de colección ni paginación (`GET /notificaciones` paginado) |
| 36 | 36 | `NotificacionDTO` no expone el campo `id` de la entidad |
| 40 | 40 | `Notificacion` genera `UUID.randomUUID()` en el campo `@Id` forzando `merge` con SELECT previo en cada guardado |
| 24 | 24 | La organización de paquetes no sigue la de `incentivos-service` (repos, dto, clientes) |
| 25 | 25 | `GestorNotificaciones` mezcla responsabilidades: no está partido por comportamiento |
---

## 3. Faltan tests de `ConsumidorNotificaciones` y de las transiciones de estado de `Notificacion`

**Estado:** abierto
**Severidad:** media
**Archivos:** `src/test/`

### Qué pasa

Se agregaron tests unitarios y de slice (`GestorNotificacionesTest`, `RabbitConfigTest`,
`NotificadorControllerTest`, `NotificacionMapperTest`, `ConfiguracionArranqueTest`), pero el núcleo
del procesamiento asíncrono sigue descubierto:

- No hay ningún test de `ConsumidorNotificaciones`: ni la distinción entre aviso y solicitud, ni el
  manejo de reentregas, ni la reacción ante errores de BD o mensajes malformados. Es el componente con
  mayor lógica del servicio y carece de tests unitarios o con Rabbit mockeado.
- `Notificacion` no tiene tests sobre el ciclo de vida de la entidad: transiciones `marcarEnviada()`
  (que debe actualizar `estado` y fijar `fechaEnvio`), `marcarFallida()` (que no debe alterar
  `fechaEnvio`), ni validación de que una notificación recién creada mantenga `fechaEnvio` en null.

### Propuesta

- Crear `ConsumidorNotificacionesTest` con mocks de `GestorNotificaciones`,
  `RepositorioNotificaciones` y `ObjectMapper`, probando el flujo de recepción de avisos, solicitudes
  válidas, solicitudes incompletas y la propagación de excepciones.
- Crear `NotificacionTest` que verifique constructores y el comportamiento de `marcarEnviada()` y
  `marcarFallida()`.

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

- Bloqueo pesimista al leer la notificacion (indById con PESSIMISTIC_WRITE dentro de una transaccion)
  o transicion a un estado intermedio EN_PROCESO. Evitar marcar ENVIADA antes del llamado a n8n para
  no mentir sobre el resultado si el despacho remoto falla.
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
  que la ruta real es logística â donaciones â notificaciones.

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

- `models/repositories/*.java` â `models/repositories/SpringRepositories/`.
- `dto/` â subcarpetas por área (por ejemplo `dto/Notificacion/` para `NotificacionDTO`,
  `SolicitudNotificacionDTO` y `NotificacionPayload`; `dto/Error/` para `ErrorResponseDTO`).
- `clientes/` â `clients/`.
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
tiene tres medios â`Mail` (`email`), `Telefono` (`telefono`) y `Whatsapp` (`whatsapp`)â, pero
ninguno se llama `sms` y `MedioDeEnvioFactory` no reconoce ese valor.

`mapearAMedioEnvio` busca el tipo tal cual (`sms`) en el mapa de beans, después prueba los alias
(`mail`/`gmail`/`tel`/`celular`/`wa`/â¦) y, si no lo encuentra, lanza
`IllegalArgumentException("Tipo de medio de contacto desconocido: â¦")`. Eso deja la notificación en
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

## 34. `POST /api/notificaciones` no valida `medioDeContacto`: acepta con 202 medios inválidos que mueren asíncronamente en `FALLIDA`

**Estado:** abierto
**Severidad:** media
**Archivos:** `src/main/java/ar/edu/utn/frba/ddsi/notificaciones/controllers/NotificadorController.java:51-61`,
`src/main/java/ar/edu/utn/frba/ddsi/notificaciones/dto/SolicitudNotificacionDTO.java:11-12`,
`src/main/java/ar/edu/utn/frba/ddsi/notificaciones/models/entities/MedioDeEnvio/MedioDeEnvioFactory.java:19-43`,
`src/main/java/ar/edu/utn/frba/ddsi/notificaciones/exceptions/GlobalExceptionHandler.java:64-72`

### Qué pasa

En el borde HTTP, `SolicitudNotificacionDTO` valida con `@NotBlank` que `medioDeContacto` no esté vacío,
pero no comprueba que corresponda a un medio soportado (`email`, `whatsapp`, `telefono` o sus alias).

Al llamar a `POST /api/notificaciones` con un medio inválido (ej: `"telegram"` o `"slack"`):
1. `NotificadorController.recibirSolicitudNotificacion` valida exitosamente con `@Valid` y responde de
   inmediato HTTP `202 ACCEPTED` ("solicitud procesada con éxito").
2. Se persiste la notificación en MySQL como `PENDIENTE` con `tipo_medio_contacto = "telegram"`.
3. Se publica el aviso interno en RabbitMQ.
4. Cuando el consumidor asíncrono toma el aviso, ejecuta `MedioDeEnvioFactory.mapearAMedioEnvio("telegram")`,
   el cual arroja `IllegalArgumentException: Tipo de medio de contacto desconocido: 'telegram'`.
5. La notificación es marcada irrevocablemente como `FALLIDA`.

El cliente HTTP recibió confirmación de aceptación para un mensaje que el sistema sabía de antemano que
era inválido y que nunca podrá entregarse. Además, el manejador `@ExceptionHandler(IllegalArgumentException.class)`
en `GlobalExceptionHandler` (documentado expresamente para *"Datos inválidos detectados por el dominio,
por ejemplo un medio de contacto desconocido"*) queda inalcanzable para el endpoint `POST` porque la
validación ocurre recién en el consumidor asíncrono.

### Cómo se dispara

Cualquier request HTTP `POST /api/notificaciones` que envíe un `medioDeContacto` no soportado o con un
error tipográfico.

### Propuesta de arreglo

- Validar el `medioDeContacto` en el borde HTTP contra `MedioDeEnvioFactory` antes de aceptar la
  solicitud, o mediante una anotación de validación personalizada / enum en `SolicitudNotificacionDTO`,
  respondiendo HTTP 400 Bad Request con el detalle de medios permitidos.

---

## 35. `GestorNotificaciones.enviarNotificacion` enmascara errores de red y timeout como `IllegalArgumentException`

**Estado:** abierto
**Severidad:** media
**Archivos:** `src/main/java/ar/edu/utn/frba/ddsi/notificaciones/models/gestores/GestorNotificaciones.java:90-95`

### Qué pasa

En `GestorNotificaciones.enviarNotificacion`:

```java
        } catch (RuntimeException excepcion) {
            notificacion.marcarFallida();
            log.error("[GESTOR] Falló el despacho de la notificación {} por el medio '{}': {}",
                    notificacion.getId(), tipoMedioDeContacto, excepcion.getMessage(), excepcion);
            throw new IllegalArgumentException(mensajeDeEnvioFallido(excepcion), excepcion);
        }
```

Cualquier falla en tiempo de ejecución ocurrida durante el envío âincluyendo fallas de red hacia n8n
(`ResourceAccessException` por connection refused, timeout de conexión o timeout de lectura) o errores
5xx devueltos por el webhook de n8n (`HttpServerErrorException`)â es capturada por el bloque genérico
`catch (RuntimeException)` y relanzada envuelta en un `IllegalArgumentException`.

`IllegalArgumentException` denota semánticamente un parámetro incorrecto provisto por quien invoca el
método. Convertir un fallo de comunicación o indisponibilidad de un servicio externo en un error de
argumento:
1. Distorsiona el diagnóstico y la tipificación del error.
2. Si se invoca en un contexto web, `GlobalExceptionHandler.manejarIllegalArgument` lo traduce a HTTP
   `400 BAD REQUEST` (atribuyendo la culpa al cliente) en lugar de un `503 SERVICE UNAVAILABLE` o
   `500 INTERNAL SERVER ERROR`.
3. En el consumidor de mensajería, impide discriminar entre errores irrecuperables de datos y fallas de
   infraestructura transitorias.

### Cómo se dispara

Cualquier interrupción temporal del servicio n8n, saturación de red o timeout al ejecutar
`enviarNotificacion`.

### Propuesta de arreglo

- No envolver excepciones de infraestructura o comunicación HTTP en `IllegalArgumentException`.
  Preservar el tipo de excepción original o utilizar una excepción de dominio dedicada (ej:
  `FalloDespachoNotificacionException`), diferenciando errores de validación de medios de fallas en el
  gateway de transporte.

---

## 36. `NotificacionDTO` no expone el campo `id` de la entidad

**Estado:** abierto
**Severidad:** baja
**Archivos:** `src/main/java/ar/edu/utn/frba/ddsi/notificaciones/dto/NotificacionDTO.java:8-16`,
`src/main/java/ar/edu/utn/frba/ddsi/notificaciones/mappers/NotificacionMapper.java:9-23`

### Qué pasa

`NotificacionDTO` incluye los campos de contenido, medio, destino, estado y timestamps, pero no incluye
el identificador `id` (UUID) de la notificación:

```java
public class NotificacionDTO {
    private String asunto;
    private String cuerpo;
    private String tipoMedioDeContacto;
    private String direccionDeContacto;
    private String fechaCreacion;
    private String fechaEnvio;
    private String estado;
}
```

Al consultar `GET /api/notificaciones/{id}`, la respuesta omite el `id` propio del registro consultado.
Más aún, cuando se implemente la colección paginada `GET /api/notificaciones` requerida en el punto 26,
el endpoint devolverá una lista de notificaciones anónimas sin identificador, impidiendo que el cliente o
frontend pueda seleccionar una notificación para consultar su detalle o para solicitar un reintento manual
(`POST /api/notificaciones/{id}/reintento`, punto 20).

### Cómo se dispara

Cualquier consulta al endpoint `GET /api/notificaciones/{id}`.

### Propuesta de arreglo

- Agregar `private String id;` a `NotificacionDTO` y mapearlo en `NotificacionMapper.notificacionDTO` con
  `notificacion.getId().toString()`.

---

## 37. `NotificacionMapper` falla con `NullPointerException` si `mensaje` o `fechaCreacion` son nulos

**Estado:** abierto
**Severidad:** media
**Archivos:** `src/main/java/ar/edu/utn/frba/ddsi/notificaciones/mappers/NotificacionMapper.java:12-17`

### Qué pasa

En `NotificacionMapper.notificacionDTO`:

```java
        notificacionDTO.setAsunto(notificacion.getMensaje().getAsunto());
        notificacionDTO.setCuerpo(notificacion.getMensaje().getCuerpo());
        notificacionDTO.setDireccionDeContacto(notificacion.getDireccionDeContacto());
        notificacionDTO.setTipoMedioDeContacto(notificacion.getTipoMedioDeContacto());
        notificacionDTO.setEstado(notificacion.getEstado().toString());
        notificacionDTO.setFechaCreacion(notificacion.getFechaCreacion().toString());
```

El mapeador asume que `notificacion.getMensaje()`, `notificacion.getEstado()` y
`notificacion.getFechaCreacion()` son siempre no nulos.
Sin embargo:
1. La relación `mensaje` en `Notificacion` no especifica `optional = false`. Si por una migración de
   datos, lectura parcial o fila anómala una notificación carece de mensaje asociado,
   `notificacion.getMensaje().getAsunto()` lanza inmediatamente `NullPointerException`.
2. Si una entidad es construida por frameworks o serializadores utilizando el constructor sin argumentos
   `@NoArgsConstructor` de `Notificacion`, `fechaCreacion` y `estado` quedan en `null`, disparando
   `NullPointerException` al invocar `.toString()`.
3. Este NPE no está contemplado por ningún handler específico en `GlobalExceptionHandler` y termina
   generando una respuesta HTTP `500 INTERNAL SERVER ERROR` genérica al consultar
   `GET /api/notificaciones/{id}`.

### Cómo se dispara

Consultar por id (`GET /api/notificaciones/{id}`) una notificación cuya fila en la base de datos tenga
`id_mensaje` nulo o campos de auditoría incompletos.

### Propuesta de arreglo

- Chequear nulidad en `NotificacionMapper` antes de invocar métodos sobre `notificacion.getMensaje()`,
  `fechaCreacion` y `estado`, asignando `null` o cadenas vacías si no están presentes.

## 38. `ConsumidorNotificaciones` traga fallos de red/timeout como `IllegalArgumentException`, marca `FALLIDA` y da ACK a RabbitMQ sin reintento

**Estado:** abierto
**Severidad:** alta
**Archivos:** `messaging/ConsumidorNotificaciones.java:181-202`, `models/gestores/GestorNotificaciones.java:120-125`

### Qué pasa

En `GestorNotificaciones.enviarNotificacion`:

```java
} catch (RuntimeException excepcion) {
    notificacion.marcarFallida();
    log.error("[GESTOR] Falló el despacho de la notificación...", ...);
    throw new IllegalArgumentException(mensajeDeEnvioFallido(excepcion), excepcion);
}
```

Cualquier caída de n8n, fallo de DNS o timeout de red (`RestClientException`) es capturado y relanzado envuelto en un `IllegalArgumentException`.
A su vez, en `ConsumidorNotificaciones.enviar`:

```java
try {
    gestor.enviarNotificacion(...);
    notificacion.marcarEnviada();
} catch (RuntimeException excepcion) {
    notificacion.marcarFallida();
    log.error("[COLA] Notificación {} FALLÓ...", ...);
}
repositorioNotificaciones.save(notificacion);
```

El consumidor captura la excepción en un `catch (RuntimeException excepcion)`, persiste la notificación con estado `FALLIDA` y **retorna normalmente sin relanzar**.
Como el método del `@RabbitListener` finaliza sin arrojar excepciones, RabbitMQ interpreta que el mensaje se consumió con éxito y envía el `ACK`.
Como el servicio de notificaciones no posee ningún scheduler ni cola de reintentos para notificaciones en estado `FALLIDA`, cualquier caída transitoria de n8n de pocos segundos provoca que la notificación se pierda definitivamente sin posibilidad de recuperación.

### Cómo se dispara

Cualquier indisponibilidad temporal, reinicio de contenedor o lentitud en el webhook de n8n mientras se envían notificaciones.

### Propuesta de arreglo

Diferenciar en `GestorNotificaciones` los errores de cliente/datos inválidos (que sí son `IllegalArgumentException`) de los errores de transporte e infraestructura (`RestClientException` / timeouts). En `ConsumidorNotificaciones`, relanzar los errores transitorios para que opere la política de retry y dead letter exchange de RabbitMQ.

---

## 39. `POST /api/notificaciones` responde 202 sin retornar el ID asignado ni cabecera `Location`

**Estado:** abierto
**Severidad:** media
**Archivos:** `controllers/NotificadorController.java:51-61`

### Qué pasa

En `NotificadorController`:

```java
@PostMapping
public ResponseEntity<String> recibirSolicitudNotificacion(@Valid @RequestBody SolicitudNotificacionDTO dto) {
    ...
    notificadorService.procesarSolicitudDeNotificacion(dto);
    return new ResponseEntity<>("solicitud procesada con éxito", HttpStatus.ACCEPTED);
}
```

El endpoint acepta la notificación con código HTTP 202 ACCEPTED y un string plano en el body. Sin embargo, no devuelve el identificador `id` (UUID) de la notificación creada ni expone una cabecera `Location: /api/notificaciones/{id}`.
Como resultado, el servicio o cliente emisor que envía la notificación no tiene ninguna forma programática de conocer el `id` para luego consultar el estado de la entrega mediante `GET /api/notificaciones/{id}`.

### Cómo se dispara

Cualquier cliente HTTP que solicite el envío de una notificación y necesite trackear su estado.

### Propuesta de arreglo

Modificar el retorno de `procesarSolicitudDeNotificacion` para devolver el `UUID` creado y responder con un DTO que incluya el `id` y la URL del recurso en la cabecera `Location`.

---

## 40. `Notificacion` genera `UUID.randomUUID()` en el campo `@Id` forzando `merge` con SELECT previo en cada guardado

**Estado:** abierto
**Severidad:** baja
**Archivos:** `models/entities/Notificacion/Notificacion.java:28`

### Qué pasa

En `Notificacion`:

```java
@Id
private UUID id = UUID.randomUUID();
```

En Spring Data JPA, cuando una entidad tiene su clave `@Id` preasignada en el constructor o declaración de campo, el método `JpaRepository.save(entity)` determina si la entidad es "nueva" mediante `entityInformation.isNew(entity)`. Al detectar que el `@Id` no es `null`, Hibernate asume que la entidad ya existe y ejecuta un `EntityManager.merge()` en lugar de `EntityManager.persist()`.
Esto obliga a Hibernate a emitir un `SELECT` previo a la tabla `notificaciones` por cada notificación nueva que se inserta, para verificar si la clave ya está en la base de datos antes de disparar el `INSERT`. Esto duplica innecesariamente el tráfico de consultas sobre MySQL.

### Cómo se dispara

Cada guardado de una nueva notificación en la base de datos.

### Propuesta de arreglo

Hacer que `Notificacion` implemente `Persistable<UUID>` con un flag `@Transient private boolean isNew = true;`, o utilizar una estrategia de generación de identificadores `@GeneratedValue` de JPA/Hibernate.

---

---

# Corregidos

Un bullet por fix. El detalle largo (mecanismo, verificación y evidencia) quedó en el historial de git.

- **1. Test de n8n no hermético** â `N8nIntegrationTest` reescrito con `MockRestServiceServer`: sin `@SpringBootTest`, sin red, con aserciones de URL, payload y 404.
- **2. El consumidor no relanzaba** â se agregó dead letter y reintento acotado; relanzar lo transitorio se evitó a propósito (duplicaría envíos). Ver 15.
- **4. Faltaba converter de mensajes** â `MessageConverterConfig` publica JSON con Jackson sin `__TypeId__`; el `RabbitTemplate` dejó de usar `SimpleMessageConverter`.
- **5. `@OneToOne` sin cascada** â `cascade = ALL` en `Notificacion.mensaje`: el `Mensaje` no se insertaba y la FK reventaba el INSERT.
- **6. El `id_mensaje` no viajaba en el JSON** â el flujo que lo disparaba ya no existe (el consumidor lee JSON crudo y arma un aviso o una solicitud): no reproducible con el código actual.
- **7. El factory no normalizaba el tipo** â `MedioDeEnvioFactory` resuelve en minúsculas y por alias (`EMAIL`, `MAIL`, `WHATSAPP`).
- **8. Sin validación en el borde HTTP** â `@NotBlank` en el DTO y `@Valid` en el controller. Ver 14.
- **9. El consumidor no era idempotente** â `procesarAvisoDeNotificacionExistente` no reenvía si el estado ya es `ENVIADA`.
- **10. `RestTemplate` sin timeouts** â connect 5 s / read 10 s configurables; se niega a arrancar si el timeout es 0.
- **11. Credenciales y Rabbit en properties** â Rabbit ya estaba configurado; `show-sql` movido al perfil dev; la credencial de MySQL quedó como default por decisión del equipo.
- **12. El GET devolvía `tipoMedioDeContacto` en null** â el mapeo pasó a `NotificacionDTO.desdeEntidad`.
- **13. Todo devolvía 401** â se sacó `spring-boot-starter-security` del pom padre; incentivos conserva su `SecurityConfig`.
- **14. DTO sin validación y handler comentado** â `GlobalExceptionHandler` real, `spring-boot-starter-validation` y `ErrorResponseDTO`. Ver 8.
- **15. Cola sin dead letter** â DLX + DLQ, `default-requeue-rejected=false` y retry acotado. Ver 2.
- **16. El `__TypeId__` rompía la integración** â converter propio sin encabezado de tipo; el log bajó de 1.9 GB a 73 KB. Ver 4.
- **17. El default de `servicio.n8n.url` daba 404** â se puso el webhook completo por defecto.
- **18. Se publicaba antes del commit** â la publicación pasó a `afterCommit` con `registerSynchronization`.
- **19. `enviarNotificacion` ignoraba `direccionContacto`** â la dirección del mensaje gana y se setea en la entidad.
- **23. Código muerto y comentarios obsoletos** â se borraron clases y métodos sin uso, y todos los javadocs largos.
- **30. El consumidor no valida `asunto`/`cuerpo`** - se validan ambos campos en `procesarSolicitud`, rechazando solicitudes incompletas a DLQ con `AmqpRejectAndDontRequeueException`.
- **32. El consumidor traga excepciones** - se elimino la captura silenciosa en `recibir()`; los fallos irrecuperables se derivan a DLQ con `AmqpRejectAndDontRequeueException` y los transitorios se relanzan para reintento en RabbitMQ.
- **31. Aviso interno con routing key de incentivos** — se definió la routing key propia `notificaciones.aviso` en `RabbitConfig` con su binding a `colaNotificaciones`, y `ProductorNotificaciones` publica los avisos bajo esa clave.
- **33. Discriminación errónea por campo `id` en JSON** — `ConsumidorNotificaciones` ahora discrimina avisos de solicitudes mediante la routing key AMQP (`notificaciones.aviso`) y no por el payload; `SolicitudNotificacionDTO` tolera campos desconocidos.
- **29. Idempotencia en camino de solicitudes** — se incorporó `idSolicitud` en el contrato compartido con los emisores (donaciones e incentivos), persistido con constraint UNIQUE en `Notificacion`, deduplicando reentregas en `ConsumidorNotificaciones` y `GestorNotificaciones`.

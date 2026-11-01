# Pendientes tÃ©cnicos de `notificaciones-service`

Registro de problemas conocidos del servicio, con el motivo y la propuesta de arreglo para
que no se pierdan de vista al crecer el cÃ³digo.

**EstÃ¡n ordenados de mÃ¡s urgente a menos urgente**, no por nÃºmero de punto. El nÃºmero es un
ID estable y no se renumera nunca, asÃ­ que quedan huecos. Un punto corregido se borra de esta
lista y pasa a la secciÃ³n [Corregidos](#corregidos) del final.

El orden no es el de la severidad declarada en cada punto sino el del daÃ±o real: cuÃ¡nto se
rompe cuando pasa, y quÃ© tan fÃ¡cil es que pase.

| # | Punto | Por quÃ© estÃ¡ acÃ¡ |
|---|---|---|
| 34 | 34 | `POST /api/notificaciones` no valida `medioDeContacto`: acepta con 202 medios invÃ¡lidos que mueren asÃ­ncronamente en `FALLIDA` |
| 35 | 35 | `GestorNotificaciones.enviarNotificacion` enmascara errores de red y timeout como `IllegalArgumentException` |
| 37 | 37 | `NotificacionMapper` falla con `NullPointerException` si `mensaje` o `fechaCreacion` son nulos |
| 20 | 20 | El reintento manual que prometen los comentarios no existe: ni endpoint, ni scheduler, ni forma de buscar por estado |
| 27 | 27 | El canal SMS del enunciado no estÃ¡ contemplado: solo email, telÃ©fono y WhatsApp |
| 3 | 3 | Faltan tests de `ConsumidorNotificaciones` y de las transiciones de estado de `Notificacion` |
| 21 | 21 | La idempotencia es check-then-act sin lock: dos entregas concurrentes mandan dos mails |
| 22 | 22 | Binding de `notificaciones.evento.logistica` que ningÃºn servicio publica |
| 28 | 28 | El mapa de la ruta viaja como URL en el texto, no como adjunto |
| 26 | 26 | No hay endpoint de colecciÃ³n ni paginaciÃ³n (`GET /notificaciones` paginado) |
| 36 | 36 | `NotificacionDTO` no expone el campo `id` de la entidad |
| 24 | 24 | La organizaciÃ³n de paquetes no sigue la de `incentivos-service` (repos, dto, clientes) |
| 25 | 25 | `GestorNotificaciones` mezcla responsabilidades: no estÃ¡ partido por comportamiento |
---

## 3. Faltan tests de `ConsumidorNotificaciones` y de las transiciones de estado de `Notificacion`

**Estado:** abierto
**Severidad:** media
**Archivos:** `src/test/`

### QuÃ© pasa

Se agregaron tests unitarios y de slice (`GestorNotificacionesTest`, `RabbitConfigTest`,
`NotificadorControllerTest`, `NotificacionMapperTest`, `ConfiguracionArranqueTest`), pero el nÃºcleo
del procesamiento asÃ­ncrono sigue descubierto:

- No hay ningÃºn test de `ConsumidorNotificaciones`: ni la distinciÃ³n entre aviso y solicitud, ni el
  manejo de reentregas, ni la reacciÃ³n ante errores de BD o mensajes malformados. Es el componente con
  mayor lÃ³gica del servicio y carece de tests unitarios o con Rabbit mockeado.
- `Notificacion` no tiene tests sobre el ciclo de vida de la entidad: transiciones `marcarEnviada()`
  (que debe actualizar `estado` y fijar `fechaEnvio`), `marcarFallida()` (que no debe alterar
  `fechaEnvio`), ni validaciÃ³n de que una notificaciÃ³n reciÃ©n creada mantenga `fechaEnvio` en null.

### Propuesta

- Crear `ConsumidorNotificacionesTest` con mocks de `GestorNotificaciones`,
  `RepositorioNotificaciones` y `ObjectMapper`, probando el flujo de recepciÃ³n de avisos, solicitudes
  vÃ¡lidas, solicitudes incompletas y la propagaciÃ³n de excepciones.
- Crear `NotificacionTest` que verifique constructores y el comportamiento de `marcarEnviada()` y
  `marcarFallida()`.

---

## 20. El reintento manual que prometen los comentarios no existe

**Estado:** abierto
**Severidad:** media
**Archivos:**
`.../controllers/NotificadorController.java`,
`.../models/repositories/RepositorioNotificaciones.java:19`

### QuÃ© pasa

El Javadoc del gestor (lÃ­neas 69-70) y el del consumidor (lÃ­neas 45-46) dicen que una notificaciÃ³n
fallida *"se puede reintentar desde la base"*, y el punto 2 de este backlog repite que *"el
reintento manual es posible hoy"*. No lo es: el Ãºnico endpoint del servicio es
`GET /notificaciones/{id}`. No hay POST de reintento, no hay scheduler, y no hay ningÃºn mÃ©todo para
buscar las `PENDIENTE` y `FALLIDA` (el `findByEstado` que existÃ­a sin uso se borrÃ³ con el punto 23).

O sea que hoy toda fila que no llega a `ENVIADA` es terminal: queda el registro como Ãºnica
evidencia, sin ningÃºn camino de recuperaciÃ³n, ni automatizado ni manual por API.

### Propuesta

- `POST /notificaciones/{id}/reintento` que republica el aviso, pasando por el mismo camino
  post-commit del punto 18.
- Un `@Scheduled` que reintente las `PENDIENTE` con fecha de creaciÃ³n antigua, con tope de
  intentos: es el automatismo que pide el punto 2.

---

## 21. La idempotencia es check-then-act sin lock

**Estado:** abierto
**Severidad:** baja
**Archivo:** `.../messaging/ConsumidorNotificaciones.java:111-118`

### QuÃ© pasa

`procesarAvisoDeNotificacionExistente` lee el estado, decide y envÃ­a, todo sin transacciÃ³n ni lock:
dos entregas concurrentes del mismo aviso (reentrega del broker con mÃ¡s de un consumidor, o un
doble publicado) pasan ambas el control de `ENVIADA` antes de que ninguna escriba el resultado, y
la notificaciÃ³n sale dos veces.

Hoy lo evita la configuraciÃ³n, no el diseÃ±o: el container levanta un consumidor por cola y la
ventana entre el `findById` y el `save` final es chica. Con `concurrency > 1` o dos instancias del
servicio apuntando a la misma cola, deja de ser gratis.

### Propuesta

- Bloqueo pesimista al leer la notificacion (indById con PESSIMISTIC_WRITE dentro de una transaccion)
  o transicion a un estado intermedio EN_PROCESO. Evitar marcar ENVIADA antes del llamado a n8n para
  no mentir sobre el resultado si el despacho remoto falla.
---

## 22. La cola escucha `notificaciones.evento.logistica` y ningÃºn servicio publica esa clave

**Estado:** abierto
**Severidad:** baja
**Archivo:** `.../config/rabbit/RabbitConfig.java:70-76`

### QuÃ© pasa

`bindingNotificacionesEventosLogistica` ata la cola al routing key `notificaciones.evento.logistica`
y no hay ningÃºn productor que lo use. `logisticas-service` publica en `logistica.eventos.exchange`
con `logistica.evento` (`ProductorEventosLogistica.java:44-47`), y este servicio recibe esos hechos
reciÃ©n cuando `donaciones-service` los reenvÃ­a por `notificaciones.donacion`.

No rompe nada: es un binding sin trÃ¡fico. El riesgo es de lectura, no de runtime: invita a pensar
que logÃ­stica habla directo con este servicio, que es exactamente lo que el enunciado prohÃ­be.

### Propuesta

- Sacar el binding y la constante `RK_EVENTO_LOGISTICA`, o dejar en ambos un comentario que diga
  que la ruta real es logÃ­stica â donaciones â notificaciones.

---

## 24. La organizaciÃ³n de paquetes no sigue la de `incentivos-service`

**Estado:** abierto
**Severidad:** baja
**Archivos:** `.../models/repositories/`, `.../dto/`, `.../clientes/`

### QuÃ© pasa

`incentivos-service` ordena por responsabilidad y por Ã¡rea; acÃ¡ quedÃ³ plano. Diferencias concretas:

- Los Spring Data van en `models/repositories/SpringRepositories/`; acÃ¡ estÃ¡n sueltos en
  `models/repositories/`.
- Los DTO se agrupan por Ã¡rea (`dto/Perfil/`, `dto/Admin/`, `dto/n8n/`); acÃ¡ hay un `dto/` plano con
  todo mezclado.
- El paquete de clientes HTTP se llama `clients/`; acÃ¡ se llama `clientes/`.
- Tiene `controllers/request/` para los filtros de query y `models/events/` para los eventos de
  dominio; acÃ¡ no hay equivalentes (todavÃ­a no hacen falta, pero conviene la misma forma).

No rompe nada: es de lectura y de consistencia entre mÃ³dulos.

### Propuesta

Renombrar y mover sin cambiar comportamiento:

- `models/repositories/*.java` â `models/repositories/SpringRepositories/`.
- `dto/` â subcarpetas por Ã¡rea (por ejemplo `dto/Notificacion/` para `NotificacionDTO`,
  `SolicitudNotificacionDTO` y `NotificacionPayload`; `dto/Error/` para `ErrorResponseDTO`).
- `clientes/` â `clients/`.
- Cuando aparezcan filtros de query, `controllers/request/`.

---

## 25. `GestorNotificaciones` mezcla responsabilidades: no estÃ¡ partido por comportamiento

**Estado:** abierto
**Severidad:** baja
**Archivo:** `.../models/gestores/GestorNotificaciones.java`

### QuÃ© pasa

`incentivos-service` nombra los gestores por lo que hacen (`ValidadorAdmin`, `SincronizacionPerfiles`,
`SecuenciaCategoria`), no por la entidad que tocan. AcÃ¡ hay un Ãºnico `GestorNotificaciones` que hace
cuatro cosas distintas:

- guardar y publicar la solicitud (`enviarSolicitudDeNotificacion`, con el `afterCommit`),
- construir y persistir (`crearNotificacion`),
- elegir el medio y despachar (`enviarNotificacion`),
- consultar (`obtenerNotificacionPorId`).

### Propuesta

Partirlo por comportamiento, con nombres que digan quÃ© hacen. Una divisiÃ³n posible:
`RegistroNotificaciones` (guardar + publicar), `DespachoNotificaciones` (elegir el medio y enviar) y
`ConsultaNotificaciones` (buscar por id / listar). Ajustar los llamadores
(`NotificadorService`, `ConsumidorNotificaciones`) y los tests.

---

## 26. No hay endpoint de colecciÃ³n ni paginaciÃ³n

**Estado:** abierto
**Severidad:** baja
**Archivos:** `.../controllers/NotificadorController.java`,
`.../models/repositories/RepositorioNotificaciones.java`

### QuÃ© pasa

El servicio solo expone `POST /notificaciones` y `GET /notificaciones/{id}`. No hay forma de listar
las notificaciones ni de filtrarlas por estado, aunque el punto 20 pide poder encontrar las
`PENDIENTE`/`FALLIDA` para reintentar.

`incentivos-service` expone sus colecciones paginadas: `GET` con
`@PageableDefault(page = 0, size = 10, sort = ...) Pageable` y `ResponseEntity<Page<DTO>>`
(`RankingController`, `MisionController`, `CategoriaController`, las insignias de `PerfilController`).

### Propuesta

Agregar `GET /notificaciones` siguiendo ese patrÃ³n:

- Controller: `ResponseEntity<Page<NotificacionDTO>>` con
  `@RequestParam(required = false) EstadoNotificacion estado` y
  `@PageableDefault(page = 0, size = 10, sort = "fechaCreacion", direction = Sort.Direction.DESC)`.
- Service: `Page<NotificacionDTO> listar(EstadoNotificacion estado, Pageable pageable)`.
- Repo: `Page<Notificacion> findByEstado(EstadoNotificacion estado, Pageable pageable)` y
  `Page<Notificacion> findAll(Pageable pageable)` (el `findByEstado` que habÃ­a, sin paginar, se borrÃ³
  por muerto con el punto 23).
- Tests de controller con `MockMvc`: 200, tamaÃ±o de pÃ¡gina y filtro por estado.

---

## 27. El canal SMS del enunciado no estÃ¡ contemplado: solo email, telÃ©fono y WhatsApp

**Estado:** abierto
**Severidad:** media
**Archivos:** `.../models/entities/MedioDeEnvio/MedioDeEnvioFactory.java`,
`.../models/entities/MedioDeEnvio/Telefono.java`

### QuÃ© pasa

El enunciado pide comunicaciÃ³n multicanal por **correo electrÃ³nico, SMS y WhatsApp**. El servicio
tiene tres medios â`Mail` (`email`), `Telefono` (`telefono`) y `Whatsapp` (`whatsapp`)â, pero
ninguno se llama `sms` y `MedioDeEnvioFactory` no reconoce ese valor.

`mapearAMedioEnvio` busca el tipo tal cual (`sms`) en el mapa de beans, despuÃ©s prueba los alias
(`mail`/`gmail`/`tel`/`celular`/`wa`/â¦) y, si no lo encuentra, lanza
`IllegalArgumentException("Tipo de medio de contacto desconocido: â¦")`. Eso deja la notificaciÃ³n en
`FALLIDA`: un productor que mande `medioDeContacto: "SMS"` no envÃ­a nada.

En todo el repo no hay una sola referencia a `SMS`: los servicios de dominio emiten `EMAIL`/`MAIL`,
`TELEFONO` y `WHATSAPP`. O sea que el "SMS" del enunciado hoy solo se cumple si se lo trata como el
medio `telefono`, y por convenciÃ³n, no por contrato.

### Propuesta

- Agregar el alias `sms` en `MedioDeEnvioFactory` (apuntando a `telefono`, o a un `MedioDeEnvio`
  `Sms` propio si el envÃ­o por n8n necesita un canal distinto del telefÃ³nico).
- Que el servicio de dominio que corresponda emita `SMS`, o dejar asentado que `TELEFONO` **es** el
  canal SMS del enunciado.
- Test que publique con `medioDeContacto: "SMS"` y verifique que se despacha por el medio esperado.

---

## 28. El mapa de la ruta viaja como URL en el texto, no como adjunto

**Estado:** abierto
**Severidad:** baja
**Archivo:** `.../dto/NotificacionPayload.java`

### QuÃ© pasa

El escenario "inicio de ruta adjuntando un mapa interactivo" **se despacha**: `logisticas-service`
arma `Ruta.urlSeguimiento` en `publicarInicioRuta`, lo manda en el evento `INICIO_RUTA`,
`donaciones-service` lo reenvÃ­a y `NotificacionViaje` arma el cuerpo con
`"Sigue la entrega: <url>"`. El link llega, pero:

- Viaja como texto dentro de `mensaje.cuerpo`. `NotificacionPayload` solo tiene `canal`,
  `direccionContacto` y `mensaje` (asunto/cuerpo): no hay campo de adjunto ni de link estructurado,
  asÃ­ que el "mapa adjunto" es una URL pegada en el texto.
- La URL es un template placeholder (`https://donaciones-app.example.com/seguimiento/<idRuta>`), no
  un mapa real; eso vive en `logisticas-service`.

No es un incumplimiento estricto (el link se envÃ­a), pero si se espera un adjunto o un campo
propio, hoy no hay dÃ³nde ponerlo.

### Propuesta

- Si alcanza con el link, dejarlo escrito en el contrato y no cambiar nada.
- Si se quiere un adjunto real, agregar un campo opcional (por ejemplo `urlMapa` o `adjuntos`) a
  `NotificacionPayload` y que el `MedioDeEnvio`/n8n lo usen.

---

## 34. `POST /api/notificaciones` no valida `medioDeContacto`: acepta con 202 medios invÃ¡lidos que mueren asÃ­ncronamente en `FALLIDA`

**Estado:** abierto
**Severidad:** media
**Archivos:** `src/main/java/ar/edu/utn/frba/ddsi/notificaciones/controllers/NotificadorController.java:51-61`,
`src/main/java/ar/edu/utn/frba/ddsi/notificaciones/dto/SolicitudNotificacionDTO.java:11-12`,
`src/main/java/ar/edu/utn/frba/ddsi/notificaciones/models/entities/MedioDeEnvio/MedioDeEnvioFactory.java:19-43`,
`src/main/java/ar/edu/utn/frba/ddsi/notificaciones/exceptions/GlobalExceptionHandler.java:64-72`

### QuÃ© pasa

En el borde HTTP, `SolicitudNotificacionDTO` valida con `@NotBlank` que `medioDeContacto` no estÃ© vacÃ­o,
pero no comprueba que corresponda a un medio soportado (`email`, `whatsapp`, `telefono` o sus alias).

Al llamar a `POST /api/notificaciones` con un medio invÃ¡lido (ej: `"telegram"` o `"slack"`):
1. `NotificadorController.recibirSolicitudNotificacion` valida exitosamente con `@Valid` y responde de
   inmediato HTTP `202 ACCEPTED` ("solicitud procesada con Ã©xito").
2. Se persiste la notificaciÃ³n en MySQL como `PENDIENTE` con `tipo_medio_contacto = "telegram"`.
3. Se publica el aviso interno en RabbitMQ.
4. Cuando el consumidor asÃ­ncrono toma el aviso, ejecuta `MedioDeEnvioFactory.mapearAMedioEnvio("telegram")`,
   el cual arroja `IllegalArgumentException: Tipo de medio de contacto desconocido: 'telegram'`.
5. La notificaciÃ³n es marcada irrevocablemente como `FALLIDA`.

El cliente HTTP recibiÃ³ confirmaciÃ³n de aceptaciÃ³n para un mensaje que el sistema sabÃ­a de antemano que
era invÃ¡lido y que nunca podrÃ¡ entregarse. AdemÃ¡s, el manejador `@ExceptionHandler(IllegalArgumentException.class)`
en `GlobalExceptionHandler` (documentado expresamente para *"Datos invÃ¡lidos detectados por el dominio,
por ejemplo un medio de contacto desconocido"*) queda inalcanzable para el endpoint `POST` porque la
validaciÃ³n ocurre reciÃ©n en el consumidor asÃ­ncrono.

### CÃ³mo se dispara

Cualquier request HTTP `POST /api/notificaciones` que envÃ­e un `medioDeContacto` no soportado o con un
error tipogrÃ¡fico.

### Propuesta de arreglo

- Validar el `medioDeContacto` en el borde HTTP contra `MedioDeEnvioFactory` antes de aceptar la
  solicitud, o mediante una anotaciÃ³n de validaciÃ³n personalizada / enum en `SolicitudNotificacionDTO`,
  respondiendo HTTP 400 Bad Request con el detalle de medios permitidos.

---

## 35. `GestorNotificaciones.enviarNotificacion` enmascara errores de red y timeout como `IllegalArgumentException`

**Estado:** abierto
**Severidad:** media
**Archivos:** `src/main/java/ar/edu/utn/frba/ddsi/notificaciones/models/gestores/GestorNotificaciones.java:90-95`

### QuÃ© pasa

En `GestorNotificaciones.enviarNotificacion`:

```java
        } catch (RuntimeException excepcion) {
            notificacion.marcarFallida();
            log.error("[GESTOR] FallÃ³ el despacho de la notificaciÃ³n {} por el medio '{}': {}",
                    notificacion.getId(), tipoMedioDeContacto, excepcion.getMessage(), excepcion);
            throw new IllegalArgumentException(mensajeDeEnvioFallido(excepcion), excepcion);
        }
```

Cualquier falla en tiempo de ejecuciÃ³n ocurrida durante el envÃ­o âincluyendo fallas de red hacia n8n
(`ResourceAccessException` por connection refused, timeout de conexiÃ³n o timeout de lectura) o errores
5xx devueltos por el webhook de n8n (`HttpServerErrorException`)â es capturada por el bloque genÃ©rico
`catch (RuntimeException)` y relanzada envuelta en un `IllegalArgumentException`.

`IllegalArgumentException` denota semÃ¡nticamente un parÃ¡metro incorrecto provisto por quien invoca el
mÃ©todo. Convertir un fallo de comunicaciÃ³n o indisponibilidad de un servicio externo en un error de
argumento:
1. Distorsiona el diagnÃ³stico y la tipificaciÃ³n del error.
2. Si se invoca en un contexto web, `GlobalExceptionHandler.manejarIllegalArgument` lo traduce a HTTP
   `400 BAD REQUEST` (atribuyendo la culpa al cliente) en lugar de un `503 SERVICE UNAVAILABLE` o
   `500 INTERNAL SERVER ERROR`.
3. En el consumidor de mensajerÃ­a, impide discriminar entre errores irrecuperables de datos y fallas de
   infraestructura transitorias.

### CÃ³mo se dispara

Cualquier interrupciÃ³n temporal del servicio n8n, saturaciÃ³n de red o timeout al ejecutar
`enviarNotificacion`.

### Propuesta de arreglo

- No envolver excepciones de infraestructura o comunicaciÃ³n HTTP en `IllegalArgumentException`.
  Preservar el tipo de excepciÃ³n original o utilizar una excepciÃ³n de dominio dedicada (ej:
  `FalloDespachoNotificacionException`), diferenciando errores de validaciÃ³n de medios de fallas en el
  gateway de transporte.

---

## 36. `NotificacionDTO` no expone el campo `id` de la entidad

**Estado:** abierto
**Severidad:** baja
**Archivos:** `src/main/java/ar/edu/utn/frba/ddsi/notificaciones/dto/NotificacionDTO.java:8-16`,
`src/main/java/ar/edu/utn/frba/ddsi/notificaciones/mappers/NotificacionMapper.java:9-23`

### QuÃ© pasa

`NotificacionDTO` incluye los campos de contenido, medio, destino, estado y timestamps, pero no incluye
el identificador `id` (UUID) de la notificaciÃ³n:

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
MÃ¡s aÃºn, cuando se implemente la colecciÃ³n paginada `GET /api/notificaciones` requerida en el punto 26,
el endpoint devolverÃ¡ una lista de notificaciones anÃ³nimas sin identificador, impidiendo que el cliente o
frontend pueda seleccionar una notificaciÃ³n para consultar su detalle o para solicitar un reintento manual
(`POST /api/notificaciones/{id}/reintento`, punto 20).

### CÃ³mo se dispara

Cualquier consulta al endpoint `GET /api/notificaciones/{id}`.

### Propuesta de arreglo

- Agregar `private String id;` a `NotificacionDTO` y mapearlo en `NotificacionMapper.notificacionDTO` con
  `notificacion.getId().toString()`.

---

## 37. `NotificacionMapper` falla con `NullPointerException` si `mensaje` o `fechaCreacion` son nulos

**Estado:** abierto
**Severidad:** media
**Archivos:** `src/main/java/ar/edu/utn/frba/ddsi/notificaciones/mappers/NotificacionMapper.java:12-17`

### QuÃ© pasa

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
1. La relaciÃ³n `mensaje` en `Notificacion` no especifica `optional = false`. Si por una migraciÃ³n de
   datos, lectura parcial o fila anÃ³mala una notificaciÃ³n carece de mensaje asociado,
   `notificacion.getMensaje().getAsunto()` lanza inmediatamente `NullPointerException`.
2. Si una entidad es construida por frameworks o serializadores utilizando el constructor sin argumentos
   `@NoArgsConstructor` de `Notificacion`, `fechaCreacion` y `estado` quedan en `null`, disparando
   `NullPointerException` al invocar `.toString()`.
3. Este NPE no estÃ¡ contemplado por ningÃºn handler especÃ­fico en `GlobalExceptionHandler` y termina
   generando una respuesta HTTP `500 INTERNAL SERVER ERROR` genÃ©rica al consultar
   `GET /api/notificaciones/{id}`.

### CÃ³mo se dispara

Consultar por id (`GET /api/notificaciones/{id}`) una notificaciÃ³n cuya fila en la base de datos tenga
`id_mensaje` nulo o campos de auditorÃ­a incompletos.

### Propuesta de arreglo

- Chequear nulidad en `NotificacionMapper` antes de invocar mÃ©todos sobre `notificacion.getMensaje()`,
  `fechaCreacion` y `estado`, asignando `null` o cadenas vacÃ­as si no estÃ¡n presentes.

---

# Corregidos

Un bullet por fix. El detalle largo (mecanismo, verificaciÃ³n y evidencia) quedÃ³ en el historial de git.

- **1. Test de n8n no hermÃ©tico** â `N8nIntegrationTest` reescrito con `MockRestServiceServer`: sin `@SpringBootTest`, sin red, con aserciones de URL, payload y 404.
- **2. El consumidor no relanzaba** â se agregÃ³ dead letter y reintento acotado; relanzar lo transitorio se evitÃ³ a propÃ³sito (duplicarÃ­a envÃ­os). Ver 15.
- **4. Faltaba converter de mensajes** â `MessageConverterConfig` publica JSON con Jackson sin `__TypeId__`; el `RabbitTemplate` dejÃ³ de usar `SimpleMessageConverter`.
- **5. `@OneToOne` sin cascada** â `cascade = ALL` en `Notificacion.mensaje`: el `Mensaje` no se insertaba y la FK reventaba el INSERT.
- **6. El `id_mensaje` no viajaba en el JSON** â el flujo que lo disparaba ya no existe (el consumidor lee JSON crudo y arma un aviso o una solicitud): no reproducible con el cÃ³digo actual.
- **7. El factory no normalizaba el tipo** â `MedioDeEnvioFactory` resuelve en minÃºsculas y por alias (`EMAIL`, `MAIL`, `WHATSAPP`).
- **8. Sin validaciÃ³n en el borde HTTP** â `@NotBlank` en el DTO y `@Valid` en el controller. Ver 14.
- **9. El consumidor no era idempotente** â `procesarAvisoDeNotificacionExistente` no reenvÃ­a si el estado ya es `ENVIADA`.
- **10. `RestTemplate` sin timeouts** â connect 5 s / read 10 s configurables; se niega a arrancar si el timeout es 0.
- **11. Credenciales y Rabbit en properties** â Rabbit ya estaba configurado; `show-sql` movido al perfil dev; la credencial de MySQL quedÃ³ como default por decisiÃ³n del equipo.
- **12. El GET devolvÃ­a `tipoMedioDeContacto` en null** â el mapeo pasÃ³ a `NotificacionDTO.desdeEntidad`.
- **13. Todo devolvÃ­a 401** â se sacÃ³ `spring-boot-starter-security` del pom padre; incentivos conserva su `SecurityConfig`.
- **14. DTO sin validaciÃ³n y handler comentado** â `GlobalExceptionHandler` real, `spring-boot-starter-validation` y `ErrorResponseDTO`. Ver 8.
- **15. Cola sin dead letter** â DLX + DLQ, `default-requeue-rejected=false` y retry acotado. Ver 2.
- **16. El `__TypeId__` rompÃ­a la integraciÃ³n** â converter propio sin encabezado de tipo; el log bajÃ³ de 1.9 GB a 73 KB. Ver 4.
- **17. El default de `servicio.n8n.url` daba 404** â se puso el webhook completo por defecto.
- **18. Se publicaba antes del commit** â la publicaciÃ³n pasÃ³ a `afterCommit` con `registerSynchronization`.
- **19. `enviarNotificacion` ignoraba `direccionContacto`** â la direcciÃ³n del mensaje gana y se setea en la entidad.
- **23. CÃ³digo muerto y comentarios obsoletos** â se borraron clases y mÃ©todos sin uso, y todos los javadocs largos.
- **30. El consumidor no valida `asunto`/`cuerpo`** - se validan ambos campos en `procesarSolicitud`, rechazando solicitudes incompletas a DLQ con `AmqpRejectAndDontRequeueException`.
- **32. El consumidor traga excepciones** - se elimino la captura silenciosa en `recibir()`; los fallos irrecuperables se derivan a DLQ con `AmqpRejectAndDontRequeueException` y los transitorios se relanzan para reintento en RabbitMQ.
- **31. Aviso interno con routing key de incentivos** — se definió la routing key propia `notificaciones.aviso` en `RabbitConfig` con su binding a `colaNotificaciones`, y `ProductorNotificaciones` publica los avisos bajo esa clave.
- **33. Discriminación errónea por campo `id` en JSON** — `ConsumidorNotificaciones` ahora discrimina avisos de solicitudes mediante la routing key AMQP (`notificaciones.aviso`) y no por el payload; `SolicitudNotificacionDTO` tolera campos desconocidos.
- **29. Idempotencia en camino de solicitudes** — se incorporó `idSolicitud` en el contrato compartido con los emisores (donaciones e incentivos), persistido con constraint UNIQUE en `Notificacion`, deduplicando reentregas en `ConsumidorNotificaciones` y `GestorNotificaciones`.

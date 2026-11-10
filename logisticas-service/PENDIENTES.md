# Pendientes técnicos de `logisticas-service`

Registro de problemas conocidos del servicio, con el motivo y la propuesta de arreglo para
que no se pierdan de vista al crecer el código.

**Están ordenados de más urgente a menos urgente**, no por número de punto. El número es un
ID estable y no se renumera nunca, así que quedan huecos. Un punto corregido se borra de esta
lista y pasa a la sección [Corregidos](#corregidos) del final.

El orden no es el de la severidad declarada en cada punto sino el del daño real: cuánto se
rompe cuando pasa, y qué tan fácil es que pase.

| # | Punto | Severidad | Por qué está acá |
|---|---|---|---|
| 1 | 64 | Alta | `DonacionListener` duerme el hilo con `Thread.sleep` y no aborta ante interrupción, bloqueando el pool AMQP |
| 2 | 60 | Alta | `ProveedorRutasExternoHttp` falla en tiempo de ejecución al serializar `ItemEntrega` con `LocalDateTime` por falta de `JavaTimeModule` en su `ObjectMapper` |
| 3 | 61 | Alta | `EntregaService.convertirADireccionDTO` y `convertirABienDTO` causan `NullPointerException` en `GET /api/entregas` ante entidades o direcciones con datos parciales |
| 4 | 69 | Alta | Los eventos de trazabilidad crecen sin cota dentro de cada ítem y el polling los carga enteros en memoria |
| 5 | 68 | Media | `EntregaService.actualizarEstado` publica eventos al broker dentro de la transacción SQL (dual-write) |
| 6 | 65 | Media | `ProveedorRutasExternoSimulado` ejecuta I/O y sleep en `ForkJoinPool.commonPool()`, saturando hilos compartidos de la JVM |
| 7 | 66 | Media | `ProveedorRutasExternoHttp` usa `sendAsync` sin handler de excepciones descartando fallas de red |
| 8 | 67 | Media | `PlanificadorRutasService.asignarChoferes` usa `synchronized` local que no previene colisiones entre réplicas en clúster |
| 9 | 49 | Media | RepositorioRutas escanea la colección entera de rutas en memoria (`findAll().stream()`) en cada búsqueda de ruta o chofer |
| 10 | 31 | Media | Callback del simulador: URL fijada a `localhost:8086`, sin timeouts y descarta status de respuesta |
| 11 | 48 | Media | Un fallo del broker al publicar después del commit deja el 500, el cambio aplicado y la notificación perdida |
| 12 | 43 | Media | El manejo de errores está repetido en cada controller: 404 ante campos obligatorios ausentes y 500 ante `IllegalStateException` |
| 13 | 44 | Media | Los listados devuelven la tabla entera: sin paginar, una respuesta crece sin techo |
| 14 | 17 | Media | El planificador resetea la carga de los camiones y nunca la persiste |
| 15 | 62 | Media | `CamionService.update` desvincula incondicionalmente el chofer asignado al camión si el DTO no envía `idChofer` |
| 16 | 70 | Media | El pool de Mongo queda en los defaults (`maxPoolSize=100` por instancia) y sin timeouts |
| 17 | 71 | Media | El callback de rutas hace una consulta por id y búsquedas O(n·m) sobre camiones e ítems |
| 18 | 72 | Media | El Dockerfile no fija `-XX:MaxRAMPercentage` y el compose no limita la memoria |
| 19 | 37 | Baja | El CRUD de camiones/choferes devuelve 500 para errores de validación |
| 20 | 40 | Baja | Los gestores están nombrados por entidad y mezclan reglas de negocio con persistencia |
| 21 | 42 | Baja | Paquetes de primer nivel y ubicación de clientes, scheduler y eventos distintos al resto de los servicios |
| 22 | 20 | Informativa | Verificado: logística no invoca a `donaciones-service` ni incentivos ni habla con notificaciones |
| 23 | 73 | Media | La ruta no persiste ni expone el trazado por calles: el front no puede dibujarla en el mapa |
| 24 | 74 | Baja | El planificador simulado podría resolver la parte geográfica con OSRM y la asignación con una heurística local |

---

## 60. `ProveedorRutasExternoHttp` falla en tiempo de ejecución al serializar `ItemEntrega` con `LocalDateTime` por falta de `JavaTimeModule` en su `ObjectMapper`

**Estado:** abierto  
**Severidad:** alta  
**Archivos:** `models/entities/PlanificadorDeRutas/ProveedorRutasExterno/ProveedorRutasExternoHttp.java:24-60`

### Qué pasa

En `ProveedorRutasExternoHttp`:

```java
public ProveedorRutasExternoHttp(String urlApiExterna) {
  this.httpClient = HttpClient.newHttpClient();
  this.urlApiExterna = urlApiExterna;
  this.objectMapper = new ObjectMapper();
}
...
Map<String, Object> payload = new HashMap<>();
payload.put("donaciones", lote);
payload.put("camiones", camionesDisponibles);

String jsonPayload = objectMapper.writeValueAsString(payload);
```

1. `objectMapper` se instancia directamente con `new ObjectMapper()` sin registrar `JavaTimeModule` ni invocar `findAndRegisterModules()`.
2. `lote` contiene instancias de `ItemEntrega`, que incluye `private LocalDateTime fechaCambioEstado;` y una lista de `EventoLogistica` (que también posee `private LocalDateTime fecha;`).
3. Por defecto, Jackson lanza `InvalidDefinitionException: Java 8 date/time type java.time.LocalDateTime not supported by default: add Module "com.fasterxml.jackson.datatype:jackson-datatype-jsr310" to enable handling`.
4. La excepción es atrapada por el bloque `catch (Exception e)` de la línea 57, logueando el fallo sin enviar jamás la petición HTTP al proveedor externo. Toda llamada con este proveedor falla de inmediato.
5. Adicionalmente, `HttpClient` y `HttpRequest` no configuran timeouts, y `sendAsync` no tiene bloque `.exceptionally(...)` para manejar fallos de conexión o caídas del servicio externo.

### Cómo se dispara

Configurar el bean `ProveedorRutasExternoHttp` en `LogisticaConfig` y disparar una planificación manual o automática.

### Propuesta de arreglo

Registrar `new JavaTimeModule()` en el `ObjectMapper` (o inyectar el `ObjectMapper` configurado por Spring Boot), configurar timeouts en la petición HTTP y encadenar `.exceptionally(...)` en el `CompletableFuture`.

---

## 61. `EntregaService.convertirADireccionDTO` y `convertirABienDTO` causan `NullPointerException` en `GET /api/entregas` ante entidades o direcciones con datos parciales

**Estado:** abierto  
**Severidad:** alta  
**Archivos:** `services/EntregaService.java:450-465`, `controllers/EntregaController.java:28-37`

### Qué pasa

En `EntregaService.convertirADireccionDTO`:

```java
private DireccionDTO convertirADireccionDTO(Entidad entidad){
  return new DireccionDTO(
      entidad.getIdEntidadBeneficiaria(),
      entidad.getDireccionDestino().getCalle1(),
      entidad.getDireccionDestino().getCalle2(),
      entidad.getDireccionDestino().getAltura(),
      entidad.getDireccionDestino().getPiso(),
      entidad.getDireccionDestino().getDepartamento(),
      entidad.getDireccionDestino().getCiudad().getNombre(),
      entidad.getDireccionDestino().getCiudad().getProvincia().getNombre(),
      entidad.getDireccionDestino().getCiudad().getProvincia().getPais().getNombre()
  );
}
```

Y en `convertirABienDTO`:

```java
private BienDTO convertirABienDTO(ItemEntrega item){
  return new BienDTO(item.getCantidad(), item.getUnidad().getNombre(), item.getEstado().toString(), item.getFechaCambioEstado(), item.getFotoComprobante(), convertirADireccionDTO(item.getEntidadDestino()), convertirEventosADTO(item.getEventos()));
}
```

A diferencia de `EntregaService.findById` (líneas 107-122), que implementa verificaciones de nulidad exhaustivas para `entidadDestino`, `direccionDestino`, `ciudad`, `provincia`, `pais`, `unidad` y `estado`, los métodos `convertirADireccionDTO` y `convertirABienDTO` desreferencian directamente toda la cadena de navegación.

Si existe un solo ítem en la base de datos con `entidadDestino` nula, dirección destino nula, o alguna relación geográfica sin completar, la invocación de `GET /api/entregas` o `GET /api/entregas/no-recibidas` falla con HTTP 500 por `NullPointerException`, bloqueando la visualización de todos los ítems del sistema.

### Cómo se dispara

Consultar `GET /api/entregas` o `GET /api/entregas/no-recibidas` existiendo en base de datos un ítem con dirección o entidad incompleta.

### Propuesta de arreglo

Replicar en `convertirADireccionDTO` y `convertirABienDTO` las comprobaciones de nulidad presentes en `findById`.

---

## 31. Callback del simulador de rutas: URL fijada a `localhost:8086`, sin timeouts y descarta status de respuesta

**Estado:** abierto  
**Severidad:** media  
**Archivos:** `models/entities/PlanificadorDeRutas/ProveedorRutasExterno/ProveedorRutasExternoSimulado.java:23-53`

### Qué pasa

En `ProveedorRutasExternoSimulado`:

1. **URL fijada en el código:** La URL del callback está cableada como constante estática:
   ```java
   private final String URL_CALLBACK_LOCAL = "http://localhost:8086/api/PlanificacionRutas/callback";
   ```
   Si el servicio corre en otro puerto (`SERVER_PORT != 8086`) o en un contenedor Docker en red puente, el callback intenta conectarse a un host o puerto erróneo y las rutas calculadas nunca regresan.
2. **Sin timeouts en el cliente HTTP:**
   ```java
   HttpClient httpClient = HttpClient.newHttpClient();
   HttpRequest request = HttpRequest.newBuilder()
           .uri(URI.create(URL_CALLBACK_LOCAL))
           .header("Content-Type", "application/json")
           .POST(HttpRequest.BodyPublishers.ofString(jsonRutas))
           .build();
   ```
   Ni el `HttpClient` define `connectTimeout` ni la `HttpRequest` define `.timeout()`. Si el callback tarda en responder o no acepta conexiones, el hilo de `CompletableFuture.runAsync` queda bloqueado indefinidamente.
3. **Código de estado descartado:**
   ```java
   HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
   System.out.println("Respuesta del servidor: " + response.body());
   ```
   Se descarta `response.statusCode()`. Si el endpoint de callback responde 400 Bad Request o 500 Internal Server Error (ej. fallas en deserialización o validación), el simulador lo ignora sin reintentar ni registrar un error estructurado, provocando que el lote de ítems se pierda en silencio y permanezca bloqueado o sin asignación.

### Cómo se dispara

- Levantar la aplicación en un puerto distinto al 8086 (`-Dserver.port=8080`).
- Simular indisponibilidad o error 500 en el endpoint de recepción del callback durante la planificación nocturna: la falla es silenciosa.

### Propuesta de arreglo

1. Extraer la URL de callback a una propiedad inyectable (`logistica.callback-url` o resolver dinámicamente según `server.port`) en `application.properties`.
2. Configurar timeouts explícitos de conexión y lectura en `HttpClient` y `HttpRequest` (ej. `Duration.ofSeconds(5)`).
3. Validar `response.statusCode() == 200` y loguear un error explícito ante respuestas fallidas o excepciones de red.

---

## 48. Un fallo del broker al publicar después del commit deja el 500, el cambio aplicado y la notificación perdida

**Estado:** abierto  
**Severidad:** media  
**Archivos:** `models/gestores/GestorPublicacionEventos.java:140-159`, `messaging/ProductorEventosLogistica.java:30-40`, `controllers/EntregaController.java:80-92`

### Qué pasa

`publicarAlCommit` registra una sincronización `afterCommit` que invoca `productorEventos.publicar(evento)`. Si RabbitMQ no está disponible en ese instante, la excepción se propaga fuera del interceptor de transacciones cuando la transacción de base de datos **ya fue confirmada**:

- El ítem queda actualizado en la base de datos (por ejemplo a `ENTREGADA`).
- El evento queda persistido en `evento_logistica`.
- El cliente HTTP recibe un **500 Internal Server Error** con el stack trace o mensaje interno.
- El evento nunca llegó al broker: `donaciones-service` no notifica.
- Al reintentar la llamada HTTP por el 500, el endpoint rechaza con 400 ("no está en camino") porque el estado ya cambió.

### Propuesta de arreglo

Atrapar la excepción de RabbitMQ dentro de `afterCommit` y loguear como advertencia, o implementar un patrón Transactional Outbox con reintentos para publicación asíncrona.

---

## 43. El manejo de errores está repetido en cada controller: 404 ante campos obligatorios ausentes y 500 ante `IllegalStateException`

**Estado:** abierto  
**Severidad:** media  
**Archivos:** `controllers/CamionController.java:101-106`, `controllers/ChoferController.java:104-109`, `controllers/RutaController.java:80-99`, `controllers/EntregaController.java:44-52`, `services/RutaService.java:156-164`

### Qué pasa

No existe un `@ControllerAdvice` o `@RestControllerAdvice` global. Cada controlador maneja excepciones manualmente con bloques `try/catch` dispersos, lo que genera inconsistencias graves y códigos HTTP erróneos:

1. **Traducción errónea de validaciones a 404:**
   - En `ChoferController.cambiarEstadoDisponibilidad` y `CamionController.cambiarEstadoDisponibilidad`: si se omite el campo obligatorio `disponible` en el body JSON, el servicio lanza `IllegalArgumentException("El campo 'disponible' es obligatorio.")`. El controller atrapa `IllegalArgumentException` y responde **404 Not Found** en lugar de 400 Bad Request, informando falsamente que el recurso no existe.
   - En contraste, `EntregaController.actualizarEstadoEntrega` traduce `IllegalArgumentException` a 400 Bad Request, mientras que `EntregaController.obtenerPorId` la traduce a 404.
2. **Fugas de `IllegalStateException` que devuelven 500:**
   - En `RutaController.terminarRuta`:
     ```java
     @PatchMapping("/chofer/{idChofer}/terminar")
     public ResponseEntity<String> terminarRuta(@PathVariable UUID idChofer) {
       try {
         rutaService.terminarRuta(idChofer);
         return ResponseEntity.ok("Ruta finalizada correctamente...");
       } catch (IllegalArgumentException e) {
         return ResponseEntity.badRequest().body(e.getMessage());
       } catch (Exception e) {
         return ResponseEntity.internalServerError().body("Error interno al finalizar la ruta: " + e.getMessage());
       }
     }
     ```
     Si un chofer intenta terminar una ruta sin tener una en curso, `RutaService.rutaDelChoferPorEstado` lanza:
     ```java
     throw new IllegalStateException("No se encontró una ruta en estado EN_CURSO para el chofer " + idChofer);
     ```
     A diferencia de `RutaController.iniciarRuta` (que atrapa `IllegalArgumentException | IllegalStateException` y responde 400), `terminarRuta` solo atrapa `IllegalArgumentException`. La `IllegalStateException` cae en `catch (Exception e)` y responde **500 Internal Server Error**, filtrando detalles internos de la aplicación ante un error que es estrictamente de precondición o estado de negocio (400/409/422).

### Cómo se dispara

- Enviar `PATCH /api/choferes/{id}/disponibilidad` con `{}` → Devuelve 404 en lugar de 400.
- Enviar `PATCH /api/rutas/chofer/{id}/terminar` sin una ruta activa → Devuelve 500 en lugar de 400/409.

### Propuesta de arreglo

Crear un `@RestControllerAdvice` global (`GlobalExceptionHandler`) que centralice el manejo de excepciones:
- `IllegalArgumentException` o `MethodArgumentNotValidException` → HTTP 400 Bad Request.
- `NoSuchElementException` o excepciones custom `RecursoNoEncontradoException` → HTTP 404 Not Found.
- `IllegalStateException` → HTTP 400 Bad Request o 409 Conflict / 422 Unprocessable Entity.
- Eliminar los bloques `try/catch` manuales y duplicados en todos los controllers para que deleguen en el advice central.

---

## 44. Los listados devuelven la tabla entera: falta paginación

**Estado:** abierto  
**Severidad:** media  
**Archivos:** `controllers/CamionController.java:31-34`, `controllers/ChoferController.java:31-34`, `controllers/EntregaController.java:28-31`, `controllers/RutaController.java:26-29`

### Qué pasa

Los endpoints de listado (`GET /api/camiones`, `GET /api/choferes`, `GET /api/entregas`, `GET /api/rutas`) devuelven todos los registros llamando a `findAll()` sin soporte para `Pageable`. En producción el tamaño de la respuesta crece sin límite.

### Propuesta de arreglo

Implementar paginación con `Pageable` y `@PageableDefault` devolviendo `Page<DTO>` o listas paginadas.

---

## 17. El planificador resetea la carga de los camiones y nunca la persiste

**Estado:** abierto  
**Severidad:** media  
**Archivos:** `models/entities/PlanificadorDeRutas/ProveedorRutasExterno/ProveedorRutasExternoSimulado.java:59-73`, `Scheduler/PlanificadorDeRutasScheduler.java:64-70`

### Qué pasa

El simulador crea copias locales de `Camion` y resetea su carga ocupada (`copia.resetearCargaOcupada()`) para simular la asignación en memoria. Sin embargo, una vez asignados los ítems y generada la ruta, el estado de ocupación de los camiones (`pesoOcupado`, `volumenOcupado`) nunca se persiste en `repoCamiones`. Las columnas en la base de datos quedan siempre en 0.

### Propuesta de arreglo

Persistir el estado de carga acumulado de los camiones tras la asignación, o derivar la ocupación directamente de las rutas activas persistidas.

---

## 62. `CamionService.update` desvincula incondicionalmente el chofer asignado al camión si el DTO no envía `idChofer`

**Estado:** abierto  
**Severidad:** media  
**Archivos:** `services/CamionService.java:60-65`, `models/gestores/GestorCamiones.java:16-28`

### Qué pasa

En `CamionService.update`:

```java
public CamionDTO update(String patente, CamionDTO dto) {
  Chofer nuevoChofer = dto.getIdChofer() != null ? repoChoferes.findById(dto.getIdChofer())
          .orElseThrow(() -> new IllegalArgumentException("Chofer no encontrado")): null;
  Camion camionExistente = gestorCamiones.actualizarCamion(patente, nuevoChofer, dto.getCapacidadVolumen(), dto.getAltura(), dto.getCapacidadCarga(), dto.getDisponible());
  return convertirADTO(camionExistente);
}
```

Y en `GestorCamiones.actualizarCamion`:

```java
public Camion actualizarCamion(String patente, Chofer nuevoChofer, Double capacidadVolumen, Double altura, Double capacidadCarga, Boolean disponible){
    Camion camionExistente = repoCamiones.findById(patente)
            .orElseThrow(() -> new IllegalArgumentException("Camión no encontrado"));

    camionExistente.setChofer(nuevoChofer);
    if (capacidadVolumen != null) camionExistente.setCapacidadVolumen(capacidadVolumen);
    if (altura != null) camionExistente.setAltura(altura);
    if (capacidadCarga != null) camionExistente.setCapacidadCarga(capacidadCarga);
    if (disponible != null) camionExistente.setDisponible(disponible);

    repoCamiones.save(camionExistente);
    return camionExistente;
}
```

A diferencia de los demás atributos que se actualizan condicionalmente solo si vienen informados (`!= null`), la asignación del chofer es incondicional (`camionExistente.setChofer(nuevoChofer)`).

Si un cliente o frontend envía un `PUT /api/camiones/{patente}` para modificar un dato operativo (por ejemplo actualizar la disponibilidad o la capacidad de carga) sin incluir la propiedad `idChofer` en el body JSON, `nuevoChofer` se resuelve como `null`. Como resultado, se remueve silenciosamente el chofer asignado al camión, pudiendo desvincular choferes asignados a rutas activas o programadas.

### Cómo se dispara

Enviar una petición `PUT /api/camiones/{patente}` con un payload que no contenga `idChofer`. El chofer que tenía asignado el camión se elimina.

### Propuesta de arreglo

En `CamionService.update` o `GestorCamiones.actualizarCamion`, solo actualizar el chofer si `dto.getIdChofer()` está presente en la petición, o proveer endpoints explícitos para vincular/desvincular choferes.

---

## 37. El CRUD de camiones/choferes devuelve 500 para errores de validación

**Estado:** abierto  
**Severidad:** baja  
**Archivos:** `controllers/CamionController.java:51-63`, `controllers/ChoferController.java:51-63`

### Qué pasa

`POST /api/camiones` y `POST /api/choferes` no usan anotaciones de validación (`@Valid`) ni manejan errores de campos requeridos faltantes. Si se envía un payload sin campos requeridos, el guardado falla con excepciones de integridad o nulos que se traducen en HTTP 500 en lugar de 400 Bad Request.

### Propuesta de arreglo

Agregar anotaciones de validación (`@NotNull`, `@NotBlank`) en `CamionDTO` y `ChoferDTO`, marcar los métodos de controller con `@Valid` y capturar `MethodArgumentNotValidException`.

---

## 40. Los gestores están nombrados por entidad y mezclan reglas con persistencia

**Estado:** abierto  
**Severidad:** baja  
**Archivos:** `models/gestores/GestorCamiones.java`, `models/gestores/GestorPublicacionEventos.java`

### Qué pasa

`GestorCamiones` mezcla actualización de entidad con persistencia directa en base de datos. La capa de gestores no tiene una responsabilidad homogénea ni desacoplada de la persistencia frente a los services.

### Propuesta de arreglo

Refactorizar gestores para que contengan exclusivamente lógica de dominio y políticas de negocio, delegando la persistencia a la capa de servicios o repositorios.

---

## 42. Paquetes de primer nivel y ubicación de clientes, scheduler y eventos

**Estado:** abierto  
**Severidad:** baja  
**Archivos:** `RabbitMQ/`, `Scheduler/`, `messaging/`

### Qué pasa

`RabbitMQ/` y `Scheduler/` son paquetes en PascalCase ubicados en el nivel raíz del código fuente, en discrepancia con las convenciones estándar de nombres de paquetes en Java y con la estructura modular del resto de los microservicios del proyecto.

### Propuesta de arreglo

Renombrar los paquetes a minúsculas (`rabbitmq`, `scheduler`) y ubicarlos bajo convenciones estándar (`config`, `scheduler`, `messaging`).

---

## 49. RepositorioRutas escanea la colección entera de rutas en memoria (`findAll().stream()`) en cada búsqueda de ruta o chofer

**Estado:** abierto  
**Severidad:** media  
**Archivos:** `models/repositories/RepositorioRutas.java:15-45`, `services/EntregaService.java:381,392,405`

### Qué pasa

`RepositorioRutas` define tres métodos `default` en la interfaz de MongoDB:

```java
default Optional<Ruta> findByIdDonacion(UUID idDonacion) {
    return this.findAll().stream().filter(...).findFirst();
}

default Optional<Ruta> findByChofer(Chofer chofer) {
    return this.findAll().stream().filter(...).findFirst();
}

default Optional<Ruta> findByChoferYEstado(Chofer chofer, EstadoRuta estado) {
    return this.findAll().stream().filter(...).findFirst();
}
```

Cada vez que una entrega cambia de estado en `EntregaService`, o se verifica la asignación de un chofer en `PlanificadorRutasService`, se ejecuta `this.findAll()`. En MongoDB esto descarga **todos los documentos de la colección `ruta` a través de la red**, los deserializa completos a objetos Java en el heap y realiza una búsqueda secuencial O(N) en memoria descartando el resto. Con cientos o miles de rutas históricas, esto colapsa la red, la memoria y el rendimiento.

### Cómo se dispara

Cualquier invocación a `actualizarEstado` en `EntregaService` o asignación de choferes en el planificador.

### Propuesta de arreglo

Eliminar los métodos `default` con `this.findAll().stream()` y reemplazarlos por consultas declaradas de Spring Data MongoDB (`@Query` o derived query methods), aprovechando índices de Mongo sobre `paradas.items.id_donacion` y `camionAsignado.chofer.idChofer`.

---

## 20. Verificado: logística no invoca a `donaciones-service` ni incentivos ni habla con notificaciones

**Estado:** verificado, sin cambios necesarios  
**Severidad:** informativa  
**Archivos:** todo `logisticas-service/src/main`

El servicio de logística mantiene el cumplimiento estricto del enunciado: no realiza llamadas síncronas HTTP a otros servicios de dominio y notifica exclusivamente publicando eventos asíncronos en RabbitMQ hacia el exchange de logística.

---

## 64. `DonacionListener` duerme el hilo con `Thread.sleep` y no aborta ante interrupción, bloqueando el pool AMQP

**Estado:** abierto  
**Severidad:** alta  
**Archivos:** `RabbitMQ/DonacionListener.java:82-89`

### Qué pasa

En `DonacionListener`:

```java
private void dormir() {
    try {
        Thread.sleep(esperaMs);
    } catch (InterruptedException interrumpido) {
        Thread.currentThread().interrupt();
        log.debug("Se interrumpió la espera entre reintentos");
    }
}
```

Durante un reintento por fallo, el listener duerme el hilo de Spring AMQP de forma síncrona durante 2 segundos (`esperaMs`). Si se acumulan mensajes fallidos, todos los hilos del consumidor quedan retenidos en espera activa durmiendo, impidiendo que el servicio procese mensajes válidos que esperan en la cola.
Además, si el hilo es interrumpido durante el apagado del contexto, el bloque `catch` restablece el flag con `interrupt()` pero el bucle `for (int intento = 1; intento <= intentos; intento++)` **nunca verifica `Thread.currentThread().isInterrupted()`**, por lo que continúa ejecutando reintentos contra un contexto que está cerrándose.

### Cómo se dispara

Cualquier falla transitoria o error al procesar una donación entrante desde RabbitMQ.

### Propuesta de arreglo

Eliminar el `Thread.sleep` manual y configurar el reintento a nivel del container de Spring AMQP con un `RetryTemplate` / `StatefulRetryOperationsInterceptor` o dead-lettering con TTL en RabbitMQ. En `dormir()` o en el bucle `for`, verificar `Thread.currentThread().isInterrupted()` y lanzar `AmqpRejectAndDontRequeueException` inmediatamente.

---

## 65. `ProveedorRutasExternoSimulado` ejecuta I/O y sleep en `ForkJoinPool.commonPool()`, saturando hilos compartidos de la JVM

**Estado:** abierto  
**Severidad:** media  
**Archivos:** `models/entities/PlanificadorDeRutas/ProveedorRutasExterno/ProveedorRutasExternoSimulado.java:34-52`

### Qué pasa

`ProveedorRutasExternoSimulado.solicitarPlanificacion` lanza una tarea asíncrona:

```java
CompletableFuture.runAsync(() -> {
    try {
        Thread.sleep(2000);
        ...
        httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    } ...
});
```

Al no pasarle un `Executor` explícito, `CompletableFuture.runAsync` delega en el pool estático `ForkJoinPool.commonPool()` de la JVM. Este pool tiene una cantidad muy limitada de hilos (igual a la cantidad de cores de CPU) y está pensado para procesamiento intensivo de cómputo en memoria, no para esperas bloqueantes ni llamadas HTTP por red. Bloquear este pool con `Thread.sleep(2000)` y peticiones HTTP reduce la capacidad de paralelismo de streams paralelos y otros componentes asíncronos en toda la JVM.

Agravante de memoria: los lotes se arman con `itemsPendientes.subList(...)` y `camionesDisponibles.subList(...)` (`PlanificadorDeRutas.java:48, 51`). `subList` devuelve una vista respaldada por la lista padre, así que cada tarea asíncrona mantiene viva la lista completa durante al menos los 2 s del `sleep`, no solo sus 100 ítems. Copiar el lote antes de entregarlo (`new ArrayList<>(subList)`) libera esa retención.

### Cómo se dispara

Cada ejecución de la planificación de rutas (automática o manual) que utilice el proveedor simulado.

### Propuesta de arreglo

Inyectar un `TaskExecutor` configurado en Spring (`ThreadPoolTaskExecutor`) dedicado para tareas en segundo plano del módulo de rutas, pasando el executor como segundo argumento: `CompletableFuture.runAsync(..., executor)`.

---

## 66. `ProveedorRutasExternoHttp` usa `sendAsync` sin handler de excepciones descartando fallas de red

**Estado:** abierto  
**Severidad:** media  
**Archivos:** `models/entities/PlanificadorDeRutas/ProveedorRutasExterno/ProveedorRutasExternoHttp.java:47-56`

### Qué pasa

En `ProveedorRutasExternoHttp.solicitarPlanificacion`:

```java
httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
        .thenAccept(response -> {
            if (response.statusCode() >= 400) {
                log.error(...);
            } else {
                log.info(...);
            }
        });
```

Se llama a `httpClient.sendAsync().thenAccept(...)` pero **no se encadena ningún `.exceptionally(...)` ni `.handle(...)`**.
Si ocurre un error de resolución DNS, falla de conexión TCP, caída del servidor externo o timeout, el `CompletableFuture` subyacente se completa de forma excepcional. Como nadie consume esa excepción, Java la descarta en silencio y en los logs del servicio no queda ningún registro del fallo, aparentando que la solicitud continúa en curso cuando en realidad murió.

### Cómo se dispara

Cualquier caída o indisponibilidad de la API externa de ruteo configurada.

### Propuesta de arreglo

Agregar `.exceptionally(ex -> { log.error("Fallo la llamada HTTP al proveedor externo de rutas", ex); return null; })` y configurar un timeout explícito en `HttpRequest.newBuilder().timeout(Duration.ofSeconds(10))`.

---

## 67. `PlanificadorRutasService.asignarChoferes` usa `synchronized` local que no previene colisiones entre réplicas en clúster

**Estado:** abierto  
**Severidad:** media  
**Archivos:** `services/PlanificadorRutasService.java:84`

### Qué pasa

El método `asignarChoferes` declara:

```java
public synchronized List<Ruta> asignarChoferes(List<Ruta> rutas)
```

La sincronización se realiza sobre el monitor del bean singleton en memoria (`this`). En una arquitectura de microservicios con múltiples réplicas (por ejemplo, dos o más contenedores de `logisticas-service`), cada contenedor posee su propia instancia de la JVM y su propio monitor. Si dos callbacks de planificación llegan en paralelo a distintas réplicas, ambas ingresan simultáneamente a `asignarChoferes`, leen los mismos choferes disponibles y asignan el mismo chofer a dos rutas distintas concurrentemente.

### Cómo se dispara

Planificación concurrente o callbacks simultáneos recibidos con más de 1 réplica activa del servicio de logística.

### Propuesta de arreglo

Coordinar la exclusión mutua de la asignación mediante un bloqueo pesimista en base de datos (`findAndModify` atómico en MongoDB sobre el chofer / camion) o un cerrojo distribuido (ShedLock o Redis).

---

## 68. `EntregaService.actualizarEstado` publica eventos al broker dentro de la transacción SQL (dual-write)

**Estado:** abierto  
**Severidad:** media  
**Archivos:** `services/EntregaService.java:367, 384, 395, 404`

### Qué pasa

En `actualizarEstado`:

```java
@Transactional
public void actualizarEstado(UUID idDonacion, ActualizacionEntregaDTO request) {
    ...
    case "ENTREGADA":
        repoItemEntrega.save(gestorPublicacionEventos.publicarEntregaConfirmada(item, rutaEntregada, request.getFotoUrl()));
        sincronizarItemEnRuta(rutaEntregada, item);
        break;
    ...
    repoItemEntrega.save(item);
}
```

`gestorPublicacionEventos.publicarEntregaConfirmada(...)` emite el mensaje a RabbitMQ sincrónicamente antes de que la transacción de base de datos confirme. Si la sincronización de la ruta en MongoDB (`repoRutas.save(ruta)`) o el commit de la transacción de Spring falla a continuación, el evento en el broker ya fue emitido y entregado a `donaciones-service` e `incentivos-service`, provocando que el sistema considere entregada una donación cuya persistencia falló localmente.

### Cómo se dispara

Cualquier fallo de conexión a base de datos, error de concurrencia optimista o excepción en el commit posterior a la emisión a RabbitMQ.

### Propuesta de arreglo

Registrar la publicación del evento en RabbitMQ para ejecutarse en el callback `afterCommit()` mediante `TransactionSynchronizationManager`, asegurando que el mensaje solo viaje si la base de datos confirmó los cambios exitosamente.

---

## 69. Los eventos de trazabilidad crecen sin cota dentro de cada ítem y el polling los carga enteros en memoria

**Estado:** abierto  
**Severidad:** alta  
**Archivos:** `models/entities/ItemEntrega/ItemEntrega.java:64-73`, `models/gestores/GestorPublicacionEventos.java:131-135`, `services/EventoLogisticaService.java:22-25`, `models/repositories/RepositorioEventoLogistica.java:16-18`, `services/EntregaService.java:92-95, 443-446`

### Qué pasa

Cada cambio de estado agrega un `EventoLogistica` a la lista embebida del ítem (`item.getEventos().add(evento)`) y nunca se quita nada; además el mismo evento se guarda en la colección `evento_logistica`. El documento del ítem crece para siempre.

`obtenerEventosNuevos(desdeId)` no tiene `limit` ni paginación: si un consumidor arranca con `desdeId=0` o se atrasa, descarga **toda** la colección a una `List<EventoLogistica>` y luego la mapea a DTO, duplicando el pico. Los `findAll()` de ítems deserializan todos los eventos históricos de todos los ítems.

### Cómo se dispara

Operación normal del servicio a lo largo del tiempo; el pico aparece cuando un consumidor nuevo o atrasado hace polling desde el inicio.

### Propuesta de arreglo

Agregar `limit`/`Pageable` al polling con cursor `desdeId`, purgar o indexar con TTL `evento_logistica.fecha`, y dejar de embeber la historia completa en `ItemEntrega` (guardar solo el último evento o los ids).

---

## 70. El pool de Mongo queda en los defaults (`maxPoolSize=100` por instancia) y sin timeouts

**Estado:** abierto  
**Severidad:** media  
**Archivos:** `src/main/resources/application.properties:83-84`, `docker-compose.yml` (comentario de escalado)

### Qué pasa

La configuración de Mongo solo define `uri` y `database`. El driver usa `maxPoolSize=100` por defecto por instancia, así que con `--scale logisticas-service=2` son 200 conexiones, cada una con buffers. Tampoco hay `serverSelectionTimeoutMS`, `connectTimeoutMS` ni `socketTimeoutMS`: si Atlas no responde, las operaciones quedan colgadas reteniendo hilos y documentos.

### Cómo se dispara

Escalado horizontal del servicio o una caída/degradación del clúster de Mongo.

### Propuesta de arreglo

Fijar `maxPoolSize`, `minPoolSize`, `serverSelectionTimeoutMS`, `connectTimeoutMS` y `socketTimeoutMS` en la URI o en properties, dimensionados según la cantidad de réplicas.

---

## 71. El callback de rutas hace una consulta por id y búsquedas O(n·m) sobre camiones e ítems

**Estado:** abierto  
**Severidad:** media  
**Archivos:** `services/PlanificadorRutasService.java:70-74`, `models/entities/PlanificadorDeRutas/PlanificadorDeRutas.java:74-86`

### Qué pasa

Para cada id del callback se hace un `findById` a Mongo (`PlanificadorRutasService.java:70-74`) y, por cada asignación, `PlanificadorDeRutas` recorre con `stream().filter(...)` todos los camiones y todos los ítems. Con N ítems son N consultas y O(n·m) comparaciones, con el consiguiente garbage y latencia. `RutaService.obtenerRutaActualizada:102-106` ya muestra el patrón correcto: un `Map<UUID, ItemEntrega>`/`Map<String, Camion>` armado una sola vez.

### Cómo se dispara

Callback de planificación con muchos ítems o camiones.

### Propuesta de arreglo

Usar `findAllById(ids)` y armar mapas por id una sola vez antes del loop de asignación.

---

## 72. El Dockerfile no fija `-XX:MaxRAMPercentage` y el compose no limita la memoria

**Estado:** abierto  
**Severidad:** media  
**Archivos:** `Dockerfile:25`, `docker-compose.yml` (sección `logisticas-service`), `PENDIENTES.md` raíz (punto 6)

### Qué pasa

El `ENTRYPOINT` es `["java", "-jar", "app.jar"]`, sin `-Xmx` ni `-XX:MaxRAMPercentage`. La JVM usa el default de 25 % de la RAM del host y el compose no define `mem_limit`, así que el heap puede crecer mucho más de lo necesario. Con los eventos y los `findAll()` de rutas (puntos 69 y 49), el riesgo de OOM es concreto.

### Cómo se dispara

Cualquier barrido de rutas o acumulación de eventos en un host con RAM abundante.

### Propuesta de arreglo

Agregar `-XX:MaxRAMPercentage=70 -XX:+ExitOnOutOfMemoryError` al `ENTRYPOINT` y definir `mem_limit` para el servicio en el compose (ver punto 6 raíz).

---

## 73. La ruta no persiste ni expone el trazado por calles: el front no puede dibujarla en el mapa

**Estado:** abierto  
**Severidad:** media  
**Archivos:** `models/entities/Ruta/Ruta.java`, `models/entities/Parada/Parada.java`, `services/PlanificadorRutasService.java` (`procesarCallbackRutas`), `controllers/RutaController.java`, `dto/rutas/RutaDTO.java`, `dto/entrega/DireccionDTO.java`, `models/entities/Direccion/Direccion.java`, `models/gestores/GestorPublicacionEventos.java:51`

### Qué pasa

`Ruta` guarda las `paradas` y un `urlSeguimiento`, pero no guarda el trazado que sigue el camión por las calles. `Parada` guarda una `Direccion` sin latitud/longitud, y `DireccionDTO` tampoco transporta coordenadas. A su vez, el callback del planificador solo acepta un `Map<patente, List<UUID idDonacion>>`; aunque el proveedor externo devuelva una geometría, `procesarCallbackRutas` la descarta.

`RutaController` expone `GET /api/rutas/{id}` con las paradas, pero no existe ningún endpoint que devuelva un GeoJSON o un `LineString`. `urlSeguimiento` es solo un enlace armado en `GestorPublicacionEventos`; no reemplaza la geometría de la ruta.

### Cómo se dispara

Cualquier intento del front de mostrar una ruta en el mapa. Sin coordenadas ni polyline, el navegador solo podría dibujar líneas rectas entre paradas, y ni siquiera eso porque las paradas tampoco tienen lat/lon.

### Propuesta de arreglo

1. Al planificar o recibir el callback, obtener el trazado real por calles con un motor de ruteo (OSRM/ORS o el mismo proveedor externo) usando las paradas en orden. Guardar por ruta:
   - `List<Coordenada>` o GeoJSON `LineString`;
   - `distanciaMetros`, `duracionSegundos`, `provider`, `fechaCalculo`.
   Se puede embeber en `Ruta` o usar una colección `trazado_ruta` separada; una polyline típica entra cómodamente en el límite de 16 MB de Mongo.
   También hay que guardar `latitud`/`longitud` en `Direccion`/`Parada` (depende del pendiente 75 de `donaciones-service`).
2. Exponer `GET /api/rutas/{id}/geojson` (o `/trazado`) con `Content-Type: application/geo+json` y `Cache-Control`/`ETag`. Si la ruta todavía no tiene geometría, devolver 404 o 204; no recalcularla en el request.
3. Hacer que `front-service` actúe de BFF: proxy del endpoint y el navegador solo hace `fetch` + `DDSIMap.addGeoJson`. La API key del proveedor queda siempre en el backend.
4. Cachear por firma de paradas ordenadas para no recalcular; invalidar si cambia una parada, el camión o la fecha. Manejar el fallo del proveedor con reintento/fallback sin romper la creación de la ruta.
5. Normalizar la respuesta a GeoJSON en el backend (no guardar el JSON crudo del proveedor) para que el front no dependa de HERE/TomTom/OSRM.
6. Tests: ruta con geometría, ruta sin geometría (204/404) y callback que no descarte el trazado si el proveedor lo manda.

---

## 74. El planificador simulado podría resolver la parte geográfica con OSRM y la asignación con una heurística local

**Estado:** abierto  
**Severidad:** baja  
**Archivos:** `models/entities/PlanificadorDeRutas/ProveedorRutasExterno/ProveedorRutasExternoSimulado.java`, `.../ProveedorRutasExternoHttp.java`, `config/LogisticaConfig.java`, `models/entities/PlanificadorDeRutas/PlanificadorDeRutas.java`, `services/PlanificadorRutasService.java`

### Qué pasa

Hoy `LogisticaConfig` registra `ProveedorRutasExternoSimulado`, que agrupa las entregas por ciudad y las asigna a camiones por capacidad, pero no usa la red vial. OSRM puede resolver la matriz de distancias/tiempos y el trazado por calles, aunque no puede hacer por sí solo la asignación de múltiples camiones con capacidad. No existe una implementación local que combine ambas partes.

### Propuesta

Crear `ProveedorRutasExternoOsrm implements ProveedorRutasExterno`:

1. Obtener lat/lon de cada parada (depende del pendiente 75 de `donaciones-service`).
2. Pedir a OSRM `/table` la matriz de duraciones/distancias.
3. Resolver la asignación con una heurística Java simple: vecino más cercano + capacidad de peso/volumen, trabajando sobre copias de los `Camion` para no mutar los documentos de Mongo antes de persistir.
4. Pedir a OSRM `/route` por cada camión para obtener el `LineString` por calles.
5. Guardar el trazado (pendiente 73) y disparar el callback actual con `Map<patente, List<UUID>>`.

Elegir la implementación por property en `LogisticaConfig`, por ejemplo `planificador.proveedor=osrm|simulado`.

No hace falta sumar VROOM ni otro microservicio: OSRM queda como infraestructura y la clase local cubre lo que OSRM no hace. Si más adelante se necesita optimización real (ventanas de tiempo, prioridades, múltiples depósitos), se reemplaza solo esta clase por un adaptador a VROOM.

### Puntos finos

- OSRM necesita un origen: hoy `Camion` no tiene dirección base. Definir un depósito global por property o agregar lat/lon al camión.
- `/table` tiene límite de coordenadas (`--max-table-size`, 100 por defecto); configurarlo o partir el lote.
- Si OSRM no responde, mantener el simulador como fallback o dejar la planificación para la próxima corrida.
- Abstraer el cliente OSRM detrás de una interfaz para poder mockear la matriz en los tests.

### Por qué es baja

Es una mejora de calidad y realismo, no un bug: el simulador actual permite cumplir el flujo de planificación. Se implementa si sobra tiempo.

---

## Corregidos

- **59** — `RutaService.terminarRuta` persistía instancias desactualizadas de `Camion` y `Chofer` embebidas en `Ruta`, disparando `OptimisticLockingFailureException` y pisando datos → Causa raíz: `terminarRuta` tomaba el camión y chofer directamente del snapshot embebido en el documento `Ruta` en lugar de sus repositorios correspondientes. Al persistir con `@Version`, si el camión o chofer fueron editados durante el día, fallaba con excepción de concurrencia optimista o sobreescribía los datos vigentes, además de provocar `NullPointerException` ante rutas sin camión. Solución: se recargan las entidades vigentes mediante `repoChoferes.findById(idChofer)` y `repoCamiones.findById(patente)` con guardas defensivas ante nulos antes de actualizar disponibilidad y persistir. Tests de regresión agregados en `RutaServiceTerminarRutaTest` (validando persistencia de entidades frescas con version superior y ruta sin camión). Archivos: `RutaService.java`, `RutaServiceTerminarRutaTest.java`.

- **58** — `Entidad` no implementaba `equals` ni `hashCode`: `Ruta.agregarEntrega` nunca agrupaba donaciones y generaba paradas duplicadas para el mismo destino → Causa raíz: al deserializar ítems desde MongoDB, cada `ItemEntrega` tiene su propia instancia de `Entidad` en memoria. Al evaluarse `Objects.equals(p.getEntidadDestino(), item.getEntidadDestino())`, Java recurría a la igualdad por identidad (`==`), fallando siempre la detección de paradas existentes para el mismo destino. Solución: se implementaron `equals` y `hashCode` en `Entidad` basados en `idEntidadBeneficiaria`. Test de regresión agregado en `RutaAgregarEntregaTest` verificando consolidación de múltiples ítems en una sola parada con instancias separadas. Archivos: `Entidad.java`, `RutaAgregarEntregaTest.java`.

- **57** — Si había menos choferes que rutas generadas, las rutas sobrantes quedaban guardadas en `PROGRAMADA` sin chofer y sus ítems bloqueados de por vida → Causa raíz: `PlanificadorRutasService.procesarCallbackRutas` guardaba incondicionalmente todas las rutas en `repoRutas` antes de asignar choferes. Como `PlanificadorDeRutasScheduler` excluye de nuevas planificaciones a los ítems en rutas con estado distinto a `FINALIZADA`, y las rutas huérfanas sin chofer no podían iniciarse ni finalizarse jamás, los ítems quedaban congelados indefinidamente en `PENDIENTE`. Solución: se eliminó la persistencia prematura en `procesarCallbackRutas`; `asignarChoferes` opera sobre una copia local de la lista de rutas generadas, asigna choferes hasta el límite de choferes disponibles y persiste (`persistirRutaAsignada`) exclusivamente aquellas rutas que consiguieron chofer asignado, dejando las no asignadas libres en memoria para que sus ítems continúen disponibles para futuras planificaciones; en `PlanificadorDeRutasController` se corrigió la comparación verificando si la cantidad de rutas asignadas cubre la totalidad de las generadas (`rutasGeneradas.size() == rutasAsignadas.size()`). Tests de regresión agregados en `PlanificadorRutasServiceAsignarChoferesTest`. Archivos: `PlanificadorRutasService.java`, `PlanificadorDeRutasController.java`, `PlanificadorRutasServiceAsignarChoferesTest.java`.

- **10** — El reingreso a depósito administrativo no validaba estado previo (`NO_RECIBIDA`) y citaba método inexistente → Causa raíz: en `EntregaService.actualizarEstado` el `case "PENDIENTE"` delegaba en `gestorPublicacionEventos.publicarReingresoDeposito(item)` asumiendo erróneamente en un comentario que un método inexistente `reingresarADeposito()` validaba el estado previo. Cualquier llamada a `PATCH /api/entregas/{id}/estado` con `PENDIENTE` sobre un ítem en `ENTREGADA` o `EN_CAMINO` lo devolvía a la cola de planificación. Además, `publicarReingresoDeposito` no tenía guarda defensiva. Solución: en `EntregaService.actualizarEstado` se valida que `item.getEstado() == EstadoEntrega.NO_RECIBIDA`, lanzando `IllegalStateException` ante otros estados. En `GestorPublicacionEventos.publicarReingresoDeposito` se agregó guarda defensiva que rechaza `ENTREGADA` y `PENDIENTE`, admitiendo `NO_RECIBIDA` y `EN_CAMINO` (para permitir el retorno al depósito al finalizar una jornada en `RutaService.terminarRuta`). Tests unitarios agregados en `EntregaServiceActualizarEstadoTest` y `GestorPublicacionEventosTest`. Archivos: `EntregaService.java`, `GestorPublicacionEventos.java`, `EntregaServiceActualizarEstadoTest.java`, `GestorPublicacionEventosTest.java`.

- **63** — Soft Delete en Chofer y desvinculación de camión asignado, preservando trazabilidad de rutas históricas → Causa raíz: `ChoferService.delete` realizaba un borrado físico directo (`repoChoferes.deleteById(id)`). Si el chofer estaba asignado a un camión en `repoCamiones`, el camión quedaba apuntando a un chofer inexistente. Si bien en MongoDB el documento `Ruta` embebe un snapshot del chofer y no se pierde la trazabilidad de rutas históricas, el chofer seguía siendo elegible si se recreaba con el mismo ID o se generaban inconsistencias de relaciones activas. Solución: se implementó baja lógica (Soft Delete) en `Chofer` (`activo = true`, `fechaBaja`, `darDeBaja()`, `isActivo()`), `Chofer.disponible()` valida que el chofer esté activo antes de cambiar el estado, `ChoferService.delete` marca baja lógica y desvincula automáticamente cualquier camión asignado (`repoCamiones.findByChofer_IdChofer(id)` -> `camion.eliminarChofer()`), `ChoferService.findAll` filtra solo choferes activos, `findById` y `cambiarDisponibilidad` rechazan choferes inactivos (404 / 400), `PlanificadorRutasService.asignarChoferes` filtra exclusivamente choferes activos, y `CamionService.update` valida que el chofer a asociar esté activo. Tests agregados en `DeleteDevuelve204Test` y `PlanificadorRutasServiceAsignarChoferesTest`. Archivos: `Chofer.java`, `ChoferDTO.java`, `ChoferService.java`, `CamionService.java`, `PlanificadorRutasService.java`, `DeleteDevuelve204Test.java`, `PlanificadorRutasServiceAsignarChoferesTest.java`.

- **34** — `UnidadDeMedida` persiste constantes estáticas con `@Version`: carreras concurrentes tiran `OptimisticLockingFailureException` y reinicios duplican catálogo → Causa raíz: `UNIDADES`, `KILOGRAMOS` y `LITROS` son singletons estáticos compartidos en memoria declarados con `@Version`, y `EntregaService` ejecutaba `repoUnidades.save` por cada donación, colisionando versiones entre hilos concurrentes. Además, se instanciaban con `UUID.randomUUID()` duplicando documentos al reiniciar. Solución: se removió `@Version` de `UnidadDeMedida`, se asignaron UUIDs deterministas basados en el nombre (`UUID.nameUUIDFromBytes`), se removió `repoUnidades.save` del flujo por donación y se pre-carga el catálogo en `@PostConstruct` de `EntregaService`. Archivos: `UnidadDeMedida.java`, `EntregaService.java`, `UnidadDeMedidaTest.java`. Referencia: Pendiente de commit (cambios en staged).

- **55** — `DonacionListener` no captura `DataIntegrityViolationException`: carreras concurrentes reintentan 3 veces innecesariamente y van a la DLQ → Causa raíz: al procesar mensajes duplicados o concurrentes que violan integridad (ej. claves duplicadas), caía en el `catch (Exception e)` genérico, agotaba los 3 reintentos de RabbitMQ y enviaba el mensaje a la DLQ. Solución: se captura `DataIntegrityViolationException` explícitamente y se retorna sin relanzar ni enviar a DLQ. Archivos: `DonacionListener.java`, `DonacionListenerCarreraTest.java`. Referencia: Pendiente de commit (cambios en staged).
- **56** — Tests unitarios desactualizados rompen el build tras migración a MongoDB → Causa raíz: `ConfiguracionArranqueTest` verificaba claves JPA eliminadas; `EventoLogisticaServiceTest` usaba reflexión buscando `(String, String)` en lugar de `(Long, String)`; `EntregaServiceIdempotenciaTest` no stubbeaba el guardado del catálogo de direcciones (`repoPaises`, `repoProvincias`, `repoCiudades`, etc.). Solución: se actualizaron las aserciones a `spring.data.mongodb.uri`, se corrigió la firma del método en la reflexión a `Long.class` y se agregaron mocks/stubbings en `@BeforeEach`. Archivos: `ConfiguracionArranqueTest.java`, `EventoLogisticaServiceTest.java`, `EntregaServiceIdempotenciaTest.java`. Referencia: Pendiente de commit (cambios en staged).

- **51** — Regresión del punto 12: `obtenerEventosNuevos` resta 1 a `desdeId`, tira `NullPointerException` con null y duplica eventos → Causa raíz: al consultar eventos se calculaba `desde = (desdeId == null ? 0L : desdeId)` pero se pasaba `(desdeId - 1)` al repositorio, provocando NPE con null y reenviando el último evento ya procesado. Solución: se pasa `desde` directamente a `repoEventos.findByIdGreaterThanOrderByIdAsc(desde)`. Archivos: `EventoLogisticaService.java`, `EventoLogisticaServiceObtenerEventosNuevosTest.java`. Referencia: Pendiente de commit (cambios en staged).
- **52** — `iniciarRuta` pierde `urlSeguimiento` porque `obtenerRutaActualizada` recarga de la BD antes de guardar → Causa raíz: `iniciarRuta` asignaba la URL de seguimiento en memoria pero luego recargaba de MongoDB (donde aún era null) antes de persistir, sobreescribiendo el campo con null. Solución: se persiste la ruta (`repoRutas.save(rutaConEvento)`) antes de recargar. Archivos: `RutaService.java`, `RutaServiceIniciarRutaTest.java`. Referencia: Pendiente de commit (cambios en staged).
- **53** — `Secuencia.siguiente()` invoca un `MongoTemplate` estático no inicializado dentro del constructor de `EventoLogistica` → Causa raíz: `EventoLogistica` invocaba `Secuencia.siguiente()` en sus constructores, el cual requería que Spring hubiese inicializado `mongoTemplate`, estallando con NPE en tests unitarios. Solución: se agregó fallback atómico en memoria `AtomicLong` cuando `mongoTemplate == null` y método `setMongoTemplate`. Archivos: `Secuencia.java`, `EventoLogisticaSecuenciaTest.java`. Referencia: Pendiente de commit (cambios en staged).
- **46** — Los lotes comparten la lista de camiones: un camión queda en dos rutas del mismo día y por encima de su capacidad → Causa raíz: al particionar en lotes de 100 ítems, se enviaba la lista completa de camiones a cada lote y el simulador reseteaba la carga ocupada, permitiendo que un camión fuera asignado a múltiples rutas el mismo día. Solución: `PlanificadorDeRutas.iniciarPlanificacion` ahora particiona la lista de camiones disponibles en subconjuntos disjuntos por cada lote de ítems, garantizando que ningún camión viaje en más de un lote. Archivos: `PlanificadorDeRutas.java`, `PlanificadorDeRutasScheduler.java`, `PlanificadorDeRutasTest.java`. Referencia: Pendiente de commit (cambios en staged).
- **47** — Dos callbacks concurrentes eligen el mismo chofer → Causa raíz: `PlanificadorRutasService.asignarChoferes` leía choferes disponibles y asignaba al azar sin exclusión mutua, permitiendo que callbacks simultáneos eligieran al mismo chofer y colisionaran en base de datos. Solución: se sincronizó `asignarChoferes` (`synchronized`) y se excluyen los choferes que ya se encuentren asignados a camiones en rutas activas (`PROGRAMADA` / `EN_CURSO`). Archivos: `PlanificadorRutasService.java`, `PlanificadorRutasServiceAsignarChoferesTest.java`. Referencia: Pendiente de commit (cambios en staged).
- **26** — Replanificar crea rutas duplicadas para los mismos ítems, cada noche y en cada manual → Causa raíz: `PlanificadorDeRutasScheduler.iniciarPlanificacionAutomatica` obtenía todos los ítems `PENDIENTE` sin excluir los que ya habían sido asignados a rutas `PROGRAMADA` o `EN_CURSO`. Solución: se inyectó `RepositorioRutas` y se filtran los ítems activos antes de enviar los lotes al proveedor externo. Archivos: `PlanificadorDeRutasScheduler.java`, `PlanificadorDeRutasSchedulerTest.java`. Referencia: Pendiente de commit (cambios en staged).
- **50** — Terminar una ruta revierte todas las entregas confirmadas a `PENDIENTE` y publica falso `REINGRESO_DEPOSITO` → Causa raíz: `terminarRuta` iteraba sobre los ítems embebidos en el documento `Ruta` que retenían el estado `EN_CAMINO` sin sincronizar con `repoItemEntrega`, y `EntregaService.actualizarEstado` no sincronizaba el documento de la ruta al registrar entregas. Solución: se agregó sincronización de paradas en el documento de ruta en `EntregaService.actualizarEstado`, se adelantó `obtenerRutaActualizada` al inicio del bucle en `RutaService.terminarRuta` y se eliminó la recarga redundante tras los deletes. Archivos: `EntregaService.java`, `RutaService.java`, `RutaServiceTerminarRutaTest.java`. Referencia: Pendiente de commit (cambios en staged).
- **2** — El estado de la ruta se persistía por efecto colateral de OSIV → Corregido en la migración a MongoDB: se eliminó el método `default` incorrecto en `RepositorioRutas` y `RutaService` persiste explícitamente mediante `repoRutas.save(rutaActual)`.
- **3** — Terminar una ruta borraba un ítem y después tiraba excepción dentro de `if(isPresent())` → Corregido en `610ccb2` / `a6f8296`: se eliminó el `throw` anómalo y la eliminación de entregas terminadas se valida con `existsById`.
- **4** — La condición del camión en `terminarRuta` estaba invertida (`camion.isPresent() throw "Camión no encontrado"`) → Corregido en `610ccb2`: se desvincula y resetea el camión directamente sobre `rutaActual.getCamionAsignado()`.
- **7** — El getter manual de `Parada.getEntidadDestino()` pisaba a Lombok e ignoraba su propio campo → Corregido en `7e1fed5`: se eliminó el getter manual y Lombok provee el getter del campo `entidadDestino`.
- **8** — Confirmar una entrega que no estaba en camino se ignoraba en silencio respondiendo 200 → Corregido: `GestorPublicacionEventos.publicarEntregaConfirmada` valida `item.getEstado() == EstadoEntrega.EN_CAMINO` y lanza `IllegalStateException`.
- **9** — Reportar una entrega fallida no validaba el estado previo → Corregido: `GestorPublicacionEventos.publicarEntregaFallida` valida `item.getEstado() == EstadoEntrega.EN_CAMINO` y lanza `IllegalStateException`.
- **18** — `return null` en el catch del planificador manual hacía fallar a Spring con 500 genérico → Corregido en `PlanificadorDeRutasController`: devuelve `ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)`.
- **27** — `findByChofer` devolvía la primera ruta histórica sin filtrar estado → Corregido: `RepositorioRutas.findByChoferYEstado` filtra por chofer y estado (`PROGRAMADA` / `EN_CURSO`).
- **29** — Un PATCH/PUT sin el campo `disponible` marcaba en ocupado o lanzaba error → Corregido: `CamionService` y `ChoferService` verifican `body.containsKey("disponible")` y los DTOs no pisan valores `null`.
- **30** — `POST /entregas` respondía 201 sin registrar nada cuando `bienes` o `idsDonaciones` venían null → Corregido: `EntregaService.procesarPeticion` lanza `IllegalArgumentException`.
- **32** — `DonacionListener` atrapaba toda `DataIntegrityViolationException` descartando mensajes como duplicados → Corregido: se removió el catch genérico en `DonacionListener` y los fallos van a la DLQ.
- **35** — Cada mensaje insertaba país, provincia y ciudad nuevos aunque la entidad ya existiera → Corregido: se consulta `repoEntidades.findById` primero y se busca por `findByNombreIgnoreCase`.
- **36** — El callback devolvía 500 para IDs inexistentes por envolver en `RuntimeException` de base de datos → Corregido: `PlanificadorRutasService` relanza `IllegalArgumentException` y `IllegalStateException` hacia el controller (códigos 400 y 422).
- **38** — Los repositorios de país, provincia y ciudad declaraban ID `UUID` y la entidad `Long` → Corregido en la migración a MongoDB: las entidades y repositorios unificaron el tipo de clave a `String`.
- **41** — Los repositorios convivían repartidos en dos esquemas de paquetes distintos → Corregido: se consolidaron todas las interfaces de repositorio en `models/repositories/`.
- **1** — El módulo no tenía un solo test → llegaron los primeros, con los casos de N instancias.
- **5** — Los tres `DELETE` borraban y después tiraban "no encontrado", devolviendo 404 tras borrado exitoso → se invirtió el chequeo.
- **6** — `ItemEntrega.eventos` usaba `mappedBy="id"` apuntando a la PK del evento → relación corregida.
- **11** — Una misma instancia de evento se metía en la lista de todos los ítems de la ruta → un solo `INICIO_RUTA` por ruta.
- **12** — El polling pedía `id > desdeId - 1` y reventaba con null → corregido inicialmente, aunque reapareció como regresión (ver punto 51).
- **13** — `findByIdGreaterThanOrderByIdAsc` era un `default` con `findAll()` → derived query real.
- **14** — `GET /entregas/{id}` devolvía la entidad JPA → devuelve `BienDTO`.
- **15** — Una donación con N bienes creaba N direcciones huérfanas → catálogo resuelto por mensaje.
- **16** — `procesarPeticion` validaba solo `request` y el resto reventaba a mitad de escritura → validaciones previas al guardado.
- **19** — Validación de justificación de entrega fallida → verificado sin inversión.
- **21** — Una redelivery reseteaba el ítem a `PENDIENTE` → guarda `existsById`.
- **22** — Concurrencia sin `@Version` → se incorporó `@Version`.
- **23** — Orden con N consumidores → reparto por hash implementado.
- **24** — Compose no escalable por `container_name` fijo → puerto efímero y `SERVER_PORT`.
- **25** — Mensajes agotados se reencolaban indefinidamente → rechazo sin requeue hacia DLQ.
- **28** — Publicación de evento dentro de transacción → movida a `afterCommit`.
- **33** — Credenciales de BD hardcodeadas → `.env` desacoplado.
- **39** — Cron corría a las 02:00 UTC en lugar de ART → `zone` explícita en `@Scheduled`.
